package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.math.Vector3
import depth.ecs.components.*
import eater.input.KeyPress
import eater.input.command
import ktx.app.KtxInputAdapter
import ktx.ashley.allOf
import ktx.math.plus
import ktx.math.times
import ktx.math.vec3


class SubmarineControlSystem(
    private val boxOfPoints: BoxOfPoints
) :
    IteratingSystem(
        allOf(
            KeyboardControlComponent::class,
            MotionState::class,
            SceneComponent::class
        ).get()
    ),
    KtxInputAdapter {
    private val family = allOf(KeyboardControlComponent::class).get()

    private val controlledEntity get() = engine.getEntitiesFor(family).first()

    private val controlComponent get() = KeyboardControlComponent.get(controlledEntity)
    private val speed = 50f

    private val controlMap = command("Controoool") {
        setBoth(
            Keys.W,
            "Throttle F",
            { controlComponent.remove(Direction.Forward) },
            { controlComponent.add(Direction.Forward) }
        )
        setBoth(
            Keys.S,
            "Throttle R",
            { controlComponent.remove(Direction.Reverse) },
            { controlComponent.add(Direction.Reverse) }
        )
        setBoth(
            Keys.A,
            "Left",
            { controlComponent.remove(Rotation.YawLeft) },
            { controlComponent.add(Rotation.YawLeft) }
        )
        setBoth(
            Keys.D,
            "Right",
            { controlComponent.remove(Rotation.YawRight) },
            { controlComponent.add(Rotation.YawRight) }
        )
        setBoth(
            Keys.UP,
            "Up",
            { controlComponent.remove(Direction.Up) },
            { controlComponent.add(Direction.Up) }
        )
        setBoth(
            Keys.DOWN,
            "Down",
            { controlComponent.remove(Direction.Down) },
            { controlComponent.add(Direction.Down) }
        )
        setUp(
            Keys.P,"Toggle points") {
         boxOfPoints.togglePoints()
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Down)
    }

    override fun keyUp(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Up)
    }

    private val forceFactor = 10f
    private val torqueFactor = 1f
    private val tmpVector = vec3()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val rigidBody = BulletRigidBody.get(entity).rigidBody
        val motionState = MotionState.get(entity)

        if (controlComponent.has(Rotation.YawLeft)) {
            rigidBody.applyTorqueImpulse(vec3(0f, torqueFactor, 0f))
        }

        if (controlComponent.has(Rotation.YawRight)) {
            rigidBody.applyTorqueImpulse(vec3(0f, -torqueFactor, 0f))
        }

        val centralForce = vec3()
        if (controlComponent.has(Direction.Up)) {
            centralForce.set(centralForce.x, forceFactor, centralForce.z)
        }
        if (controlComponent.has(Direction.Down)) {
            centralForce.set(centralForce.x, -forceFactor, centralForce.z)
        }
        if (controlComponent.has(Direction.Forward)) {
            tmpVector.set(motionState.forward).scl(forceFactor)
            centralForce.set(tmpVector.x, centralForce.y, tmpVector.z)
        }
        if (controlComponent.has(Direction.Reverse)) {
            tmpVector.set(motionState.backwards).scl(forceFactor)
            centralForce.set(tmpVector.x, centralForce.y, tmpVector.z)
        }
        rigidBody.applyCentralImpulse(centralForce)
    }
}
