package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import net.mgsx.gltf.scene3d.scene.SceneManager


fun <E> MutableSet<E>.addIndexed(element: E): Int {
    this.add(element)
    return this.indexOf(element)
}

class RenderSystem3d(
    private val sceneManager: SceneManager
) : EntitySystem() {
    override fun update(deltaTime: Float) {
        renderScenes(deltaTime)
    }

    private fun renderScenes(deltaTime: Float) {
        sceneManager.update(deltaTime)
        sceneManager.render()
    }
}
