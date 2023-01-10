package depth.voxel

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.sudoplay.joise.mapping.*
import depth.marchingcubes.pow
import make.some.noise.Noise


abstract class Terrain(
    protected val points: Int,
    protected val size: Float,
    protected val heightMagnitude: Float
) : Disposable {
    open lateinit var modelInstance: ModelInstance
}

open class MarchingCubeTerrain(points: Int, size: Float, heightMagnitude: Float = 1f, cubes: List<List<Vector3>>): Terrain(points, size, heightMagnitude) {
    init {

        val meshBuilder = MeshBuilder()
        meshBuilder.begin(VertexAttributes.Usage.Position.toLong() or
            VertexAttributes.Usage.Normal.toLong() or
            VertexAttributes.Usage.ColorUnpacked.toLong() or
            VertexAttributes.Usage.TextureCoordinates.toLong(), GL20.GL_TRIANGLES)
        for((i, cube) in cubes.withIndex()) {
            meshBuilder.part(i.toString(), GL20.GL_TRIANGLES)
            for(j in cube.indices) {
                if(j % 3 ==0) {
                    val v1 = VertexInfo().setPos(cube[j])
                    val v2 = VertexInfo().setPos(cube[j+1])
                    val v3 = VertexInfo().setPos(cube[j+2])
                    meshBuilder.triangle(v1, v2, v3)
                }
            }
        }

        val mesh = meshBuilder.end()

        val mb = ModelBuilder()
        mb.begin()
        mb.part("terrain", mesh, GL20.GL_TRIANGLES, Material())
        modelInstance = ModelInstance(mb.end())
    }
    override fun dispose() {
        modelInstance
    }

}

open class HeightMapTerrain(points: Int, size: Float, heightMagnitude: Float,
                            heightValues: DoubleArray = DoubleArray(points.pow(2)) { 0.0 }
) : Terrain(points, size, heightMagnitude) {
    private val noise = Noise((0..1000).random(), 1f / 4f, Noise.CELLULAR)

//    init {
//        var minVal = 0f
//        var maxval = 0f
//        val basis = ModuleBasisFunction()
//        basis.setType(BasisType.SIMPLEX)
//        basis.seed = (1..1000).random().toLong()
//
//        val correct = ModuleAutoCorrect()
//        correct.setSource(basis)
//        correct.calculateAll()
//
//        val scaleDomain = ModuleScaleDomain()
//        scaleDomain.setSource(correct)
//        val scale = 8.0
//        scaleDomain.setScaleX(scale)
//        scaleDomain.setScaleY(scale)
//        scaleDomain.setScaleZ(scale)
//        scaleDomain.setScaleU(scale)
//        scaleDomain.setScaleW(scale)
//        Mapping.map2DNoZ(MappingMode.SEAMLESS_XY, points, points, scaleDomain, MappingRange.DEFAULT,
//            IMapping2DWriter { x, y, value ->
//                if(value < minVal) {
//                    minVal = value.toFloat()
//                    info { "Min: $minVal" }
//                }
//                if(value > maxVal) {
//                    maxVal = value.toFloat()
//                    info { "Max: $maxVal" }
//                }
//                heightValues[x][y] = value
//                             }, IMappingUpdateListener.NULL_LISTENER)
//
//    }

    private var minVal = 0f
    private var maxVal = 0f


    private val heightField = HeightField(
        true,
        heightValues,
        points,
        points,
        false,
        VertexAttributes.Usage.Position or
            VertexAttributes.Usage.Normal or
            VertexAttributes.Usage.ColorUnpacked or
            VertexAttributes.Usage.TextureCoordinates
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
