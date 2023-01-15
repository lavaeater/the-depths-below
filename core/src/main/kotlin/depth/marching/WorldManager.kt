package depth.marching

import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import net.mgsx.gltf.scene3d.scene.SceneManager

class WorldManager(
    private val world: btDynamicsWorld,
    private val sceneManager: SceneManager
) {
    /**
     * Statically generate some more chunks to start off
     */

    private val chunks = mutableListOf<MarchingChunk>()
    fun generateChunks() {
        (-1..1).map { x ->
            (-1..1).map { y ->
                (-1..1).map { z ->
                    val chunk = MarchingChunk(x,y,z,4,25f)
                    chunks.add(chunk)
                }
            }
        }
        for(chunk in chunks) {
            sceneManager.addScene(chunk.scene)
            world.addRigidBody(chunk.rigidBody)
        }
    }
}
