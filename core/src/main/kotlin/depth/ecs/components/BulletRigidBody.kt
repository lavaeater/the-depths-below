package depth.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor

class MotionState(transform: Matrix4): btMotionState() {

    var transform: Matrix4

    init {
        this.transform=transform
    }

    override fun getWorldTransform(worldTrans: Matrix4) {
        worldTrans.set(transform)
    }

    override fun setWorldTransform(worldTrans: Matrix4) {
        transform.set(worldTrans)
    }
}


class BulletRigidBody: Component, Pool.Poolable {
    private var _rigidBody: btRigidBody? = null
    var rigidBody: btRigidBody
        get() = _rigidBody!!
        set(value) {
            _rigidBody = value
        }


    override fun reset() {
        _rigidBody = null
    }

    companion object {
        val mapper = mapperFor<BulletRigidBody>()
        fun has(entity: Entity): Boolean {
            return mapper.has(entity)
        }
        fun get(entity: Entity): BulletRigidBody {
            return mapper.get(entity)
        }
    }
}
