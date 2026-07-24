package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import depth.ecs.components.*
import depth.marching.MarchingCubeBuilder
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
            "Toggle Points"
        ) {
            marchingCubeBuilder.togglePoints()
        }
        setUp(
            Keys.B,
            "Toggle Started"
        ) {
            marchingCubeBuilder.toggleStarted()
        }
        setUp(
            Keys.SPACE,
            "Update Model"
        ) {
            marchingCubeBuilder.updateModel()
        }
        setUp(
            Keys.U,
            "Move In"
        ) {
            marchingCubeBuilder.moveIn()
        }
        setUp(
            Keys.J,
            "Move Out"
        ) {
            marchingCubeBuilder.moveOut()
        }
        setUp(
            Keys.H,
            "Move Left"
        ) {
            marchingCubeBuilder.moveLeft()
        }
        setUp(
            Keys.L,
            "Move Right"
        ) {
            marchingCubeBuilder.moveRight()
        }
        setUp(
            Keys.Y,
            "Move Up"
        ) {
            marchingCubeBuilder.moveUp()
        }
        setUp(
            Keys.I,
            "Move Down"
        ) {
            marchingCubeBuilder.moveDown()
        }

        setUp(
            Keys.M,
            "Index Up"
        ) {
            marchingCubeBuilder.indexUp()
        }
        setUp(
            Keys.N,
            "Index Down"
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
            "Ascend",
            { controlComponent.remove(Direction.Up) },
            { controlComponent.add(Direction.Up) }
        )
        setBoth(
            Keys.DOWN,
            "Descend",
            { controlComponent.remove(Direction.Down) },
            { controlComponent.add(Direction.Down) }
        )
        setBoth(
            Keys.LEFT,
            "Yaw Left",
            { controlComponent.remove(Rotation.YawLeft) },
            { controlComponent.add(Rotation.YawLeft) }
        )
        setBoth(
            Keys.RIGHT,
            "Yaw right",
            { controlComponent.remove(Rotation.YawRight) },
            { controlComponent.add(Rotation.YawRight) }
        )
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

    override fun update(deltaTime: Float) {
        Gdx.input.inputProcessor = this
        super.update(deltaTime)
    }

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
