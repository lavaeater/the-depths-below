package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import depth.ecs.components.MotionState
import depth.voxel.generateMarchingCubeTerrain
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager


fun <E> MutableSet<E>.addIndexed(element: E): Int {
    this.add(element)
    return this.indexOf(element)
}

class RenderSystem3d(
    private val sceneManager: SceneManager,
    private val world: btDynamicsWorld
    ) : EntitySystem() {
    override fun update(deltaTime: Float) {
        renderScenes(deltaTime)
    }

    //
    private val terrain = generateMarchingCubeTerrain(16, 25f)


    init {
        // Added by suggestion of JamesTKhan as a troubleshooting measure.
//        val config = DefaultShader.Config()
//        config.defaultCullFace = GL20.GL_NONE
//        val provider = DefaultShaderProvider(config)

        sceneManager.addScene(Scene(terrain.modelInstance))

        val cShape: btCollisionShape = Bullet.obtainStaticNodeShape(terrain.modelInstance.model.nodes)
        val motionState = MotionState().apply {
            transform = terrain.modelInstance.transform
        }
        val info = btRigidBody.btRigidBodyConstructionInfo(0f, motionState, cShape, Vector3.Zero)

        world.addRigidBody(btRigidBody(info))
    }


    private fun renderScenes(deltaTime: Float) {
        sceneManager.update(deltaTime)
        sceneManager.render()
    }
}

//        MarchingCubeTerrain(
//        floatArrayOf(
//            -10f, -10f, -10f, // triangle 1 : begin
//            -10f, -10f, 10f,
//            -10f, 10f, 10f, // triangle 1 : end
//            10f, 10f, -10f, // triangle 2 : begin
//            -10f, -10f, -10f,
//            -10f, 10f, -10f, // triangle 2 : end
//            10f, -10f, 10f,
//            -10f, -10f, -10f,
//            10f, -10f, -10f,
//            10f, 10f, -10f,
//            10f, -10f, -10f,
//            -10f, -10f, -10f,
//            -10f, -10f, -10f,
//            -10f, 10f, 10f,
//            -10f, 10f, -10f,
//            10f, -10f, 10f,
//            -10f, -10f, 10f,
//            -10f, -10f, -10f,
//            -10f, 10f, 10f,
//            -10f, -10f, 10f,
//            10f, -10f, 10f,
//            10f, 10f, 10f,
//            10f, -10f, -10f,
//            10f, 10f, -10f,
//            10f, -10f, -10f,
//            10f, 10f, 10f,
//            10f, -10f, 10f,
//            10f, 10f, 10f,
//            10f, 10f, -10f,
//            -10f, 10f, -10f,
//            10f, 10f, 10f,
//            -10f, 10f, -10f,
//            -10f, 10f, 10f,
//            10f, 10f, 10f,
//            -10f, 10f, 10f,
//            10f, -10f, 10f
//        ), 200f
//    )


//        val mesh = terrain.modelInstance.model.meshes.first()
//        val stride = mesh.vertexSize / 8
//        val vertices = FloatArray(mesh.numVertices * stride)
//        mesh.getVertices(vertices)
//        val vectors = mutableSetOf<Vector3>()
//        val triangles = mutableListOf<Triple<Vector3, Vector3, Vector3>>()
//        val triangleIndexes = mutableListOf<Triple<Int, Int, Int>>()
//        val stepSize =  3 * stride
//        for(i in vertices.indices step stepSize) {
//            val v0 = vec3(
//                vertices[i],
//                vertices[i + 1],
//                vertices[i + 2],
//            )
//            val v1 = vec3(
//                vertices[i+3],
//                vertices[i + 4],
//                vertices[i + 5],
//            )
//            val v2 = vec3(
//                vertices[i + 6],
//                vertices[i + 7],
//                vertices[i + 8],
//            )
//            val i0 = vectors.addIndexed(v0)
//            val i1 = vectors.addIndexed(v1)
//            val i2 = vectors.addIndexed(v2)
//            triangleIndexes.add(Triple(i0, i1, i2))
//        }
//        val shape = Mesh(true, true, triangleIndexes.size * 3, vectors.size,  VertexAttributes(VertexAttribute.Position()))
//        val indices = triangleIndexes.map { shortArrayOf(it.first.toShort(), it.second.toShort(), it.third.toShort()) }.flatMap { it.asIterable() }.toShortArray()
//        val vertInputs = vectors.map { floatArrayOf(it.x, it.y, it.z) }.flatMap { it.asIterable() }.toFloatArray()
//        shape.setVertices(vertInputs)
//        for(i in (indices.indices step 150)) {
//            val countLeft = min(indices.size - i, 150)
//            shape.setIndices(indices, i, countLeft)
//        }
//
//        val mb = ModelBuilder()
//        mb.begin()
//        mb.part("collsionreef", shape, GL20.GL_TRIANGLES, Material())
//        val model = mb.end()
