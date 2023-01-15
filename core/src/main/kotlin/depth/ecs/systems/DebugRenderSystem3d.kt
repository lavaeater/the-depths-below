package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.bullet.DebugDrawer
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw
import com.badlogic.gdx.utils.viewport.Viewport
import depth.ecs.components.MotionState
import ktx.ashley.allOf
import ktx.math.plus
import ktx.math.vec3

class DebugRenderSystem3d(private val viewport: Viewport, private val bulletWorld: btDynamicsWorld) : IteratingSystem(
    allOf(
        MotionState::class
    ).get()
) {
    val debugDrawer = DebugDrawer().apply {
        debugMode = btIDebugDraw.DebugDrawModes.DBG_DrawWireframe
        bulletWorld.debugDrawer = this
    }

    private val forwardColor = vec3(0f, 0f, 1f)
    private val upColor = vec3(0f, 1f, 0f)
    private val rightColor = vec3(1f, 0f, 0f)

    override fun update(deltaTime: Float) {
        debugDrawer.begin(viewport)
        bulletWorld!!.debugDrawWorld()
        super.update(deltaTime)
        debugDrawer.end()
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val motionState = MotionState.get(entity)
        //Draw the normals!

        debugDrawer.drawLine(motionState.position, motionState.position + motionState.forward.cpy().scl(5f), forwardColor)
        debugDrawer.drawLine(motionState.position, motionState.position + motionState.up.cpy().scl(5f), upColor)
        debugDrawer.drawLine(motionState.position, motionState.position + motionState.right.cpy().scl(5f), rightColor)
    }
}
