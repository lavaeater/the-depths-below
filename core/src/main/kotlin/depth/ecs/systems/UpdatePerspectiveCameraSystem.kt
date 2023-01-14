package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import depth.ecs.components.BulletRigidBody
import depth.ecs.components.Camera3dFollowComponent
import ktx.ashley.allOf
import ktx.math.minus
import ktx.math.plus
import ktx.math.vec3

class UpdatePerspectiveCameraSystem(
    private val perspectiveCamera: PerspectiveCamera
) :
    IteratingSystem(
        allOf(
            BulletRigidBody::class,
            Camera3dFollowComponent::class
        ).get()
    ) {

    val position = vec3()
    val orientaton = vec3()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val tf = BulletRigidBody.get(entity)
        tf.rigidBody.worldTransform.getTranslation(position)
        orientaton.set(tf.rigidBody.orientation.y,tf.rigidBody.orientation.x, tf.rigidBody.orientation.z)
        val offset = Camera3dFollowComponent.get(entity).offset
        val backwards = orientaton.cpy().rotate(Vector3.Y, 180f).scl(offset.x).add(0f, offset.y, 0f)
        val target = position + backwards
        perspectiveCamera.position.lerp(target, 0.1f)
//        val cameraDirection = (position - perspectiveCamera.position).nor()
        val cameraDirection = ((position + orientaton.cpy().scl(10f)) - perspectiveCamera.position).nor()

        perspectiveCamera.direction.lerp(cameraDirection, 0.1f)
        perspectiveCamera.update()
    }
}
