package depth.voxel

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import ktx.math.vec3

class Block(val x: Int, val y: Int, val z: Int, val blockType: BlockType) {
    val modelInstance = ModelInstance(blockType.model).apply {
        transform.setToWorld(
            vec3(x * BlockManager.blockSize, y * BlockManager.blockSize, z * BlockManager.blockSize),
            Vector3.X,
            Vector3.Y
        )
    }
}
