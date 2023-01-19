package depth.marching

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import depth.injection.assets
import depth.voxel.Terrain
import ktx.math.minus
import ktx.math.vec3
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute

open class MarchingCubeTerrain(private val vertices: FloatArray, size: Float) : Terrain(size) {
    private var mesh: Mesh
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
                x =-(u.y * v.z - u.z * v.y)
                y = -(u.z * v.x - u.x * v.z)
                z = -(u.x * v.y - u.y * v.x)
            }.nor().scl(-1f)// We invert the normal if the model seems to be inverted.

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

        mesh = meshBuilder.end()
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
        mesh.dispose()
    }

}
