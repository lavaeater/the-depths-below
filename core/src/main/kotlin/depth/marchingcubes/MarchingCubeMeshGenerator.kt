package depth.marchingcubes

import com.sudoplay.joise.mapping.IMapping2DWriter
import com.sudoplay.joise.mapping.IMappingUpdateListener
import com.sudoplay.joise.mapping.Mapping
import com.sudoplay.joise.mapping.MappingMode
import com.sudoplay.joise.mapping.MappingRange
import com.sudoplay.joise.module.ModuleAutoCorrect
import com.sudoplay.joise.module.ModuleBasisFunction
import com.sudoplay.joise.module.ModuleScaleDomain
import depth.marchingcubes.MarchingCubes.marchingCubesDouble
import depth.voxel.HeightMapTerrain
import depth.voxel.MarchingCubeTerrain
import depth.voxel.Terrain
import ktx.log.info


fun generateTerrain(
    size: Int,
    voxSize: FloatArray,
    isoValue: Double,
    terrainSize: Float
) : Terrain {
    val heightValues = Array(size) { x ->
        Array(size) { y ->
            0.0
        }
    }
    var minVal = 0.0
    var maxVal = 0.0
    val basis = ModuleBasisFunction()
    basis.setType(ModuleBasisFunction.BasisType.SIMPLEX)
    basis.seed = (1..1000).random().toLong()

    val correct = ModuleAutoCorrect()
    correct.setSource(basis)
    correct.calculateAll()

    val scaleDomain = ModuleScaleDomain()
    scaleDomain.setSource(correct)
    val scale = 8.0
    scaleDomain.setScaleX(scale)
    scaleDomain.setScaleY(scale)
    scaleDomain.setScaleZ(scale)
    scaleDomain.setScaleU(scale)
    scaleDomain.setScaleW(scale)
    Mapping.map2DNoZ(MappingMode.SEAMLESS_XY, size, size, scaleDomain, MappingRange.DEFAULT,
        IMapping2DWriter { x, y, value ->
            if (value < minVal) {
                minVal = value
                info { "Min: $minVal" }
            }
            if (value > maxVal) {
                maxVal = value
                info { "Max: $maxVal" }
            }
            heightValues[x][y] = value
        }, IMappingUpdateListener.NULL_LISTENER
    )


    val scalarField = heightValues.flatten().toDoubleArray()
    // Z axis offset for vertice position calculation
    println("PROGRESS: Executing marching cubes.")
    val results =
                marchingCubesDouble(
                    scalarField, size,
                    voxSize, isoValue)
    println("PROGRESS: Writing results to output file.")
    return MarchingCubeTerrain(size, terrainSize, 15f, results)
}
