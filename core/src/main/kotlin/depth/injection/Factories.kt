package depth.injection

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import depth.ecs.components.*
import eater.core.engine
import eater.injection.InjectionContext.Companion.inject
import ktx.ashley.entity
import ktx.ashley.with
import ktx.math.vec3
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager

fun createSubMarine() {
    val submarineScene = Scene(assets().submarine.scene).apply {
        this.modelInstance.transform.setToWorld(
            vec3(50f, 100f, -50f), Vector3.X, Vector3.Y
        )
    }

    val compound = btCompoundShape(true, 2).apply {
        addChildShape(Matrix4().rotate(Quaternion().setEulerAngles(90f, 90f, 0f)).setTranslation(-0.2f,.75f,0f), btCylinderShape(vec3(0.6f, 1f, 1f)))
    }

    val motionState = MotionState(submarineScene.modelInstance.transform)
    val info = btRigidBody.btRigidBodyConstructionInfo(10f, motionState, compound, Vector3.Zero)
    val submarineBody = btRigidBody(info)
    submarineBody.angularFactor = Vector3.Y
    inject<btDynamicsWorld>().addRigidBody(submarineBody)

    engine().entity {
        with<SceneComponent> {
            scene =submarineScene
            inject<SceneManager>().addScene(submarineScene)
        }
        with<Transform3d>()
        with<Camera3dFollowComponent> {
            offset.set(5f, 2.5f, 5f)
        }
        with<KeyboardControlComponent>()
        with<BulletRigidBody> {
            rigidBody = submarineBody
        }
    }
}
