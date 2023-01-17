package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import depth.ecs.components.*
import eater.input.KeyPress
import eater.input.command
import ktx.app.KtxInputAdapter
import ktx.ashley.allOf
import ktx.log.info
import ktx.math.plus
import ktx.math.times
import ktx.math.vec3


class KeyboardControlSystem(
    private val boxOfPoints: BoxOfPoints,
    private val camera: PerspectiveCamera) :
    EntitySystem(),
    KtxInputAdapter {

    private val controlComponent = DirectionThing()

    private val controlMap = command("Controoool") {
        setUp(
            Keys.W,
            "Throttle F"
        ) {
            currentPoint++
            updateCameraPosition()
        }
        setUp(
            Keys.S,
            "Throttle R"
        ) {
            currentPoint--
            updateCameraPosition()
        }
        setBoth(
            Keys.A,
            "Left",
            { controlComponent.remove(Rotation.YawLeft) },
            { controlComponent.add(Rotation.YawLeft) }
        )
        setBoth(
            Keys.D,
            "Right",
            { controlComponent.remove(Rotation.YawRight) },
            { controlComponent.add(Rotation.YawRight) }
        )
        setBoth(
            Keys.UP,
            "Up",
            { controlComponent.remove(Direction.Up) },
            { controlComponent.add(Direction.Up) }
        )
        setBoth(
            Keys.DOWN,
            "Down",
            { controlComponent.remove(Direction.Down) },
            { controlComponent.add(Direction.Down) }
        )
    }

    override fun keyDown(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Down)
    }

    override fun keyUp(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Up)
    }

    private var currentPoint = 0

    override fun update(deltaTime: Float) {

        if (controlComponent.has(Rotation.YawLeft)) {
        }

        if (controlComponent.has(Rotation.YawRight)) {
        }

        if (controlComponent.has(Direction.Up)) {
        }
        if (controlComponent.has(Direction.Down)) {
        }
        if (controlComponent.has(Direction.Forward)) {
            currentPoint++
            updateCameraPosition()
        }
        if (controlComponent.has(Direction.Reverse)) {
            currentPoint--
            updateCameraPosition()
        }
    }

    val offset = vec3(125f, 0f, 125f)
    val currentTarget = vec3()

    private fun updateCameraPosition() {
        info { "currentIndex = $currentPoint" }
        if(currentPoint > boxOfPoints.boxPoints.lastIndex)
            currentPoint = 0
        if(currentPoint < 0)
            currentPoint = boxOfPoints.boxPoints.lastIndex

        info { "currentIndex = $currentPoint" }
        val currentBox = boxOfPoints.boxPoints[currentPoint]
        info { "Currentbox: $currentBox" }
        /**
         *
         * Aaah... the current box, eh.  Here we go again.
         * Yes, we cannot ACTUALLY use the outermost points.
         * Or rather, they are of course not where the 0 vertex index is.
         *
         * The position of the camera should be in the middle of the 8 vertices
         * that form this box
         *
         * But we shall start with just looking at this particular point.
         */
        currentTarget.set(currentBox.x * 25f,currentBox.y * 25f,currentBox.z * 25f)
        camera.position.set(currentTarget + offset)
        camera.lookAt(currentTarget)
        camera.update()
    }
}
