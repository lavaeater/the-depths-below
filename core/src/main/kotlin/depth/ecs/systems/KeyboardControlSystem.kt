package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import depth.ecs.components.*
import eater.input.KeyPress
import eater.input.command
import ktx.app.KtxInputAdapter
import ktx.log.info
import ktx.math.plus
import ktx.math.vec3
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute


class KeyboardControlSystem(
    private val boxOfPoints: BoxOfPoints,
    private val camera: PerspectiveCamera
) :
    EntitySystem(),
    KtxInputAdapter {
    private var dTime = 0f
    private val controlComponent = DirectionThing()
    private var currentPoint = 0
    private val offset = vec3(-125f, 50f, -50f)
    private val currentTarget = vec3()

    private fun move(x:Int, y:Int, z:Int) {
        val newPoint = boxOfPoints.boxPoints[currentPoint].coord.add(x, y, z)
        val potentialBox = boxOfPoints.boxPoints.firstOrNull { it.coord == newPoint }
        if(potentialBox != null) {
            turnColorsOff()
            currentPoint = boxOfPoints.boxPoints.indexOf(potentialBox)
            updateCameraPosition()
        }
    }

    private fun moveLeft() {
        move(-1, 0,0)
    }

    private fun moveRight() {
        move(1, 0,0)

    }

    private fun moveUp() {
        move(0, 1,0)

    }

    private fun moveDown() {
        move(0, -1,0)
    }

    private fun moveIn() {
        move(0, 0,-1)
    }

    private fun moveOut() {
        move(0, 0,1)
    }

    private val controlMap = command("Controoool") {
        setUp(
            Keys.W,
            "Move In"
        ) {
            moveIn()
        }
        setUp(
            Keys.S,
            "Move Out"
        ) {
            moveOut()
        }
        setUp(
            Keys.A,
            "Move Left"
        ) {
            moveLeft()
        }
        setUp(
            Keys.D,
            "Move Right"
        ) {
            moveRight()
        }
        setUp(
            Keys.UP,
            "Move Up"
        ) {
            moveUp()
        }
        setUp(
            Keys.DOWN,
            "Move DOwn"
        ) {
            moveDown()
        }

        setUp(
            Keys.RIGHT,
            "Move Up"
        ) {
            indexUp()
        }
        setUp(
            Keys.LEFT,
            "Move DOwn"
        ) {
            indexDown()
        }

    }

    private fun indexUp() {
        turnColorsOff()
        currentPoint++
        updateCameraPosition()
    }

    private fun indexDown() {
        turnColorsOff()
        currentPoint--
        updateCameraPosition()
    }

    override fun keyDown(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Down)
    }

    override fun keyUp(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Up)
    }

    private val green = Color(0f, 1f, 0f, 0.5f)
    private val blue = Color(0f, 0f, 1f, 0.5f)

    private fun turnColorsOff() {
        val currentBox = boxOfPoints.boxPoints[currentPoint]
        for (vertIndex in 1..7) {
            val otherBox = boxOfPoints
                .boxPoints
                .firstOrNull {
                    it.coord == currentBox
                        .coord
                        .getCoordForVertex(vertIndex)
                }
            otherBox?.modelInstance?.getMaterial("cube")?.set(
                PBRColorAttribute.createEmissive(
                    if (otherBox.on)
                        green else
                        blue
                )
            )
        }

        currentBox.modelInstance.getMaterial("cube")?.set(
            PBRColorAttribute.createEmissive(
                if (currentBox.on)
                    green else
                    blue
            )
        )
    }

    private fun turnOnColors() {

        val currentBox = boxOfPoints.boxPoints[currentPoint]
        for (vertIndex in 1..7) {
            val otherBox = boxOfPoints
                .boxPoints
                .firstOrNull {
                    it.coord == currentBox
                        .coord
                        .getCoordForVertex(vertIndex)
                }
            otherBox?.modelInstance?.getMaterial("cube")?.set(
                PBRColorAttribute.createEmissive(
                    if (otherBox.on)
                        Color.RED else
                        Color.YELLOW
                )
            )
        }

        currentBox.modelInstance.getMaterial("cube")?.set(
            PBRColorAttribute.createEmissive(
                if (currentBox.on)
                    Color.PINK
                else Color.ORANGE
            )
        )
    }

    private fun updateCameraPosition() {
        info { "currentIndex = $currentPoint" }

        if (currentPoint > boxOfPoints.boxPoints.lastIndex)
            currentPoint = 0
        if (currentPoint < 0)
            currentPoint = boxOfPoints.boxPoints.lastIndex

        info { "currentIndex = $currentPoint" }
        turnOnColors()


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
        currentTarget.set(currentBox.x * 25f, currentBox.y * 25f, currentBox.z * 25f)
        camera.position.set(currentTarget + offset)
        camera.lookAt(currentTarget)
        camera.update()
    }

    private fun updatedModel() {
        /**
         * This is where the fun begins, I guess.
         */
    }
}
