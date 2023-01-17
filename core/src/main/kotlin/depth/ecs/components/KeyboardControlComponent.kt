package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor

class DirectionThing {
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

    fun clear() {
        rotational.clear()
        orthogonal.clear()
    }
}

class KeyboardControlComponent: Component, Pool.Poolable {

    val directionThing = DirectionThing()
    fun has(direction: Direction) : Boolean {
        return directionThing.has(direction)
    }

    fun has(rotation: Rotation): Boolean {
        return directionThing.has(rotation)
    }

    fun add(direction: Direction) {
        directionThing.add(direction)
    }
    fun remove(direction: Direction) {
        directionThing.remove(direction)
    }

    fun add(rotation: Rotation) {
        directionThing.add(rotation)
    }
    fun remove(rotation: Rotation) {
        directionThing.remove(rotation)
    }

    override fun reset() {
        directionThing.clear()
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
