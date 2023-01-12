package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import depth.ecs.components.KeyboardControlComponent
import depth.ecs.components.SceneComponent
import eater.input.command
import ktx.ashley.allOf

class SubmarineControlSystem:IteratingSystem(allOf(SceneComponent::class, KeyboardControlComponent::class).get()) {
    val controlMap = command()
    override fun processEntity(entity: Entity, deltaTime: Float) {
    }
}
