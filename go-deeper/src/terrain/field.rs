//! The scalar field that decides which points in space are solid rock and which are open
//! water. Ported from `kotlin/core/.../depth/marching/Joiser.kt`.
//!
//! The original samples 6D simplex noise, projecting the `y` and `z` integer coordinates
//! onto circles in higher dimensions so the field tiles seamlessly as those coordinates
//! wrap around [`JoiseField::period`]. The Rust `noise` crate's simplex tops out at 4D, so
//! this is a faithful-in-spirit approximation: `x` maps linearly, `y` maps to a full circle
//! (2 dims), and `z` contributes a single projected coordinate (the original's `nv` was
//! always 0 and `nu` is dropped here). Swap in a true 6D simplex later if seams show —
//! everything downstream only talks to the [`ScalarField`] trait.

use bevy::math::IVec3;
use noise::{NoiseFn, Simplex};
use rand::{Rng, SeedableRng, rngs::StdRng};
use std::f64::consts::TAU;

use super::{
    NOISE_PERIOD, NOISE_SCALE, NOISE_SEED, POINTS_PER_CHUNK, SEA_FLOOR_OFFSET, SEA_FREQUENCY,
    SEA_HARD_FLOOR_WEIGHT, SEA_HARD_FLOOR_Y, SEA_LACUNARITY, SEA_NOISE_WEIGHT, SEA_OCTAVES,
    SEA_PERSISTENCE, SEA_WEIGHT_MULTIPLIER, SOLID_THRESHOLD,
};

/// Anything that can answer "is this integer grid point inside solid terrain?".
pub trait ScalarField: Send + Sync {
    /// Raw field value at an integer grid point. A point is solid when this is below
    /// [`ScalarField::iso`]; the surface is placed where it crosses that threshold.
    fn value(&self, x: i32, y: i32, z: i32) -> f32;

    /// The iso threshold that separates solid from open.
    fn iso(&self) -> f32 {
        SOLID_THRESHOLD
    }

    fn is_solid(&self, x: i32, y: i32, z: i32) -> bool {
        self.value(x, y, z) < self.iso()
    }
}

/// The seamless simplex field used by the real game.
pub struct JoiseField {
    noise: Simplex,
    /// Wrap period of the domain (`Joiser.numberOfPoints`). Coordinates that differ by a
    /// multiple of this map to (nearly) the same noise sample, giving a tiling world.
    period: f64,
    scale: f64,
}

impl JoiseField {
    pub fn new(period: f64) -> Self {
        Self {
            noise: Simplex::new(NOISE_SEED),
            period,
            scale: NOISE_SCALE,
        }
    }
}

impl Default for JoiseField {
    fn default() -> Self {
        Self::new(NOISE_PERIOD)
    }
}

impl ScalarField for JoiseField {
    fn value(&self, x: i32, y: i32, z: i32) -> f32 {
        // MappingRange.DEFAULT in Joise spans [-1, 1] on every axis.
        let (dx, dy, dz) = (2.0_f64, 2.0_f64, 2.0_f64);
        let dy_div_2pi = dy / TAU;
        let dz_div_2pi = dz / TAU;

        let p = x as f64 / self.period;
        // The original scales q and r by (map1 - map0) / d, which is 1.0 for the default range.
        let q = y as f64 / self.period;
        let r = z as f64 / self.period;

        // x -> linear position; y -> circle in (ny, nz); z -> projected coordinate nw.
        let nx = -1.0 + p * dx;
        let ny = -1.0 + (q * TAU).cos() * dy_div_2pi;
        let nz = -1.0 + (q * TAU).sin() * dy_div_2pi;
        let nw = -1.0 + (r * TAU).cos() * dz_div_2pi;

        let v = self.noise.get([
            nx * self.scale,
            ny * self.scale,
            nz * self.scale,
            nw * self.scale,
        ]);

        // Simplex returns roughly [-1, 1]; ModuleAutoCorrect normalized to [0, 1].
        ((v + 1.0) * 0.5) as f32
    }
}

/// Sebastian-Lague-style density: multi-octave *ridged* simplex FBM carving terrain out of a
/// water column that hardens into a solid sea bed below. Signed value with `iso == 0`: solid
/// where negative. Ported in spirit from `Marching-Cubes/.../NoiseDensity.compute`.
pub struct SeaField {
    noise: Simplex,
    /// Per-octave random offsets so octaves don't align (Sebastian's `offsets`).
    offsets: Vec<[f64; 3]>,
}

impl SeaField {
    pub fn new(seed: u32) -> Self {
        let mut rng = StdRng::seed_from_u64(seed as u64);
        let offsets = (0..SEA_OCTAVES)
            .map(|_| {
                [
                    (rng.random::<f64>() * 2.0 - 1.0) * 1000.0,
                    (rng.random::<f64>() * 2.0 - 1.0) * 1000.0,
                    (rng.random::<f64>() * 2.0 - 1.0) * 1000.0,
                ]
            })
            .collect();
        Self {
            noise: Simplex::new(seed),
            offsets,
        }
    }

    /// Ridged fractal noise, `>= 0`.
    fn fbm(&self, x: i32, y: i32, z: i32) -> f32 {
        let (px, py, pz) = (x as f64, y as f64, z as f64);
        let mut frequency = SEA_FREQUENCY;
        let mut amplitude = 1.0_f32;
        let mut weight = 1.0_f32;
        let mut noise = 0.0_f32;
        for offset in &self.offsets {
            let n = self.noise.get([
                px * frequency + offset[0],
                py * frequency + offset[1],
                pz * frequency + offset[2],
            ]) as f32;
            // Ridged: peaks where the noise crosses zero.
            let mut v = 1.0 - n.abs();
            v *= v;
            v *= weight;
            weight = (v * SEA_WEIGHT_MULTIPLIER).clamp(0.0, 1.0);
            noise += v * amplitude;
            amplitude *= SEA_PERSISTENCE;
            frequency *= SEA_LACUNARITY;
        }
        noise
    }
}

impl Default for SeaField {
    fn default() -> Self {
        Self::new(NOISE_SEED)
    }
}

impl ScalarField for SeaField {
    fn iso(&self) -> f32 {
        0.0
    }

    fn value(&self, x: i32, y: i32, z: i32) -> f32 {
        let yf = y as f32;
        // Solid below, water above; noise raises the surface into hills/arches.
        let mut v = (yf + SEA_FLOOR_OFFSET) - self.fbm(x, y, z) * SEA_NOISE_WEIGHT;
        // Hard sea bed underneath.
        if yf < SEA_HARD_FLOOR_Y {
            v -= SEA_HARD_FLOOR_WEIGHT;
        }
        v
    }
}

/// Wraps another field and forces one chunk to be fully open water, giving the submarine a
/// clean starting cavern instead of spawning inside rock.
pub struct CarvedField<F: ScalarField> {
    base: F,
    carved_chunk: IVec3,
}

impl<F: ScalarField> CarvedField<F> {
    pub fn new(base: F, carved_chunk: IVec3) -> Self {
        Self { base, carved_chunk }
    }
}

impl<F: ScalarField> ScalarField for CarvedField<F> {
    fn iso(&self) -> f32 {
        self.base.iso()
    }

    fn value(&self, x: i32, y: i32, z: i32) -> f32 {
        let chunk = IVec3::new(
            x.div_euclid(POINTS_PER_CHUNK),
            y.div_euclid(POINTS_PER_CHUNK),
            z.div_euclid(POINTS_PER_CHUNK),
        );
        if chunk == self.carved_chunk {
            // Clearly above iso -> open, and smooth-interpolates cleanly against solid neighbors.
            return self.base.iso() + 1.0;
        }
        self.base.value(x, y, z)
    }
}
