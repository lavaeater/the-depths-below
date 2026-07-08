package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor

class VisibleComponent: Component, Pool.Poolable {
    override fun reset() {

    }

    companion object {
        val mapper = mapperFor<VisibleComponent>()
        fun has(entity: Entity): Boolean {
            return mapper.has(entity)
        }
        fun get(entity: Entity): VisibleComponent {
            return mapper.get(entity)
        }
    }
}
