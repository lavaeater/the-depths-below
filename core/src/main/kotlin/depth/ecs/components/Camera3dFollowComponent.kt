package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor
import ktx.math.vec3

class Camera3dFollowComponent:Component, Pool.Poolable {
    val offset = vec3()
    override fun reset() {
        offset.setZero()
    }

    companion object {
        val mapper = mapperFor<Camera3dFollowComponent>()
        fun has(entity: Entity): Boolean {
            return mapper.has(entity)
        }
        fun get(entity: Entity): Camera3dFollowComponent {
            return mapper.get(entity)
        }
    }
}
