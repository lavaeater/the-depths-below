package depth.voxel

import ktx.collections.GdxArray
import ktx.collections.toGdxArray

sealed class VoxelType {
    object Coral: VoxelType()
    object Water: VoxelType()
}

data class Voxel(val x: Int, val y: Int, val z:Int)

class Chunk(val offsetX: Int, val offsetY: Int) {
    val voxels = Array(100) {
        Array(100) {
            Array(100) {
                if((1..100).random() > 5) VoxelType.Water else VoxelType.Coral
            }.toGdxArray()
        }.toGdxArray()
    }.toGdxArray()
}


