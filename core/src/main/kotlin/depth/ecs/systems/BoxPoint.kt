package depth.ecs.systems

import com.badlogic.gdx.graphics.g3d.ModelInstance
import net.mgsx.gltf.scene3d.scene.Scene

data class BoxPoint(val coord: PointCoord, val isoValue: Float) {
    val x:Int get() = coord.x
    val y:Int get() = coord.y
    val z:Int get() = coord.z
    lateinit var scene: Scene
    lateinit var modelInstance: ModelInstance
    val on: Boolean get() = isoValue < 0.55f
    var actualOn = false

}
