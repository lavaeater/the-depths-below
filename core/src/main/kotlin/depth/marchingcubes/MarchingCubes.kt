package depth.marchingcubes

import ktx.math.vec3
import sun.security.ec.point.ProjectivePoint.Mutable

/**
 * Created by Primoz on 11. 07. 2016.
 */
object MarchingCubes {
    fun lerp(vec1: FloatArray, vec2: FloatArray, alpha: Float): FloatArray {
        return floatArrayOf(
            vec1[0] + (vec2[0] - vec1[0]) * alpha,
            vec1[1] + (vec2[1] - vec1[1]) * alpha,
            vec1[2] + (vec2[2] - vec1[2]) * alpha
        )
    }

    fun marchingCubesDouble(
        values: DoubleArray,
        size: Int,
        voxDim: Vector3,
        isoLevel: Double
    ): List<List<DoubleArray>> {
        val result = mutableListOf<MutableList<DoubleArray>>()
        val vertices = mutableListOf<DoubleArray>()
        // Actual position along edge weighted according to function values.
        val vertList = Array(12) { vec3() }


        // Calculate maximal possible axis value (used in vertice normalization)
        val maxX = voxDim[0] * (size - 1)
        val maxY = voxDim[1] * (size - 1)
        val maxZ = voxDim[2] * (size - 1)
        val maxAxisVal = Math.max(maxX, Math.max(maxY, maxZ))

        // Volume iteration
        for (z in 0 until size - 1) {
            for (y in 0 until size - 1) {
                for (x in 0 until size - 1) {

                    // Indices pointing to cube vertices
                    //              pyz  ___________________  pxyz
                    //                  /|                 /|
                    //                 / |                / |
                    //                /  |               /  |
                    //          pz   /___|______________/pxz|
                    //              |    |              |   |
                    //              |    |              |   |
                    //              | py |______________|___| pxy
                    //              |   /               |   /
                    //              |  /                |  /
                    //              | /                 | /
                    //              |/__________________|/
                    //             p                     px
                    val p = x + size * y + size * size * z
                    val px = p + 1
                    val py = p + size
                    val pxy = py + 1
                    val pz = p + size * size
                    val pxz = px + size * size
                    val pyz = py + size * size
                    val pxyz = pxy + size * size

                    //							  X              Y                    Z
                    val position = vec3(x * voxDim[0], y * voxDim[1], z * voxDim[2])

                    // Voxel intensities
                    val value0 = values[p]
                    val value1 = values[px]
                    val value2 = values[py]
                    val value3 = values[pxy]
                    val value4 = values[pz]
                    val value5 = values[pxz]
                    val value6 = values[pyz]
                    val value7 = values[pxyz]

                    // Voxel is active if its intensity is above isolevel
                    var cubeindex = 0
                    if (value0 > isoLevel) cubeindex = cubeindex or 1
                    if (value1 > isoLevel) cubeindex = cubeindex or 2
                    if (value2 > isoLevel) cubeindex = cubeindex or 8
                    if (value3 > isoLevel) cubeindex = cubeindex or 4
                    if (value4 > isoLevel) cubeindex = cubeindex or 16
                    if (value5 > isoLevel) cubeindex = cubeindex or 32
                    if (value6 > isoLevel) cubeindex = cubeindex or 128
                    if (value7 > isoLevel) cubeindex = cubeindex or 64

                    // Fetch the triggered edges
                    val bits = TablesMC.MC_EDGE_TABLE[cubeindex]

                    // If no edge is triggered... skip
                    if (bits == 0) continue

                    // Interpolate the positions based od voxel intensities
                    var mu = 0.5f

                    // bottom of the cube
                    if (bits and 1 != 0) {
                        mu = ((isoLevel - value0) / (value1 - value0)).toFloat()
                        vertList[0].set(position).lerp(
                            vec3(
                                position.x + voxDim.x,
                                position.y, position.z
                            ), mu
                        )
                    }
                    if (bits and 2 != 0) {
                        mu = ((isoLevel - value1) / (value3 - value1)).toFloat()
                        vertList[1].set(
                                position.x + voxDim.x,
                                position.y, position.z)
                            ).lerp(vec3(position.x + voxDim.x, position.y + voxDim.y, position.z), mu)
                    }
                    if (bits and 4 != 0) {
                        mu = ((isoLevel - value2) / (value3 - value2)).toFloat()
                        vertList[2] = lerp(
                            floatArrayOf(
                                position[0], position[1] + voxDim[1],
                                position[2]
                            ), floatArrayOf(position[0] + voxDim[0], position[1] + voxDim[1], position[2]), mu
                        )
                    }
                    if (bits and 8 != 0) {
                        mu = ((isoLevel - value0) / (value2 - value0)).toFloat()
                        vertList[3] = lerp(
                            position, floatArrayOf(
                                position[0], position[1] + voxDim[1],
                                position[2]
                            ), mu
                        )
                    }
                    // top of the cube
                    if (bits and 16 != 0) {
                        mu = ((isoLevel - value4) / (value5 - value4)).toFloat()
                        vertList[4] = lerp(
                            floatArrayOf(position[0], position[1], position[2] + voxDim[2]), floatArrayOf(
                                position[0] + voxDim[0], position[1], position[2] + voxDim[2]
                            ), mu
                        )
                    }
                    if (bits and 32 != 0) {
                        mu = ((isoLevel - value5) / (value7 - value5)).toFloat()
                        vertList[5] = lerp(
                            floatArrayOf(
                                position[0] + voxDim[0],
                                position[1], position[2] + voxDim[2]
                            ),
                            floatArrayOf(position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]),
                            mu
                        )
                    }
                    if (bits and 64 != 0) {
                        mu = ((isoLevel - value6) / (value7 - value6)).toFloat()
                        vertList[6] = lerp(
                            floatArrayOf(position[0], position[1] + voxDim[1], position[2] + voxDim[2]), floatArrayOf(
                                position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]
                            ), mu
                        )
                    }
                    if (bits and 128 != 0) {
                        mu = ((isoLevel - value4) / (value6 - value4)).toFloat()
                        vertList[7] = lerp(
                            floatArrayOf(position[0], position[1], position[2] + voxDim[2]), floatArrayOf(
                                position[0], position[1] + voxDim[1], position[2] + voxDim[2]
                            ), mu
                        )
                    }
                    // vertical lines of the cube
                    if (bits and 256 != 0) {
                        mu = ((isoLevel - value0) / (value4 - value0)).toFloat()
                        vertList[8] = lerp(
                            position, floatArrayOf(
                                position[0],
                                position[1], position[2] + voxDim[2]
                            ), mu
                        )
                    }
                    if (bits and 512 != 0) {
                        mu = ((isoLevel - value1) / (value5 - value1)).toFloat()
                        vertList[9] = lerp(
                            floatArrayOf(
                                position[0] + voxDim[0],
                                position[1], position[2]
                            ), floatArrayOf(position[0] + voxDim[0], position[1], position[2] + voxDim[2]), mu
                        )
                    }
                    if (bits and 1024 != 0) {
                        mu = ((isoLevel - value3) / (value7 - value3)).toFloat()
                        vertList[10] = lerp(
                            floatArrayOf(
                                position[0] + voxDim[0], position[1] + voxDim[1],
                                position[2]
                            ),
                            floatArrayOf(position[0] + voxDim[0], position[1] + voxDim[1], position[2] + voxDim[2]),
                            mu
                        )
                    }
                    if (bits and 2048 != 0) {
                        mu = ((isoLevel - value2) / (value6 - value2)).toFloat()
                        vertList[11] = lerp(
                            floatArrayOf(
                                position[0], position[1] + voxDim[1],
                                position[2]
                            ), floatArrayOf(position[0], position[1] + voxDim[1], position[2] + voxDim[2]), mu
                        )
                    }

                    // construct triangles -- get correct vertices from triTable.
                    var i = 0
                    // "Re-purpose cubeindex into an offset into triTable."
                    cubeindex = cubeindex shl 4
                    while (TablesMC.MC_TRI_TABLE[cubeindex + i] != -1) {
                        val index1 = TablesMC.MC_TRI_TABLE[cubeindex + i]
                        val index2 = TablesMC.MC_TRI_TABLE[cubeindex + i + 1]
                        val index3 = TablesMC.MC_TRI_TABLE[cubeindex + i + 2]

                        // Add triangles vertices normalized with the maximal possible value
                        vertices.add(
                            doubleArrayOf(
                                vertList[index3][0] / maxAxisVal - 0.5,
                                vertList[index3][1] / maxAxisVal - 0.5,
                                vertList[index3][2] / maxAxisVal - 0.5
                            )
                        )
                        vertices.add(
                            doubleArrayOf(
                                vertList[index2][0] / maxAxisVal - 0.5,
                                vertList[index2][1] / maxAxisVal - 0.5,
                                vertList[index2][2] / maxAxisVal - 0.5
                            )
                        )
                        vertices.add(
                            doubleArrayOf(
                                vertList[index1][0] / maxAxisVal - 0.5,
                                vertList[index1][1] / maxAxisVal - 0.5,
                                vertList[index1][2] / maxAxisVal - 0.5
                            )
                        )
                        i += 3
                    }
                    result.add(vertices)
                    vertices.clear()
                }
            }
        }

        return result
    }
}

