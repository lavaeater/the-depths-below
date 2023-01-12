package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.PerspectiveCamera
import depth.ecs.components.Camera3dFollowComponent
import depth.ecs.components.Transform3d
import ktx.ashley.allOf
import ktx.math.plus
import ktx.math.times

class UpdatePerspectiveCameraSystem(
    private val perspectiveCamera: PerspectiveCamera) :
    IteratingSystem(
        allOf(
            Transform3d::class,
            Camera3dFollowComponent::class).get()){
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val tf = Transform3d.get(entity)
        val position = tf.position
        val offset = Camera3dFollowComponent.get(entity).offset
        val target = position + (offset)// * (tf.forward * -1))
        perspectiveCamera.position.lerp(target, .25f)
        perspectiveCamera.lookAt(position)
        perspectiveCamera.update()
    }
}
