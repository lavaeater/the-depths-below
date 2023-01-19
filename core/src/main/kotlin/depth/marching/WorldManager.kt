package depth.marching

import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import depth.ecs.systems.MarchingCubeBuilder
import ktx.log.info
import net.mgsx.gltf.scene3d.scene.SceneManager
import java.awt.SystemColor.info

class WorldManager(
    private val marchingCubeBuilder: MarchingCubeBuilder,
    private val sceneManager: SceneManager,
    private val world: btDynamicsWorld
) {
    /**
     * Statically generate some more chunks to start off
     */

    private val chunks = mutableListOf<MarchingChunk>()
    fun generateChunks() {
        (-1..1).map { x ->
            (-1..1).map { y ->
                (-1..1).map { z ->
                    chunks.add(marchingCubeBuilder.buildChunk(x, y, z))
                }
            }
        }

        info { "Chunks: ${chunks.size}" }
        for (chunk in chunks) {
            sceneManager.addScene(chunk.scene)
            world.addRigidBody(chunk.rigidBody)
        }
    }
}
