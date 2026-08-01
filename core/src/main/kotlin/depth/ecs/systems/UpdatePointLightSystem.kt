package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.PerspectiveCamera
import depth.ecs.components.Direction
import depth.ecs.components.MotionState
import depth.ecs.components.PointLightComponent
import ktx.ashley.allOf
import ktx.math.plus
import ktx.math.vec3
import net.mgsx.gltf.scene3d.lights.PointLightEx
import net.mgsx.gltf.scene3d.lights.SpotLightEx

class UpdatePointLightSystem:IteratingSystem(allOf(PointLightComponent::class, MotionState::class).get()) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val pointLightComponent = PointLightComponent.get(entity)
        val motionState = MotionState.get(entity)
        val position = motionState.position.cpy()
        val directionVector = when(pointLightComponent.offsetDirection) {
            Direction.Down -> motionState.down.cpy()
            Direction.Forward -> motionState.forward.cpy()
            Direction.Left -> motionState.left.cpy()
            Direction.Neutral -> vec3()
            Direction.Reverse -> motionState.backwards.cpy()
            Direction.Right -> motionState.right.cpy()
            Direction.Up -> motionState.up.cpy()
        }.scl(pointLightComponent.offset)

        pointLightComponent.pointLightEx.setPosition(position + directionVector)

    }
}
