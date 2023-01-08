package depth.injection

import com.badlogic.gdx.math.Vector3
import eater.injection.InjectionContext.Companion.inject
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager

fun createSubMarine() {
    inject<SceneManager>().addScene(Scene(assets().submarine.scene)
        .apply {
            this.modelInstance.transform.setToWorld(
                Vector3.Zero, Vector3.X, Vector3.Y
            )
        }, true
    )
}
