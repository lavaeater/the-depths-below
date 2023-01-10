package depth.marchingcubes

import java.math.BigInteger

/**
 * Created by Primoz on 11. 07. 2016.
 */
object VolumeGenerator {
    fun generateScalarFieldChar(size: Int): CharArray {
        val scalarField = CharArray(size.pow(3))
        val axisMin = -10f
        val axisMax = 10f
        val axisRange = axisMax - axisMin
        for (k in 0 until size) {
            for (j in 0 until size) {
                for (i in 0 until size) {
                    // actual values
                    val x = (axisMin + axisRange * i / (size - 1)).toInt().toChar()
                    val y = (axisMin + axisRange * j / (size - 1)).toInt().toChar()
                    val z = (axisMin + axisRange * k / (size - 1)).toInt().toChar()
                    scalarField[k + size * (j + size * i)] =
                        (x.code * x.code + y.code * y.code - z.code * z.code - 25).toChar()
                }
            }
        }
        return scalarField
    }

    fun generateScalarFieldShort(size: Int): ShortArray {
        val scalarField = ShortArray(size.pow(3))
        val axisMin = -10f
        val axisMax = 10f
        val axisRange = axisMax - axisMin
        for (k in 0 until size) {
            for (j in 0 until size) {
                for (i in 0 until size) {
                    // actual values
                    val x = (axisMin + axisRange * i / (size - 1)).toInt().toShort()
                    val y = (axisMin + axisRange * j / (size - 1)).toInt().toShort()
                    val z = (axisMin + axisRange * k / (size - 1)).toInt().toShort()
                    scalarField[k + size * (j + size * i)] = (x * x + y * y - z * z - 25).toShort()
                }
            }
        }
        return scalarField
    }

    fun generateScalarFieldInt(size: Int): IntArray {
        val scalarField = IntArray(size * size * size)
        val axisMin = -10f
        val axisMax = 10f
        val axisRange = axisMax - axisMin
        for (k in 0 until size) {
            for (j in 0 until size) {
                for (i in 0 until size) {
                    // actual values
                    val x = (axisMin + axisRange * i / (size - 1)).toInt()
                    val y = (axisMin + axisRange * j / (size - 1)).toInt()
                    val z = (axisMin + axisRange * k / (size- 1)).toInt()
                    scalarField[k + size * (j + size * i)] = (x * x + y * y - z * z - 25)
                }
            }
        }
        return scalarField
    }

    fun generateScalarFieldFloat(size: Int): FloatArray {
        val scalarField = FloatArray(size.pow(3))
        val axisMin = -10f
        val axisMax = 10f
        val axisRange = axisMax - axisMin
        for (k in 0 until size) {
            for (j in 0 until size) {
                for (i in 0 until size) {
                    // actual values
                    val x = axisMin + axisRange * i / (size - 1)
                    val y = axisMin + axisRange * j / (size - 1)
                    val z = axisMin + axisRange * k / (size - 1)
                    scalarField[k + size * (j + size * i)] = x * x + y * y - z * z - 25
                }
            }
        }
        return scalarField
    }

    fun generateScalarFieldDouble(size: Int): DoubleArray {
        val scalarField = DoubleArray(size.pow(3))
        val axisMin = -10.0
        val axisMax = 10.0
        val axisRange = axisMax - axisMin
        for (k in 0 until size) {
            for (j in 0 until size) {
                for (i in 0 until size) {
                    // actual values
                    val x = axisMin + axisRange * i / (size - 1)
                    val y = axisMin + axisRange * j / (size - 1)
                    val z = axisMin + axisRange * k / (size - 1)
                    scalarField[k + size * (j + size * i)] = x * x + y * y - z * z - 25
                }
            }
        }
        return scalarField
    }
}

fun Int.pow(exponent: Int):Int {
    return BigInteger.valueOf(this.toLong()).pow(3).toInt()
}
