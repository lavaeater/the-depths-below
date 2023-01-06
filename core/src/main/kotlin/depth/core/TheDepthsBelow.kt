package depth.core

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.utils.viewport.ExtendViewport
import depth.injection.Context
import eater.core.MainGame
import eater.core.engine
import eater.core.toColor
import eater.injection.InjectionContext.Companion.inject
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely
import ktx.assets.toInternalFile
import ktx.graphics.use

class TheDepthsBelow : MainGame() {
    override fun goToGameSelect() {
        TODO("Not yet implemented")
    }

    override fun goToGameScreen() {
        TODO("Not yet implemented")
    }

    override fun goToGameOver() {
        TODO("Not yet implemented")
    }

    override fun gotoGameVictory() {
        TODO("Not yet implemented")
    }

    lateinit var camController: CameraInputController

    override fun create() {
        Context.initialize(this)
        camController = CameraInputController(inject<PerspectiveCamera>()).apply { autoUpdate = true }
        Gdx.input.inputProcessor = camController
        addScreen(FirstScreen(this, inject(), inject()))
        setScreen<FirstScreen>()
    }

    override fun render() {
        camController.update()
        super.render()
    }
}

open class Screen3d(protected val game: MainGame,
                    protected val engine: Engine, protected val viewport: ExtendViewport) : KtxScreen {
    private val bgColor = "BBBB75".toColor()
    override fun render(delta: Float) {
        clearScreen(bgColor.r, bgColor.g, bgColor.b)
        engine.update(delta)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }
}

class FirstScreen(game: MainGame, engine: Engine, viewport: ExtendViewport
) : Screen3d(game, engine, viewport)
