//! Marching-cubes mesh generation, ported from
//! `kotlin/core/.../depth/marching/MarchingCubeBuilder.buildChunk` and `MarchingCubeTerrain`.
//!
//! For every integer cell in a chunk we evaluate the 8 corners against the [`ScalarField`],
//! build a solidity bitmask, look up the triangles for that configuration, and emit one
//! vertex per triangle-edge at the *midpoint* of the edge (no isovalue interpolation — this
//! is what gives the terrain its characteristic blocky look). Normals are flat (one per
//! triangle) and vertices are duplicated per triangle, matching the original.

use bevy::asset::RenderAssetUsages;
use bevy::mesh::{Mesh, PrimitiveTopology};
use bevy::prelude::*;

use super::field::ScalarField;
use super::tables::{EDGES, TRIANGLE_TABLE};
use super::{POINTS_PER_CHUNK, SIDE_LENGTH};

/// Corner index -> offset from the cell base, in cell units. This is the game's own corner
/// numbering (`PointCoord.oldVertexIndexToPointCoordinate`), kept consistent with [`EDGES`].
pub const CORNER_OFFSET: [IVec3; 8] = [
    IVec3::new(0, 0, 0), // 0
    IVec3::new(0, 0, 1), // 1
    IVec3::new(1, 0, 1), // 2
    IVec3::new(1, 0, 0), // 3
    IVec3::new(0, 1, 0), // 4
    IVec3::new(0, 1, 1), // 5
    IVec3::new(1, 1, 1), // 6
    IVec3::new(1, 1, 0), // 7
];

/// World-space position of a cube corner, given the cell base position and side length.
fn corner_position(base: Vec3, corner: usize) -> Vec3 {
    let o = CORNER_OFFSET[corner];
    base + Vec3::new(o.x as f32, o.y as f32, o.z as f32) * SIDE_LENGTH
}

/// Generate the triangle-soup positions for a single chunk, in global world space.
///
/// `chunk` is the integer chunk coordinate; cells inside it run over
/// `[chunk * POINTS_PER_CHUNK, (chunk + 1) * POINTS_PER_CHUNK)`.
pub fn build_chunk_positions(field: &dyn ScalarField, chunk: IVec3) -> Vec<Vec3> {
    let mut positions = Vec::new();
    let origin = chunk * POINTS_PER_CHUNK;

    for lx in 0..POINTS_PER_CHUNK {
        for ly in 0..POINTS_PER_CHUNK {
            for lz in 0..POINTS_PER_CHUNK {
                let cell = origin + IVec3::new(lx, ly, lz);

                // Build the 8-bit corner-solidity mask.
                let mut mask = 0usize;
                for (i, offset) in CORNER_OFFSET.iter().enumerate() {
                    let c = cell + *offset;
                    if field.is_solid(c.x, c.y, c.z) {
                        mask |= 1 << i;
                    }
                }

                let edges = TRIANGLE_TABLE[mask];
                if edges.is_empty() {
                    continue;
                }

                let base = Vec3::new(cell.x as f32, cell.y as f32, cell.z as f32) * SIDE_LENGTH;
                for edge in edges {
                    let [from_corner, to_corner] = EDGES[*edge];
                    let from = corner_position(base, from_corner);
                    let to = corner_position(base, to_corner);
                    positions.push(from.lerp(to, 0.5));
                }
            }
        }
    }

    positions
}

/// Build a flat-shaded [`Mesh`] from triangle-soup positions. Returns `None` when empty.
pub fn mesh_from_positions(positions: Vec<Vec3>) -> Option<Mesh> {
    if positions.is_empty() {
        return None;
    }

    let mut verts: Vec<[f32; 3]> = Vec::with_capacity(positions.len());
    let mut normals: Vec<[f32; 3]> = Vec::with_capacity(positions.len());
    let mut uvs: Vec<[f32; 2]> = Vec::with_capacity(positions.len());

    for tri in positions.chunks_exact(3) {
        let (a, b, c) = (tri[0], tri[1], tri[2]);
        // Flat normal (the original's double negation cancels out to a plain cross product).
        let normal = (b - a).cross(c - a).normalize_or_zero();
        for &v in tri {
            verts.push([v.x, v.y, v.z]);
            normals.push([normal.x, normal.y, normal.z]);
        }
        // Simple planar UVs so a tiled texture has something to map to.
        uvs.push([a.x, a.z]);
        uvs.push([b.x, b.z]);
        uvs.push([c.x, c.z]);
    }

    let mut mesh = Mesh::new(
        PrimitiveTopology::TriangleList,
        RenderAssetUsages::RENDER_WORLD | RenderAssetUsages::MAIN_WORLD,
    );
    mesh.insert_attribute(Mesh::ATTRIBUTE_POSITION, verts);
    mesh.insert_attribute(Mesh::ATTRIBUTE_NORMAL, normals);
    mesh.insert_attribute(Mesh::ATTRIBUTE_UV_0, uvs);
    Some(mesh)
}

/// Convenience: generate a chunk's mesh directly. Returns `None` for an empty chunk.
pub fn build_chunk_mesh(field: &dyn ScalarField, chunk: IVec3) -> Option<Mesh> {
    mesh_from_positions(build_chunk_positions(field, chunk))
}
