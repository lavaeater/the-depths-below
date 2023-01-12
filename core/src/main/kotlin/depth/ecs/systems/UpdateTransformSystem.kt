package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import depth.ecs.components.SceneComponent
import depth.ecs.components.Transform3d
import ktx.ashley.allOf

class UpdateTransformSystem: IteratingSystem(allOf(SceneComponent::class, Transform3d::class).get()) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        SceneComponent.get(entity).scene.modelInstance.transform.getTranslation(Transform3d.get(entity).position)
    }
}
