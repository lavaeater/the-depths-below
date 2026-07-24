//! Marching-cubes mesh generation, ported from
//! `kotlin/core/.../depth/marching/MarchingCubeBuilder.buildChunk` and `MarchingCubeTerrain`.
//!
//! For every integer cell in a chunk we evaluate the 8 corners against the [`ScalarField`],
//! build a solidity bitmask, look up the triangles for that configuration, and emit one
//! vertex per triangle-edge at the *midpoint* of the edge (no isovalue interpolation — this
//! is what gives the terrain its characteristic blocky look). Normals are flat (one per
//! triangle) and vertices are duplicated per triangle, matching the original.

use bevy::asset::RenderAssetUsages;
use bevy::mesh::{Indices, Mesh, PrimitiveTopology};
use bevy::prelude::*;

use super::field::ScalarField;
use super::tables::{EDGES, TRIANGLE_TABLE};
use super::{
    COLOR_NORMAL_OFFSET, COLOR_Y_MAX, COLOR_Y_MIN, FLIP_WINDING, POINTS_PER_CHUNK, SIDE_LENGTH,
};

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
/// `[chunk * POINTS_PER_CHUNK, (chunk + 1) * POINTS_PER_CHUNK)`. When `smooth`, edge vertices
/// are placed at the density iso-crossing (Sebastian-style); otherwise at the edge midpoint
/// (the original blocky look).
pub fn build_chunk_positions(field: &dyn ScalarField, chunk: IVec3, smooth: bool) -> Vec<Vec3> {
    let mut positions = Vec::new();
    let origin = chunk * POINTS_PER_CHUNK;
    let iso = field.iso();

    // Sample the (expensive) field once per grid point instead of up to 8× per shared corner.
    // The grid spans `POINTS_PER_CHUNK + 1` points along each axis (cells need their far corner).
    let dim = (POINTS_PER_CHUNK + 1) as usize;
    let cache_index = |x: usize, y: usize, z: usize| (x * dim + y) * dim + z;
    let mut cache = vec![0.0f32; dim * dim * dim];
    for gx in 0..dim {
        for gy in 0..dim {
            for gz in 0..dim {
                let p = origin + IVec3::new(gx as i32, gy as i32, gz as i32);
                cache[cache_index(gx, gy, gz)] = field.value(p.x, p.y, p.z);
            }
        }
    }

    for lx in 0..POINTS_PER_CHUNK {
        for ly in 0..POINTS_PER_CHUNK {
            for lz in 0..POINTS_PER_CHUNK {
                let cell = origin + IVec3::new(lx, ly, lz);

                // Read the 8 corner densities from the cache, and build the solidity mask.
                let mut vals = [0.0f32; 8];
                let mut mask = 0usize;
                for (i, offset) in CORNER_OFFSET.iter().enumerate() {
                    let v = cache[cache_index(
                        (lx + offset.x) as usize,
                        (ly + offset.y) as usize,
                        (lz + offset.z) as usize,
                    )];
                    vals[i] = v;
                    if v < iso {
                        mask |= 1 << i;
                    }
                }

                let edges = TRIANGLE_TABLE[mask];
                if edges.is_empty() {
                    continue;
                }

                let base = Vec3::new(cell.x as f32, cell.y as f32, cell.z as f32) * SIDE_LENGTH;
                for edge in edges {
                    let [a, b] = EDGES[*edge];
                    let pa = corner_position(base, a);
                    let pb = corner_position(base, b);
                    let t = if smooth {
                        let d = vals[b] - vals[a];
                        if d.abs() < 1e-6 {
                            0.5
                        } else {
                            ((iso - vals[a]) / d).clamp(0.0, 1.0)
                        }
                    } else {
                        0.5
                    };
                    positions.push(pa.lerp(pb, t));
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
    let mut colors: Vec<[f32; 4]> = Vec::with_capacity(positions.len());

    for raw in positions.chunks_exact(3) {
        // WINDING NOTE: the marching-cubes tables emit triangles wound so their front face
        // points *into* the solid rock. That's invisible when we render both sides
        // (`double_sided`), but with single-sided back-face culling (the `low_spec` preset)
        // the submarine — which sits inside the open water — sees the culled back faces and
        // the caves look inside-out. Reversing the winding (swap 2nd/3rd vertex) makes the
        // front face point into the open water, so interiors render correctly. The normal is
        // derived from the same winding below, so it flips with it and lighting stays right.
        // Toggle `FLIP_WINDING` in `terrain/mod.rs` if a future table change inverts this.
        let tri: [Vec3; 3] = if FLIP_WINDING {
            [raw[0], raw[2], raw[1]]
        } else {
            [raw[0], raw[1], raw[2]]
        };
        let (a, b, c) = (tri[0], tri[1], tri[2]);
        // Flat normal, consistent with the (possibly flipped) winding above.
        let normal = (b - a).cross(c - a).normalize_or_zero();
        for &v in &tri {
            verts.push([v.x, v.y, v.z]);
            normals.push([normal.x, normal.y, normal.z]);
            // Simple planar UVs so a tiled texture has something to map to.
            uvs.push([v.x, v.z]);
            // Height/slope-based color from the terrain gradient.
            colors.push(terrain_color(v.y, normal.y));
        }
    }

    let mut mesh = Mesh::new(
        PrimitiveTopology::TriangleList,
        RenderAssetUsages::RENDER_WORLD | RenderAssetUsages::MAIN_WORLD,
    );
    // Sequential indices for the triangle soup so avian can build a trimesh collider.
    let indices: Vec<u32> = (0..verts.len() as u32).collect();
    mesh.insert_attribute(Mesh::ATTRIBUTE_POSITION, verts);
    mesh.insert_attribute(Mesh::ATTRIBUTE_NORMAL, normals);
    mesh.insert_attribute(Mesh::ATTRIBUTE_UV_0, uvs);
    mesh.insert_attribute(Mesh::ATTRIBUTE_COLOR, colors);
    mesh.insert_indices(Indices::U32(indices));
    Some(mesh)
}

/// Convenience: generate a chunk's mesh directly. Returns `None` for an empty chunk.
pub fn build_chunk_mesh(field: &dyn ScalarField, chunk: IVec3, smooth: bool) -> Option<Mesh> {
    mesh_from_positions(build_chunk_positions(field, chunk, smooth))
}

/// Linear-RGBA color for a terrain vertex, sampled from a depth gradient by world height
/// (nudged by surface slope, like Sebastian's `normalOffsetWeight`).
fn terrain_color(world_y: f32, normal_y: f32) -> [f32; 4] {
    let h = smoothstep(
        COLOR_Y_MIN,
        COLOR_Y_MAX,
        world_y + normal_y * COLOR_NORMAL_OFFSET,
    );
    let [r, g, b] = sample_ramp(h);
    [r, g, b, 1.0]
}

fn smoothstep(edge0: f32, edge1: f32, x: f32) -> f32 {
    let t = ((x - edge0) / (edge1 - edge0)).clamp(0.0, 1.0);
    t * t * (3.0 - 2.0 * t)
}

/// Deep-to-shallow color ramp (linear RGB). Deep water blue -> teal -> rocky green -> sandy.
fn sample_ramp(h: f32) -> [f32; 3] {
    const STOPS: [(f32, [f32; 3]); 5] = [
        (0.00, [0.01, 0.03, 0.09]), // deep dark blue
        (0.35, [0.03, 0.12, 0.22]), // blue
        (0.60, [0.05, 0.28, 0.26]), // teal
        (0.82, [0.16, 0.32, 0.14]), // mossy rock
        (1.00, [0.55, 0.48, 0.30]), // sandy shallows
    ];
    let h = h.clamp(0.0, 1.0);
    let mut i = 0;
    while i + 1 < STOPS.len() && h > STOPS[i + 1].0 {
        i += 1;
    }
    let (t0, c0) = STOPS[i];
    let (t1, c1) = STOPS[(i + 1).min(STOPS.len() - 1)];
    let f = if (t1 - t0).abs() < 1e-6 {
        0.0
    } else {
        ((h - t0) / (t1 - t0)).clamp(0.0, 1.0)
    };
    [
        c0[0] + (c1[0] - c0[0]) * f,
        c0[1] + (c1[1] - c0[1]) * f,
        c0[2] + (c1[2] - c0[2]) * f,
    ]
}
