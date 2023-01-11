package depth.injection

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.viewport.ExtendViewport
import depth.ecs.systems.RenderSystem3d
import depth.voxel.BlockManager
import depth.voxel.DeepGameSettings
import eater.core.MainGame
import eater.ecs.ashley.systems.RemoveEntitySystem
import eater.injection.InjectionContext
import ktx.assets.disposeSafely
import ktx.math.vec3
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import net.mgsx.gltf.scene3d.utils.EnvironmentUtil


object Context: InjectionContext() {
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
                position.set(vec3(15f, 15f, 15f))
                lookAt(vec3(0f,0f,0f))
                near = 1f
                far = 300f
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

    fun createSceneManager(camera: PerspectiveCamera) : SceneManager {
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

        sceneManager.setAmbientLight(1f)
        sceneManager.environment.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT))
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap))
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap))

        // setup skybox

        // setup skybox
        val skybox = SceneSkybox(environmentCubemap)
        sceneManager.skyBox = skybox
        return sceneManager
    }

    private fun getEngine(gameSettings: DeepGameSettings, debugBox2d: Boolean): Engine {
        return PooledEngine().apply {
            addSystem(RemoveEntitySystem())
            addSystem(RenderSystem3d(inject(), inject(), inject()))
        }
    }
}
