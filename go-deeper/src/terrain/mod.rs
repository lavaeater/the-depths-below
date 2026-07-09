//! Procedural underwater terrain: a seamless simplex scalar field turned into blocky
//! marching-cubes geometry the submarine flies through.
//!
//! Ported from the Kotlin/libGDX game's `depth.marching` / `depth.voxel` packages. See
//! `docs/plan.md` for the porting plan and phase breakdown.

use crate::*;

pub mod field;
pub mod marching;
pub mod tables;

use field::{JoiseField, ScalarField};

// ---- Terrain constants (mirroring `DeepGameSettings` / `Joiser` / the Context wiring) ----

/// World size of a single marching-cube cell (`DeepGameSettings.sideLength`).
pub const SIDE_LENGTH: f32 = 25.0;
/// Number of cells along each axis of a chunk (`MarchingCubeBuilder(... , 10, ...)`).
pub const POINTS_PER_CHUNK: i32 = 10;
/// A grid point is solid when its field value is below this (`isoValue < 0.55`).
pub const SOLID_THRESHOLD: f32 = 0.55;
/// Simplex seed (`ModuleBasisFunction.seed = 14`).
pub const NOISE_SEED: u32 = 14;
/// Domain scaling (`ModuleScaleDomain` scale 4.0).
pub const NOISE_SCALE: f64 = 4.0;
/// Default wrap period (`generateChunks(5)` -> `5 * 2 * 10`).
pub const NOISE_PERIOD: f64 = 100.0;

/// Marks a spawned terrain chunk entity.
#[derive(Component, Debug, Clone, Copy)]
pub struct TerrainChunk(pub IVec3);

/// Holds the scalar field used to generate terrain.
#[derive(Resource)]
pub struct TerrainField(pub Box<dyn ScalarField>);

impl Default for TerrainField {
    fn default() -> Self {
        Self(Box::new(JoiseField::default()))
    }
}

pub fn plugin(app: &mut App) {
    app.init_resource::<TerrainField>()
        .add_systems(OnEnter(Screen::Gameplay), spawn_terrain_spike);
}

/// Phase 0/1 spike: generate a small cube of chunks around the origin so we can eyeball the
/// field + mesher before wiring up streaming and physics.
fn spawn_terrain_spike(
    field: Res<TerrainField>,
    mut commands: Commands,
    mut meshes: ResMut<Assets<Mesh>>,
    mut materials: ResMut<Assets<StandardMaterial>>,
) {
    let material = materials.add(StandardMaterial {
        base_color: Color::srgb(0.3, 0.55, 0.4),
        perceptual_roughness: 0.9,
        ..default()
    });

    let mut spawned = 0;
    for cx in -1..=1 {
        for cy in -1..=1 {
            for cz in -1..=1 {
                let coord = IVec3::new(cx, cy, cz);
                let Some(mesh) = marching::build_chunk_mesh(field.0.as_ref(), coord) else {
                    continue;
                };
                // Static trimesh collider so the submarine can't fly through the rock.
                let collider = Collider::trimesh_from_mesh(&mesh);
                let mut entity = commands.spawn((
                    DespawnOnExit(Screen::Gameplay),
                    TerrainChunk(coord),
                    Mesh3d(meshes.add(mesh)),
                    MeshMaterial3d(material.clone()),
                    Transform::IDENTITY,
                ));
                if let Some(collider) = collider {
                    entity.insert((RigidBody::Static, collider));
                }
                spawned += 1;
            }
        }
    }

    info!("spawned {spawned} terrain chunk(s)");
}
