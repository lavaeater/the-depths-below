package depth.voxel

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import eater.injection.InjectionContext.Companion.inject
import ktx.math.vec3

class Chunk {
    companion object {
        const val SizeX = 100
        const val SizeZ = SizeX
        const val SizeY = SizeX
    }
}

class DeepGameSettings {
    val gameWidth = 120f
    val gameHeight = 80f
    val viewDistance = 50

}

/**
 * Could also be simply air and clouds and flying beings in
 * the sky, you know
 */
sealed class BlockType(val model: Model) : Disposable {

    object Water : BlockType(
        modelBuilder.createBox(
            BlockManager.blockSize,
            BlockManager.blockSize,
            BlockManager.blockSize,
            Material(ColorAttribute.createDiffuse(Color.BLUE)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
    )

    object Coral : BlockType(
        modelBuilder.createBox(
            BlockManager.blockSize,
            BlockManager.blockSize,
            BlockManager.blockSize,
            Material(ColorAttribute.createDiffuse(Color.CORAL)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
    )

    override fun dispose() {
        model.dispose()
    }

    companion object {
        val modelBuilder = ModelBuilder()
    }
}

class Block(val x: Int, val y: Int, val z: Int, val blockType: BlockType) {
    val modelInstance = ModelInstance(blockType.model).apply {
        transform.setToWorld(vec3(x * BlockManager.blockSize, y * BlockManager.blockSize, z * BlockManager.blockSize), Vector3.X, Vector3.Y)
    }
}

class BlockManager(private val gameSettings: DeepGameSettings) {
    val blocks: Array<Block> = Array(sizeAll) { x ->
        Array(sizeAll) { y ->
            Array(sizeAll) { z ->
                if((1..100).random() > 5)
                    Block(x, y, z, BlockType.Water)
                else
                    Block(x, y, z, BlockType.Coral)
            }
        }.flatten().toTypedArray()
    }.flatten().toTypedArray()

    fun getIndex(x: Int, y: Int, z: Int): Int {
        val modx = x % SizeX
        val mody = y % SizeY
        val modz = z % SizeZ

        return (modz * SizeXY) + (modx * SizeY) + mody;
    }

    companion object {
        private val gameSettings by lazy { inject<DeepGameSettings>() }
        val SizeX = (Chunk.SizeX * ((gameSettings.viewDistance * 2) + 1))
        val SizeZ = (Chunk.SizeZ * ((gameSettings.viewDistance * 2) + 1));
        const val SizeY = Chunk.SizeY;
        val SizeXY = SizeX * SizeY;
        val BufferSize = SizeX * SizeY * SizeZ;
        const val sizeAll = 10
        val AddX = SizeY;
        val SubX = -SizeY;
        val AddY = 1;
        val SubY = -1;
        val AddZ = SizeXY;
        val SubZ = -SizeXY;
        val blockSize = 50f
    }
}
