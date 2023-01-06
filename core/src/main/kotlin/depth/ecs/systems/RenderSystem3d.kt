package depth.ecs.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import depth.voxel.BlockManager

class RenderSystem3d(private val camera: PerspectiveCamera, private val blockManager: BlockManager): EntitySystem() {
    private val modelBatch = ModelBatch()
    private val environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        add(DirectionalLight().apply {
            set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f)
        })

    }
    override fun update(deltaTime: Float) {
        modelBatch.begin(camera)
        modelBatch.render(blockManager.blocks.map { it.modelInstance }, environment)
        modelBatch.end()
    }
}
