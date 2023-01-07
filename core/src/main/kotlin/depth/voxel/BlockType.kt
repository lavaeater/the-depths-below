package depth.voxel

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.Disposable
import depth.injection.assets

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
            Material(ColorAttribute.createDiffuse(Color.CORAL), TextureAttribute.createDiffuse(assets().coralTexture)),
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

