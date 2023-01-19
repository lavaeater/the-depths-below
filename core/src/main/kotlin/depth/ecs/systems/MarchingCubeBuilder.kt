package depth.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import depth.ecs.components.MotionState
import depth.marching.*
import depth.marching.MarchingCubesTables
import depth.voxel.pow
import ktx.log.info
import ktx.math.vec3
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager

class MarchingCubeBuilder(
    private val sceneManager: SceneManager,
    private val dynamicsWorld: btDynamicsWorld,
    val numberOfPoints: Int,
    private var started: Boolean = true,
    private val useCooldown: Boolean = false,
    private val useUnlimitedNoise: Boolean = true,
    private val interactive: Boolean = false,
    private val useNoise: Boolean = true,
    private val useBox: Boolean = false
) {
    private var needsPoints = true
    private var currentCubeIndex = 0
    private val hasPoints get() = !needsPoints
    private val sideLength = 25f
    private val green = Color(0f, 1f, 0f, 0.5f)
    private val blue = Color(0f, 0f, 1f, 0.5f)
    private val coolDown = 0.05f
    private var acc = 0f
    private var createPoints = false
    private val cubesInModel = mutableSetOf<Int>()
    private val triangles = mutableListOf<Float>()
    private lateinit var model: Model
    private lateinit var modelInstance: ModelInstance
    private lateinit var scene: Scene

    private var boxOfPoints = if (useBox) BoxOfPoints(sceneManager, numberOfPoints, 0, 0, 0, useNoise) else BoxOfPoints(
        sceneManager,
        0,
        0,
        0,
        0,
        useNoise
    )


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
        if (useBox) {
            if (currentCubeIndex > boxOfPoints.boxPoints.lastIndex)
                currentCubeIndex = 0
            if (currentCubeIndex < 0)
                currentCubeIndex = boxOfPoints.boxPoints.lastIndex
        }
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
        if (hasPoints && useBox) {
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
        if (hasPoints && useBox) {

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
        return if (useUnlimitedNoise)
            (0..7).map { vertexIndex ->
                val newCoord = coord.coordForIndex(vertexIndex)
                if (useBox) {
                    var actualPoint = boxOfPoints.boxPoints.firstOrNull { it.coord == newCoord }
                    if (actualPoint == null) {
                        actualPoint = BoxPoint(
                            newCoord,
                            Joiser.getValueFor(
                                newCoord.x,
                                newCoord.y,
                                newCoord.z
                            )
                        )
                    }
                    vertexIndex to actualPoint!!.on
                } else {
                    val isoValue = Joiser.getValueFor(newCoord.x, newCoord.y, newCoord.z)
                    vertexIndex to (isoValue * 100000f < 55000f)
//                    * (1f + ((maxWorldSize - newCoord.y) / maxWorldSize))
                }
            }.toMap()
        else
            (0..7).map { vertexIndex ->
                val newCoord = coord.coordForIndex(vertexIndex)
                var actualPoint = boxOfPoints.boxPoints.firstOrNull { it.coord == newCoord }
                if (actualPoint == null) {
                    actualPoint = BoxPoint(
                        newCoord,
                        1.0f
                    )
                }
                vertexIndex to actualPoint!!.on
            }.toMap()

    }

    fun update(deltaTime: Float) {
        if (started) {
            if (useBox) {
                if (createPoints) {
                    createPoints = false
                    togglePoints()
                }
                if (useCooldown) {
                    acc += deltaTime
                    if (acc > coolDown) {
                        acc = 0f
                        updateModel()
                        indexUp()
                        turnOnColors()
                    }
                } else {
                    updateModel()
                    indexUp()
                    turnOnColors()
                }
            } else {
                updateModel()
            }
        }
    }

    private var hasRun = false

    fun updateModel() {
        if (useBox) {
            if (hasRun && interactive) {
                sceneManager.removeScene(scene)
            }
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

            val vertValues = getOnOffCoord(currentCoord)

            var marchingCubeIndex = 0
            for ((index, vertVal) in vertValues) {
                marchingCubeIndex =
                    if (vertVal) marchingCubeIndex or 2.pow(index) else marchingCubeIndex
            }

            // Now get that cube!
            val sidesForTriangles = MarchingCubesTables.TRIANGLE_TABLE[marchingCubeIndex]
            val cubeBasePosition = vec3()
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

            if (interactive) {
                val terrain = MarchingCubeTerrain(triangles.toTypedArray().toFloatArray(), 1f)
                modelInstance = terrain.modelInstance
                scene = Scene(modelInstance)
                model = modelInstance.model
                sceneManager.addScene(scene)
            }
            hasRun = true
//

            if (cubesInModel.size >= boxOfPoints.boxPoints.size) {
                info { "Totally done!" }
                if (!interactive) {
                    val terrain = MarchingCubeTerrain(triangles.toTypedArray().toFloatArray(), 1f)
                    modelInstance = terrain.modelInstance
                    scene = Scene(modelInstance)
                    model = modelInstance.model
                    sceneManager.addScene(scene)
                }
                started = false
//                val cShape: btCollisionShape = Bullet.obtainStaticNodeShape(modelInstance.model.nodes)
//                val motionState = MotionState().apply {
//                    transform = modelInstance.transform
//                }
//                val info = btRigidBody.btRigidBodyConstructionInfo(0f, motionState, cShape, Vector3.Zero)
//
//                dynamicsWorld.addRigidBody(btRigidBody(info))
            }
        } else {
            started = false
            val xOffset = -5
            val yOffset = -5
            val zOffset = -5
            val points = Array(numberOfPoints) { x ->
                Array(numberOfPoints) { y ->
                    Array(numberOfPoints) { z ->
                        PointCoord(x + xOffset, y + yOffset, z + zOffset)
                    }
                }.flatten()
            }.flatMap { it.asIterable() }

            for ((currentIndex, currentCoord) in points.withIndex()) {
                info { "Current Index: $currentIndex" }
                info { "Current Coord: $currentCoord" }

                val vertValues = getOnOffCoord(currentCoord)
                if (vertValues.values.any { it }) {
                    info { "ON: ${vertValues.filterValues { it }.keys}" }
                }

                var marchingCubeIndex = 0
                for ((index, vertVal) in vertValues) {
                    marchingCubeIndex =
                        if (vertVal) marchingCubeIndex or 2.pow(index) else marchingCubeIndex
                }

                // Now get that cube!
                val sidesForTriangles = MarchingCubesTables.TRIANGLE_TABLE[marchingCubeIndex]
                val cubeBasePosition = vec3()
                /**
                 * Make it blocky first, because of course blocky
                 */
                cubeBasePosition.set(
                    sideLength * currentCoord.x,
                    sideLength * currentCoord.y,
                    sideLength * currentCoord.z
                )
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
            }
            hasRun = true
            info { "Zero boxes, tons of indexes, all done, man!" }
            val terrain = MarchingCubeTerrain(triangles.toTypedArray().toFloatArray(), 1f)
            modelInstance = terrain.modelInstance
            scene = Scene(modelInstance)
            model = modelInstance.model
            sceneManager.addScene(scene)

            val cShape: btCollisionShape = Bullet.obtainStaticNodeShape(modelInstance.model.nodes)
            val motionState = MotionState().apply {
                transform = modelInstance.transform
            }
            val info = btRigidBody.btRigidBodyConstructionInfo(0f, motionState, cShape, Vector3.Zero)

            dynamicsWorld.addRigidBody(btRigidBody(info))
        }
    }

    fun buildChunk(chunkX: Int, chunkY: Int, chunkZ: Int): MarchingChunk {
        val points = Array(numberOfPoints) { x ->
            Array(numberOfPoints) { y ->
                Array(numberOfPoints) { z ->
                    PointCoord(x + chunkX * numberOfPoints, y + chunkY * numberOfPoints, z + chunkZ * numberOfPoints)
                }
            }.flatten()
        }.flatMap { it.asIterable() }

        val localTriangles = mutableListOf<Float>()
        for ((currentIndex, currentCoord) in points.withIndex()) {
            val vertValues = getOnOffCoord(currentCoord)
            var marchingCubeIndex = 0
            for ((index, vertVal) in vertValues) {
                marchingCubeIndex =
                    if (vertVal) marchingCubeIndex or 2.pow(index) else marchingCubeIndex
            }

            // Now get that cube!
            val sidesForTriangles = MarchingCubesTables.TRIANGLE_TABLE[marchingCubeIndex]
            val cubeBasePosition = vec3()
            /**
             * Make it blocky first, because of course blocky
             */
            cubeBasePosition.set(
                sideLength * currentCoord.x,
                sideLength * currentCoord.y,
                sideLength * currentCoord.z
            )
            for (triangleIndex in sidesForTriangles.indices.step(3)) {
                for (i in 0..2) { //per vertex in this particular triangle - and each vertex IS AN EDGE!

                    val edge = MarchingCubesTables.EDGES[sidesForTriangles[triangleIndex + i]]

                    val from = getVertex(cubeBasePosition, edge.first(), sideLength)

                    val to = getVertex(cubeBasePosition, edge.last(), sideLength)

                    from.lerp(to, 0.5f)
                    localTriangles.add(from.x)
                    localTriangles.add(from.y)
                    localTriangles.add(from.z)
                }
            }
        }
        info { "Chunk is DONE!" }
        val terrain = MarchingCubeTerrain(localTriangles.toTypedArray().toFloatArray(), 1f)
        return MarchingChunk(chunkX, chunkY, chunkZ, terrain.modelInstance)
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
