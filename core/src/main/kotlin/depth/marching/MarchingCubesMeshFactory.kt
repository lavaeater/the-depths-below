//package depth.marching
//
//import com.badlogic.gdx.graphics.Mesh
//import com.badlogic.gdx.math.Vector3
//import java.nio.FloatBuffer
//import java.nio.IntBuffer
//
//
//class MarchingCubesMeshFactory {
//    private var m_scalarField: Array<Array<FloatArray>>
//    private var m_isoLevel: Float
//    private var m_cubeDiameter: Float
//    private var m_gridLengthX: Int
//    private var m_gridLengthY: Int
//    private var m_gridLengthZ: Int
//    private var m_cubeScalars: FloatArray
//    private var m_origin: Vector3
//    private var m_vertrexList: MutableList<Vector3>? = null
//
//    /**
//     * Constructs a new MarchingCubesMeshFactory for generating meshes out of a scalar field with the marching cubes algorithm.
//     *
//     * @param scalarField Contains the density of each position.
//     * @param isoLevel The minimum density needed for a position to be considered solid.
//     * @param cubeDiameter The diameter of a single voxel.
//     */
//    constructor(scalarField: Array<Array<FloatArray>>, isoLevel: Float, cubeDiameter: Float) {
//        m_scalarField = scalarField
//        m_isoLevel = isoLevel
//        m_cubeDiameter = cubeDiameter
//        m_gridLengthX = scalarField.size
//        m_gridLengthY = scalarField[0].size
//        m_gridLengthZ = scalarField[0][0].size
//        m_origin = computeCenterPoint()
//    }
//
//    /**
//     * Constructs a new MarchingCubesMeshFactory for generating meshes out of a scalar field with the marching cubes algorithm.
//     *
//     * @param scalarField Contains the density of each position.
//     * @param isoLevel The minimum density needed for a position to be considered solid.
//     * @param origin The local origin for all vertices of the generated mesh.
//     * @param cubeDiameter The diameter of a single voxel.
//     */
//    constructor(scalarField: Array<Array<FloatArray>>, isoLevel: Float, origin: Vector3, cubeDiameter: Float) {
//        m_scalarField = scalarField
//        m_isoLevel = isoLevel
//        m_cubeDiameter = cubeDiameter
//        m_gridLengthX = scalarField.size
//        m_gridLengthY = scalarField[0].size
//        m_gridLengthZ = scalarField[0][0].size
//        m_origin = origin
//    }
//
//    fun createMesh(): Mesh {
//        val mesh: Mesh = Mesh()
//        val positionBuffer = createPositionBuffer()
//        val indexBuffer = createIndexBuffer()
//        val normalBuffer = createNormalBuffer()
//        mesh.setBuffer(Type.Position, MeshBufferUtils.VERTEX_BUFFER_COMPONENT_COUNT, positionBuffer)
//        mesh.setBuffer(Type.Index, MeshBufferUtils.INDEX_BUFFER_COMPONENT_COUNT, indexBuffer)
//        mesh.setBuffer(Type.Normal, MeshBufferUtils.NORMAL_BUFFER_COMPONENT_COUNT, normalBuffer)
//        mesh.updateBound()
//        return mesh
//    }
//
//    private fun createNormalBuffer(): FloatBuffer {
//        val normalBuffer = VertexBuffer.createBuffer(
//            Format.Float,
//            MeshBufferUtils.NORMAL_BUFFER_COMPONENT_COUNT,
//            m_vertrexList!!.size
//        ) as FloatBuffer
//        var i: Int = MeshBufferUtils.VERTICES_PER_TRIANGLE - 1
//        while (i < m_vertrexList!!.size) {
//            val normal: Vector3 = computeTriangleNormal(
//                m_vertrexList!![i - 2],
//                m_vertrexList!![i - 1], m_vertrexList!![i]
//            )
//            for (j in 0 until MeshBufferUtils.NORMAL_BUFFER_COMPONENT_COUNT) normalBuffer.put(normal.x).put(normal.y)
//                .put(normal.z)
//            i += MeshBufferUtils.VERTICES_PER_TRIANGLE
//        }
//        return normalBuffer
//    }
//
//    private fun createIndexBuffer(): IntBuffer {
//        val indexBuffer = VertexBuffer.createBuffer(
//            Format.Int, MeshBufferUtils.INDEX_BUFFER_COMPONENT_COUNT,
//            m_vertrexList!!.size / MeshBufferUtils.INDEX_BUFFER_COMPONENT_COUNT
//        ) as IntBuffer
//        for (vertexIndex in m_vertrexList!!.indices) indexBuffer.put(vertexIndex)
//        return indexBuffer
//    }
//
//    private fun createPositionBuffer(): FloatBuffer {
//        m_vertrexList = ArrayList<Vector3>()
//        for (x in -1..m_gridLengthX) for (y in -1..m_gridLengthY) for (z in -1..m_gridLengthZ) {
//            val cubeVertices: Array<Vector3> = arrayOfNulls<Vector3>(MeshBufferUtils.SHARED_VERTICES_PER_CUBE)
//            val cubeIndex = computeCubeIndex(cubeVertices, x, y, z)
//            val edgeBitField = MarchingCubesTables.EDGE_TABLE[cubeIndex]
//            if (edgeBitField == 0) continue
//            val mcVertices: Array<Vector3> = computeMCVertices(cubeVertices, edgeBitField, m_isoLevel)
//            addVerticesToList(m_vertrexList, mcVertices, cubeIndex)
//        }
//        return addVerticesToPositionBuffer()
//    }
//
//    private fun addVerticesToPositionBuffer(): FloatBuffer {
//        val positionBuffer = VertexBuffer.createBuffer(
//            Format.Float,
//            MeshBufferUtils.VERTEX_BUFFER_COMPONENT_COUNT,
//            m_vertrexList!!.size
//        ) as FloatBuffer
//        for (i in m_vertrexList!!.indices) {
//            val position: Vector3 = m_vertrexList!![i]
//            positionBuffer.put(position.x).put(position.y).put(position.z)
//        }
//        return positionBuffer.flip()
//    }
//
//    /**
//     * Add the generated vertices by the marching cubes algorithm to a list. The added vertices are modified so that they respect the origin.
//     *
//     * @param vertrexList The list where to add the marching cubes vertices.
//     * @param mcVertices The marching cubes vertices.
//     * @param cubeIndex The cubeIndex.
//     */
//    private fun addVerticesToList(vertrexList: MutableList<Vector3>?, mcVertices: Array<Vector3>, cubeIndex: Int) {
//        val vertexCount = MarchingCubesTables.TRIANGLE_TABLE[cubeIndex].size
//        for (i in 0 until vertexCount) vertrexList!!.add(
//            mcVertices[MarchingCubesTables.TRIANGLE_TABLE[cubeIndex][i]].add(
//                m_origin
//            )
//        )
//    }
//
//    /**
//     * Computes the marching cubes vertices. Those are the lerped vertices that can later be used to form triangles.
//     *
//     * @param cubeVertices The vertices of a cube, i.e. the 8 corners.
//     * @param edgeBitField The bit field representing all the edges that should be drawn.
//     * @param isoLevel The minimum density needed for a position to be considered solid.
//     * @return The lerped vertices of a cube to form the marching cubes shape.
//     */
//    private fun computeMCVertices(
//        cubeVertices: Array<Vector3>,
//        edgeBitField: Int,
//        isoLevel: Float
//    ): Array<Vector3> {
//        val lerpedVertices: Array<Vector3> = arrayOfNulls<Vector3>(MarchingCubesTables.EDGE_BITS)
//        for (i in 0 until MarchingCubesTables.EDGE_BITS) {
//            if (edgeBitField and (1 shl i) != 0) {
//                val edgeFirstIndex = MarchingCubesTables.EDGE_FIRST_VERTEX[i]
//                val edgetSecondIndex = MarchingCubesTables.EDGE_SECOND_VERTEX[i]
//                lerpedVertices[i] = MCLerp(
//                    cubeVertices[edgeFirstIndex],
//                    cubeVertices[edgetSecondIndex], m_cubeScalars[edgeFirstIndex], m_cubeScalars[edgetSecondIndex]
//                )
//            }
//        }
//        return lerpedVertices
//    }
//
//    /**
//     * Lerps two vertices of a cube along their shared designed edge according to their densities.
//     *
//     * @param firstVertex The edge's first vertex.
//     * @param secondVertex The edge's second vertex.
//     * @param firstScalar The first vertex's density.
//     * @param secondScalar The second vertex's density.
//     * @return The lerped resulting vertex along the edge.
//     */
//    private fun MCLerp(
//        firstVertex: Vector3,
//        secondVertex: Vector3,
//        firstScalar: Float,
//        secondScalar: Float
//    ): Vector3 {
//        if (Math.abs(m_isoLevel - firstScalar) < Math.ulp(1f)) return firstVertex
//        if (Math.abs(m_isoLevel - secondScalar) < Math.ulp(1f)) return secondVertex
//        if (Math.abs(firstScalar - secondScalar) < Math.ulp(1f)) return firstVertex
//        val lerpFactor = (m_isoLevel - firstScalar) / (secondScalar - firstScalar)
//        return firstVertex.cpy().lerp(secondVertex, lerpFactor)
//    }
//
//    /**
//     * Computes the cubeIndex, which represents the adjacent voxels' densities.
//     *
//     * @param cubeVertices The 8 corners of a cube.
//     * @param indexX The X position of the marching cube in the grid.
//     * @param indexY The Y position of the marching cube in the grid.
//     * @param indexZ The Z position of the marching cube in the grid.
//     * @return The cubeIndex.
//     */
//    private fun computeCubeIndex(cubeVertices: Array<Vector3>, indexX: Int, indexY: Int, indexZ: Int): Int {
//        m_cubeScalars = FloatArray(MeshBufferUtils.SHARED_VERTICES_PER_CUBE)
//        val edgeLength = 2
//        var cubeVertexIndex = 0
//        var cubeIndex = 0
//        var cubeIndexRHS = 1
//
//        /*- Vertex indices
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
//        */for (y in 0 until edgeLength) for (z in 0 until edgeLength) {
//            var x = z % edgeLength
//            while (x >= 0 && x < edgeLength) {
//                cubeVertices[cubeVertexIndex] = Vector3(
//                    (indexX + x) * m_cubeDiameter,
//                    (indexY + y) * m_cubeDiameter,
//                    (indexZ + z) * m_cubeDiameter
//                )
//                m_cubeScalars[cubeVertexIndex++] = queryGridScalar(indexX + x, indexY + y, indexZ + z)
//                if (queryGridIsSolid(indexX + x, indexY + y, indexZ + z)) cubeIndex = cubeIndex or cubeIndexRHS
//                cubeIndexRHS = cubeIndexRHS shl 1
//                x += if (z == 0) 1 else -1
//            }
//        }
//        return cubeIndex
//    }
//
//    /**
//     * Queries if the grid is dense enough to be considered solid at the give (x, y, z) point.
//     *
//     * @param x The index on the X axis.
//     * @param y The index on the Y axis.
//     * @param z The index on the Z axis.
//     * @return If the grid is solid or empty at the given point.
//     */
//    private fun queryGridIsSolid(x: Int, y: Int, z: Int): Boolean {
//        return isScalarSolid(queryGridScalar(x, y, z))
//    }
//
//    /**
//     * Queries the grid scalar at the given point and manages the boundaries, i.e. it's ok if x = -1 or is bigger than the gridLengthX.
//     *
//     * @param x The scalar X position in the grid.
//     * @param y The scalar X position in the grid.
//     * @param z The scalar X position in the grid.
//     * @return The grid scalar at the (x, y, z) position.
//     */
//    private fun queryGridScalar(x: Int, y: Int, z: Int): Float {
//        return if (x >= 0 && x < m_scalarField.size && y >= 0 && y < m_scalarField[0].size && z >= 0 && z < m_scalarField[0][0].size) m_scalarField[x][y][z] else 0f
//    }
//
//    fun computeCenterPoint(): Vector3 {
//        return Vector3(
//            (-m_gridLengthX * m_cubeDiameter + m_cubeDiameter) / 2,
//            (-m_gridLengthY * m_cubeDiameter + m_cubeDiameter) / 2,
//            (-m_gridLengthZ * m_cubeDiameter + m_cubeDiameter) / 2
//        )
//    }
//
//    private fun isScalarSolid(scalar: Float): Boolean {
//        return scalar > m_isoLevel
//    }
//
//    companion object {
//        fun computeTriangleNormal(vertices: Array<Vector3>): Vector3 {
//            return computeTriangleNormal(vertices[0], vertices[1], vertices[2])
//        }
//
//        fun computeTriangleNormal(p1: Vector3, p2: Vector3, p3: Vector3): Vector3 {
//            return p2.subtract(p1).crossLocal(p3.subtract(p1)).normalizeLocal()
//        }
//    }
//}
