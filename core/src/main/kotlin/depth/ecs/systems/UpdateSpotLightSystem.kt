package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.environment.SpotLight
import ktx.math.plus
import net.mgsx.gltf.scene3d.lights.PointLightEx

class UpdateSpotLightSystem(private val spotLight: PointLightEx, private val camera:PerspectiveCamera):EntitySystem() {
    override fun update(deltaTime: Float) {
        spotLight.setPosition(camera.position + camera.direction.cpy().scl(20f))
    }
}
