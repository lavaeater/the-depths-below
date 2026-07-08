package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import depth.ecs.components.AddedToRenderableList
import depth.ecs.components.SceneComponent
import depth.ecs.components.VisibleComponent
import depth.marching.WorldManager
import eater.physics.addComponent
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.collections.addAll
import ktx.collections.removeAll
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager


fun <E> MutableSet<E>.addIndexed(element: E): Int {
    this.add(element)
    return this.indexOf(element)
}

class RenderSystem3d(
    private val sceneManager: SceneManager,
    private val worldManager: WorldManager
) : IteratingSystem(allOf(SceneComponent::class, VisibleComponent::class).exclude(AddedToRenderableList::class).get()) {
    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        renderScenes(deltaTime)
    }

    private val scenesToRender = mutableListOf<Scene>()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val sceneComponent = SceneComponent.get(entity)
        if (!sceneComponent.added) {
            scenesToRender.add(sceneComponent.scene)
            sceneComponent.added = true // why not...
            entity.addComponent<AddedToRenderableList>()
        }
    }

    private fun renderScenes(deltaTime: Float) {
        if(worldManager.shouldUpdatedRenderables) {
            val toAddAndToRemove = worldManager.getScenesToRender()
            sceneManager.renderableProviders.removeAll(toAddAndToRemove.first, true)
            sceneManager.renderableProviders.addAll(toAddAndToRemove.second)
        }

        sceneManager.update(deltaTime)
        sceneManager.render()
    }
}
