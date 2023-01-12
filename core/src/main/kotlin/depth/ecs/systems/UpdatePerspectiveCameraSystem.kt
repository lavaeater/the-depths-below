package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.PerspectiveCamera
import depth.ecs.components.Camera3dFollowComponent
import depth.ecs.components.Transform3d
import ktx.ashley.allOf
import ktx.math.plus

class UpdatePerspectiveCameraSystem(private val perspectiveCamera: PerspectiveCamera) : IteratingSystem(allOf(Transform3d::class, Camera3dFollowComponent::class).get()){
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val position = Transform3d.get(entity).position
        val offset = Camera3dFollowComponent.get(entity).offset
        val target = position + offset
        perspectiveCamera.position.lerp(target, 0.25f)
        perspectiveCamera.lookAt(position)
    }
}
