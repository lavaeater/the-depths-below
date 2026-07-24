# Performance — running `go-deeper` on weak GPUs (e.g. Intel UHD)

The game targets a submarine flying through streaming marching-cubes caves. On a strong
discrete GPU the default settings are fine; on an integrated GPU (tested: Lenovo T14,
Intel CometLake-U UHD Graphics) the default look is far too heavy. This doc lists what
costs performance and what we did about it.

The GPU-side settings are gated behind a Cargo feature: **`low_spec`**. CPU-side wins that
don't change the look are always on.

## How to run on a weak machine

```sh
cargo run --release --no-default-features --features native,third_person,low_spec
```

`low_spec` turns off the expensive post-processing, drops one shadow-casting light, thins
the shadow cascades, and renders terrain single-sided. Drop `low_spec` to get the full-fat
look back on capable hardware. The meshlet pipeline is no longer pulled in by `native`; add
`--features meshlet` if you actually want it (see below).

---

## The findings, in rough order of impact

### 1. Build in release (biggest single lever)
Debug Rust runs the marching-cubes + FBM noise 10–30× slower than release. The default
feature set (`dev_native`) also enables `dynamic_linking`, an aggressive file watcher,
`bevy_remote`, the egui inspector and the perf UI — all overhead. Always benchmark with
`--release`. (In release the `tracing` `release_max_level_warn` feature compiles out
debug/trace logs, so the `LogPlugin` level doesn't matter there.)

### 2. Camera post-processing stack — **`low_spec`**
`src/camera/mod.rs` stacked SSAO + TAA + Bloom on an HDR target. On an iGPU these are the
single biggest GPU cost:
- **`ScreenSpaceAmbientOcclusion`** — very expensive on integrated graphics.
- **`TemporalAntiAliasing`** — needs motion vectors + a history buffer.
- **`Bloom::NATURAL`** — a multi-pass downsample/upsample chain.

Under `low_spec` these are removed and `ShadowFilteringMethod` drops from `Temporal`
(which relies on TAA jitter) to the cheap `Hardware2x2`. HDR + tonemapping stay.

### 3. Two shadow-casting directional lights — **`low_spec`**
`src/scene/skybox.rs` spawned a Sun *and* a Moon, both `shadows_enabled: true`. Each
cascaded shadow re-renders the whole streaming terrain every frame. Under `low_spec` the
Moon no longer casts shadows, the Sun's cascade count drops (4 → 2) and its shadow distance
is halved.

### 4. Terrain material overdraw — **`low_spec`**
`setup_terrain` used `cull_mode: None` + `double_sided: true`, doubling fragment work on
all terrain. Under `low_spec` terrain renders single-sided (back-face culled). Marching-cubes triangles
are wound front-face-into-the-rock, which single-sided culling would show inside-out, so
`marching::mesh_from_positions` reverses the winding (const `FLIP_WINDING` in
`terrain/mod.rs`) to point front faces into the open water. Flip that const if a future
tables change inverts it again.

### 5. Corner-value caching (always on, look-preserving)
`marching::build_chunk_positions` recomputed the (expensive, 4-octave FBM) field value at
every one of a cell's 8 corners, so each shared grid point was sampled up to 8×. It now
samples each grid point once into a per-chunk cache — roughly an 8× cut in noise work with
no change to the output.

### 6. Fewer FBM octaves + smaller load radius (always on)
- `SEA_OCTAVES` 5 → 4: FBM cost is linear in octaves.
- `LOAD_RADIUS_XZ` 3 → 2: the fog (`ExponentialSquared`, density 0.004) is fully opaque by
  ~450 world units; radius 2 (≈512 units) already reaches past the fog, so radius 3 was
  generating ~2.3× the chunks for terrain you can't see.

### 7. Frustum culling of chunk generation (always on)
`stream_chunks` no longer builds chunks that fall outside the camera frustum, except for an
always-loaded near ring (`ALWAYS_LOAD_RADIUS`) kept for collision and so turning in place
doesn't reveal empty space. Mirrors Sebastian Lague's `TestPlanesAABB`.

### 8. Colliders only near the submarine (always on)
Every chunk used to get an avian static trimesh collider, so physics broadphase ran against
hundreds of trimeshes each frame. Trimeshes are still *built* off-thread, but the active
`Collider`/`RigidBody` is only attached to chunks within `COLLIDER_RADIUS` of the sub and
removed again when it moves away.

### 9. Meshlet pipeline (compile-time)
`native → enhanced` pulled in `bevy/meshlet` (Nanite-style). We don't spawn any
`MeshletMesh`, so at runtime it's unused, but integrated GPUs support that path poorly. It
now lives in its own opt-in `meshlet` feature (`--features meshlet`); nothing in the game
depends on it, so the default build no longer compiles it in.

---

## Tuning knobs (all in `src/terrain/mod.rs`)

| Const | Meaning | Cheaper = |
| --- | --- | --- |
| `POINTS_PER_CHUNK` | voxels per chunk axis | lower |
| `SIDE_LENGTH` | world size of a voxel | higher (coarser) |
| `SEA_OCTAVES` | FBM octaves | lower |
| `LOAD_RADIUS_XZ` / `LOAD_RADIUS_Y` | chunks kept loaded | lower |
| `ALWAYS_LOAD_RADIUS` | chunks loaded regardless of frustum | lower |
| `COLLIDER_RADIUS` | chunks with active colliders | lower |
| `CHUNKS_PER_FRAME` | generation budget per frame | lower (smoother) |

Measure one change at a time with `bevy_perf_ui` (the `dev` feature).
</content>
</invoke>
