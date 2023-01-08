package depth.injection

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.viewport.ExtendViewport
import depth.ecs.systems.RenderSystem3d
import depth.voxel.BlockManager
import depth.voxel.DeepGameSettings
import eater.core.GameSettings
import eater.core.MainGame
import eater.ecs.ashley.systems.RemoveEntitySystem
import eater.injection.InjectionContext
import ktx.assets.disposeSafely
import ktx.math.vec3
import net.mgsx.gltf.scene3d.scene.SceneManager
import space.earlygrey.shapedrawer.ShapeDrawer

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
                fieldOfView = 30f
                position.set(vec3(-10f, -10f, -10f))
                lookAt(vec3(0f,0f,0f))
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
            bindSingleton(SceneManager().apply {
                setCamera(inject<PerspectiveCamera>())
            })
            bindSingleton(getEngine(gameSettings, false))
        }
    }
    private fun getEngine(gameSettings: DeepGameSettings, debugBox2d: Boolean): Engine {
        return PooledEngine().apply {
            addSystem(RemoveEntitySystem())
            addSystem(RenderSystem3d(inject(), inject(), inject()))
        }
    }
}
