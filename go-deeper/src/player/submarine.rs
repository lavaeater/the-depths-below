//! Submarine flight, ported (with a control-scheme tweak) from the Kotlin game's
//! `SubmarineControlSystem.kt` and Sebastian Lague's `Submarine.cs` (which adds pitch).
//!
//! Orientation (yaw + pitch) is tracked in [`SubmarineOrientation`] and written straight to
//! the body's rotation each frame — physics never spins the sub, so there's no roll drift and
//! collisions can't tumble it. Thrust adds velocity along the body's local axes; linear
//! damping gives the "moving through water" drag.
//!
//! Controls: `W/S` forward/reverse, `A/D` yaw left/right, `←/→` strafe, `↑/↓` pitch up/down.
//! A fixed chase camera ([`follow_camera`]) sits behind and above the sub and looks slightly
//! ahead of it, so the view turns with the sub and needs no manual control.

use super::*;

/// Marks the player entity as the controllable submarine.
#[derive(Component, Debug, Default, Clone, Copy)]
pub struct Submarine;

/// Accumulated heading of the submarine, in radians. We drive the body's rotation from this
/// directly rather than through the physics solver.
#[derive(Component, Debug, Default, Clone, Copy)]
pub struct SubmarineOrientation {
    pub yaw: f32,
    pub pitch: f32,
}

/// Linear acceleration (world units/s²) applied while a thrust key is held.
pub const THRUST_ACCEL: f32 = 60.0;
/// Yaw turn rate (rad/s) while a turn key is held.
pub const YAW_RATE: f32 = 1.1;
/// Pitch turn rate (rad/s) while a pitch key is held.
pub const PITCH_RATE: f32 = 0.9;
/// Maximum nose up/down angle (radians, ~75°).
pub const PITCH_LIMIT: f32 = 1.3;
/// Drag on linear motion; terminal speed ≈ `THRUST_ACCEL / LINEAR_DAMPING`.
pub const LINEAR_DAMPING: f32 = 1.2;

/// Chase-camera distance behind the sub.
pub const CAM_DISTANCE: f32 = 22.0;
/// Chase-camera height above the sub.
pub const CAM_HEIGHT: f32 = 10.0;
/// How far ahead of the sub the camera looks (keeps the sub low in frame, more view ahead).
pub const CAM_LOOK_AHEAD: f32 = 8.0;
/// Camera position smoothing rate (higher = snappier).
pub const CAM_SMOOTH: f32 = 8.0;
/// Gap kept between the camera and any wall it's pulled up against.
pub const CAM_WALL_BUFFER: f32 = 3.0;
/// Camera never comes closer to the sub than this (avoids clipping into the sub on tight pull-ins).
pub const CAM_MIN_DISTANCE: f32 = 4.0;

pub fn plugin(app: &mut App) {
    app.add_systems(
        FixedUpdate,
        submarine_control
            .run_if(in_state(Screen::Gameplay))
            .in_set(AppSystems::UserInput),
    )
    .add_systems(
        PostUpdate,
        follow_camera
            .run_if(in_state(Screen::Gameplay))
            .before(TransformSystems::Propagate),
    );
}

/// Physics + control components to make an entity a submarine. Added alongside [`Submarine`].
///
/// `RigidBody::Dynamic` overrides the `Kinematic` that `Player` otherwise requires.
pub fn submarine_physics() -> impl Bundle {
    (
        Submarine,
        SubmarineOrientation::default(),
        RigidBody::Dynamic,
        GravityScale(0.0),
        LinearDamping(LINEAR_DAMPING),
        LinearVelocity::default(),
        AngularVelocity::default(),
    )
}

/// Held-key polling: `W/S` forward/reverse, `←/→` strafe, `↑/↓` pitch up/down, `A/D` yaw.
fn submarine_control(
    time: Res<Time>,
    keys: Res<ButtonInput<KeyCode>>,
    mut subs: Query<
        (
            &mut SubmarineOrientation,
            &mut Rotation,
            &mut LinearVelocity,
            &mut AngularVelocity,
        ),
        With<Submarine>,
    >,
) {
    let dt = time.delta_secs();

    for (mut ori, mut rot, mut lin_vel, mut ang_vel) in &mut subs {
        // --- Orientation: integrate yaw/pitch from input, write it straight to the body. ---
        if keys.pressed(KeyCode::KeyA) {
            ori.yaw += YAW_RATE * dt;
        }
        if keys.pressed(KeyCode::KeyD) {
            ori.yaw -= YAW_RATE * dt;
        }
        if keys.pressed(KeyCode::ArrowUp) {
            ori.pitch += PITCH_RATE * dt;
        }
        if keys.pressed(KeyCode::ArrowDown) {
            ori.pitch -= PITCH_RATE * dt;
        }
        ori.pitch = ori.pitch.clamp(-PITCH_LIMIT, PITCH_LIMIT);

        let orientation =
            Quat::from_axis_angle(Vec3::Y, ori.yaw) * Quat::from_axis_angle(Vec3::X, ori.pitch);
        rot.0 = orientation;
        // Cancel any spin the solver picked up from collisions so orientation stays exact.
        ang_vel.0 = Vec3::ZERO;

        // --- Thrust along the (now pitched) local axes. ---
        let forward = orientation * Vec3::NEG_Z;
        let right = orientation * Vec3::X;
        let mut thrust = Vec3::ZERO;
        if keys.pressed(KeyCode::KeyW) {
            thrust += forward;
        }
        if keys.pressed(KeyCode::KeyS) {
            thrust -= forward;
        }
        if keys.pressed(KeyCode::ArrowRight) {
            thrust += right;
        }
        if keys.pressed(KeyCode::ArrowLeft) {
            thrust -= right;
        }
        if thrust != Vec3::ZERO {
            lin_vel.0 += thrust.normalize() * THRUST_ACCEL * dt;
        }
    }
}

/// Fixed chase camera: keeps the `SceneCamera` behind and above the submarine, looking
/// slightly ahead of it, so the view follows the sub's heading without manual control. A
/// raycast from the sub to the camera pulls it in when terrain is in the way, so we never
/// see through walls.
fn follow_camera(
    time: Res<Time>,
    spatial: SpatialQuery,
    subs: Query<(Entity, &Transform), With<Submarine>>,
    mut cam: Query<&mut Transform, (With<SceneCamera>, Without<Submarine>)>,
) {
    let Ok((sub_entity, sub)) = subs.single() else {
        return;
    };
    let Ok(mut cam) = cam.single_mut() else {
        return;
    };

    let forward = *sub.forward();
    let free = sub.translation - forward * CAM_DISTANCE + Vec3::Y * CAM_HEIGHT;

    // Exponential smoothing so the camera eases through turns instead of snapping.
    let t = (time.delta_secs() * CAM_SMOOTH).clamp(0.0, 1.0);
    let mut target = cam.translation.lerp(free, t);

    // Wall avoidance: if terrain sits between the sub and the (smoothed) camera position,
    // pull the camera in to just short of that wall.
    let pivot = sub.translation;
    let offset = target - pivot;
    let dist = offset.length();
    if let Ok(dir) = Dir3::new(offset) {
        let filter = SpatialQueryFilter::default().with_excluded_entities([sub_entity]);
        if let Some(hit) = spatial.cast_ray(pivot, dir, dist, true, &filter) {
            let pulled = (hit.distance - CAM_WALL_BUFFER).max(CAM_MIN_DISTANCE);
            target = pivot + *dir * pulled;
        }
    }

    cam.translation = target;
    cam.look_at(sub.translation + forward * CAM_LOOK_AHEAD, Vec3::Y);
}
