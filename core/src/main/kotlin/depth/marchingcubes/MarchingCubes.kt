package depth.marchingcubes

import com.badlogic.gdx.math.Vector3
import ktx.math.vec3

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
    ): List<List<Vector3>> {
        val result = mutableListOf<MutableList<Vector3>>()
        val vertices = mutableListOf<Vector3>()
        // Actual position along edge weighted according to function values.
        val vertList = Array(12) { vec3() }


        // Calculate maximal possible axis value (used in vertice normalization)
        val maxX = voxDim.x * (size - 1)
        val maxY = voxDim.y * (size - 1)
        val maxZ = voxDim.z * (size - 1)
        val maxAxisVal = maxX.coerceAtLeast(maxY.coerceAtLeast(maxZ))
        val smallSize = size - 2

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
                    val p = x + smallSize * y + smallSize * smallSize * z
                    val px = p + 1
                    val py = p + smallSize
                    val pxy = py + 1
                    val pz = p + smallSize * smallSize
                    val pxz = px + smallSize * smallSize
                    val pyz = py + smallSize * smallSize
                    val pxyz = pxy + smallSize * smallSize

                    //							  X              Y                    Z
                    val position = vec3(x * voxDim.x, y * voxDim.y, z * voxDim.z)

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
                            .lerp(vec3(position.x + voxDim.x, position.y + voxDim.y, position.z), mu)
                    }
                    if (bits and 4 != 0) {
                        mu = ((isoLevel - value2) / (value3 - value2)).toFloat()
                        vertList[2].set(
                                position.x, position.y + voxDim.y,
                                position.z
                            ).lerp(vec3(position.x + voxDim.x, position.y + voxDim.y, position.z), mu)
                    }
                    if (bits and 8 != 0) {
                        mu = ((isoLevel - value0) / (value2 - value0)).toFloat()
                        vertList[3].set(
                            position).lerp(vec3(
                                position.x, position.y + voxDim.y,
                                position.z
                            ), mu
                        )
                    }
                    // top of the cube
                    if (bits and 16 != 0) {
                        mu = ((isoLevel - value4) / (value5 - value4)).toFloat()
                        vertList[4].set(position.x, position.y, position.z + voxDim.z).lerp(vec3(
                                position.x + voxDim.x, position.y, position.z + voxDim.z
                            ), mu
                        )
                    }
                    if (bits and 32 != 0) {
                        mu = ((isoLevel - value5) / (value7 - value5)).toFloat()
                        vertList[5].set(
                                position.x + voxDim.x,
                                position.y, position.z + voxDim.z
                            ).lerp(vec3(position.x + voxDim.x, position.y + voxDim.y, position.z + voxDim.z),
                            mu
                        )
                    }
                    if (bits and 64 != 0) {
                        mu = ((isoLevel - value6) / (value7 - value6)).toFloat()
                        vertList[6].set(position.x, position.y + voxDim.y, position.z + voxDim.z).lerp(vec3(
                                position.x + voxDim.x, position.y + voxDim.y, position.z + voxDim.z
                            ), mu
                        )
                    }
                    if (bits and 128 != 0) {
                        mu = ((isoLevel - value4) / (value6 - value4)).toFloat()
                        vertList[7].set(
                            position.x, position.y, position.z + voxDim.z).lerp(vec3(
                                position.x, position.y + voxDim.y, position.z + voxDim.z
                            ), mu
                        )
                    }
                    // vertical lines of the cube
                    if (bits and 256 != 0) {
                        mu = ((isoLevel - value0) / (value4 - value0)).toFloat()
                        vertList[8].set(
                            position).lerp(vec3(
                                position.x,
                                position.y, position.z + voxDim.z
                            ), mu
                        )
                    }
                    if (bits and 512 != 0) {
                        mu = ((isoLevel - value1) / (value5 - value1)).toFloat()
                        vertList[9].set(
                                position.x + voxDim.x,
                                position.y, position.z
                            ).lerp(vec3(position.x + voxDim.x, position.y, position.z + voxDim.z), mu
                        )
                    }
                    if (bits and 1024 != 0) {
                        mu = ((isoLevel - value3) / (value7 - value3)).toFloat()
                        vertList[10].set(

                                position.x + voxDim.x, position.y + voxDim.y,
                                position.z
                            ).lerp(vec3(position.x + voxDim.x, position.y + voxDim.y, position.z + voxDim.z),
                            mu
                        )
                    }
                    if (bits and 2048 != 0) {
                        mu = ((isoLevel - value2) / (value6 - value2)).toFloat()
                        vertList[11].set(
                                position.x, position.y + voxDim.y,
                                position.z
                            ).lerp(vec3(position.x, position.y + voxDim.y, position.z + voxDim.z), mu
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
                            vec3(
                                vertList[index3].x / maxAxisVal - 0.5f,
                                vertList[index3].y / maxAxisVal - 0.5f,
                                vertList[index3].z / maxAxisVal - 0.5f
                            )
                        )
                        vertices.add(
                            vec3(
                                vertList[index2].x / maxAxisVal - 0.5f,
                                vertList[index2].y / maxAxisVal - 0.5f,
                                vertList[index2].z / maxAxisVal - 0.5f
                            )
                        )
                        vertices.add(
                            vec3(
                                vertList[index1].x / maxAxisVal - 0.5f,
                                vertList[index1].y / maxAxisVal - 0.5f,
                                vertList[index1].z / maxAxisVal - 0.5f
                            )
                        )
                        i += 3
                    }
                    result.add(vertices)
//                    vertList.forEach { it.setZero() }
                    vertices.clear()
                }
            }
        }

        return result
    }
}

