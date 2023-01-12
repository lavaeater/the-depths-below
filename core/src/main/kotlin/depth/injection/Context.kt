package depth.injection

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.ExtendViewport
import depth.ecs.systems.RenderSystem3d
import depth.ecs.systems.UpdatePointLightSystem
import depth.voxel.BlockManager
import depth.voxel.DeepGameSettings
import eater.core.MainGame
import eater.ecs.ashley.systems.RemoveEntitySystem
import eater.injection.InjectionContext
import ktx.assets.disposeSafely
import ktx.math.vec3
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight
import net.mgsx.gltf.scene3d.lights.PointLightEx
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import net.mgsx.gltf.scene3d.shaders.PBRDepthShader
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider.createDefaultConfig
import net.mgsx.gltf.scene3d.shaders.PBREmissiveShaderProvider
import net.mgsx.gltf.scene3d.utils.EnvironmentUtil


object Context : InjectionContext() {
    private val shapeDrawerRegion: TextureRegion by lazy {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.drawPixel(0, 0)
        val texture = Texture(pixmap) //remember to dispose of later
        pixmap.disposeSafely()
        TextureRegion(texture, 0, 0, 1, 1)
    }

    fun initialize(game: MainGame) {
        buildContext {
            val gameSettings = DeepGameSettings()
            bindSingleton(gameSettings)
            bindSingleton(Assets(inject()))
            bindSingleton(BlockManager(inject()))
            bindSingleton(game)
            bindSingleton(PerspectiveCamera().apply {
                fieldOfView = 67f
                position.set(vec3(0f, 100f, 0f))
//                lookAt(Vector3.Zero)
                lookAt(vec3(50f, 100f, -50f))
                near = 1f
                far = 3000f
            })
            bindSingleton(
                ExtendViewport(
                    gameSettings.gameWidth,
                    gameSettings.gameHeight,
                    inject<PerspectiveCamera>() as Camera
                )
            )
            bindSingleton(createSceneManager(inject()))
            bindSingleton(getEngine(gameSettings, false))
        }
    }

    fun createSceneManager(camera: PerspectiveCamera): SceneManager {
        val sceneManager = SceneManager().apply {
            setCamera(inject<PerspectiveCamera>())
        }
        val environmentCubemap = EnvironmentUtil.createCubemap(
            InternalFileHandleResolver(),
            "textures/environment/environment_", ".png", EnvironmentUtil.FACE_NAMES_NEG_POS
        )
        val diffuseCubemap = EnvironmentUtil.createCubemap(
            InternalFileHandleResolver(),
            "textures/diffuse/diffuse_", ".png", EnvironmentUtil.FACE_NAMES_NEG_POS
        )
        val specularCubemap = EnvironmentUtil.createCubemap(
            InternalFileHandleResolver(),
            "textures/specular/specular_", "_", ".png", 10, EnvironmentUtil.FACE_NAMES_NEG_POS
        )
        val brdfLUT = Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"))

//        sceneManager.setAmbientLight(1f)

//        sceneManager.setShaderProvider(
//            PBRDepthShaderProvider(createDefaultConfig()))

        sceneManager.environment.apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, .1f, .1f, .1f, 1f))
            add(DirectionalShadowLight().apply {
                set(1f, 1f, 1f, -1f, -1f, 0f)
            })
            add(DirectionalShadowLight().apply {
                set(1f, 1f, 1f, 1f, 0f, -1f)
            })
        }
        sceneManager.environment.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT))
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap))
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap))

        // setup skybox

//        // setup skybox
        val skybox = SceneSkybox(environmentCubemap)
        sceneManager.skyBox = skybox
        return sceneManager
    }

    private fun getEngine(gameSettings: DeepGameSettings, debugBox2d: Boolean): Engine {
        return PooledEngine().apply {
            addSystem(RemoveEntitySystem())
            addSystem(UpdatePointLightSystem(PointLightEx().apply {
                setColor(Color.GREEN)
                setIntensity(100000f)
                inject<SceneManager>().environment.add(this)
            }, inject()))
            addSystem(RenderSystem3d(inject()))
        }
    }
}
