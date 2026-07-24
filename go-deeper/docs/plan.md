# Porting *The Depths Below* to Bevy (`go-deeper`)

Goal: reimplement the Kotlin/libGDX game **The Depths Below** on top of the existing
Bevy `bevy_new_3d_rpg` template. The essence of the game is a submarine flying through
an infinite, procedurally-generated 3D cave system built with **marching cubes** over a
**seamlessly-tiling noise field**.

This document is the implementation plan. It maps every meaningful piece of the Kotlin
game to a Bevy equivalent, calls out the algorithm details that must be preserved to get
the "same" terrain, and orders the work into phases that each end in something runnable.

---

## 1. What the Kotlin game actually does

Source of truth lives in `kotlin/core/src/main/kotlin/depth/`. The important pieces:

### 1.1 The scalar field — `marching/Joiser.kt`
- A single global noise object (Joise library) configured as **SIMPLEX** basis →
  `ModuleAutoCorrect` → `ModuleScaleDomain` (scale 4.0 on all axes), fixed `seed = 14`.
- `getValueFor(x, y, z, width, height, depth)` does **not** sample 3D noise directly.
  It maps the `y` and `z` integer coordinates onto **circles in higher dimensions**
  (`cos`/`sin` of `q*2π` and `r*2π`) and samples **6D** noise `noiser.get(nx,ny,nz,nw,nu,nv)`.
  This is what makes the terrain **tile seamlessly** — as `y` (and `z`) wrap around the
  `numberOfPoints` domain, the sample returns to the same point on the circle.
- `numberOfPoints` (default 1000, overwritten by `WorldManager` to
  `size * 2 * builder.numberOfPoints`) is the wrap period of the domain.
- Returns a float roughly in `[0,1]`.

### 1.2 Solid/empty test
In `MarchingCubeBuilder.getOnOffCoord` (the non-box, "unlimited noise" path that the real
game uses via `buildChunk`):
```
isoValue = Joiser.getValueFor(x, y, z)
on = (isoValue * 100000f) < 55000f      // i.e. isoValue < 0.55  → solid
```
So a grid vertex is **solid** when noise < 0.55.

### 1.3 Marching cubes — `marching/MarchingCubeBuilder.buildChunk` + `MarchingCubesTables.kt`
- For each integer cell coordinate `currentCoord` in the chunk, evaluate the 8 corners.
  Corner→offset mapping is `PointCoord.oldVertexIndexToPointCoordinate` (a **non-standard**
  corner numbering — see note below).
- Build an 8-bit `marchingCubeIndex` (bit `i` set when corner `i` is solid).
- Look up `TRIANGLE_TABLE[marchingCubeIndex]` → list of edge indices (multiples of 3).
- For each edge, `EDGES[e]` gives the two corner indices; the emitted vertex is the
  **midpoint** of those two corners (`from.lerp(to, 0.5f)` — no isovalue interpolation,
  hence the characteristic blocky look). `getVertex(base, cornerIdx, sideLength)` gives
  corner world positions; `sideLength = 25` (`DeepGameSettings.kt`).
- Output is a flat `FloatArray` triangle soup (9 floats per triangle).
- `MarchingCubesTables` holds the standard **Paul Bourke** `EDGE_TABLE` / `TRIANGLE_TABLE`
  and a 12-entry `EDGES` array. These can be reused verbatim (they're public-domain and
  identical to every marching-cubes reference implementation).

> **Corner-numbering caveat.** `getVertex` (in `Extensions.kt`) and
> `oldVertexIndexToPointCoordinate` (in `PointCoord.kt`) use a bespoke cube-corner order
> that does *not* match the Bourke tables' canonical order, yet the game looks fine
> because the mapping is internally consistent. To reproduce the terrain **exactly**,
> port `getVertex` + `oldVertexIndexToPointCoordinate` + the tables together, unchanged.
> If we instead adopt a clean canonical implementation, the caves will be *visually
> equivalent* (same noise, same threshold) but not bit-identical. **Recommendation:**
> port faithfully first (de-risks "does it look like the original"), clean up later.

### 1.4 Mesh building — `marching/MarchingCubeTerrain.kt`
- Flat shading: one normal per triangle, computed from the cross product of two edges,
  then **negated** (the original notes the model came out inverted). Watch winding order
  when porting — if faces are inside-out in Bevy, flip the normal/winding.
- Single texture (`assets().diffuseTexture`) applied as base color.

### 1.5 Chunking & streaming — `marching/WorldManager.kt` + `MarchingChunk.kt`
- World is a grid of chunks; each chunk is `numberOfPoints³` cells of size `sideLength`.
- `generateChunks(size)` pre-builds a cube of chunks and sets the noise wrap period.
- `expandTheWorld(motionState)`: when the submarine crosses into a new chunk or changes
  heading, recompute a set of chunk coords to show (a box biased **forward**:
  `xExtent=4, yExtent=4, zExtent=8`, forward-only in z) and queue missing chunks to build.
- `buildIfNecessary()` builds **one** queued chunk per call (nearest first) to spread cost
  across frames; visibility is toggled rather than rebuilding.
- Optional static collision bodies per chunk (`initRigidBody`, currently disabled in most
  paths).

### 1.6 Submarine control — `ecs/systems/SubmarineControlSystem.kt`
- A Bullet rigid body. Held keys accumulate a `Direction`/`Rotation` set on a component;
  each frame apply **central impulses** along the body's local axes and **torque impulses**
  for yaw.
- `forceFactor = 10`, `torqueFactor = 0.1`.
- Keybindings (`kotlin/keybindings.md`): `W/S` forward/reverse, `A/D` strafe,
  `↑/↓` ascend/descend, `←/→` yaw. (Plus debug keys for the interactive marching-cube
  inspector, which we do **not** need to port.)

### 1.7 Camera
Third-person follow of the submarine (`Camera3dFollowComponent`,
`UpdatePerspectiveCameraSystem`). The Bevy template already ships
`bevy_third_person_camera`.

---

## 2. What the Bevy template already gives us

From `go-deeper/src` + `Cargo.toml`:
- **avian3d 0.6** physics (rigid bodies, colliders, impulses/torque) — replaces Bullet.
- **bevy_enhanced_input** — replaces the libGDX command/keybinding layer.
- **bevy_third_person_camera** (`third_person` feature, on by default) — replaces the
  follow camera; `ThirdPersonCameraTarget` marker.
- **Screen state machine** (`models/states.rs` → `Screen::{Splash,Title,Gameplay,...}`);
  gameplay entities spawn on `OnEnter(Screen::Gameplay)` and use `DespawnOnExit`.
- **Player scaffolding** (`player/`) — currently an RPG humanoid with a `bevy_ahoy`
  `CharacterController`. We will **repurpose `player/` into the submarine** (dynamic rigid
  body + impulse control) rather than a walking character controller.
- Asset loading (`asset_loading/`), UI, audio, dev tools, scene/skybox — reused as-is.
- `rand`, `ron`, `serde`, `image` crates available.

Nothing in the template does procedural mesh generation or noise — that is the new work.

---

## 3. Key design decision: the noise field

The seamless tiling depends on the 6D circle-projection trick in `Joiser`. Two options:

- **(A — recommended) Port `Joiser` faithfully.** Implement a simplex-noise sampler and
  the exact `getValueFor` projection in Rust. Bevy/`rand` don't ship simplex noise, so add
  the **`noise` crate** (`OpenSimplex`/`Simplex`, supports 4D; we need up to 6D — see
  below) or the **`libnoise`** crate. Joise's 6D `get(nx..nv)` is the sticking point:
  most Rust noise crates cap at 4D. Since the original passes `nv = 0.0` and derives
  `nu`/`nw`/`nz`/`ny` from only two independent angles (`q` from y, `r` from z) plus `nx`
  from x, the sample is effectively **1 (x) + 2 (y-circle) + 2 (z-circle) = 5D with one
  dead axis**. We can fold this into a 4D simplex call `f(nx, y_angle_pt, z_angle_pt)` by
  packing the two circle points, at the cost of not being byte-identical to Joise. A fully
  faithful port needs a **6D simplex** implementation (port Joise's `Simplex` basis, ~a few
  hundred lines, MIT/Public domain) or a generic n-D simplex.
- **(B) Substitute a simpler tiling noise.** Use 3D domain-warped simplex and accept that
  chunk seams may not perfectly match. Simplest, but risks visible seams in an *infinite*
  world — the whole point of the projection is seamlessness. Not recommended.

**Plan of record:** start with (A) using a 4D simplex approximation of the projection to
get caves on screen fast, structured behind a `trait ScalarField { fn value(x,y,z) -> f32 }`
so the sampler can be swapped for a faithful 6D port without touching the mesher. Flag the
"exact vs approximate" fidelity as a follow-up to eyeball against the Kotlin build.

---

## 4. Proposed module layout (new code under `src/terrain/`)

```
src/terrain/
  mod.rs          // plugin: registers resources, chunk streaming systems
  field.rs        // ScalarField trait + Joiser-equivalent noise sampler (seed 14)
  tables.rs       // EDGES, TRIANGLE_TABLE (ported verbatim from MarchingCubesTables.kt)
  marching.rs     // cube corner mapping + build_chunk_mesh(coord) -> Mesh (triangle soup)
  chunk.rs        // Chunk component, ChunkCoord, world<->chunk coord math
  streaming.rs    // WorldManager-equivalent: show/hide + build-one-per-frame around sub
```
Submarine work goes in the existing `src/player/` (repurposed) or a new `src/submarine/`.
Constants (`SIDE_LENGTH=25.0`, `POINTS_PER_CHUNK`, `SOLID_THRESHOLD=0.55`, extents) live in
one place, e.g. `terrain/mod.rs` or `models/`.

---

## 5. Phased implementation

Each phase compiles and runs. Verify visually with `bevy run` (or `make run`).

### Phase 0 — Scaffolding & spike
- Add `noise` (or `libnoise`) to `Cargo.toml`. Create `src/terrain/mod.rs` plugin, wire it
  into `main.rs`'s plugin tuple.
- Port `tables.rs` (copy `EDGES` + `TRIANGLE_TABLE` arrays; they're plain int arrays).
- Spike: at `OnEnter(Screen::Gameplay)`, generate **one** chunk mesh at the origin with a
  trivial field (e.g. a sphere or fixed noise) and spawn it with a `StandardMaterial`.
  Confirms mesh construction + winding/normals before noise correctness matters.
- **Done when:** a blocky marching-cubes blob renders in the gameplay screen.

### Phase 1 — Scalar field
- Implement `ScalarField` trait and the `Joiser`-equivalent sampler in `field.rs`
  (seed 14, scale 4.0, the `getValueFor` projection; start with the 4D approximation from
  §3). Threshold `< 0.55` = solid.
- Swap the spike field for the real one.
- **Done when:** the single chunk shows recognizable cave/coral structure that stays fixed
  for a given seed.

### Phase 2 — Faithful mesher
- Port `getVertex` + `oldVertexIndexToPointCoordinate` corner mapping and the midpoint
  emission exactly (§1.3). Build a Bevy `Mesh` (`PrimitiveTopology::TriangleList`) with
  per-triangle flat normals (§1.4), duplicating vertices per triangle (no sharing — matches
  the original and gives flat shading). Fix winding so faces render outward.
- Apply the terrain texture (`assets/models/texture_06.png` or a new one) as base color.
- **Done when:** geometry matches the Kotlin look (spot-check side by side).

### Phase 3 — Chunk streaming
- `ChunkCoord`, chunk↔world math (`chunk.rs`), matching `sideLength`/`numberOfPoints`.
- Port `WorldManager`: a resource/query tracking loaded chunks; a system that, when the
  submarine changes chunk or heading, computes the forward-biased visible set
  (`xExtent=4, yExtent=4, zExtent=8`, forward-only z) and toggles chunk `Visibility`.
- Build **one** queued chunk per frame (nearest-first) to avoid hitches; consider
  `AsyncComputeTaskPool` for mesh gen if single-frame builds stutter.
- Set the noise wrap period (`Joiser.numberOfPoints` equivalent) from world size so chunks
  tile seamlessly.
- **Done when:** flying forward continuously reveals new, seamless terrain and hides
  chunks behind you.

### Phase 4 — Submarine & controls
- Repurpose `player/`: spawn a dynamic avian `RigidBody` with the submarine mesh
  (`assets/models/player.glb` for now, or a sub model) and a `ThirdPersonCameraTarget`.
- Define `bevy_enhanced_input` actions matching `keybindings.md`
  (`W/S` fwd/rev, `A/D` strafe, `↑/↓` ascend/descend, `←/→` yaw). Held-key state → per-frame
  `apply_impulse` along local axes and `apply_angular_impulse` for yaw
  (`force ≈ 10`, `torque ≈ 0.1`, tune for avian units). Add linear/angular damping so the
  sub feels like it's in water.
- Remove/disable the `bevy_ahoy` walking `CharacterController` path for the sub.
- **Done when:** you can pilot the submarine through the caves with the original bindings.

### Phase 5 — Collision (optional but wanted)
- Generate an avian `Collider::trimesh_from_mesh` per chunk (mirrors Kotlin's optional
  static rigid bodies) so the sub can't pass through walls. Attach/detach with chunk
  visibility to bound collider count. Watch trimesh cost; only collide near chunks.
- **Done when:** the sub collides with terrain instead of clipping through.

### Phase 6 — Atmosphere & polish
- Underwater look: fog/`DistanceFog`, blue-green ambient (template already sets
  `GlobalAmbientLight`), a headlight `SpotLight` on the sub (mirrors
  `UpdatePointLightSystem`/`PointLightComponent`). Skybox/`cosmic_sphere` already present.
- Wire into screens: `Screen::Title` → play → `Screen::Gameplay` spawns terrain + sub.
- Optional: particle trail (`bevy_sprinkles` already in template), engine audio
  (`bevy_seedling`).
- **Done when:** it reads as "underwater submarine exploration," not "grey blob world."

---

## 6. Kotlin → Bevy mapping cheatsheet

| Kotlin / libGDX | Bevy / crate |
|---|---|
| Ashley ECS systems | Bevy systems + `Plugin`s |
| Bullet physics, `btRigidBody` | avian3d `RigidBody`, `Collider`, `apply_impulse` |
| libGDX input + `command{}` DSL | `bevy_enhanced_input` actions/contexts |
| `Camera3dFollowComponent` | `bevy_third_person_camera` `ThirdPersonCameraTarget` |
| Joise 6D simplex (`Joiser`) | `noise`/`libnoise` crate behind `ScalarField` (§3) |
| `MarchingCubesTables` | `terrain/tables.rs` (verbatim) |
| `MarchingCubeBuilder.buildChunk` | `terrain/marching.rs::build_chunk_mesh` |
| `MarchingCubeTerrain` (MeshBuilder) | Bevy `Mesh` + `StandardMaterial` |
| `WorldManager` / `MarchingChunk` | `terrain/streaming.rs` + `Chunk` component |
| `DeepGameSettings.sideLength = 25` | `SIDE_LENGTH` const |
| Screen/lifecycle | `Screen` states, `OnEnter(Gameplay)`, `DespawnOnExit` |

---

## 7. Risks & open questions
- **Noise fidelity (biggest):** 6D Joise vs. Rust crates capped at 4D (§3). Decide whether
  byte-exact terrain matters or "same character" is enough. Default: approximate now, port
  faithful 6D simplex if seams/look demand it.
- **Corner-numbering:** must port `getVertex` + offset map together with the tables to
  avoid subtly wrong geometry (§1.3).
- **Streaming cost:** per-frame trimesh + mesh generation can hitch; use nearest-first
  single-build-per-frame and consider async tasks.
- **avian units:** impulse/torque/damping constants won't transfer 1:1 from Bullet; expect
  to retune (§Phase 4).
- **Winding/normals:** the original negates normals; verify faces aren't inside-out in
  Bevy's coordinate/winding conventions.

---

## 8. Suggested first PR
Phases 0–2 as one branch: a static, correct, textured marching-cubes cave chunk rendered in
the gameplay screen from the real noise field. That proves the hard part (mesher + field)
before touching streaming, physics, and controls.
