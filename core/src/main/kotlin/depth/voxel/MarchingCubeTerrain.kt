package depth.voxel

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils.*
import com.badlogic.gdx.math.Vector3
import com.sudoplay.joise.mapping.IMappingUpdateListener
import com.sudoplay.joise.mapping.Mapping
import com.sudoplay.joise.mapping.MappingMode
import com.sudoplay.joise.mapping.MappingRange
import com.sudoplay.joise.module.ModuleAutoCorrect
import com.sudoplay.joise.module.ModuleBasisFunction
import com.sudoplay.joise.module.ModuleScaleDomain
import depth.marching.MarchingCubesTables
import ktx.math.vec3

object Joiser {

    val noiser by lazy {
        val basis = ModuleBasisFunction()
        basis.setType(ModuleBasisFunction.BasisType.SIMPLEX)
        basis.seed = (1..1000).random().toLong()

        val correct = ModuleAutoCorrect()
        correct.setSource(basis)
        correct.calculateAll()

        val scaleDomain = ModuleScaleDomain()
        scaleDomain.setSource(correct)
        val scale = 8.0
        scaleDomain.setScaleX(scale)
        scaleDomain.setScaleY(scale)
        scaleDomain.setScaleZ(scale)
        scaleDomain.setScaleU(scale)
        scaleDomain.setScaleW(scale)
        scaleDomain
    }

    fun getValueFor(x: Int, y: Int, z: Int, width: Int = 1000, height: Int = 1000, depth: Int = 1000): Float {
        val range = MappingRange.DEFAULT
        val dx = range.map1.x - range.map0.x
        val dy = range.loop1.y - range.loop0.y
        val dz = range.loop1.z - range.loop0.z

        val dy_div_2pi = dy / PI2
        val dz_div_2pi = dz / PI2
        val iw = 1.0 / width.toDouble()
        val ih = 1.0 / height.toDouble()
        val id = 1.0 / depth.toDouble()


        var p = x.toDouble() * iw
        var q = y.toDouble() * ih
        var r = z.toDouble() * id

        q = q * (range.map1.y - range.map0.y) / dy
        r = r * (range.map1.z - range.map0.z) / dz

        val nx = range.map0.x + p * dx
        val ny = range.loop0.y + cos(q.toFloat() * PI2) * dy_div_2pi
        val nz = range.loop0.y + sin(q.toFloat() * PI2) * dy_div_2pi
        val nw = range.loop0.z + cos(r.toFloat() * PI2) * dz_div_2pi
        val nu = range.loop0.z + sin(r.toFloat() * PI2) * dz_div_2pi
        val nv = 0.0
        return noiser.get(nx, ny, nz, nw, nu, nv).toFloat()
    }

    fun get3dNoise(size: Int): FloatArray {
        val floatArray = FloatArray(size.pow(3))
        var minVal = 0f
        var maxval = 0f
        val basis = ModuleBasisFunction()
        basis.setType(ModuleBasisFunction.BasisType.SIMPLEX)
        basis.seed = (1..1000).random().toLong()

        val correct = ModuleAutoCorrect()
        correct.setSource(basis)
        correct.calculateAll()

        val scaleDomain = ModuleScaleDomain()
        scaleDomain.setSource(correct)
        val scale = 8.0
        scaleDomain.setScaleX(scale)
        scaleDomain.setScaleY(scale)
        scaleDomain.setScaleZ(scale)
        scaleDomain.setScaleU(scale)
        scaleDomain.setScaleW(scale)
        Mapping.map3D(
            MappingMode.SEAMLESS_XYZ, size, size, size, scaleDomain, MappingRange.DEFAULT, { x, y, z, value ->
                floatArray[getIndex(x, y, z, size)] = value.toFloat()
            }, IMappingUpdateListener.NULL_LISTENER
        )
        return floatArray
    }
}

fun getIndex(x: Int, y: Int, z: Int, size: Int): Int {
    return x + (size * (y + size * z))
}

fun getVertex(v0: Vector3, index: Int, sideLength: Float): Vector3 {
    return when(index) {
        1 -> vec3(v0.x + sideLength, v0.y, v0.z)
        2 -> vec3(v0.x + sideLength, v0.y, v0.z - sideLength)
        3 -> vec3(v0.x, v0.y, v0.z - sideLength)
        4 -> vec3(v0.x, v0.y + sideLength, v0.z)
        5 -> vec3(v0.x + sideLength, v0.y + sideLength, v0.z)
        6 -> vec3(v0.x + sideLength, v0.y + sideLength, v0.z - sideLength)
        7 -> vec3(v0.x, v0.y + sideLength, v0.z - sideLength)
        else -> { v0.cpy() }
    }
}

fun generateMarchingCubeTerrain(cubesPerSide: Int, sideLength: Float): MarchingCubeTerrain {
    /*
    1 cube per side  = 1 cube  = 8 points
    2 cubes per side = 8 cubes =
     */
    val v0 = vec3()
    val totalPoints = ((cubesPerSide).pow(3) + 1) * 4
    val totalCubes = cubesPerSide.pow(3)
    //val noise = Joiser.get3dNoise(totalPoints)
    val pointsPerSide = 2
    val vertValues = FloatArray(8)
    val triangles = mutableListOf<Float>() //Can be transformed to floatarray by a badass
    for (cX in 0 until cubesPerSide) {
        for (cY in 0 until cubesPerSide) {
            for (cZ in 0 until cubesPerSide) {
                for (x in cX until cX + pointsPerSide) {
                    for (y in cY until cY + pointsPerSide) {
                        for (z in cZ until cZ + pointsPerSide) {
                            val vertIndex = getIndex(
                                x - cX * pointsPerSide,
                                y - cY * pointsPerSide,
                                z - cZ * pointsPerSide,
                                pointsPerSide
                            ) //yes!
                            vertValues[vertIndex] = Joiser.getValueFor(x, y, z)
                        }
                    }
                }
                //The cube is done here. Yay
                var marchingCubeIndex = 0
                for ((index, vertVal) in vertValues.withIndex()) {
                    marchingCubeIndex = if (vertVal > 0.55f) marchingCubeIndex or 2.pow(index) else marchingCubeIndex
                }
                // Now get that cube!
                val sidesForTriangles = MarchingCubesTables.TRIANGLE_TABLE[marchingCubeIndex]
                /**
                 * Make it blocky first, because of course blocky
                 */
                v0.set(sideLength * cX,sideLength * cY, sideLength * cZ)
                for (triangleIndex in sidesForTriangles.indices.step(3)) {
                    for (i in 0..2) { //per vertex in this particular triangle
                        val edge = MarchingCubesTables.EDGES[sidesForTriangles[triangleIndex + i]]
                        val from = getVertex(v0, edge.first(), sideLength)
                        val to = getVertex(v0, edge.last(), sideLength)
                        from.lerp(to,0.5f)
                        triangles.add(from.x)
                        triangles.add(from.y)
                        triangles.add(from.z)
                    }
                }
            }
        }
    }
    /*- Vertex indices
//                        4  ___________________  5
//                          /|                 /|
//                         / |                / |
//                        /  |               /  |
//                   7   /___|______________/6  |
//                      |    |              |   |
//                      |    |              |   |
//                      |  0 |______________|___| 1
//                      |   /               |   /
//                      |  /                |  /
//                      | /                 | /
//                      |/__________________|/
//                     3                     2
     */
    return MarchingCubeTerrain(triangles.toTypedArray().toFloatArray(), 1f)
}


open class MarchingCubeTerrain(private val vertices: FloatArray, size: Float) : Terrain(size) {
    init {
        val meshBuilder = MeshBuilder()
        meshBuilder.begin(
            VertexAttributes.Usage.Position.toLong() or
                VertexAttributes.Usage.Normal.toLong() or
                VertexAttributes.Usage.ColorUnpacked.toLong() or
                VertexAttributes.Usage.TextureCoordinates.toLong(), GL20.GL_TRIANGLES
        )

//        meshBuilder.part("Entire thing", GL20.GL_TRIANGLES)
        for (i in vertices.indices.step(9)) {
            meshBuilder.triangle(
                vec3(
                    vertices[i],
                    vertices[i + 1],
                    vertices[i + 2]
                ),
                vec3(
                    vertices[i + 3],
                    vertices[i + 4],
                    vertices[i + 5]
                ),
                vec3(
                    vertices[i + 6],
                    vertices[i + 7],
                    vertices[i + 8]
                ),
            )
        }

        val mb = ModelBuilder()
        mb.begin()
        mb.part("terrain", meshBuilder.end(), GL20.GL_TRIANGLES, Material())
        modelInstance = ModelInstance(mb.end()).apply { transform.setToWorld(Vector3.Zero, Vector3.X, Vector3.Y) }
    }

    override fun dispose() {
    }

}
