package depth.core

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.viewport.ExtendViewport
import depth.ecs.systems.SubmarineControlSystem
import depth.injection.createSubMarine
import eater.core.MainGame
import ktx.ashley.getSystem

class GameScreen(game: MainGame, engine: Engine, viewport: ExtendViewport
) : Screen3d(game, engine, viewport) {
    var needsInit = true
    override fun show() {
        super.show()
        if(needsInit) {
            needsInit = false
            //createSubMarine()
        }
       // Gdx.input.inputProcessor = engine.getSystem<SubmarineControlSystem>()
    }




}
