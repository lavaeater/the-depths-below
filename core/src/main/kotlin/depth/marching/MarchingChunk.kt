package depth.marching

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import depth.ecs.components.MotionState
import net.mgsx.gltf.scene3d.scene.Scene

class MarchingChunk(val chunkX: Int, val chunkY: Int, val chunkZ: Int,val modelInstance: ModelInstance) {

    val scene = Scene(modelInstance)
    private lateinit var collisionShape: btCollisionShape
    private val motionState = MotionState().apply {
        transform = modelInstance.transform
    }
    lateinit var rigidBody: btRigidBody
    fun initRigidBody() {
        collisionShape = Bullet.obtainStaticNodeShape(modelInstance.model.nodes)
        rigidBody  = btRigidBody(btRigidBody.btRigidBodyConstructionInfo(0f, motionState, collisionShape, Vector3.Zero))
    }

}
