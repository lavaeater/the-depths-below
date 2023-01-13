package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.physics.bullet.DebugDrawer
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw
import com.badlogic.gdx.utils.viewport.Viewport

class DebugRenderSystem3d(private val viewport: Viewport, private val bulletWorld: btCollisionWorld): EntitySystem() {
    val debugDrawer = DebugDrawer().apply {
        debugMode = btIDebugDraw.DebugDrawModes.DBG_DrawWireframe
        (bulletWorld as btDiscreteDynamicsWorld).debugDrawer = this
    }
    override fun update(deltaTime: Float) {
        debugDrawer.begin(viewport)
        bulletWorld!!.debugDrawWorld()
        debugDrawer.end()
    }

}
