package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import depth.ecs.components.*
import depth.marching.MarchingCubesTables
import depth.voxel.Joiser
import depth.voxel.MarchingCubeTerrain
import depth.voxel.getVertex
import depth.voxel.pow
import eater.input.KeyPress
import eater.input.command
import ktx.app.KtxInputAdapter
import ktx.log.info
import ktx.math.plus
import ktx.math.vec3
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager


class KeyboardControlSystem(
    private val boxOfPoints: BoxOfPoints,
    private val sceneManager: SceneManager,
    private var started: Boolean,
    private var createPoints: Boolean
) :
    EntitySystem(),
    KtxInputAdapter {
    private var dTime = 0f
    private val controlComponent = DirectionThing()
    private var currentPoint = 0
    private val offset = vec3(-125f, 50f, -50f)
    private val currentTarget = vec3()

    private fun move(x: Int, y: Int, z: Int) {
        val newPoint = boxOfPoints.boxPoints[currentPoint].coord.add(x, y, z)
        val potentialBox = boxOfPoints.boxPoints.firstOrNull { it.coord == newPoint }
        if (potentialBox != null) {
            turnColorsOff()
            currentPoint = boxOfPoints.boxPoints.indexOf(potentialBox)
            updateCameraPosition()
        }
    }

    private fun moveLeft() {
        move(-1, 0, 0)
    }

    private fun moveRight() {
        move(1, 0, 0)

    }

    private fun moveUp() {
        move(0, 1, 0)

    }

    private fun moveDown() {
        move(0, -1, 0)
    }

    private fun moveIn() {
        move(0, 0, -1)
    }

    private fun moveOut() {
        move(0, 0, 1)
    }

    private val cubesInModel = mutableSetOf<Int>()

    private val controlMap = command("Controoool") {
        setUp(
            Keys.P,
            "Move In"
        ) {
            togglePoints()
        }
        setUp(
            Keys.B,
            "Move In"
        ) {
            started = !started
        }
        setUp(
            Keys.SPACE,
            "Move In"
        ) {
            updatedModel()
        }
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

    private var needsPoints = true
    private val hasPoints get() = !needsPoints
    private fun togglePoints() {
        if(needsPoints) {
            needsPoints = false
            boxOfPoints.createPoints()
        } else {
            needsPoints = true
            boxOfPoints.destroyPoints()
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
        if(hasPoints) {
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
    }

    private fun turnOnColors() {
        if(hasPoints) {

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
    }

    private val sideLength = 25f

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
        currentTarget.set(currentBox.x * sideLength, currentBox.y * sideLength, currentBox.z * sideLength)
//        camera.position.set(currentTarget + offset)
//        camera.lookAt(currentTarget)
//        camera.update()
    }

    fun getOnOffCoord(coord: PointCoord): Map<PointCoord, Boolean> {
        val something = PointCoord.vertexIndexToPointCoordinate.map { (index, c) ->
            val newCoord = coord.add(c)
            var actualPoint = boxOfPoints.boxPoints.firstOrNull { it.coord == newCoord }
            if (actualPoint == null) {
                actualPoint = BoxPoint(
                    newCoord,
                    Joiser.getValueFor(
                        newCoord.x,
                        newCoord.y,
                        newCoord.z,
                        boxOfPoints.numberOfPoints,
                        boxOfPoints.numberOfPoints,
                        boxOfPoints.numberOfPoints
                    )
                )
            }
            actualPoint!!.coord to actualPoint.on
        }
        return something.toMap()
    }

    val coolDown = 0.1f
    var acc = 0f
    override fun update(deltaTime: Float) {
        if (started) {
            if(createPoints) {
                createPoints = false
                togglePoints()
            }
//            acc += deltaTime
//            if (acc > coolDown) {
                acc = 0f
                updatedModel()
                turnColorsOff()
                currentPoint++
                updateCameraPosition()
            }
//        }
    }

    private fun updatedModel() {
        /**
         * This is where the fun begins, I guess.
         *
         * Every point is actually the basis of a unique cube, so that's cool.
         *
         * Let's try and create the triangles for the current cube we are currently checking out.
         */
        cubesInModel.add(currentPoint)
        val currentCube = boxOfPoints.boxPoints[currentPoint]
        val currentCoord = currentCube.coord
        val vertValues = getOnOffCoord(currentCoord)

        var marchingCubeIndex = 0
        for ((index, vertVal) in vertValues.values.withIndex()) {
            marchingCubeIndex =
                if (vertVal) marchingCubeIndex or 2.pow(index) else marchingCubeIndex
        }

        // Now get that cube!
        val sidesForTriangles = MarchingCubesTables.TRIANGLE_TABLE[marchingCubeIndex]
        val v0 = vec3()
        val triangles = mutableListOf<Float>() //Can be transformed to floatarray by a badass
        /**
         * Make it blocky first, because of course blocky
         */
        v0.set(sideLength * currentCoord.x, sideLength * currentCoord.y, sideLength * currentCoord.z)
        for (triangleIndex in sidesForTriangles.indices.step(3)) {
            for (i in 0..2) { //per vertex in this particular triangle
                val edge = MarchingCubesTables.EDGES[sidesForTriangles[triangleIndex + i]]
                val from = getVertex(v0, edge.first(), sideLength)
                val to = getVertex(v0, edge.last(), sideLength)
                from.lerp(to, 0.5f)
                triangles.add(from.x)
                triangles.add(from.y)
                triangles.add(from.z)
            }
        }

        val terrain = MarchingCubeTerrain(triangles.toTypedArray().toFloatArray(), 1f)
        sceneManager.addScene(Scene(terrain.modelInstance))
        if(cubesInModel.size >= boxOfPoints.boxPoints.size)
            started = false
    }


}
