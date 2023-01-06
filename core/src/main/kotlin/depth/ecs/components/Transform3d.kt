package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor
import ktx.math.vec3

class Transform3d: Component, Pool.Poolable {
    val position = vec3()
    override fun reset() {
        position.setZero()
    }

    companion object {
        val mapper = mapperFor<Transform3d>()
        fun has(entity: Entity): Boolean {
            return mapper.has(entity)
        }
        fun get(entity: Entity): Transform3d {
            return mapper.get(entity)
        }
    }
}
