package depth.voxel

import make.some.noise.Noise

class MapGenerator {
    private val noise = Noise(12, 1f / 32f, Noise.PERLIN)

    fun isCoral(x: Int, y: Int, z: Int): Boolean {
        val noiseVal = noise.getPerlin(x.toFloat() * 5f, y.toFloat() * 5f, z.toFloat() * 5f)
        return noiseVal > 0.25f
    }
}
