package depth.marching

import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import depth.ecs.components.MotionState
import depth.voxel.pow
import ktx.collections.GdxArray
import ktx.collections.toGdxArray
import ktx.log.info
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager

class WorldManager(
    private val marchingCubeBuilder: MarchingCubeBuilder,
    private val sceneManager: SceneManager,
    private val world: btDynamicsWorld,
    private val addCollisionBodies: Boolean = false
) {
    private val chunks = mutableListOf<MarchingChunk>()
    fun generateChunks(size: Int) {
        //This was an honest mistake, but a very cool one - I used power of 2 instead of just multiplying by 2!
        Joiser.numberOfPoints = size.pow(2) * marchingCubeBuilder.numberOfPoints


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

    fun buildAndAddNewChunk(chunkCoord: PointCoord) {
        info { "Build new chunk at $chunkCoord" }
        shouldUpdatedRenderables = true
        val chunk = marchingCubeBuilder.buildChunk(chunkCoord.x, chunkCoord.y, chunkCoord.z).apply { visible = false }
        chunks.add(chunk)
        sceneManager.addScene(chunk.scene)
        if (addCollisionBodies) {
            chunk.initRigidBody()
            world.addRigidBody(chunk.rigidBody)
        }
    }

    private var previousChunkDirX = -1000
    private var previousChunkDirZ = -1000
    private var previousPlayerCoord = PointCoord(1000,1000,1000)
    private val chunkCoordsToShow = mutableSetOf<PointCoord>()
    var shouldUpdatedRenderables = true
        private set

    private var toAdd = GdxArray<Scene>()
    private var toRemove = GdxArray<Scene>()
    private val toAddAndToRemove = Pair(toRemove, toAdd)
    fun getScenesToRender(): Pair<GdxArray<Scene>,GdxArray<Scene>> {
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
                chunkCoordsToShow.any { it == chunk.chunkCoord } }.forEach {
                toAdd.add(it.scene)
                it.visible = true }
        }
        return toAddAndToRemove
    }

    val chunksToBuild = mutableSetOf<PointCoord>()

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
            for (x in -5..5) // Left-right
                for (y in -3..3) // Up-down
                    for (z in -15..2) { // forwards only
                        val point = currentPlayerCoord.add(x + chunkDirX * x, y, z + z * chunkDirZ)
                        chunkCoordsToShow.add(point)
                    }

            /**
             * Check if all these coordinates exist in the list of chunks!
             */
            var i = 0
            chunkCoordsToShow.filter { pointCoord ->
                chunks.none {
                    it.chunkCoord == pointCoord
                }
            }.forEach {
                chunksToBuild.add(it)
            }
        }

        /**
         * We should simply create a cube in front of the player:
         * From the back:
         *
         * CCCCC
         * CCPCC
         * CCCCC
         *
         * From the side
         *
         * CCC
         * CCP
         * CCC
         *
         * To begin with, then we can figure out the rest later. This means
         * that we will render 2 chunks in front of  the player, all we have to do is figure out what
         * "in front of the player" means.
         *
         * So, we shall construct a min-max type of thing of what cubes we need to have. it will emanate from
         * the x and z of the direction
         */


    }

    fun hideChunk(chunk: MarchingChunk) {
        if (chunk.visible) {
            sceneManager.removeScene(chunk.scene)
            chunk.visible = false
        }
    }

    fun showChunk(chunk: MarchingChunk) {
        if (chunk.hidden) {
            sceneManager.addScene(chunk.scene)
            chunk.visible = true
        }
    }
}
