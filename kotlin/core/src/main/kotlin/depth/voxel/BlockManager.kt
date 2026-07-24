package depth.voxel

import depth.core.DeepGameSettings
import eater.injection.InjectionContext
import ktx.collections.toGdxArray

class BlockManager(private val gameSettings: DeepGameSettings) {
    private val mapGenerator = MapGenerator()
    private val blocks by lazy {
        val blocks = mutableListOf<Block>()
        for(x in -sizeAll until sizeAll - 1)
            for(y in -sizeAll until sizeAll - 1)
                for(z in -sizeAll until sizeAll - 1) {
                    if(mapGenerator.isCoral(x,y,z)) {
                        blocks.add(Block(x, y, z, BlockType.Coral))
                    }
                }
        blocks.toGdxArray()
    }

    val modelsToRender by lazy {
        blocks.map { it.modelInstance }
    }

    fun getIndex(x: Int, y: Int, z: Int): Int {
        val modx = x % SizeX
        val mody = y % SizeY
        val modz = z % SizeZ

        return (modz * SizeXY) + (modx * SizeY) + mody;
    }

    companion object {
        private val gameSettings by lazy { InjectionContext.inject<DeepGameSettings>() }
        val SizeX = (Chunk.SizeX * ((gameSettings.viewDistance * 2) + 1))
        val SizeZ = (Chunk.SizeZ * ((gameSettings.viewDistance * 2) + 1));
        const val SizeY = Chunk.SizeY;
        val SizeXY = SizeX * SizeY;
        val BufferSize = SizeX * SizeY * SizeZ;
        const val sizeAll = 5
        val AddX = SizeY;
        val SubX = -SizeY;
        val AddY = 1;
        val SubY = -1;
        val AddZ = SizeXY;
        val SubZ = -SizeXY;
        val blockSize = 1f
    }
}
