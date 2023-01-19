package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.systems.IntervalSystem
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.collision.BoundingBox
import depth.ecs.components.Camera3dFollowComponent
import depth.ecs.components.MotionState
import depth.marching.WorldManager
import ktx.ashley.allOf
import ktx.log.info
import ktx.math.vec3

class UpdateChunkSystem(
    private val worldManager: WorldManager,
    private val camera: PerspectiveCamera
) : IteratingSystem(allOf(Camera3dFollowComponent::class, MotionState::class).get()) {
    private val playerPosition = vec3()
    private val playerOrientation = vec3()
    override fun update(deltaTime: Float) {
//        for (chunk in worldManager.chunks) {
//            if (anyPointInFrustum(chunk.boundingBox)) {
//                worldManager.showChunk(chunk)
//            } else {
//                worldManager.hideChunk(chunk)
//            }
//        }
    }

    override fun processEntity(entity: Entity?, deltaTime: Float) {
        TODO("Not yet implemented")
    }

    private val tmpVector = vec3()
    private fun anyPointInFrustum(boundingBox: BoundingBox): Boolean {
//        info { boundingBox.toString() }
        return camera.frustum.boundsInFrustum(boundingBox)

//        val visible = camera.frustum.pointInFrustum(boundingBox.getCenter(tmpVector)) ||
//            camera.frustum.pointInFrustum(boundingBox.getCorner001(tmpVector)) ||
//            camera.frustum.pointInFrustum(boundingBox.getCorner010(tmpVector)) ||
//            camera.frustum.pointInFrustum(boundingBox.getCorner100(tmpVector)) ||
//            camera.frustum.pointInFrustum(boundingBox.getCorner101(tmpVector)) ||
//            camera.frustum.pointInFrustum(boundingBox.getCorner110(tmpVector)) ||
//            camera.frustum.pointInFrustum(boundingBox.getCorner111(tmpVector)) ||
//            camera.frustum.pointInFrustum(boundingBox.getCorner000(tmpVector)) ||
//            camera.frustum.pointInFrustum(boundingBox.getCorner011(tmpVector))
//        return visible
    }

}
