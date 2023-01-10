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
import depth.voxel.Terrain
import ktx.log.info


fun generateTerrain(
    size: Int,
    voxSize: FloatArray,
    isoValue: Double,
    nThreads: Int,
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
//    if (inputFile != null) {
//        try {
//            var idx = 0
//            scalarField = DoubleArray(size.pow(3))
//            val `in` = DataInputStream(FileInputStream(inputFile))
//            while (`in`.available() > 0) {
//                // Size does not match
//                if (idx >= scalarField.size) {
//                    `in`.close()
//                    println("Invalid volume size was specified.")
//                    return
//                }
//                scalarField[idx++] = `in`.readDouble()
//            }
//            `in`.close()
//
//            // Size does not match
//            if (idx != scalarField.size) {
//                println("Invalid volume size was specified.")
//                return
//            }
//        } catch (e: Exception) {
//            println("Something went wrong while reading the volume")
//            return
//        }
//    } else {
//        println("PROGRESS: Generating volume data.")
//        scalarField = generateScalarFieldDouble(size)
//    }

    // TIMER
//    val threads = ArrayList<Thread>()
    val results = ArrayList<ArrayList<DoubleArray>>()

    // Thread work distribution
//    var remainder = size % nThreads
//    val segment = size / nThreads

    // Z axis offset for vertice position calculation
    var zAxisOffset = 0
    println("PROGRESS: Executing marching cubes.")
//    for (i in 0 until nThreads) {
        // Distribute remainder among first (remainder) threads
//        val segmentSize = if (remainder-- > 0) segment + 1 else segment

        // Padding needs to be added to correctly close the gaps between segments
//        val paddedSegmentSize = if (i != nThreads - 1) segmentSize + 1 else segmentSize


        // Finished callback
        val callback: CallbackMC = object : CallbackMC() {
            override fun run() {
                results.add(vertices!!)
            }
        }

//        // Java...
//        val finalZAxisOffset = zAxisOffset

        // Start the thread
//        val t: Thread = object : Thread() {
//            override fun run() {
                marchingCubesDouble(
                    scalarField, size,
                    voxSize, isoValue, callback
                )
//            }
//        }
//        threads.add(t)
//        t.start()

        // Correct offsets for next iteration
//        zAxisOffset += segmentSize
//    }

    // Join the threads
//    for (i in threads.indices) {
//        try {
//            threads[i].join()
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//        }
//    }
    println("PROGRESS: Writing results to output file.")
    return HeightMapTerrain(size, terrainSize, 15f, results)
    //outputToFile(results, outFile)
}
