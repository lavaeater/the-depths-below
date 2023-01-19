package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Input.Keys
import depth.marching.MarchingCubeBuilder
import eater.input.KeyPress
import eater.input.command
import ktx.app.KtxInputAdapter
import ktx.math.vec3

class KeyboardControlSystem(
    private val marchingCubeBuilder: MarchingCubeBuilder
) :
    EntitySystem(),
    KtxInputAdapter {

    private val currentTarget = vec3()


    private val controlMap = command("Controoool") {
        setUp(
            Keys.P,
            "Move In"
        ) {
            marchingCubeBuilder.togglePoints()
        }
        setUp(
            Keys.B,
            "Move In"
        ) {
            marchingCubeBuilder.toggleStarted()
        }
        setUp(
            Keys.SPACE,
            "Move In"
        ) {
            marchingCubeBuilder.updateModel()
        }
        setUp(
            Keys.W,
            "Move In"
        ) {
            marchingCubeBuilder.moveIn()
        }
        setUp(
            Keys.S,
            "Move Out"
        ) {
            marchingCubeBuilder.moveOut()
        }
        setUp(
            Keys.A,
            "Move Left"
        ) {
            marchingCubeBuilder.moveLeft()
        }
        setUp(
            Keys.D,
            "Move Right"
        ) {
            marchingCubeBuilder.moveRight()
        }
        setUp(
            Keys.UP,
            "Move Up"
        ) {
            marchingCubeBuilder.moveUp()
        }
        setUp(
            Keys.DOWN,
            "Move DOwn"
        ) {
            marchingCubeBuilder.moveDown()
        }

        setUp(
            Keys.RIGHT,
            "Move Up"
        ) {
            marchingCubeBuilder.indexUp()
        }
        setUp(
            Keys.LEFT,
            "Move DOwn"
        ) {
            marchingCubeBuilder.indexDown()
        }

    }


    override fun keyDown(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Down)
    }

    override fun keyUp(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Up)
    }

    private fun updateCameraPosition() {

    }


    private val coolDown = 0.05f
    private var acc = 0f
    override fun update(deltaTime: Float) {
        marchingCubeBuilder.update(deltaTime)
    }
}
