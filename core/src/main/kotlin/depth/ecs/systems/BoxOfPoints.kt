package depth.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import depth.voxel.Joiser
import ktx.log.info
import ktx.math.vec3
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager


class BoxOfPoints(
    private val sceneManager: SceneManager,
    val numberOfPoints: Int
) {


    /**
     * This is where the confusion starts, right?
     */


    val boxPoints: List<BoxPoint> = Array(numberOfPoints) { x ->
        Array(numberOfPoints) { y ->
            Array(numberOfPoints) { z ->
                BoxPoint(
                    PointCoord(x, y, z),
                    Joiser.getValueFor(x, y, z)
                )
            }
        }.flatten()
    }.flatMap { it.asIterable() }

    var needsPoints = true
    fun createPoints() {
        if(needsPoints) {
            var max = Float.MIN_VALUE
            var min = Float.MAX_VALUE
            val mb = ModelBuilder()
            for (boxPoint in boxPoints) {
                if (boxPoint.isoValue < min) {
                    min = boxPoint.isoValue
                    info { "Min: $boxPoint" }
                }
                if (boxPoint.isoValue > max) {
                    max = boxPoint.isoValue
                    info { "Max: $boxPoint" }
                }
                sceneManager.addScene(
                    Scene(
                        mb.createSphere(
                            5f,
                            5f,
                            5f,
                            4,
                            4,
                            Material("cube").apply {
                                set(
                                    PBRColorAttribute.createEmissive(
                                        if (boxPoint.on) Color.GREEN else Color.BLUE
                                    )
                                )
                            },
                            VertexAttributes.Usage.Position.toLong() or
                                VertexAttributes.Usage.Normal.toLong()
                        )
                    ).apply {
                        modelInstance.transform.setToWorld(
                            vec3(boxPoint.x * 25f, boxPoint.y * 25f, boxPoint.z * 25f),
                            Vector3.Z,
                            Vector3.Y
                        )
                        boxPoint.scene = this
                        boxPoint.modelInstance = this.modelInstance
                    })
            }
            needsPoints = false
        } else {
            for(bp in boxPoints)
                sceneManager.addScene(bp.scene)
        }
    }

    fun destroyPoints() {
        for(bp in boxPoints) {
            sceneManager.removeScene(bp.scene)
        }
    }

    var pointsVisible = true
    fun togglePoints() {
        pointsVisible = !pointsVisible
        if(pointsVisible)
            createPoints()
        else
            destroyPoints()
    }
}
