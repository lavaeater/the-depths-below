@file:JvmName("Lwjgl3Launcher")

package depth.core.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import depth.core.TheDepthsBelow

/** Launches the desktop (LWJGL3) application. */
fun main() {
    Lwjgl3Application(TheDepthsBelow(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("the-depths-below")
        setWindowedMode(1920, 1080)
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
    })
}
