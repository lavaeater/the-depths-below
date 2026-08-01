package depth.core

import com.badlogic.gdx.physics.bullet.Bullet
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

    override fun create() {
        Bullet.init()
        Context.initialize(this)
        addScreen(GameScreen(this, inject(), inject()))
        setScreen<GameScreen>()
    }

    override fun render() {
        super.render()
    }
}

