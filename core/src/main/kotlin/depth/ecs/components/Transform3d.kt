package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor
import ktx.math.vec3

class Transform3d: Component, Pool.Poolable {
    val position = vec3()
    val up = Vector3.Y.cpy()
    val forward = Vector3.Z.cpy()
    override fun reset() {
        position.setZero()
        up.set(Vector3.Y)
        forward.set(Vector3.Z)
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
