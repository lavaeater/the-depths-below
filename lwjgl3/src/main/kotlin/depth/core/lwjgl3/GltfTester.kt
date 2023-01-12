package depth.core.lwjgl3

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import net.mgsx.gltf.loaders.shared.geometry.MeshTangentSpaceGenerator
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.shaders.PBREmissiveShaderProvider
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider

class ProceduralExamples : ApplicationAdapter() {
    private var defaultShaderProvider = true
    private var manager: SceneManager? = null
    private var camera: PerspectiveCamera? = null
    private var cameraController: CameraInputController? = null
    private var modelInstance: ModelInstance? = null
    private val disposables = com.badlogic.gdx.utils.Array<Disposable>()
    private val models = com.badlogic.gdx.utils.Array<ModelInstance?>()
    override fun create() {
        val diffuseTexture = Texture(Gdx.files.classpath("textures/red_bricks_04_diff_1k.jpg"), true)
        val normalTexture = Texture(Gdx.files.classpath("textures/red_bricks_04_nor_gl_1k.jpg"), true)
        val mrTexture = Texture(Gdx.files.classpath("textures/red_bricks_04_rough_1k.jpg"), true)
        disposables.add(diffuseTexture, normalTexture, mrTexture)

        // minimal box with empty material
        run {
            val mb = ModelBuilder()
            mb.begin()
            val material = Material()
            val mpb = mb.part(
                "cube",
                GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position.toLong(),
                material
            )
            BoxShapeBuilder.build(mpb, 1f, 1f, 1f)
            val model = mb.end()
            disposables.add(model)
            models.add(ModelInstance(model))
        }

        // unlit box with solid color
        run {
            val mb = ModelBuilder()
            mb.begin()
            val material = Material()
            material.set(
                PBRColorAttribute.createBaseColorFactor(
                    Color(Color.WHITE).fromHsv(15f, .9f, .8f)
                )
            )
            val mpb = mb.part(
                "cube",
                GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position.toLong(),
                material
            )
            BoxShapeBuilder.build(mpb, 1f, 1f, 1f)
            val model = mb.end()
            disposables.add(model)
            models.add(ModelInstance(model))
        }

        // lit box with solid color
        run {
            val mb = ModelBuilder()
            mb.begin()
            val material = Material()
            material.set(
                PBRColorAttribute.createBaseColorFactor(
                    Color(Color.WHITE).fromHsv(15f, .9f, .8f)
                )
            )
            val mpb = mb.part(
                "cube",
                GL20.GL_TRIANGLES,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
                material
            )
            BoxShapeBuilder.build(mpb, 1f, 1f, 1f)
            val model = mb.end()
            disposables.add(model)
            models.add(ModelInstance(model))
        }

        // lit box with solid color and emissive
        run {
            val mb = ModelBuilder()
            mb.begin()
            val material = Material()
            material.set(
                PBRColorAttribute.createBaseColorFactor(
                    Color(Color.WHITE).fromHsv(15f, .9f, .8f)
                )
            )
            material.set(PBRColorAttribute.createEmissive(Color(Color.RED)))
            val mpb = mb.part(
                "cube",
                GL20.GL_TRIANGLES,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
                material
            )
            BoxShapeBuilder.build(mpb, 1f, 1f, 1f)
            val model = mb.end()
            disposables.add(model)
            models.add(ModelInstance(model))
        }

        // lit box with emissive only
        run {
            val mb = ModelBuilder()
            mb.begin()
            val material = Material()
            material.set(PBRColorAttribute.createEmissive(Color(Color.RED)))
            val mpb = mb.part(
                "cube",
                GL20.GL_TRIANGLES,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
                material
            )
            BoxShapeBuilder.build(mpb, 1f, 1f, 1f)
            val model = mb.end()
            disposables.add(model)
            models.add(ModelInstance(model))
        }

        // lit sphere with a diffuse texture
        run {
            val mb = ModelBuilder()
            mb.begin()
            val material = Material()
            material.set(PBRTextureAttribute.createBaseColorTexture(diffuseTexture))
            val mpb = mb.part(
                "cube",
                GL20.GL_TRIANGLES,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong(),
                material
            )
            SphereShapeBuilder.build(mpb, 1f, 1f, 1f, 32, 16)
            val model = mb.end()
            disposables.add(model)
            models.add(ModelInstance(model))
        }

        // lit sphere with normal maps
        run {
            val mb = ModelBuilder()
            mb.begin()
            val material = Material()
            material.set(
                PBRColorAttribute.createBaseColorFactor(
                    Color(Color.WHITE).fromHsv(15f, .9f, .8f)
                )
            )
            material.set(PBRTextureAttribute.createNormalTexture(normalTexture))
            val attributes = VertexAttributes(
                VertexAttribute.Position(),
                VertexAttribute.Normal(),
                VertexAttribute(
                    VertexAttributes.Usage.Tangent,
                    4,
                    ShaderProgram.TANGENT_ATTRIBUTE
                ),
                VertexAttribute.TexCoords(0)
            )
            val mpb =
                mb.part("sphere", GL20.GL_TRIANGLES, attributes, material)
            SphereShapeBuilder.build(mpb, 1f, 1f, 1f, 32, 16)
            val model = mb.end()
            // generate tangents
            for (mesh in model.meshes) {
                MeshTangentSpaceGenerator.computeTangentSpace(mesh, material, false, true)
            }
            disposables.add(model)
            models.add(ModelInstance(model))
        }

        // lit sphere with all textures
        run {
            val mb = ModelBuilder()
            mb.begin()
            val material = Material()
            material.set(PBRTextureAttribute.createBaseColorTexture(diffuseTexture))
            material.set(PBRTextureAttribute.createNormalTexture(normalTexture))
            material.set(PBRTextureAttribute.createMetallicRoughnessTexture(mrTexture))
            val attributes = VertexAttributes(
                VertexAttribute.Position(),
                VertexAttribute.Normal(),
                VertexAttribute(
                    VertexAttributes.Usage.Tangent,
                    4,
                    ShaderProgram.TANGENT_ATTRIBUTE
                ),
                VertexAttribute.TexCoords(0)
            )
            val mpb =
                mb.part("sphere", GL20.GL_TRIANGLES, attributes, material)
            SphereShapeBuilder.build(mpb, 1f, 1f, 1f, 32, 16)
            val model = mb.end()
            // generate tangents
            for (mesh in model.meshes) {
                MeshTangentSpaceGenerator.computeTangentSpace(mesh, material, false, true)
            }
            disposables.add(model)
            models.add(ModelInstance(model))
        }
        modelInstance = models.first()
        manager = SceneManager(0)
        manager!!.setAmbientLight(0.01f)
        manager!!.environment.add(DirectionalLightEx().set(Color.WHITE, Vector3(-1f, -4f, -2f), 5f))
        camera = PerspectiveCamera(60f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        manager!!.camera = camera
        camera!!.position.set(1f, 1f, 1f).scl(3f)
        camera!!.up.set(Vector3.Y)
        camera!!.lookAt(Vector3.Zero)
        camera!!.near = .01f
        camera!!.far = 100f
        camera!!.update()
        cameraController = CameraInputController(camera)
        Gdx.input.inputProcessor = cameraController
    }

    override fun dispose() {
        for (d in disposables) {
            d.dispose()
        }
        manager!!.dispose()
    }

    override fun resize(width: Int, height: Int) {
        camera!!.viewportWidth = width.toFloat()
        camera!!.viewportHeight = height.toFloat()
    }

    override fun render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            var index = models.indexOf(modelInstance, true)
            index = (index + 1) % models.size
            modelInstance = models[index]
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            defaultShaderProvider = !defaultShaderProvider
            if (defaultShaderProvider) {
                manager!!.setShaderProvider(PBRShaderProvider.createDefault(0))
            } else {
                manager!!.setShaderProvider(PBREmissiveShaderProvider(PBREmissiveShaderProvider.createConfig(0)))
            }
        }
        cameraController!!.update()
        val delta = Gdx.graphics.deltaTime
        manager!!.update(delta)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        manager!!.renderableProviders.add(modelInstance)
        manager!!.render()
        manager!!.renderableProviders.clear()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Lwjgl3Application(ProceduralExamples())
        }
    }
}
