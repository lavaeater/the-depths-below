package depth.marching

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import depth.ecs.components.MotionState
import net.mgsx.gltf.scene3d.scene.Scene

class MarchingChunk(val chunkX: Int, val chunkY: Int, val chunkZ: Int, val size: Int, val cubeSideLength: Float) {
    val modelInstance: ModelInstance = generateMarchingCubeTerrain(size, cubeSideLength, chunkX, chunkY, chunkZ)
    val scene = Scene(modelInstance)
    val collisionShape = Bullet.obtainStaticNodeShape(modelInstance.model.nodes)
    val motionState = MotionState().apply {
        transform = modelInstance.transform
    }
    val rigidBody = btRigidBody(btRigidBody.btRigidBodyConstructionInfo(0f, motionState, collisionShape, Vector3.Zero))

}
