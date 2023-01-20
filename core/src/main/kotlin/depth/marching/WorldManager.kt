package depth.marching

import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import depth.ecs.components.MotionState
import depth.voxel.pow
import ktx.collections.GdxArray
import ktx.collections.toGdxArray
import ktx.log.info
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager
import kotlin.math.absoluteValue

class WorldManager(
    private val marchingCubeBuilder: MarchingCubeBuilder,
    private val sceneManager: SceneManager,
    private val world: btDynamicsWorld,
    private var someFactor: Int = 5,
    private val addCollisionBodies: Boolean = false
) {
    private val chunks = mutableListOf<MarchingChunk>()
    fun generateChunks(size: Int) {
        //This was an honest mistake, but a very cool one - I used power of 2 instead of just multiplying by 2!
        Joiser.numberOfPoints = someFactor.pow(2) * marchingCubeBuilder.numberOfPoints
//        Joiser.numberOfPoints = size.pow(2) * marchingCubeBuilder.numberOfPoints


        (-size until size).map { x ->
            (-size until size).map { y ->
                (-size until size).map { z ->
                    chunks.add(marchingCubeBuilder.buildChunk(x, y, z))
                }
            }
        }

        info { "Chunks: ${chunks.size}" }
        for (chunk in chunks) {
            sceneManager.addScene(chunk.scene)
            if (addCollisionBodies) {
                chunk.initRigidBody()
                world.addRigidBody(chunk.rigidBody)
            }
        }
    }

    fun buildIfNecessary() {
        if (chunksToBuild.any()) {
            val chunkToBuild =
                chunksToBuild.minByOrNull { (it.z - previousPlayerCoord.z + it.x - previousPlayerCoord.x).absoluteValue }!!
            chunksToBuild.remove(chunkToBuild)
            buildAndAddNewChunk(chunkToBuild)
        }
    }

    private var chunksBuilt = 0
    private var cutoff = 5
    private fun buildAndAddNewChunk(chunkCoord: PointCoord) {
        val chunk = marchingCubeBuilder.buildChunk(chunkCoord.x, chunkCoord.y, chunkCoord.z).apply { visible = false }
        chunksBuilt++
        chunks.add(chunk)
        sceneManager.addScene(chunk.scene)
        if (chunksBuilt > cutoff) {
            chunksBuilt = 0
            shouldUpdatedRenderables = true
        }
        if (addCollisionBodies) {
            chunk.initRigidBody()
            world.addRigidBody(chunk.rigidBody)
        }
    }

    private var previousChunkDirX = -1000
    private var previousChunkDirZ = -1000
    private var previousPlayerCoord = PointCoord(1000, 1000, 1000)
    private val chunkCoordsToShow = mutableSetOf<PointCoord>()
    var shouldUpdatedRenderables = true
        private set

    private var toAdd = GdxArray<Scene>()
    private var toRemove = GdxArray<Scene>()
    private val toAddAndToRemove = Pair(toRemove, toAdd)
    fun getScenesToRender(): Pair<GdxArray<Scene>, GdxArray<Scene>> {
        if (shouldUpdatedRenderables) {
            shouldUpdatedRenderables = false
            toRemove.clear()
            chunks.filter { chunk ->
                chunk.visible &&
                    chunkCoordsToShow.none {
                        it == chunk.chunkCoord
                    }
            }.forEach {
                toRemove.add(it.scene)
                it.visible = false
            }
            toAdd.clear()
            chunks.filter { chunk ->
                !chunk.visible &&
                    chunkCoordsToShow.any { it == chunk.chunkCoord }
            }.forEach {
                toAdd.add(it.scene)
                it.visible = true
            }
        }
        return toAddAndToRemove
    }

    private val chunksToBuild = mutableSetOf<PointCoord>()

    val xExtent = 4
    val yExtent = 4
    val zExtent = 8

    fun expandTheWorld(motionState: MotionState) {
        val position = motionState.position
        val forward = motionState.forward
        val chunkSize = chunks.first().worldSize
        var currentPlayerCoord = PointCoord(
            (position.x / chunkSize).toInt(),
            (position.y / chunkSize).toInt(),
            (position.z / chunkSize).toInt()
        )
        val chunkDirX = forward.x.toInt()
        val chunkDirZ = forward.z.toInt()
        if (
            currentPlayerCoord != previousPlayerCoord || //Have we left this chunk?
            chunkDirX != previousChunkDirX || // Are we facing another direction?
            chunkDirZ != previousChunkDirZ
        ) {
            info { "entering new chunk" }
            previousChunkDirX = chunkDirX
            previousChunkDirZ = chunkDirZ
            previousPlayerCoord = currentPlayerCoord

            chunkCoordsToShow.clear()
            shouldUpdatedRenderables = true

            //Do it 3 by 3 to start off
            for (x in -xExtent..xExtent) // Left-right
                for (y in -yExtent..yExtent) // Up-down
                    for (z in -zExtent..1) { // forwards only
                        val point = currentPlayerCoord.add(x + chunkDirX * x, y, z + z * chunkDirZ)
                        chunkCoordsToShow.add(point)
                    }

            /**
             * Check if all these coordinates exist in the list of chunks!
             */
            for (x in -(xExtent * 1.5f).toInt()..(xExtent * 1.5f).toInt()) // Left-right
                for (y in -(yExtent * 1.5f).toInt()..(yExtent * 1.5f).toInt()) // Up-down
                    for (z in -(zExtent*1.5f).toInt()..2) { // forwards only
                        val point = currentPlayerCoord.add(x + chunkDirX * x, y, z + z * chunkDirZ)
                        if (chunks.none {
                                it.chunkCoord == point
                            }) {
                            chunksToBuild.add(point)
                        }
                    }
        }
    }
}
