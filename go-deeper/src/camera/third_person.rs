use super::*;
use bevy_third_person_camera::ThirdPersonCamera;

pub fn plugin(app: &mut App) {
    app.add_systems(OnEnter(Screen::Gameplay), add_tpv_cam)
        .add_systems(OnExit(Screen::Gameplay), rm_tpv_cam);
}

fn add_tpv_cam(
    cfg: Res<Config>,
    mut commands: Commands,
    mut camera: Query<Entity, With<SceneCamera>>,
) -> Result {
    let Ok(cam) = camera.single_mut() else {
        return Ok(());
    };

    // The submarine uses a fixed chase camera (`player::submarine::follow_camera`) instead of
    // the mouse-orbit `ThirdPersonCamera`, so we only set the projection here.
    commands.entity(cam).insert(Projection::from(PerspectiveProjection {
        fov: cfg.player.fov.to_radians(),
        ..Default::default()
    }));

    Ok(())
}

fn rm_tpv_cam(mut commands: Commands, mut camera: Query<Entity, With<ThirdPersonCamera>>) {
    if let Ok(camera) = camera.single_mut() {
        commands
            .entity(camera)
            .remove::<RigidBody>()
            .remove::<ThirdPersonCamera>();
    }
}
