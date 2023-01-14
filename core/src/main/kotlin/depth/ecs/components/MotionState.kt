package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState
import com.badlogic.gdx.utils.Pool.Poolable
import ktx.ashley.mapperFor
import ktx.math.vec3


/**
 * TODO: Fix the forward problem thingie, it can probably be handled
 * by using the orientation of the quaternion of the bodies.
 */

class MotionState: btMotionState(), Component, Poolable  {

    private var _transform: Matrix4? = null
    var transform: Matrix4
        get() = _transform!!
        set(value) {
            _transform = value
        }

    val position = vec3()


    override fun getWorldTransform(worldTrans: Matrix4) {
        worldTrans.set(transform)
    }

    override fun setWorldTransform(worldTrans: Matrix4) {
        transform.set(worldTrans)
        transform.getTranslation(position)
    }

    override fun reset() {
        _transform = null
        position.setZero()
    }

    companion object {
        val mapper = mapperFor<MotionState>()
        fun has(entity: Entity): Boolean {
            return mapper.has(entity)
        }
        fun get(entity: Entity): MotionState {
            return mapper.get(entity)
        }
    }
}
