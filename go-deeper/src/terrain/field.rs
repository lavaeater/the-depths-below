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
use std::f64::consts::TAU;

use super::{NOISE_PERIOD, NOISE_SCALE, NOISE_SEED, POINTS_PER_CHUNK, SOLID_THRESHOLD, START_CHUNK};

/// Anything that can answer "is this integer grid point inside solid terrain?".
pub trait ScalarField: Send + Sync {
    /// Raw field value at an integer grid point, normalized to roughly `[0, 1]`.
    fn value(&self, x: i32, y: i32, z: i32) -> f32;

    /// A point is solid when its value is below the iso threshold (matches the original's
    /// `isoValue * 100000 < 55000`, i.e. `< 0.55`).
    fn is_solid(&self, x: i32, y: i32, z: i32) -> bool {
        self.value(x, y, z) < SOLID_THRESHOLD
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

impl Default for CarvedField<JoiseField> {
    fn default() -> Self {
        Self::new(JoiseField::default(), START_CHUNK)
    }
}

impl<F: ScalarField> ScalarField for CarvedField<F> {
    fn value(&self, x: i32, y: i32, z: i32) -> f32 {
        self.base.value(x, y, z)
    }

    fn is_solid(&self, x: i32, y: i32, z: i32) -> bool {
        let chunk = IVec3::new(
            x.div_euclid(POINTS_PER_CHUNK),
            y.div_euclid(POINTS_PER_CHUNK),
            z.div_euclid(POINTS_PER_CHUNK),
        );
        if chunk == self.carved_chunk {
            return false;
        }
        self.base.is_solid(x, y, z)
    }
}
