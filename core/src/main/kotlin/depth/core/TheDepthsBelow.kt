package depth.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import depth.injection.Context
import eater.core.MainGame
import eater.injection.InjectionContext.Companion.inject

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
        camController = CameraInputController(inject<PerspectiveCamera>()).apply {
            translateUnits = 100f
            rotateLeftKey = Input.Keys.A
            rotateRightKey = Input.Keys.D
            autoUpdate = true

        }
        Gdx.input.inputProcessor = camController
        addScreen(GameScreen(this, inject(), inject()))
        setScreen<GameScreen>()
    }

    override fun render() {
        camController.update()
        super.render()
    }
}

