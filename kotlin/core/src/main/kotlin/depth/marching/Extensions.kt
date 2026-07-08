package depth.marching

import com.badlogic.gdx.math.Vector3
import depth.voxel.pow
import ktx.log.info
import ktx.math.vec3

/**    6_____7
 *   / |     |
 * 2___|_3   |
 * |   4_____5
 * |  /   | /
 * 0/____1
 *
 *
 * y     z
 * |    /
 * |  /
 * |/______x
 *
 *
 *
 *
 */

fun getVertIndexProper(x: Int, y: Int, z: Int): Int {
    var index = -1
    if (x == 0 && y == 0 && z == 0) {
        index = 0
    } else if (x == 1 && y == 0 && z == 0) {
        index = 2
    } else if (x == 1 && y == 1 && z == 0) {
        index = 6
    } else if (x == 1 && y == 1 && z == 1) {
        index = 7
    } else if (x == 1 && y == 0 && z == 1) {
        index = 3
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
