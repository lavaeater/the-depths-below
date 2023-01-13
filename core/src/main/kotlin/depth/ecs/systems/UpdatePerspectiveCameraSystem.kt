package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import depth.ecs.components.Camera3dFollowComponent
import depth.ecs.components.Transform3d
import ktx.ashley.allOf
import ktx.math.minus
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
        val backwards = tf.forward.cpy().rotate(Vector3.Y, 180f).scl(offset.x).add(0f, offset.y,0f)
        val target = position + backwards
        perspectiveCamera.position.set(target)
        val cameraDirection = ((position + tf.forward.cpy().scl(10f)) - perspectiveCamera.position).nor()


        perspectiveCamera.direction.set(cameraDirection)
        perspectiveCamera.update()
    }
}
