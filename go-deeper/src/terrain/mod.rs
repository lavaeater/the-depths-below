//! Procedural underwater terrain: a seamless simplex scalar field turned into blocky
//! marching-cubes geometry the submarine flies through.
//!
//! Ported from the Kotlin/libGDX game's `depth.marching` / `depth.voxel` packages. See
//! `docs/plan.md` for the porting plan and phase breakdown.

use crate::*;
use std::collections::HashMap;

pub mod field;
pub mod marching;
pub mod tables;

use field::{CarvedField, ScalarField, SeaField};

// ---- Terrain constants (mirroring `DeepGameSettings` / `Joiser` / the Context wiring) ----

/// World size of a single marching-cube cell (`DeepGameSettings.sideLength`).
pub const SIDE_LENGTH: f32 = 25.0;
/// Number of cells along each axis of a chunk (`MarchingCubeBuilder(... , 10, ...)`).
pub const POINTS_PER_CHUNK: i32 = 10;
/// A grid point is solid when its field value is below this (`isoValue < 0.55`).
pub const SOLID_THRESHOLD: f32 = 0.45;
/// Simplex seed (`ModuleBasisFunction.seed = 14`).
pub const NOISE_SEED: u32 = 14;
/// Domain scaling (`ModuleScaleDomain` scale 4.0).
pub const NOISE_SCALE: f64 = 4.0;
/// Default wrap period (`generateChunks(5)` -> `5 * 2 * 10`).
pub const NOISE_PERIOD: f64 = 100.0;

// ---- Sea-floor FBM density (ported in spirit from Sebastian Lague's `NoiseDensity.compute`) ----

/// Number of FBM octaves.
pub const SEA_OCTAVES: usize = 5;
/// Frequency multiplier between octaves.
pub const SEA_LACUNARITY: f64 = 2.0;
/// Amplitude multiplier between octaves.
pub const SEA_PERSISTENCE: f32 = 0.5;
/// Base noise frequency (per cell).
pub const SEA_FREQUENCY: f64 = 0.03;
/// How strongly the noise carves terrain out of the water column.
pub const SEA_NOISE_WEIGHT: f32 = 9.0;
/// Erosion-like per-octave weighting (Sebastian's `weightMultiplier`).
pub const SEA_WEIGHT_MULTIPLIER: f32 = 2.5;
/// Raises the whole surface — larger = more open water above the sea bed.
pub const SEA_FLOOR_OFFSET: f32 = 6.0;
/// Below this height (cells) the world hardens into a solid sea bed.
pub const SEA_HARD_FLOOR_Y: f32 = -14.0;
/// How strongly the hard floor forces solidity.
pub const SEA_HARD_FLOOR_WEIGHT: f32 = 3.0;

// ---- Rendering options ----

/// Smooth density-interpolated surfaces (Sebastian-style) vs the original blocky midpoint
/// vertices. Flip to `false` for the retro blocky look.
pub const SMOOTH_TERRAIN: bool = true;

/// World-height range mapped onto the color gradient (below -> deep colors, above -> shallow).
pub const COLOR_Y_MIN: f32 = -150.0;
pub const COLOR_Y_MAX: f32 = 150.0;
/// How much surface slope (normal.y) shifts the sampled color, for cliff/flat variation.
pub const COLOR_NORMAL_OFFSET: f32 = 25.0;

// ---- Streaming radii (a simpler, symmetric take on `WorldManager`'s forward-biased box) ----

/// Chunks kept loaded around the sub, horizontally.
pub const LOAD_RADIUS_XZ: i32 = 4;
/// Chunks kept loaded around the sub, vertically (caves are wider than they are tall).
pub const LOAD_RADIUS_Y: i32 = 2;
/// Extra margin before a chunk outside the load radius is despawned (hysteresis).
pub const UNLOAD_MARGIN: i32 = 1;
/// Max chunks built per frame, to spread generation cost (mirrors `buildIfNecessary`).
pub const CHUNKS_PER_FRAME: i32 = 4;

/// World size of one chunk along an axis.
pub const CHUNK_WORLD_SIZE: f32 = POINTS_PER_CHUNK as f32 * SIDE_LENGTH;

/// The chunk carved fully open at game start, so the sub begins in clear water (temporary).
pub const START_CHUNK: IVec3 = IVec3::ZERO;

/// Marks a spawned terrain chunk entity.
#[derive(Component, Debug, Clone, Copy)]
pub struct TerrainChunk(pub IVec3);

/// Holds the scalar field used to generate terrain.
#[derive(Resource)]
pub struct TerrainField(pub Box<dyn ScalarField>);

impl Default for TerrainField {
    fn default() -> Self {
        Self(Box::new(CarvedField::new(SeaField::default(), START_CHUNK)))
    }
}

/// Index of currently-loaded chunk coords -> their entity (empty chunks get a marker entity
/// so they aren't rebuilt every frame).
#[derive(Resource, Default)]
pub struct LoadedChunks(pub HashMap<IVec3, Entity>);

/// Shared material for all terrain chunks.
#[derive(Resource)]
pub struct TerrainMaterial(pub Handle<StandardMaterial>);

pub fn plugin(app: &mut App) {
    app.init_resource::<TerrainField>()
        .init_resource::<LoadedChunks>()
        .add_systems(OnEnter(Screen::Gameplay), setup_terrain)
        .add_systems(Update, stream_chunks.run_if(in_state(Screen::Gameplay)))
        .add_systems(OnExit(Screen::Gameplay), teardown_terrain);
}

/// World position -> the chunk coordinate that contains it.
pub fn world_to_chunk(pos: Vec3) -> IVec3 {
    (pos / CHUNK_WORLD_SIZE).floor().as_ivec3()
}

fn setup_terrain(mut commands: Commands, mut materials: ResMut<Assets<StandardMaterial>>) {
    let material = materials.add(StandardMaterial {
        // White so the per-vertex height gradient (see `marching::mesh_from_positions`) shows.
        base_color: Color::WHITE,
        perceptual_roughness: 0.9,
        // Render walls from both sides so cave interiors (and any wall the sub is next to)
        // are visible — otherwise back-face culling makes them look invisible from inside.
        cull_mode: None,
        double_sided: true,
        ..default()
    });
    commands.insert_resource(TerrainMaterial(material));
}

fn teardown_terrain(mut loaded: ResMut<LoadedChunks>) {
    // Chunk entities are `DespawnOnExit(Gameplay)`; just clear our index.
    loaded.0.clear();
}

/// Spawn chunks in a box around the submarine (nearest first, budget-limited) and despawn
/// chunks that fall outside the keep radius. Ported in spirit from `WorldManager`.
fn stream_chunks(
    field: Res<TerrainField>,
    material: Res<TerrainMaterial>,
    subs: Query<&Transform, With<player::Submarine>>,
    mut loaded: ResMut<LoadedChunks>,
    mut meshes: ResMut<Assets<Mesh>>,
    mut commands: Commands,
) {
    let Ok(sub) = subs.single() else {
        return;
    };
    let center = world_to_chunk(sub.translation);

    // Despawn chunks that drifted outside the keep radius.
    let keep_xz = LOAD_RADIUS_XZ + UNLOAD_MARGIN;
    let keep_y = LOAD_RADIUS_Y + UNLOAD_MARGIN;
    loaded.0.retain(|coord, entity| {
        let d = *coord - center;
        let inside = d.x.abs() <= keep_xz && d.z.abs() <= keep_xz && d.y.abs() <= keep_y;
        if !inside {
            commands.entity(*entity).despawn();
        }
        inside
    });

    // Build missing chunks nearest-first, up to the per-frame budget.
    let mut budget = CHUNKS_PER_FRAME;
    for r in 0..=LOAD_RADIUS_XZ {
        for dx in -r..=r {
            for dz in -r..=r {
                // Only the shell at Chebyshev distance `r` this pass (nearest-first ordering).
                if dx.abs().max(dz.abs()) != r {
                    continue;
                }
                for dy in -LOAD_RADIUS_Y..=LOAD_RADIUS_Y {
                    let coord = center + IVec3::new(dx, dy, dz);
                    if loaded.0.contains_key(&coord) {
                        continue;
                    }

                    let entity = match marching::build_chunk_mesh(
                        field.0.as_ref(),
                        coord,
                        SMOOTH_TERRAIN,
                    ) {
                        Some(mesh) => {
                            let collider = Collider::trimesh_from_mesh(&mesh);
                            let mut e = commands.spawn((
                                DespawnOnExit(Screen::Gameplay),
                                TerrainChunk(coord),
                                Mesh3d(meshes.add(mesh)),
                                MeshMaterial3d(material.0.clone()),
                                Transform::IDENTITY,
                            ));
                            if let Some(collider) = collider {
                                e.insert((RigidBody::Static, collider));
                            }
                            e.id()
                        }
                        // Empty (fully open or fully solid) chunk: marker so we don't retry it.
                        None => commands
                            .spawn((DespawnOnExit(Screen::Gameplay), TerrainChunk(coord)))
                            .id(),
                    };
                    loaded.0.insert(coord, entity);

                    budget -= 1;
                    if budget <= 0 {
                        return;
                    }
                }
            }
        }
    }
}

/// Returns true when a cell is fully open water (all 8 corners empty), so nothing solid
/// passes through it.
fn cell_is_open(field: &dyn ScalarField, cell: IVec3) -> bool {
    marching::CORNER_OFFSET.iter().all(|offset| {
        let c = cell + *offset;
        !field.is_solid(c.x, c.y, c.z)
    })
}

/// Required open clearance (in cells) around a spawn cell so the sub starts in a genuine
/// pocket, not right up against a wall.
const SPAWN_CLEARANCE: i32 = 1;

/// True when `cell` and every cell within [`SPAWN_CLEARANCE`] of it are fully open.
fn region_is_open(field: &dyn ScalarField, cell: IVec3) -> bool {
    for dx in -SPAWN_CLEARANCE..=SPAWN_CLEARANCE {
        for dy in -SPAWN_CLEARANCE..=SPAWN_CLEARANCE {
            for dz in -SPAWN_CLEARANCE..=SPAWN_CLEARANCE {
                if !cell_is_open(field, cell + IVec3::new(dx, dy, dz)) {
                    return false;
                }
            }
        }
    }
    true
}

/// Find an open spawn position near `near` so the submarine never starts inside — or right
/// against — a structure. Scans outward (nearest-first) for a cell with open clearance all
/// around it; falls back to `near` if none is found.
pub fn find_open_spawn(field: &dyn ScalarField, near: Vec3) -> Vec3 {
    const MAX_RADIUS: i32 = 32;
    let start = (near / SIDE_LENGTH).floor().as_ivec3();

    for r in 0..=MAX_RADIUS {
        for dx in -r..=r {
            for dy in -r..=r {
                for dz in -r..=r {
                    // Only the shell at Chebyshev distance `r`.
                    if dx.abs().max(dy.abs()).max(dz.abs()) != r {
                        continue;
                    }
                    let cell = start + IVec3::new(dx, dy, dz);
                    if region_is_open(field, cell) {
                        // Center of the open cell.
                        return (cell.as_vec3() + Vec3::splat(0.5)) * SIDE_LENGTH;
                    }
                }
            }
        }
    }

    near
}
