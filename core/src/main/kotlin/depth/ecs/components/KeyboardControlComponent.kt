package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor

sealed class Direction {
    object Left: Direction()
    object Right: Direction()
    object Up: Direction()
    object Down: Direction()
    object Forward: Direction()
    object Backward: Direction()
    object Neutral: Direction()
}

class KeyboardControlComponent: Component, Pool.Poolable {
    var left = false
    var right = false
    override fun reset() {

    }

    companion object {
        val mapper = mapperFor<KeyboardControlComponent>()
        fun has(entity: Entity): Boolean {
            return mapper.has(entity)
        }
        fun get(entity: Entity): KeyboardControlComponent {
            return mapper.get(entity)
        }
    }
}
