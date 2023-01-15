package depth.marching

import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import ktx.log.info
import net.mgsx.gltf.scene3d.scene.SceneManager
import java.awt.SystemColor.info

class WorldManager(
    private val world: btDynamicsWorld,
    private val sceneManager: SceneManager
) {
    /**
     * Statically generate some more chunks to start off
     */

    private val chunks = mutableListOf<MarchingChunk>()
    fun generateChunks() {
        (-2..2).map { x ->
            (-2..2).map { y ->
                (-2..2).map { z ->
                    val chunk = MarchingChunk(x, y, z, 8, 50f)
                    chunks.add(chunk)
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
