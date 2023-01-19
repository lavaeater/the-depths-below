package depth.marching

import com.badlogic.gdx.math.Vector3
import ktx.math.vec3

data class MarchingTriangle(
    val cubeX: Int,
    val y: Int,
    val z: Int,
    val v1: Vector3 = vec3(),
    val v2: Vector3 = vec3(),
    val v3: Vector3 = vec3(),
    val n1: Vector3 = vec3(),
    val n2: Vector3 = vec3(),
    val n3: Vector3 = vec3()
)
