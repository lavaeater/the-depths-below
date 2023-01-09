package depth.voxel

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.Disposable
import com.github.tommyettinger.digital.MathTools.norm
import make.some.noise.Noise

abstract class Terrain(
    protected val points: Int,
    protected val size: Float,
    protected val heightMagnitude: Float
) : Disposable {
    open lateinit var modelInstance: ModelInstance
}

open class HeightMapTerrain(points: Int, size: Float, heightMagnitude: Float) : Terrain(points, size, heightMagnitude) {
    private val noise = Noise(12, 1f / 32f, Noise.PERLIN)
    private val heightValues = Array(points) { x ->
        Array(points) { y ->
            norm(-1f, 1f, noise.getValue(x.toFloat() * 10f, y.toFloat() * 10f))
        }
    }.flatten().toFloatArray()


    private val heightField = HeightField(
        true,
        heightValues,
        points,
        points,
        true,
        VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.ColorUnpacked or VertexAttributes.Usage.TextureCoordinates
    ).apply {
        corner00.set(0f, 0f, 0f)
        corner10.set(size, 0f, 0f)
        corner01.set(0f, 0f, size)
        corner11.set(size, 0f, size)
        color00.set(0f, 0f, 1f, 1f)
        color01.set(0f, 1f, 1f, 1f)
        color10.set(1f, 0f, 1f, 1f)
        color11.set(1f, 1f, 1f, 1f)
        magnitude.set(0f, heightMagnitude, 0f)
        update()
    }

    init {
        val mb = ModelBuilder()
        mb.begin()
        mb.part("terrain", heightField.mesh, GL20.GL_TRIANGLES, Material())
        modelInstance = ModelInstance(mb.end())
    }

    override fun dispose() {
        heightField.dispose()
    }

}
