package depth.injection

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.physics.bullet.collision.*
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver
import com.badlogic.gdx.physics.bullet.softbody.btSoftRigidDynamicsWorld
import com.badlogic.gdx.utils.viewport.ExtendViewport
import depth.ecs.systems.*
import depth.marching.WorldManager
import depth.voxel.BlockManager
import depth.core.DeepGameSettings
import depth.marching.MarchingCubeBuilder
import eater.core.MainGame
import eater.ecs.ashley.systems.RemoveEntitySystem
import eater.injection.InjectionContext
import ktx.assets.disposeSafely
import ktx.inject.Context
import ktx.math.vec3
import net.mgsx.gltf.scene3d.attributes.FogAttribute
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.SceneSkybox
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
                fieldOfView = 45f
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
            bindSingleton(createSceneManager())
            setupBullet(this)
            bindSingleton(MarchingCubeBuilder(inject(), inject(), 10, inject()))
            bindSingleton(WorldManager(inject(), inject(), inject()).apply {
                generateChunks(5)
            })
            bindSingleton(getEngine(gameSettings, false))
        }
    }

    fun createSceneManager(): SceneManager {
        val sceneManager = SceneManager().apply {
            setCamera(inject<PerspectiveCamera>())
        }
        val environmentCubemap = EnvironmentUtil.createCubemap(
            InternalFileHandleResolver(),
            "textures/interstellar_skybox/interstellar_", ".png", EnvironmentUtil.FACE_NAMES_NEG_POS
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

        sceneManager.environment.apply {
            set(ColorAttribute(ColorAttribute.Fog, Color(0.2f, 0.2f, 0.5f, 0.5f)))
            set(FogAttribute(FogAttribute.FogEquation).set(1f, 400f, 0.7f))
            set(ColorAttribute(ColorAttribute.AmbientLight, 0f, 0f, .3f, 1f))
            add(DirectionalShadowLight().apply {
                set(1f, 1f, 1f, -1f, -1f, 0f)
            })
            add(DirectionalShadowLight().apply {
                set(1f, 1f, 1f, 1f, 0f, -1f)
            })
        }
//        sceneManager.environment.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT))
//        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap))
//        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap))

        // setup skybox

//        // setup skybox
//        val skybox = SceneSkybox(environmentCubemap)
//        sceneManager.skyBox = skybox
        return sceneManager
    }

    private fun setupBullet(context: Context) {
        context.apply {
            bindSingleton<btCollisionConfiguration>(btDefaultCollisionConfiguration())
            bindSingleton<btDispatcher>(btCollisionDispatcher(inject()))
            bindSingleton<btBroadphaseInterface>(btDbvtBroadphase())
            bindSingleton<btConstraintSolver>(btSequentialImpulseConstraintSolver())
            bindSingleton<btDynamicsWorld>(btSoftRigidDynamicsWorld(inject(), inject(), inject(), inject()).apply {
                gravity = vec3(0f, 0f, 0f)
            })

        }
    }

    private fun getEngine(gameSettings: DeepGameSettings, debugBox2d: Boolean): Engine {
        return PooledEngine().apply {
            addSystem(RemoveEntitySystem())
            addSystem(UpdatePointLightSystem())
            addSystem(UpdatePerspectiveCameraSystem(inject()))
            addSystem(BulletUpdateSystem(inject()))
            addSystem(SubmarineControlSystem(inject()).apply {
                Gdx.input.inputProcessor = this
            })
            addSystem(RenderSystem3d(inject(), inject()))
            addSystem(UpdateChunkSystem(inject(), inject()))
//            addSystem(DebugRenderSystem3d(inject<ExtendViewport>(), inject()))
        }
    }
}
