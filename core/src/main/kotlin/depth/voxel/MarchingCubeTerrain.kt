package depth.voxel

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.MathUtils.*
import com.badlogic.gdx.math.Vector3
import com.sudoplay.joise.mapping.IMappingUpdateListener
import com.sudoplay.joise.mapping.Mapping
import com.sudoplay.joise.mapping.MappingMode
import com.sudoplay.joise.mapping.MappingRange
import com.sudoplay.joise.module.ModuleAutoCorrect
import com.sudoplay.joise.module.ModuleBasisFunction
import com.sudoplay.joise.module.ModuleScaleDomain
import depth.injection.assets
import depth.marching.MarchingCubesTables
import ktx.assets.disposeSafely
import ktx.log.info
import ktx.math.minus
import ktx.math.times
import ktx.math.vec3
import net.mgsx.gltf.loaders.shared.geometry.MeshTangentSpaceGenerator
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute


object Joiser {

    val noiser by lazy {
        val basis = ModuleBasisFunction()
        basis.setType(ModuleBasisFunction.BasisType.SIMPLEX)
        basis.seed = 14 // always same values, good

        val correct = ModuleAutoCorrect()
        correct.setSource(basis)
        correct.calculateAll()

        val scaleDomain = ModuleScaleDomain()
        scaleDomain.setSource(correct)
        val scale = 32.0
        scaleDomain.setScaleX(scale)
        scaleDomain.setScaleY(scale)
        scaleDomain.setScaleZ(scale)
        scaleDomain.setScaleU(scale)
        scaleDomain.setScaleW(scale)
        scaleDomain
    }

    fun getValueFor(x: Int, y: Int, z: Int, width: Int = 100, height: Int = 100, depth: Int = 100): Float {
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
        val scale = 1.0
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

data class MarchingTriangle(
    val cubeX: Int,
    val y: Int,
    val z: Int,
    val v1: Vector3 = vec3(),
    val v2: Vector3 = vec3(),
    val v3: Vector3 = vec3(),
    val n1: Vector3 = vec3(),
    val n2: Vector3 = vec3(),
    val n3: Vector3 = vec3()
)

fun getIndex(x: Int, y: Int, z: Int, size: Int): Int {
    return x + (size * (y + size * z))
}

fun getVertIndexProper(x: Int, y: Int, z: Int): Int {
    var index = 0
    if (x == 0 && y == 0 && z == 0) {
        index = 0
    } else if (x == 1 && y == 0 && z == 0) {
        index = 3
    } else if (x == 1 && y == 1 && z == 0) {
        index = 7
    } else if (x == 1 && y == 1 && z == 1) {
        index = 6
    } else if (x == 1 && y == 0 && z == 1) {
        index = 2
    } else if (x == 0 && y == 1 && z == 1) {
        index = 5
    } else if (x == 0 && y == 0 && z == 1) {
        index = 1
    } else if (x == 0 && y == 1 && z == 0) {
        index = 4
    }
    return index
}

fun getVertex(v0: Vector3, index: Int, s: Float): Vector3 {
    return when (index) {
        1 -> vec3(v0.x, v0.y, v0.z + s)
        2 -> vec3(v0.x + s, v0.y, v0.z + s)
        3 -> vec3(v0.x + s, v0.y, v0.z)
        4 -> vec3(v0.x, v0.y + s, v0.z)
        5 -> vec3(v0.x, v0.y + s, v0.z + s)
        6 -> vec3(v0.x + s, v0.y + s, v0.z + s)
        7 -> vec3(v0.x + s, v0.y + s, v0.z)
        else -> {
            v0.cpy()
        }
    }
}

fun generateMarchingCubeTerrain(cubesPerSide: Int, sideLength: Float): MarchingCubeTerrain {
    /*
    1 cube per side  = 1 cube  = 8 points
    2 cubes per side = 8 cubes =
     */
    val v0 = vec3()
    var min = Float.MAX_VALUE
    var max = Float.MIN_VALUE
    val totalPoints = ((cubesPerSide).pow(3) + 1) * 4
    val totalCubes = cubesPerSide.pow(3)
    //val noise = Joiser.get3dNoise(totalPoints)
    val pointsPerSide = 2
    val totalPerSide = cubesPerSide * pointsPerSide
    val vertValues = FloatArray(8)
    val triangles = mutableListOf<Float>() //Can be transformed to floatarray by a badass
    val allVertVals = mutableListOf<Float>()
    var log = false
    for (cX in 0 until cubesPerSide) {
        for (cY in 0 until cubesPerSide) {
            for (cZ in 0 until cubesPerSide) {
                for (x in cX until cX + pointsPerSide) {
                    for (y in cY until cY + pointsPerSide) {
                        for (z in cZ until cZ + pointsPerSide) {
                            val vertIndex = getVertIndexProper(
                                x - (cX),
                                y - (cY),
                                z - (cZ)
                            ) //yes!

//                            getIndex(
//                                x - (cX),
//                                y - (cY),
//                                z - (cZ),
//                                pointsPerSide
//                            ) //yes!
                            val vertVal =
                                Joiser.getValueFor(
                                    x,
                                    y,
                                    z,
                                    totalPerSide,
                                    totalPerSide,
                                    totalPerSide
                                ) * 100000f * (1f + ((totalPerSide - y) / totalPerSide))
                            allVertVals.add(vertVal)
                            vertValues[vertIndex] = vertVal
                        }
                    }
                }
                //The cube is done here. Yay
                var marchingCubeIndex = 0
                for ((index, vertVal) in vertValues.withIndex()) {
                    marchingCubeIndex =
                        if (vertVal < 55000f) marchingCubeIndex or 2.pow(index) else marchingCubeIndex
                }
                // Now get that cube!
                val sidesForTriangles = MarchingCubesTables.TRIANGLE_TABLE[marchingCubeIndex]
                /**
                 * Make it blocky first, because of course blocky
                 */
                v0.set(sideLength * cX, sideLength * cY, sideLength * cZ)
                for (triangleIndex in sidesForTriangles.indices.step(3)) {
                    for (i in 0..2) { //per vertex in this particular triangle
                        val edge = MarchingCubesTables.EDGES[sidesForTriangles[triangleIndex + i]]
                        val from = getVertex(v0, edge.first(), sideLength)
                        val to = getVertex(v0, edge.last(), sideLength)
                        from.lerp(to, 0.5f)
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
    info { "Average: ${allVertVals.average()}" }
    info { "Median: ${allVertVals.sorted()[allVertVals.count() / 2]}" }
    info { "Max: ${allVertVals.max()}" }
    info { "Min: ${allVertVals.min()}" }

    return MarchingCubeTerrain(triangles.toTypedArray().toFloatArray(), 1f)
}


open class MarchingCubeTerrain(private val vertices: FloatArray, size: Float) : Terrain(size) {
    val colors = listOf(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN, Color.CORAL)

    init {
        /*
        or
                VertexAttributes.Usage.Normal.toLong() or
                VertexAttributes.Usage.ColorUnpacked.toLong() or
                VertexAttributes.Usage.TextureCoordinates.toLong()
                 or
                VertexAttributes.Usage.TextureCoordinates.toLong()
         */
        val meshBuilder = MeshBuilder()
//        val attributes = VertexAttributes(
//            VertexAttribute.Position(),
//            VertexAttribute.Normal(),
////            VertexAttribute(VertexAttributes.Usage.Tangent, 4, ShaderProgram.TANGENT_ATTRIBUTE),
//            VertexAttribute.TexCoords(0)
//        )
        meshBuilder.begin(
            VertexAttributes.Usage.Position.toLong() or
                VertexAttributes.Usage.Normal.toLong() or
                VertexAttributes.Usage.TextureCoordinates.toLong(), GL20.GL_TRIANGLES
        )

//        meshBuilder.part("Entire thing", GL20.GL_TRIANGLES)
        val color = Color(0.3f, 0.6f, 0.3f, 1f)
        for (i in vertices.indices.step(9)) {
            val vn1 = vec3(
                vertices[i],
                vertices[i + 1],
                vertices[i + 2]
            )
            val vn2 = vec3(
                vertices[i + 3],
                vertices[i + 4],
                vertices[i + 5]
            )
            val vn3 = vec3(
                vertices[i + 6],
                vertices[i + 7],
                vertices[i + 8]
            )

            val u = vn2 - vn1
            val v = vn3 - vn1


            val normal = vec3().apply {
                x = -(u.y * v.z - u.z * v.y)
                y = -(u.z * v.x - u.x * v.z)
                z = -(u.x * v.y - u.y * v.x)
            }.nor().scl(-1f)

            val v0 = VertexInfo()
                .setPos(
                    vn1
                )
                .setNor(normal)
            val v1 = VertexInfo()
                .setPos(
                    vn2
                )
                .setNor(normal)
            val v2 = VertexInfo()
                .setPos(
                    vn3
                )
                .setNor(normal)
            meshBuilder.triangle(v0, v1, v2)
        }

        val mesh = meshBuilder.end()
        val mb = ModelBuilder()
        mb.begin()
        val material = Material()
//        material.set(
//            PBRColorAttribute.createBaseColorFactor(
//                Color(Color.WHITE).fromHsv(15f, .9f, .8f)
//            )
//        )
        material.set(PBRTextureAttribute.createBaseColorTexture(assets().diffuseTexture))
//        material.set(PBRTextureAttribute.createNormalTexture(assets().normalTexture))
//        material.set(PBRTextureAttribute.createMetallicRoughnessTexture(assets().mrTexture))

        mb.part("terrain", mesh, GL20.GL_TRIANGLES, material)
        val model = mb.end()
//        for (mesh in model.meshes) {
//            MeshTangentSpaceGenerator.computeTangentSpace(mesh, material, false, true)
//        }

        modelInstance = ModelInstance(model)//.apply { transform.setToWorld(Vector3.Zero, Vector3.X, Vector3.Y) }
    }

    override fun dispose() {
    }

}
