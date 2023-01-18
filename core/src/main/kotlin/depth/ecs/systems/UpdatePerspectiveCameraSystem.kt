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
            Camera3dFollowComponent::class
        ).get()
    ) {

    val target = vec3()
    val cameraDirection = vec3()
    val tmpVector = vec3()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val motionState = MotionState.get(entity)

        val position = motionState.position
        val cc = Camera3dFollowComponent.get(entity)
        val offset = cc.offset
        tmpVector
            .set(motionState.backwards)
            .scl(offset.x)
            .add(0f,offset.y, 0f)
        target.set(
            position
                .cpy()
                .add(tmpVector)
        )
        tmpVector.setZero()
        tmpVector
            .set(motionState.forward)
            .scl(10f)
        perspectiveCamera.position.lerp(target, 0.8f)
        cameraDirection.set(
            position
                .cpy()
                .add(tmpVector)
        )

        perspectiveCamera.lookAt(cameraDirection)
        perspectiveCamera.up.set(Vector3.Y)
        perspectiveCamera.update()
    }
}
