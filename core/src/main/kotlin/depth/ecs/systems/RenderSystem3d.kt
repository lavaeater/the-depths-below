package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.Cubemap
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import depth.ecs.components.SceneComponent
import depth.voxel.BlockManager
import depth.voxel.HeightMapTerrain
import ktx.ashley.allOf
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.SceneSkybox


class RenderSystem3d(
    private val camera: PerspectiveCamera,
    private val blockManager: BlockManager,
    private val sceneManager: SceneManager,

): EntitySystem() {
//    private val sceneAsset: SceneAsset? = null
//    private val scene: Scene? = null
    private val diffuseCubemap: Cubemap? = null
    private val environmentCubemap: Cubemap? = null
    private val specularCubemap: Cubemap? = null
//    private val brdfLUT: Texture? = null
    private val time = 0f
    private val skybox: SceneSkybox? = null
    private val light: DirectionalLightEx? = null

    private val modelBatch = ModelBatch()
    private val environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.1f, 0.1f, 0.4f, 1f))
        add(DirectionalLight().apply {
            set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f)
        })
        add(DirectionalLight().apply {
            set(0.2f, 0.8f, 0.8f, 1f, 0.8f, 0.2f)
        })
        add(DirectionalLight().apply {
            set(0.7f, 0.2f, 0.2f, 0f, 0.8f, 0.2f)
        })
    }

//    private val sceneFamily = allOf(SceneComponent::class).get()
    override fun update(deltaTime: Float) {
        renderScenes(deltaTime)
    }

    private val terrain = HeightMapTerrain(1000, 2000f, 15f)
    init {
        sceneManager.addScene(Scene(terrain.modelInstance))
    }

    private fun renderScenes(deltaTime: Float) {
        sceneManager.update(deltaTime)
        sceneManager.render()
    }
}
