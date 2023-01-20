package depth.injection

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import depth.ecs.components.*
import eater.core.engine
import eater.injection.InjectionContext.Companion.inject
import ktx.ashley.entity
import ktx.ashley.with
import ktx.math.vec3
import net.mgsx.gltf.scene3d.lights.PointLightEx
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager

fun createSubMarine() {
    val submarineScene = Scene(assets().submarine.scene).apply {
        this.modelInstance.transform.setToWorld(
            vec3(50f, 100f, -50f), Vector3.Z, Vector3.Y
        )
    }

    val submarineShape = btCompoundShape(true, 2).apply {
        addChildShape(
            Matrix4().rotate(Quaternion().setEulerAngles(0f, 90f, 0f)).setTranslation(-0.2f, .75f, 0f),
            btCylinderShape(vec3(0.6f, 1f, 1f))
        )
    }
    val localInertia = vec3()

    engine().entity {
        with<VisibleComponent>()
        with<SceneComponent> {
            scene = submarineScene
            inject<SceneManager>().addScene(submarineScene)
        }
        with<Camera3dFollowComponent> {
            offset.set(5f, 5f, 0f)
        }
        with<KeyboardControlComponent>()
        lateinit var motionState: MotionState
        with<MotionState> {
            motionState = this
            transform = submarineScene.modelInstance.transform
        }
        with<BulletRigidBody> {
            submarineShape.calculateLocalInertia(10f, localInertia)
            val info = btRigidBody.btRigidBodyConstructionInfo(10f, motionState, submarineShape, localInertia)
            val submarineBody = btRigidBody(info).apply {
                setDamping(0.5f, 0.5f)
                angularFactor = Vector3.Y
            }
            rigidBody = submarineBody
            inject<btDynamicsWorld>().addRigidBody(submarineBody)
        }
        with<PointLightComponent> {
            offset.set(0f, 0f, 10f)
            pointLightEx = PointLightEx().apply {
                setColor(Color(.5f, .5f, 1f, 1f))
                setIntensity(50000f)
                inject<SceneManager>().environment.add(this)
            }

        }
    }
}
