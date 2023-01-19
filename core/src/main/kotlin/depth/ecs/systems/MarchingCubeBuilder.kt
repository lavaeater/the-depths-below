package depth.ecs.systems

import com.badlogic.gdx.graphics.Color
import depth.marching.*
import depth.marching.MarchingCubesTables
import depth.voxel.pow
import ktx.log.info
import ktx.math.vec3
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager

class MarchingCubeBuilder(
    private val boxOfPoints: BoxOfPoints,
    private val sceneManager: SceneManager,
    var started: Boolean = false
    ) {
    private var needsPoints = true
    private var currentCubeIndex = 0
    private val hasPoints get() = !needsPoints
    private val sideLength = 25f
    private val green = Color(0f, 1f, 0f, 0.5f)
    private val blue = Color(0f, 0f, 1f, 0.5f)
    private val coolDown = 0.05f
    private var acc = 0f
    private var createPoints = true
    private val cubesInModel = mutableSetOf<Int>()

    fun toggleStarted() {
        started = !started
    }

    fun togglePoints() {
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

    fun indexUp() {
        turnColorsOff()
        currentCubeIndex++
        fixCubeIndex()
    }

    fun indexDown() {
        turnColorsOff()
        currentCubeIndex--
        fixCubeIndex()
    }


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

    private fun getOnOffCoord(coord: PointCoord): Map<Int, Boolean> {
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

    fun update(deltaTime: Float) {
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


    fun updateModel() {
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

                val edge = MarchingCubesTables.EDGES[sidesForTriangles[triangleIndex + i]]

                val from = getVertex(cubeBasePosition, edge.first(), sideLength)

                val to = getVertex(cubeBasePosition, edge.last(), sideLength)

                from.lerp(to, 0.5f)
                triangles.add(from.x)
                triangles.add(from.y)
                triangles.add(from.z)
            }
        }

        val terrain = MarchingCubeTerrain(triangles.toTypedArray().toFloatArray(), 1f)
        sceneManager.addScene(Scene(terrain.modelInstance))
        if (cubesInModel.size >= boxOfPoints.boxPoints.size)
            started = false
    }

    fun move(x: Int, y: Int, z: Int) {
        val newPoint = boxOfPoints.boxPoints[currentCubeIndex].coord.add(x, y, z)
        val potentialBox = boxOfPoints.boxPoints.firstOrNull { it.coord == newPoint }
        if (potentialBox != null) {
            turnColorsOff()
            currentCubeIndex = boxOfPoints.boxPoints.indexOf(potentialBox)
        }
    }

    fun moveLeft() {
        move(-1, 0, 0)
    }

    fun moveRight() {
        move(1, 0, 0)

    }

    fun moveUp() {
        move(0, 1, 0)

    }

    fun moveDown() {
        move(0, -1, 0)
    }

    fun moveIn() {
        move(0, 0, -1)
    }

    fun moveOut() {
        move(0, 0, 1)
    }
}
