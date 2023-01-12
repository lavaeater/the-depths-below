package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.PerspectiveCamera
import ktx.math.plus
import net.mgsx.gltf.scene3d.lights.PointLightEx
import net.mgsx.gltf.scene3d.lights.SpotLightEx

class UpdatePointLightSystem(private val pointLight: PointLightEx, private val camera:PerspectiveCamera):EntitySystem() {
    override fun update(deltaTime: Float) {
        pointLight.setPosition(camera.position + camera.direction.cpy().scl(5f))
    }
}
