package depth.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import depth.ecs.components.Direction
import depth.ecs.components.KeyboardControlComponent
import depth.ecs.components.SceneComponent
import depth.ecs.components.Transform3d
import eater.input.KeyPress
import eater.input.command
import ktx.app.KtxInputAdapter
import ktx.ashley.allOf
import ktx.math.plus
import ktx.math.times
import ktx.math.vec3


class SubmarineControlSystem :
    IteratingSystem(
        allOf(
            KeyboardControlComponent::class,
            SceneComponent::class).get()),
    KtxInputAdapter {
    private val family = allOf(KeyboardControlComponent::class).get()

    private val controlledEntity get() = engine.getEntitiesFor(family).first()

    private val controlComponent get() = KeyboardControlComponent.get(controlledEntity)
    private val speed = 50f

    private val controlMap = command("Controoool") {
        setBoth(
            Keys.W,
            "Throttle F",
            { controlComponent.throttle = Direction.Neutral },
            {
                controlComponent.throttle = Direction.Forward
            }
        )
        setBoth(
            Keys.S,
            "Throttle R",
            { controlComponent.throttle = Direction.Neutral },
            { controlComponent.throttle = Direction.Reverse }
        )
        setBoth(
            Keys.A,
            "Left",
            { controlComponent.horizontal = Direction.Neutral },
            { controlComponent.horizontal = Direction.Left }
        )
        setBoth(
            Keys.D,
            "Right",
            { controlComponent.horizontal = Direction.Neutral },
            { controlComponent.horizontal = Direction.Right }
        )
        setBoth(
            Keys.UP,
            "Up",
            { controlComponent.vertical = Direction.Neutral },
            { controlComponent.vertical = Direction.Up }
        )
        setBoth(
            Keys.DOWN,
            "Down",
            { controlComponent.vertical = Direction.Neutral },
            { controlComponent.vertical = Direction.Down }
        )
    }

    override fun keyDown(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Down)
    }

    override fun keyUp(keycode: Int): Boolean {
        return controlMap.execute(keycode, KeyPress.Up)
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val sc = SceneComponent.get(entity).scene
        val tf = Transform3d.get(entity)
        val position = tf.position
        when(controlComponent.horizontal) {
            Direction.Left -> {
                tf.forward.rotate(Vector3.Y, speed * deltaTime)
                sc.modelInstance.transform.rotate(Vector3.Y, speed * deltaTime)
            }
            Direction.Right -> {
                tf.forward.rotate(Vector3.Y, -speed * deltaTime)
                sc.modelInstance.transform.rotate(Vector3.Y, -speed * deltaTime)
            }
            else -> {}
        }
        when(controlComponent.vertical) {
            Direction.Up -> {
                sc.modelInstance.transform.setTranslation(position + vec3(0f,speed*deltaTime,0f) )
            }
            Direction.Down -> {
                sc.modelInstance.transform.setTranslation(position + vec3(0f,-speed*deltaTime,0f) )
            }
            else -> {}
        }
        when(controlComponent.throttle) {
            Direction.Forward -> {
                sc.modelInstance.transform.setTranslation(position + tf.forward * (speed * deltaTime))
            }
            Direction.Reverse -> {
                sc.modelInstance.transform.setTranslation(position + tf.forward * (-speed * deltaTime))
            }
            else -> {}
        }
    }
}
