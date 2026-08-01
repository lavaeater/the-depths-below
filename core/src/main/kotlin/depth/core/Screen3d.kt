package depth.core

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.utils.viewport.ExtendViewport
import eater.core.MainGame
import eater.core.toColor
import ktx.app.KtxScreen
import ktx.app.clearScreen

open class Screen3d(protected val game: MainGame,
                    protected val engine: Engine, protected val viewport: ExtendViewport
) : KtxScreen {
    private val bgColor = "000022".toColor()
    override fun render(delta: Float) {
        clearScreen(bgColor.r, bgColor.g, bgColor.b)
        engine.update(delta)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }
}
