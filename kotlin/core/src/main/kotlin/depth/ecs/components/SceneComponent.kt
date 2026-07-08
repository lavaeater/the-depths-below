package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor
import net.mgsx.gltf.scene3d.scene.Scene

class SceneComponent: Component, Pool.Poolable {
    private var _scene: Scene? = null
    var added = false
    var scene:Scene
        get() {
            return _scene!!
        }
        set(value) {
            _scene = value
        }

    override fun reset() {
        _scene = null
        added = false
    }

    companion object {
        val mapper = mapperFor<SceneComponent>()
        fun has(entity: Entity): Boolean {
            return mapper.has(entity)
        }
        fun get(entity: Entity): SceneComponent {
            return mapper.get(entity)
        }
    }
}
