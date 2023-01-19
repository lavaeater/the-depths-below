package depth.marching

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import depth.ecs.components.MotionState
import ktx.math.vec3
import net.mgsx.gltf.scene3d.scene.Scene

data class MarchingChunk(val chunkX: Int, val chunkY: Int, val chunkZ: Int) {
    var visible = true
    val hidden get() = !visible
    var cubes: Int = 10
    var sideLength: Float = 25f
    lateinit var modelInstance: ModelInstance
    val worldSize = (cubes - 1) * sideLength
    val worldX = chunkX * sideLength
    val worldY = chunkY * sideLength
    val worldZ = chunkZ * sideLength
    val worldMinimum = vec3(worldX, worldY, worldZ)
    val worldMaximum = vec3(worldX + worldSize, worldY + worldSize, worldZ + worldSize)
    val worldCenter = vec3(worldX + worldSize / 2f, worldY + worldSize / 2f,worldZ + worldSize / 2f)
    val scene by lazy { Scene(modelInstance) }
    val boundingBox = BoundingBox(worldMinimum, worldMaximum)
    private lateinit var collisionShape: btCollisionShape
    private val motionState by lazy {
        MotionState().apply {
            transform = modelInstance.transform
        }
    }
    lateinit var rigidBody: btRigidBody
    fun initRigidBody() {
        collisionShape = Bullet.obtainStaticNodeShape(modelInstance.model.nodes)
        rigidBody  = btRigidBody(btRigidBody.btRigidBodyConstructionInfo(0f, motionState, collisionShape, Vector3.Zero))
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

}
