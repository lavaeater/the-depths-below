package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Input.Keys
import depth.ecs.components.*
import depth.marching.BoxOfPoints
import eater.input.KeyPress
import eater.input.command
import ktx.app.KtxInputAdapter
import ktx.ashley.allOf
import ktx.math.vec3


class SubmarineControlSystem(
    private val marchingCubeBuilder: MarchingCubeBuilder,
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

    private val controlMap = command("Controoool") {
        setUp(
            Keys.P,
            "Move In"
        ) {
            marchingCubeBuilder.togglePoints()
        }
        setUp(
            Keys.B,
            "Move In"
        ) {
            marchingCubeBuilder.toggleStarted()
        }
        setUp(
            Keys.SPACE,
            "Move In"
        ) {
            marchingCubeBuilder.updateModel()
        }
        setUp(
            Keys.W,
            "Move In"
        ) {
            marchingCubeBuilder.moveIn()
        }
        setUp(
            Keys.S,
            "Move Out"
        ) {
            marchingCubeBuilder.moveOut()
        }
        setUp(
            Keys.A,
            "Move Left"
        ) {
            marchingCubeBuilder.moveLeft()
        }
        setUp(
            Keys.D,
            "Move Right"
        ) {
            marchingCubeBuilder.moveRight()
        }
        setUp(
            Keys.UP,
            "Move Up"
        ) {
            marchingCubeBuilder.moveUp()
        }
        setUp(
            Keys.DOWN,
            "Move DOwn"
        ) {
            marchingCubeBuilder.moveDown()
        }

        setUp(
            Keys.RIGHT,
            "Move Up"
        ) {
            marchingCubeBuilder.indexUp()
        }
        setUp(
            Keys.LEFT,
            "Move DOwn"
        ) {
            marchingCubeBuilder.indexDown()
        }
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
            { controlComponent.remove(Direction.Left) },
            { controlComponent.add(Direction.Left) }
        )
        setBoth(
            Keys.D,
            "Right",
            { controlComponent.remove(Direction.Right) },
            { controlComponent.add(Direction.Right) }
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
        setBoth(
            Keys.LEFT,
            "Up",
            { controlComponent.remove(Rotation.YawLeft) },
            { controlComponent.add(Rotation.YawLeft) }
        )
        setBoth(
            Keys.RIGHT,
            "Down",
            { controlComponent.remove(Rotation.YawRight) },
            { controlComponent.add(Rotation.YawRight) }
        )
        setUp(
            Keys.P, "Toggle points"
        ) {
            marchingCubeBuilder.togglePoints()
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Down)
    }

    override fun keyUp(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Up)
    }

    private val forceFactor = 10f
    private val torqueFactor = 0.1f
    private val tmpVector = vec3()
    private val centralForce = vec3()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val rigidBody = BulletRigidBody.get(entity).rigidBody
        val motionState = MotionState.get(entity)

        if (controlComponent.has(Rotation.YawLeft)) {
            rigidBody.applyTorqueImpulse(vec3(0f, torqueFactor, 0f))
        }

        if (controlComponent.has(Rotation.YawRight)) {
            rigidBody.applyTorqueImpulse(vec3(0f, -torqueFactor, 0f))
        }

        centralForce.setZero()
        if (controlComponent.has(Direction.Left)) {
            tmpVector.setZero()
            tmpVector.set(motionState.right).scl(forceFactor)
            centralForce.add(tmpVector)
        }
        if (controlComponent.has(Direction.Right)) {
            tmpVector.setZero()
            tmpVector.set(motionState.left).scl(forceFactor)
            centralForce.add(tmpVector)
        }
        if (controlComponent.has(Direction.Up)) {
            tmpVector.setZero()
            tmpVector.set(motionState.up).scl(forceFactor)
            centralForce.add(tmpVector)
        }
        if (controlComponent.has(Direction.Down)) {
            tmpVector.setZero()
            tmpVector.set(motionState.down).scl(forceFactor)
            centralForce.add(tmpVector)
        }
        if (controlComponent.has(Direction.Forward)) {
            tmpVector.setZero()
            tmpVector.set(motionState.forward).scl(forceFactor)
            centralForce.add(tmpVector)
        }
        if (controlComponent.has(Direction.Reverse)) {
            tmpVector.setZero()
            tmpVector.set(motionState.backwards).scl(forceFactor)
            centralForce.add(tmpVector)
        }
        rigidBody.applyCentralImpulse(centralForce)
    }
}
