use crate::*;
#[cfg(not(feature = "third_person"))]
use bevy::window::{CursorGrabMode, CursorOptions, PrimaryWindow};
#[cfg(all(feature = "native", not(feature = "low_spec")))]
use bevy::{anti_alias::taa::TemporalAntiAliasing, pbr::ScreenSpaceAmbientOcclusion};
#[cfg(not(feature = "low_spec"))]
use bevy::post_process::bloom::Bloom;
use bevy::{
    camera::Exposure, core_pipeline::tonemapping::Tonemapping, light::ShadowFilteringMethod,
    render::view::Hdr,
};

// mod gamepad_cursor;
mod hdr;
#[cfg(feature = "third_person")]
mod third_person;
#[cfg(feature = "top_down")]
mod top_down;

pub fn plugin(app: &mut App) {
    app.add_plugins(hdr::plugin)
        // app.add_plugins((hdr::plugin, gamepad_cursor::plugin))
        .add_systems(Startup, spawn_camera)
        .add_observer(on_toggle_cam_cursor);

    #[cfg(feature = "third_person")]
    app.add_plugins(third_person::plugin);
    #[cfg(feature = "top_down")]
    app.add_plugins(top_down::plugin);
}

pub fn spawn_camera(mut commands: Commands) {
    commands.spawn((
        SceneCamera,
        IsDefaultUiCamera,
        Camera::default(),
        Camera3d::default(),
        Transform::from_xyz(100., 50., 100.).looking_at(Vec3::ZERO, Vec3::Y),
        (
            Exposure::BLENDER,
            Tonemapping::TonyMcMapface,
            // Bloom is a multi-pass downsample/upsample chain — too costly on iGPUs.
            #[cfg(not(feature = "low_spec"))]
            Bloom::NATURAL,
            Hdr,
        ),
        // performance critical
        (
            Msaa::Off,
            // TAA (motion vectors + history buffer) and SSAO are the heaviest post-fx on
            // integrated GPUs, so `low_spec` drops them.
            #[cfg(all(feature = "native", not(feature = "low_spec")))] // breaks wasm
            TemporalAntiAliasing::default(),
            // Temporal shadow filtering relies on TAA jitter; without TAA use the cheap
            // hardware 2x2 filter instead.
            #[cfg(not(feature = "low_spec"))]
            ShadowFilteringMethod::Temporal,
            #[cfg(feature = "low_spec")]
            ShadowFilteringMethod::Hardware2x2,
            #[cfg(all(feature = "native", not(feature = "low_spec")))] // See https://github.com/bPluginevyengine/bevy/issues/20459
            ScreenSpaceAmbientOcclusion::default(),
        ),
    ));
}

// TODO: make it work in a local split screen
fn on_toggle_cam_cursor(
    _: On<ToggleCamCursor>,
    #[cfg(feature = "third_person")] mut cam: Query<&mut ThirdPersonCamera>,
    #[cfg(not(feature = "third_person"))] mut window_q: Query<
        &mut CursorOptions,
        With<PrimaryWindow>,
    >,
) {
    #[cfg(feature = "third_person")]
    if let Ok(mut cam) = cam.single_mut() {
        cam.cursor_lock_active = !cam.cursor_lock_active;
        return;
    };

    #[cfg(not(feature = "third_person"))]
    if let Ok(mut cursor_options) = window_q.single_mut() {
        cursor_options.visible = !cursor_options.visible;
        debug!("cursor will be visible: {}", cursor_options.visible);
        if cursor_options.visible {
            cursor_options.grab_mode = CursorGrabMode::None;
        } else {
            cursor_options.grab_mode = CursorGrabMode::Locked;
        }
    }
}
