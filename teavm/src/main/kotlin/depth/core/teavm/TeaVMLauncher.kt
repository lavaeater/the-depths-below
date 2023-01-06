@file:JvmName("TeaVMLauncher")

package depth.core.teavm

import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration
import com.github.xpenatan.gdx.backends.web.WebApplication
import depth.core.TheDepthsBelow

/** Launches the TeaVM/HTML application. */
fun main() {
    val config = TeaApplicationConfiguration("canvas").apply {
        width = 640
        height = 480
    }
    WebApplication(TheDepthsBelow(), config)
}
