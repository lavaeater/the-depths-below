package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor
import ktx.math.vec3
import net.mgsx.gltf.scene3d.lights.PointLightEx

class PointLightComponent: Component, Pool.Poolable {
    val offset = vec3()
    var offsetDirection: Direction = Direction.Forward
    var pointLightEx: PointLightEx = PointLightEx()
    override fun reset() {
        offsetDirection = Direction.Forward
        offset.setZero()
        pointLightEx = PointLightEx()
    }

    companion object {
        val mapper = mapperFor<PointLightComponent>()
        fun has(entity: Entity): Boolean {
            return mapper.has(entity)
        }
        fun get(entity: Entity): PointLightComponent {
            return mapper.get(entity)
        }
    }
}
