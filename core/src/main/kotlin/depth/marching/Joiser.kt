package depth.marching

import com.badlogic.gdx.math.MathUtils
import com.sudoplay.joise.mapping.IMappingUpdateListener
import com.sudoplay.joise.mapping.Mapping
import com.sudoplay.joise.mapping.MappingMode
import com.sudoplay.joise.mapping.MappingRange
import com.sudoplay.joise.module.ModuleAutoCorrect
import com.sudoplay.joise.module.ModuleBasisFunction
import com.sudoplay.joise.module.ModuleScaleDomain
import depth.voxel.pow

object Joiser {

    val noiser by lazy {
        val basis = ModuleBasisFunction()
        basis.setType(ModuleBasisFunction.BasisType.SIMPLEX)
        basis.seed = 14 // always same values, good

        val correct = ModuleAutoCorrect()
        correct.setSource(basis)
        correct.calculateAll()

        val scaleDomain = ModuleScaleDomain()
        scaleDomain.setSource(correct)
        val scale = 32.0
        scaleDomain.setScaleX(scale)
        scaleDomain.setScaleY(scale)
        scaleDomain.setScaleZ(scale)
        scaleDomain.setScaleU(scale)
        scaleDomain.setScaleW(scale)
        scaleDomain
    }

    fun getValueFor(x: Int, y: Int, z: Int, width: Int = 100, height: Int = 100, depth: Int = 100): Float {
        val range = MappingRange.DEFAULT
        val dx = range.map1.x - range.map0.x
        val dy = range.loop1.y - range.loop0.y
        val dz = range.loop1.z - range.loop0.z

        val dy_div_2pi = dy / MathUtils.PI2
        val dz_div_2pi = dz / MathUtils.PI2
        val iw = 1.0 / width.toDouble()
        val ih = 1.0 / height.toDouble()
        val id = 1.0 / depth.toDouble()


        var p = x.toDouble() * iw
        var q = y.toDouble() * ih
        var r = z.toDouble() * id

        q = q * (range.map1.y - range.map0.y) / dy
        r = r * (range.map1.z - range.map0.z) / dz

        val nx = range.map0.x + p * dx
        val ny = range.loop0.y + MathUtils.cos(q.toFloat() * MathUtils.PI2) * dy_div_2pi
        val nz = range.loop0.y + MathUtils.sin(q.toFloat() * MathUtils.PI2) * dy_div_2pi
        val nw = range.loop0.z + MathUtils.cos(r.toFloat() * MathUtils.PI2) * dz_div_2pi
        val nu = range.loop0.z + MathUtils.sin(r.toFloat() * MathUtils.PI2) * dz_div_2pi
        val nv = 0.0
        return noiser.get(nx, ny, nz, nw, nu, nv).toFloat()
    }

    fun get3dNoise(size: Int): FloatArray {
        val floatArray = FloatArray(size.pow(3))
        var minVal = 0f
        var maxval = 0f
        val basis = ModuleBasisFunction()
        basis.setType(ModuleBasisFunction.BasisType.SIMPLEX)
        basis.seed = (1..1000).random().toLong()

        val correct = ModuleAutoCorrect()
        correct.setSource(basis)
        correct.calculateAll()

        val scaleDomain = ModuleScaleDomain()
        scaleDomain.setSource(correct)
        val scale = 1.0
        scaleDomain.setScaleX(scale)
        scaleDomain.setScaleY(scale)
        scaleDomain.setScaleZ(scale)
        scaleDomain.setScaleU(scale)
        scaleDomain.setScaleW(scale)
        Mapping.map3D(
            MappingMode.SEAMLESS_XYZ, size, size, size, scaleDomain, MappingRange.DEFAULT, { x, y, z, value ->
                floatArray[getIndex(x, y, z, size)] = value.toFloat()
            }, IMappingUpdateListener.NULL_LISTENER
        )
        return floatArray
    }
}
