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
    object Reverse: Direction()
    object Neutral: Direction()
}
sealed class Rotation {
    object YawLeft: Rotation()
    object YawRight: Rotation()
    object PitchUp: Rotation()
    object PitchDown: Rotation()
    object RollLeft: Rotation()
    object RollRight: Rotation()
}

class KeyboardControlComponent: Component, Pool.Poolable {
    val orthogonal = mutableSetOf<Direction>()
    val rotational = mutableSetOf<Rotation>()

    fun has(direction: Direction) : Boolean {
        return orthogonal.contains(direction)
    }

    fun has(rotation: Rotation): Boolean {
        return rotational.contains(rotation)
    }

    fun add(direction: Direction) {
        orthogonal.add(direction)
    }
    fun remove(direction: Direction) {
        orthogonal.remove(direction)
    }

    fun add(rotation: Rotation) {
        rotational.add(rotation)
    }
    fun remove(rotation: Rotation) {
        rotational.remove(rotation)
    }

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
