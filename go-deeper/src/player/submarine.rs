//! Submarine flight: impulse-based 6-DOF control, ported (with a control-scheme tweak) from
//! the Kotlin game's `depth/ecs/systems/SubmarineControlSystem.kt`.
//!
//! The submarine is a dynamic avian rigid body with gravity disabled (it's neutrally
//! buoyant) and pitch/roll locked, so it stays level and only yaws. Held keys add velocity
//! along the body's local axes each fixed step, exactly like the original applied central
//! impulses; linear/angular damping gives the "moving through water" drag that bleeds the
//! velocity back off when keys are released.
//!
//! Controls: `W/S` forward/reverse, `A/D` yaw left/right, `←/→` strafe, `↑/↓` ascend/descend.
//! A fixed chase camera ([`follow_camera`]) sits behind and above the sub and looks slightly
//! ahead of it, so the view turns with the sub and needs no manual control.

use super::*;
use avian3d::prelude::*;

/// Marks the player entity as the controllable submarine.
#[derive(Component, Debug, Default, Clone, Copy)]
pub struct Submarine;

/// Linear acceleration (world units/s²) applied while a thrust key is held.
pub const THRUST_ACCEL: f32 = 60.0;
/// Yaw acceleration (rad/s²) applied while a turn key is held.
pub const YAW_ACCEL: f32 = 3.0;
/// Drag on linear motion; terminal speed ≈ `THRUST_ACCEL / LINEAR_DAMPING`.
pub const LINEAR_DAMPING: f32 = 1.2;
/// Drag on yaw; terminal turn rate ≈ `YAW_ACCEL / ANGULAR_DAMPING`.
pub const ANGULAR_DAMPING: f32 = 4.0;

/// Chase-camera distance behind the sub.
pub const CAM_DISTANCE: f32 = 22.0;
/// Chase-camera height above the sub.
pub const CAM_HEIGHT: f32 = 10.0;
/// How far ahead of the sub the camera looks (keeps the sub low in frame, more view ahead).
pub const CAM_LOOK_AHEAD: f32 = 8.0;
/// Camera position smoothing rate (higher = snappier).
pub const CAM_SMOOTH: f32 = 8.0;

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
        RigidBody::Dynamic,
        GravityScale(0.0),
        LinearDamping(LINEAR_DAMPING),
        AngularDamping(ANGULAR_DAMPING),
        // Keep the sub level: only yaw (rotation about Y) is free.
        LockedAxes::new().lock_rotation_x().lock_rotation_z(),
        LinearVelocity::default(),
        AngularVelocity::default(),
    )
}

/// Held-key polling: `W/S` forward/reverse, `←/→` strafe, `↑/↓` ascend/descend, `A/D` yaw.
fn submarine_control(
    time: Res<Time>,
    keys: Res<ButtonInput<KeyCode>>,
    mut subs: Query<(&Transform, &mut LinearVelocity, &mut AngularVelocity), With<Submarine>>,
) {
    let dt = time.delta_secs();

    for (transform, mut lin_vel, mut ang_vel) in &mut subs {
        let forward = *transform.forward();
        let right = *transform.right();
        let up = *transform.up();

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
        if keys.pressed(KeyCode::ArrowUp) {
            thrust += up;
        }
        if keys.pressed(KeyCode::ArrowDown) {
            thrust -= up;
        }
        if thrust != Vec3::ZERO {
            lin_vel.0 += thrust.normalize() * THRUST_ACCEL * dt;
        }

        let mut yaw = 0.0;
        if keys.pressed(KeyCode::KeyA) {
            yaw += 1.0;
        }
        if keys.pressed(KeyCode::KeyD) {
            yaw -= 1.0;
        }
        ang_vel.0.y += yaw * YAW_ACCEL * dt;
    }
}

/// Fixed chase camera: keeps the `SceneCamera` behind and above the submarine, looking
/// slightly ahead of it, so the view follows the sub's heading without manual control.
fn follow_camera(
    time: Res<Time>,
    subs: Query<&Transform, With<Submarine>>,
    mut cam: Query<&mut Transform, (With<SceneCamera>, Without<Submarine>)>,
) {
    let Ok(sub) = subs.single() else {
        return;
    };
    let Ok(mut cam) = cam.single_mut() else {
        return;
    };

    let forward = *sub.forward();
    let desired = sub.translation - forward * CAM_DISTANCE + Vec3::Y * CAM_HEIGHT;

    // Exponential smoothing so the camera eases through turns instead of snapping.
    let t = (time.delta_secs() * CAM_SMOOTH).clamp(0.0, 1.0);
    cam.translation = cam.translation.lerp(desired, t);
    cam.look_at(sub.translation + forward * CAM_LOOK_AHEAD, Vec3::Y);
}
