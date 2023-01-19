package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import depth.ecs.components.*
import depth.marching.*
import depth.marching.MarchingCubesTables
import depth.voxel.*
import eater.input.KeyPress
import eater.input.command
import ktx.app.KtxInputAdapter
import ktx.log.info
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
    private var currentCubeIndex = 0
    private val offset = vec3(-125f, 50f, -50f)
    private val currentTarget = vec3()

    private fun move(x: Int, y: Int, z: Int) {
        val newPoint = boxOfPoints.boxPoints[currentCubeIndex].coord.add(x, y, z)
        val potentialBox = boxOfPoints.boxPoints.firstOrNull { it.coord == newPoint }
        if (potentialBox != null) {
            turnColorsOff()
            currentCubeIndex = boxOfPoints.boxPoints.indexOf(potentialBox)
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
            updateModel()
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
        if (needsPoints) {
            needsPoints = false
            boxOfPoints.createPoints()
        } else {
            needsPoints = true
            boxOfPoints.destroyPoints()
        }
    }

    fun fixCubeIndex() {
        if (currentCubeIndex > boxOfPoints.boxPoints.lastIndex)
            currentCubeIndex = 0
        if (currentCubeIndex < 0)
            currentCubeIndex = boxOfPoints.boxPoints.lastIndex
    }

    private fun indexUp() {
        turnColorsOff()
        currentCubeIndex++
        fixCubeIndex()
    }

    private fun indexDown() {
        turnColorsOff()
        currentCubeIndex--
        fixCubeIndex()
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
        if (hasPoints) {
            val currentBox = boxOfPoints.boxPoints[currentCubeIndex]
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
        if (hasPoints) {

            val currentBox = boxOfPoints.boxPoints[currentCubeIndex]
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
        info { "currentIndex = $currentCubeIndex" }

        info { "currentIndex = $currentCubeIndex" }
        turnOnColors()


        val currentBox = boxOfPoints.boxPoints[currentCubeIndex]
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

    fun getOnOffCoord(coord: PointCoord): Map<Int, Boolean> {
        val something = (0..7).map { vertexIndex ->
            val newCoord = coord.coordForIndex(vertexIndex)
            var actualPoint = boxOfPoints.boxPoints.firstOrNull { it.coord == newCoord }
            if (actualPoint == null) {
                actualPoint = BoxPoint(
                    newCoord,
                    1.0f
                )
            }
            vertexIndex to actualPoint.on
        }
//        val something = PointCoord.vertexIndexToPointCoordinate.map { (index, c) ->
//            val newCoord = coord.add(c)
//            var actualPoint = boxOfPoints.boxPoints.firstOrNull { it.coord == newCoord }
//            if (actualPoint == null) {
//                actualPoint = BoxPoint(
//                    newCoord,
//                    Joiser.getValueFor(
//                        newCoord.x,
//                        newCoord.y,
//                        newCoord.z,
//                        boxOfPoints.numberOfPoints,
//                        boxOfPoints.numberOfPoints,
//                        boxOfPoints.numberOfPoints
//                    )
//                )
//            }
//            actualPoint!!.coord to actualPoint.on
//        }
        return something.toMap()
    }

    private val coolDown = 0.25f
    private var acc = 0f
    override fun update(deltaTime: Float) {
        if (started) {
            if (createPoints) {
                createPoints = false
                togglePoints()
            }
            acc += deltaTime
            if (acc > coolDown) {
                acc = 0f
                updateModel()
                indexUp()
                turnOnColors()
     //           updateCameraPosition()
            }
        }
    }

    val fromVector = vec3()
    val toVector = vec3()

    private fun updateModel() {
        /**
         * This is where the fun begins, I guess.
         *
         * Every point is actually the basis of a unique cube, so that's cool.
         *
         * Let's try and create the triangles for the current cube we are currently checking out.
         */
        cubesInModel.add(currentCubeIndex)
        val currentCube = boxOfPoints.boxPoints[currentCubeIndex]
        val currentCoord = currentCube.coord

//        if (
//            currentCoord.x in 1 until boxOfPoints.numberOfPoints &&
//            currentCoord.y in 1 until boxOfPoints.numberOfPoints &&
//            currentCoord.z in 1 until boxOfPoints.numberOfPoints
//        ) {

        info { "Current Index: $currentCubeIndex" }
        info { "Current coord: $currentCoord" }

        val vertValues = getOnOffCoord(currentCoord)
        if(vertValues.any { it.value }) {
            info { "Points that are ON" }
            info { vertValues.filterValues { it }.keys.toString() }
        }

        var marchingCubeIndex = 0
        for ((index, vertVal) in vertValues) {
            marchingCubeIndex =
                if (vertVal) marchingCubeIndex or 2.pow(index) else marchingCubeIndex
        }

        // Now get that cube!
        val sidesForTriangles = MarchingCubesTables.TRIANGLE_TABLE[marchingCubeIndex]
        val cubeBasePosition = vec3()
        val triangles = mutableListOf<Float>() //Can be transformed to floatarray by a badass
        /**
         * Make it blocky first, because of course blocky
         */
        cubeBasePosition.set(sideLength * currentCoord.x, sideLength * currentCoord.y, sideLength * currentCoord.z)
        for (triangleIndex in sidesForTriangles.indices.step(3)) {
            for (i in 0..2) { //per vertex in this particular triangle - and each vertex IS AN EDGE!
//                    val f = currentCoord.coordForIndex(MarchingCubesTables.EDGE_FIRST_VERTEX[sidesForTriangles[triangleIndex + i]])
//                    val t = currentCoord.coordForIndex(MarchingCubesTables.EDGE_SECOND_VERTEX[sidesForTriangles[triangleIndex + i]])
//                    // f and t are now the index for the triangle
//                    fromVector.set(f.x * sideLength, f.y * sideLength, f.z * sideLength)
//                    toVector.set(t.x * sideLength, t.y * sideLength, t.z * sideLength)

                val edge = MarchingCubesTables.EDGES[sidesForTriangles[triangleIndex + i]]

                val from = getVertex(cubeBasePosition, edge.first(), sideLength)

                val to = getVertex(cubeBasePosition, edge.last(), sideLength)

//                    fromVector.lerp(toVector, 0.5f)
//                    triangles.add(fromVector.x)
//                    triangles.add(fromVector.y)
//                    triangles.add(fromVector.z)

                from.lerp(to, 0.5f)
                triangles.add(from.x)
                triangles.add(from.y)
                triangles.add(from.z)
            }
        }

        val terrain = MarchingCubeTerrain(triangles.toTypedArray().toFloatArray(), 1f)
        sceneManager.addScene(Scene(terrain.modelInstance))
        if (cubesInModel.size >= boxOfPoints.boxPoints.size)
//                .boxPoints
//                .filter {
//                    it.coord.x in 1 until boxOfPoints.numberOfPoints &&
//                        it.coord.y in 1 until boxOfPoints.numberOfPoints &&
//                        it.coord.z in 1 until boxOfPoints.numberOfPoints
//                }.size
//        )
            started = false
    }
//    }


}
