package depth.marching

import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import depth.ecs.systems.MarchingCubeBuilder
import depth.voxel.pow
import ktx.log.info
import net.mgsx.gltf.scene3d.scene.SceneManager
import java.awt.SystemColor.info

class WorldManager(
    private val marchingCubeBuilder: MarchingCubeBuilder,
    private val sceneManager: SceneManager,
    private val world: btDynamicsWorld,
    private val addCollisionBodies: Boolean = false
) {
    /**
     * Statically generate some more chunks to start off
     */

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
            if(addCollisionBodies) {
                chunk.initRigidBody()
                world.addRigidBody(chunk.rigidBody)
            }
        }
    }
}
