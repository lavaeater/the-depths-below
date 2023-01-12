package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.Cubemap
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider
import depth.voxel.BlockManager
import depth.voxel.MarchingCubeTerrain
import depth.voxel.generateMarchingCubeTerrain
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.lights.PointLightEx
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.SceneSkybox


class RenderSystem3d(
    private val sceneManager: SceneManager
    ) : EntitySystem() {
    override fun update(deltaTime: Float) {
        renderScenes(deltaTime)
    }

    //
    private val terrain = generateMarchingCubeTerrain(16, 25f)
//        MarchingCubeTerrain(
//        floatArrayOf(
//            -10f, -10f, -10f, // triangle 1 : begin
//            -10f, -10f, 10f,
//            -10f, 10f, 10f, // triangle 1 : end
//            10f, 10f, -10f, // triangle 2 : begin
//            -10f, -10f, -10f,
//            -10f, 10f, -10f, // triangle 2 : end
//            10f, -10f, 10f,
//            -10f, -10f, -10f,
//            10f, -10f, -10f,
//            10f, 10f, -10f,
//            10f, -10f, -10f,
//            -10f, -10f, -10f,
//            -10f, -10f, -10f,
//            -10f, 10f, 10f,
//            -10f, 10f, -10f,
//            10f, -10f, 10f,
//            -10f, -10f, 10f,
//            -10f, -10f, -10f,
//            -10f, 10f, 10f,
//            -10f, -10f, 10f,
//            10f, -10f, 10f,
//            10f, 10f, 10f,
//            10f, -10f, -10f,
//            10f, 10f, -10f,
//            10f, -10f, -10f,
//            10f, 10f, 10f,
//            10f, -10f, 10f,
//            10f, 10f, 10f,
//            10f, 10f, -10f,
//            -10f, 10f, -10f,
//            10f, 10f, 10f,
//            -10f, 10f, -10f,
//            -10f, 10f, 10f,
//            10f, 10f, 10f,
//            -10f, 10f, 10f,
//            10f, -10f, 10f
//        ), 200f
//    )

    init {
        // Added by suggestion of JamesTKhan as a troubleshooting measure.
//        val config = DefaultShader.Config()
//        config.defaultCullFace = GL20.GL_NONE
//        val provider = DefaultShaderProvider(config)

        sceneManager.addScene(Scene(terrain.modelInstance))
    }

    private fun renderScenes(deltaTime: Float) {
        sceneManager.update(deltaTime)
        sceneManager.render()
    }
}
