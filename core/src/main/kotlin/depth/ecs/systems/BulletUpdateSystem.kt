package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld

class BulletUpdateSystem(private val world:btDynamicsWorld):EntitySystem() {
    override fun update(deltaTime: Float) {
        world.stepSimulation(deltaTime)
    }
}
