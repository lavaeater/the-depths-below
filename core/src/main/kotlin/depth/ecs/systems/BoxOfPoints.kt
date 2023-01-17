package depth.ecs.systems

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

class Cube()
class BoxOfPoints(private val sceneManager: SceneManager, val numberOfPoints: Int) {
    init {
        val numberOfPoints = 100
        for (x in 0 until numberOfPoints)
            for (y in 0 until numberOfPoints)
                for (z in 0 until numberOfPoints) {

                }
        val joiser = Joiser
    }

    val boxPoints: List<BoxPoint> = Array(numberOfPoints) { x ->
        Array(numberOfPoints) { y ->
            Array(numberOfPoints) { z ->
                BoxPoint(x, y, z, Joiser.getValueFor(x, y, z, numberOfPoints, numberOfPoints, numberOfPoints))
            }
        }.flatten()
    }.flatMap { it.asIterable() }

    fun createPoints() {
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
                        5f, 5f, 5f, 4, 4, Material().apply {
//                            set(
//                                PBRColorAttribute.createBaseColorFactor(
//                                    Color(
//                                        boxPoint.isoValue,
//                                        boxPoint.isoValue,
//                                        boxPoint.isoValue,
//                                        1f
//                                    )
//                                )
//                            )
                            set(
                                PBRColorAttribute.createEmissive(
                                    boxPoint.isoValue,
                                    boxPoint.isoValue,
                                    boxPoint.isoValue,
                                    1f
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
                })
        }
    }
}
