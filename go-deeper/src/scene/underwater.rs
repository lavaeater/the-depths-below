//! Underwater atmosphere (Phase 6): swaps the template's earthlike sky for a deep-water look
//! — a dark blue-green background, dense tinted distance fog (which also hides terrain chunk
//! pop-in), and cooler, dimmer lighting. The submarine's headlight is spawned in
//! `player::spawn_player`.

use super::*;
use bevy::light::AtmosphereEnvironmentMapLight;
use bevy::pbr::{Atmosphere, AtmosphereSettings, DistanceFog, FogFalloff};

/// Deep-water color, used for BOTH the background and the fog so distant terrain dissolves
/// seamlessly into the water instead of ending at a visible edge.
fn water_color() -> Color {
    Color::srgb(0.02, 0.09, 0.13)
}

/// How quickly visibility falls off with distance (squared falloff — see below).
pub const FOG_DENSITY: f32 = 0.004;

pub fn plugin(app: &mut App) {
    app.insert_resource(ClearColor(water_color()))
        .add_systems(OnEnter(Screen::Gameplay), go_underwater);
}

fn go_underwater(
    camera: Single<Entity, With<SceneCamera>>,
    mut sun: Query<&mut DirectionalLight, With<Sun>>,
    mut commands: Commands,
) {
    // Replace the earthlike sky with deep water: remove atmosphere, add dense tinted fog.
    commands
        .entity(*camera)
        .remove::<Atmosphere>()
        .remove::<AtmosphereSettings>()
        .remove::<AtmosphereEnvironmentMapLight>()
        .insert(DistanceFog {
            color: water_color(),
            // Squared falloff keeps nearby water clear but thickens quickly with distance,
            // giving that murky "can't see far underwater" feel and hiding chunk pop-in.
            falloff: FogFalloff::ExponentialSquared {
                density: FOG_DENSITY,
            },
            ..default()
        });

    // Dim, blue-green ambient like light filtering down from the surface.
    commands.insert_resource(GlobalAmbientLight {
        color: Color::srgb(0.4, 0.7, 0.9),
        brightness: 250.0,
        ..default()
    });

    // Soften the harsh daylight sun into a cooler, dimmer glow from above.
    if let Ok(mut sun) = sun.single_mut() {
        sun.illuminance = 3000.0;
        sun.color = Color::srgb(0.6, 0.8, 1.0);
    }
}
