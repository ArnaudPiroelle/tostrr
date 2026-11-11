package app.tostrr.ui.detail.components

import android.content.Context
import java.io.DataInputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * The land dots of the shipping globe, from `assets/globe_land.bin` (built by `tools/globe/land_dots.py`), as unit
 * vectors `x, y, z` one after the other. Read once per process.
 */
internal object GlobeLand {
    @Volatile private var dots: FloatArray? = null

    fun load(context: Context): FloatArray = dots ?: synchronized(this) {
        dots ?: read(context).also { dots = it }
    }

    private fun read(context: Context): FloatArray =
        DataInputStream(context.assets.open("globe_land.bin").buffered()).use { input ->
            val count = input.readInt()
            FloatArray(count * 3).also { out ->
                repeat(count) { i ->
                    val lat = Math.toRadians(input.readShort() / 100.0)
                    val lon = Math.toRadians(input.readShort() / 100.0)
                    out[i * 3] = (cos(lat) * cos(lon)).toFloat()
                    out[i * 3 + 1] = (cos(lat) * sin(lon)).toFloat()
                    out[i * 3 + 2] = sin(lat).toFloat()
                }
            }
        }
}
