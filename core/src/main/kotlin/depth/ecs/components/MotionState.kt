package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState
import com.badlogic.gdx.utils.Pool.Poolable
import ktx.ashley.mapperFor
import ktx.math.times
import ktx.math.vec3


/**
 * TODO: Fix the forward problem thingie, it can probably be handled
 * by using the orientation of the quaternion of the bodies.
 */

class MotionState : btMotionState(), Component, Poolable {

    private var _transform: Matrix4? = null
    var transform: Matrix4
        get() = _transform!!
        set(value) {
            _transform = value
        }

    val position = vec3()
    val forward = vec3()
    val up = vec3()
    val right = vec3()
    private val tmpVector = vec3()
    val backwards: Vector3
        get() = tmpVector.set(forward).rotate(Vector3.Y, 180f)
    val down: Vector3
        get() = tmpVector.set(up).rotate(Vector3.X, 180f)
    val left: Vector3
        get() = tmpVector.set(right).rotate(Vector3.Y, 180f)


    override fun getWorldTransform(worldTrans: Matrix4) {
        worldTrans.set(transform)
    }

    override fun setWorldTransform(worldTrans: Matrix4) {
        transform.set(worldTrans)
        transform.getTranslation(position)
        getDirection(worldTrans)
    }

    fun getDirection(transform: Matrix4?) {
        forward.set(Vector3.Z)
        up.set(Vector3.Y)
        right.set(Vector3.X)
        forward.rot(transform).nor()
        up.rot(transform).nor()
        right.rot(transform).nor()
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
