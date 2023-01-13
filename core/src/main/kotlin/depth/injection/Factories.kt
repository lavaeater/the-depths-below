package depth.injection

import com.badlogic.gdx.math.Vector3
import depth.ecs.components.Camera3dFollowComponent
import depth.ecs.components.KeyboardControlComponent
import depth.ecs.components.SceneComponent
import depth.ecs.components.Transform3d
import eater.core.engine
import eater.injection.InjectionContext.Companion.inject
import ktx.ashley.create
import ktx.ashley.entity
import ktx.ashley.with
import ktx.math.vec3
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager

fun createSubMarine() {
    engine().entity {
        with<SceneComponent> {
            scene = Scene(assets().submarine.scene).apply {
                this.modelInstance.transform.setToWorld(
                    vec3(50f, 100f, -50f), Vector3.X, Vector3.Y
                )
            }
            inject<SceneManager>().addScene(scene)
        }
        with<Transform3d>()
        with<Camera3dFollowComponent> {
            offset.set(5f, 2.5f, 0f)
        }
        with<KeyboardControlComponent>()
    }
}
