package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import depth.ecs.components.BulletRigidBody
import depth.ecs.components.Camera3dFollowComponent
import depth.ecs.components.MotionState
import ktx.ashley.allOf
import ktx.math.minus
import ktx.math.plus
import ktx.math.vec3

class UpdatePerspectiveCameraSystem(
    private val perspectiveCamera: PerspectiveCamera
) :
    IteratingSystem(
        allOf(
            MotionState::class,
            BulletRigidBody::class,
            Camera3dFollowComponent::class
        ).get()
    ) {

    private val orientaton = vec3()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val bulletRigidBody = BulletRigidBody.get(entity)
        val motionState = MotionState.get(entity)

        val position = motionState.position
        orientaton.set(bulletRigidBody.rigidBody.orientation.y,0f,0f)
        val cc = Camera3dFollowComponent.get(entity)
        val offset = cc.offset
        val backwards = orientaton.cpy().rotate(Vector3.Y, 180f).scl(offset.x).add(0f, offset.y, 0f)
        val target = position + backwards
        perspectiveCamera.position.lerp(target, 0.8f)
//        val cameraDirection = (position - perspectiveCamera.position).nor()
        val cameraDirection = ((position + orientaton.cpy().scl(10f)) - perspectiveCamera.position).nor()

        perspectiveCamera.direction.lerp(cameraDirection, 0.1f)
        perspectiveCamera.update()
    }
}
