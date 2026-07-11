# Sebastian Lague's Marching Cubes — feature gap analysis & plan

We vendored [SebLague/Marching-Cubes](https://github.com/SebLague/Marching-Cubes) (the
"Coding Adventure: Marching Cubes" project) as a submodule at `Marching-Cubes/`. It's the
same underwater-submarine-through-caves idea as ours, but more advanced. This doc lists what
it does that **go-deeper** doesn't yet, and a plan to close the gaps in our Bevy/Rust code.

Source of truth for their side: `Marching-Cubes/Assets/Scripts/**`.
Ours: `go-deeper/src/terrain/**`, `src/player/submarine.rs`, `src/scene/underwater.rs`.

---

## 1. How their project is built

- **Everything runs on the GPU.** Density is computed by a compute shader
  (`Compute/NoiseDensity.compute` → `Includes/Density.compute`) into a `points` buffer of
  `float4(pos, density)`; then `Compute/MarchingCubes.compute` marches that buffer and
  *appends* triangles to a GPU buffer, which is read back into a Unity `Mesh`
  (`MeshGenerator.UpdateChunkMesh`). This is why they can afford `numPointsPerAxis` up to 100.
- **Smooth iso-surface.** `interpolateVerts` places each vertex along a cube edge at
  `t = (isoLevel - v1.w) / (v2.w - v1.w)` — interpolated by density, so surfaces are smooth.
- **Rich FBM density with terrain shaping** (`NoiseDensity.compute`): multi-octave ridged
  simplex (`1-abs(n)`, squared, `weightMultiplier` erosion), plus a **floor**:
  `finalVal = -(pos.y + floorOffset) + noise*noiseWeight + (pos.y % params.x)*params.y`,
  a `hardFloor`/`hardFloorWeight` sea bed, and an optional `closeEdges` world seal.
- **Height/normal-based coloring** (`ColourGenerator.cs` + `Shaders/Terrain.shader` /
  `SeaWorld.shader`): a `Gradient` baked to a 1-D `ramp` texture, sampled in the surface
  shader by normalized world height `smoothstep(-boundsY/2, boundsY/2, worldPos.y +
  worldNormal.y*normalOffsetWeight)`. `SeaWorld` repeats the ramp into depth strata.
- **Streaming with pooling + frustum culling** (`MeshGenerator.InitVisibleChunks`):
  spherical view distance, recycles chunk `GameObject`s from a queue, and only builds chunks
  that pass `GeometryUtility.TestPlanesAABB` (camera frustum).
- **Animated submarine** (`Submarine/Submarine.cs`): yaw **and pitch**, smoothed velocity,
  spinning `propeller`, deflecting `rudderYaw`/`rudderPitch`, prop-blur material alpha.
- **Chase camera** (`Submarine/CamFollow.cs`): `SmoothDamp` position at a local-space offset
  + look-ahead + slerped rotation. (No wall-avoidance — ours already beats it here.)

---

## 2. What we have vs. what they have

| Feature | Sebastian | go-deeper (now) |
| --- | --- | --- |
| Marching cubes tables | ✅ GPU | ✅ CPU (`tables.rs`) |
| Surface vertex placement | ✅ **smooth** (density interp) | ⚠️ **blocky** (fixed midpoint `lerp(0.5)`) |
| Density function | ✅ **FBM** ridged, multi-octave | ⚠️ single-octave 4D simplex (tiling trick) |
| Sea floor / terrain shaping | ✅ floor + hard floor + strata | ❌ none (isotropic caves, no "up/down") |
| Coloring | ✅ height/normal gradient ramp | ❌ one flat green material |
| Voxel resolution / chunk | 30–100 pts/axis | 10 pts/axis (coarse) |
| Generation location | ✅ GPU compute | CPU (single-threaded, on main thread) |
| Streaming | sphere + pooling + **frustum cull** | box radius, spawn/despawn (`stream_chunks`) |
| Colliders | ✅ MeshCollider | ✅ avian trimesh |
| Submarine DOF | yaw **+ pitch** | yaw only |
| Submarine animation | ✅ propeller + rudders | ❌ static model |
| Chase camera | ✅ smooth | ✅ smooth **+ wall-avoidance** |
| Fog / underwater | fog=bg color, view-dist tie | ✅ squared fog, tinted (`underwater.rs`) |

**The four gaps that most change the feel:** smooth surfaces, a sea floor, height-based
color, and an animated/pitchable submarine. Resolution and GPU generation are performance
enablers behind those.

---

## 3. Gap-closing plan (Bevy/Rust)

Ordered by value-for-effort. Each step is independently shippable and testable.

### A. Sea floor + FBM density — *"a bottom" and richer shapes* (highest impact)
Our world is currently isotropic blobs with no up/down. Add terrain shaping so there's a sea
bed below and open water above, with overhangs.
- In `terrain/field.rs`, add a `SeaField` `ScalarField` returning a **signed density** whose
  sign flips at the iso threshold, mirroring `NoiseDensity.compute`:
  `-(y + floor_offset) + fbm(pos)*noise_weight (+ strata term) (+ hard_floor)`.
- FBM: octave loop with `lacunarity`/`persistence`, ridged `(1-|n|)²`, per-octave random
  offsets from a seeded RNG (drop the circle-projection tiling — world coords are already
  infinite). Keep `NOISE_SEED`.
- Swap `TerrainField::default()` to wrap `SeaField` in `CarvedField` (start cavern still
  works). Expose the shaping params as consts next to the others.
- Effort: **M**. Touches `field.rs` only. No mesher change required to see it.

### B. Smooth surface interpolation — *smooth caves instead of blocky*
Their smoothness comes from interpolating edge vertices by density value, not midpoints.
- Extend `ScalarField` with `value()` already present; in `marching.rs::build_chunk_positions`
  sample the **float value** at all 8 corners (cache per cell), build the mask by comparing to
  the threshold, and replace `from.lerp(to, 0.5)` with
  `t = (ISO - va) / (vb - va); from.lerp(to, t)` (guard `vb==va`).
- Note this abandons the original Kotlin game's deliberately blocky aesthetic — that's the
  point of matching Sebastian. Keep the midpoint path behind a flag if we want both looks.
- Effort: **S–M**. Localized to `marching.rs`. Pairs naturally with A.

### C. Height/normal-based vertex coloring — *"more colors"*
- Simplest path (no custom shader): compute a per-vertex color in `mesh_from_positions` by
  sampling a Rust-defined gradient (a `Vec<(f32, Color)>` ramp) at
  `h = smoothstep(min_y, max_y, world_y + normal_y*offset)`, and write
  `Mesh::ATTRIBUTE_COLOR`. Set the terrain `StandardMaterial { base_color: WHITE, .. }` so
  vertex colors show. Depth strata = wrap `h` like `SeaWorld.shader`.
- Nicer path (later): a Bevy `MaterialExtension`/WGSL that samples a 1-D ramp texture in the
  fragment shader (true per-pixel gradient, like their `ramp`). The template already wires
  custom materials (`scene/cosmic_sphere.rs`) as a reference.
- Effort: **S** (vertex colors) / **M** (shader). Touches `marching.rs` + `terrain/mod.rs`.

### D. Submarine: pitch + animated parts
- Add **pitch** (their `Vertical` axis): a second angular DOF. Currently we lock rotation
  X+Z (`submarine_physics`) and only yaw; allow controlled pitch (e.g. free the X lock and
  drive it, or apply a target orientation). Map to `↑/↓` (moving strafe-up/down elsewhere) or
  new keys — decide the scheme with the user.
- Animate `propeller` (spin about local axis, speed ∝ throttle) and `rudderYaw`/`rudderPitch`
  (deflect with turn input) **if** `submarine3.gltf` has those named nodes — otherwise skip
  or swap to their sub model. Query the child nodes by name post-spawn and rotate them.
- Effort: **S** (pitch) / **M** (rig-dependent animation).

### E. Resolution + async generation (performance enabler for A–C)
Smooth + colored terrain wants more voxels than 10/axis; CPU on the main thread will hitch.
- Bump `POINTS_PER_CHUNK` (e.g. 16–24) and move chunk mesh+collider generation onto
  `AsyncComputeTaskPool` (spawn a task per chunk, poll for completion in `stream_chunks`),
  so the frame never blocks. Keep the nearest-first budget.
- Add **frustum culling** like their `TestPlanesAABB`: skip building chunks whose AABB isn't
  in the camera frustum (needs the camera's `Frustum`, available in Bevy).
- Optional: pool despawned chunk meshes instead of dropping them.
- Effort: **M–L**. Touches `terrain/mod.rs`.

### F. GPU compute generation (stretch — matches their architecture)
Port density + marching to Bevy compute shaders (WGSL) writing to storage buffers, read back
to a `Mesh` (or render directly). Unlocks 30–100 pts/axis in real time and editor-speed
iteration.
- Large effort: needs `bevy_render` compute pipeline, buffer management, and readback
  (colliders still need CPU-side vertices). Only worth it if E isn't enough.
- Effort: **XL**. New module; biggest architectural change.

---

## 4. Suggested order

1. **A (sea floor + FBM)** — biggest change to the world's character; field-only.
2. **B (smooth surfaces)** — pairs with A; small, localized.
3. **C (vertex-color gradient)** — cheap "more colors" win.
4. **E (resolution + async)** — needed before pushing detail further.
5. **D (sub pitch + animation)** — polish.
6. **F (GPU compute)** — only if we want their scale/real-time editing.

A + B + C together get us ~80% of the "looks like Sebastian's" impression for a fraction of
F's cost, and all stay on the CPU pipeline we already have. Recommend doing A–C as one branch,
eyeballing, then deciding whether E/F are worth it.

---

## 5. Notes / decisions to confirm
- **Blocky vs smooth:** B abandons the original Kotlin look. Confirm we want smooth (we can
  keep both behind a flag).
- **Tiling:** A drops the seamless circle-projection field for FBM+offsets. Fine for an
  infinite world; loses exact tiling (which we weren't relying on).
- **Control scheme:** adding pitch (D) needs a key mapping decision.
- **Sub model:** propeller/rudder animation depends on named nodes in `submarine3.gltf`; may
  want to import their sub or author the rig.
```
