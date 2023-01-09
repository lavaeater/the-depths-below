package depth.voxel

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.Disposable
import com.github.tommyettinger.digital.MathTools.norm
import com.sudoplay.joise.mapping.Mapping
import com.sudoplay.joise.mapping.MappingMode
import com.sudoplay.joise.module.ModuleAutoCorrect
import com.sudoplay.joise.module.ModuleBasisFunction
import com.sudoplay.joise.module.ModuleBasisFunction.BasisType
import com.sudoplay.joise.module.ModuleScaleDomain
import ktx.log.info
import make.some.noise.Noise


abstract class Terrain(
    protected val points: Int,
    protected val size: Float,
    protected val heightMagnitude: Float
) : Disposable {
    open lateinit var modelInstance: ModelInstance
}

open class HeightMapTerrain(points: Int, size: Float, heightMagnitude: Float) : Terrain(points, size, heightMagnitude) {
    private val noise = Noise((0..1000).random(), 1f / 4f, Noise.CELLULAR)
    private val heightValues = Array(points) { x ->
        Array(points) { y ->
            0f
        }
    }.flatten().toFloatArray()
    init {
        val basis = ModuleBasisFunction()
        basis.setType(BasisType.SIMPLEX)
        basis.seed = 42

        val correct = ModuleAutoCorrect()
        correct.setSource(basis)
        correct.calculateAll()

        val scaleDomain = ModuleScaleDomain()
        scaleDomain.setSource(correct)
        scaleDomain.setScaleX(4.0)
        scaleDomain.setScaleY(4.0)
        scaleDomain
        Mapping.map3D(MappingMode.SEAMLESS_XYZ, points, points, points,scaleDomain)

    }
    private val otherNoise by lazy {

        val basis = ModuleBasisFunction()
        basis.setType(BasisType.SIMPLEX)
        basis.seed = 42

        val correct = ModuleAutoCorrect()
        correct.setSource(basis)
        correct.calculateAll()

        val scaleDomain = ModuleScaleDomain()
        scaleDomain.setSource(correct)
        scaleDomain.setScaleX(4.0)
        scaleDomain.setScaleY(4.0)
        scaleDomain
    }
    private var minVal = 0f
    private var maxVal = 0f


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
