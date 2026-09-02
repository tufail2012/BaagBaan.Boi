package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Random

/**
 * High-performance, memory-efficient procedural soft noise texture generator.
 * Creates a single 64x64 repeating tile of organic analog micro-grain.
 * Used for tactile, frosted "noisy blur" effects on navigation bar,
 * segmented control (New Entry / Records tabs), and profile menu.
 */
object SoftNoiseTexture {
    @Volatile
    private var cachedBrush: ShaderBrush? = null

    fun getOrCreateBrush(): ShaderBrush {
        val existing = cachedBrush
        if (existing != null) return existing
        return synchronized(this) {
            cachedBrush ?: run {
                val size = 64
                val pixels = IntArray(size * size)
                val random = Random(424242L) // Stable deterministic seed for natural, non-flickering grain
                for (i in pixels.indices) {
                    val grey = random.nextInt(256)
                    // Soft variable alpha (~8% to 22% opacity) to provide authentic stippled frosted glass texture
                    val alpha = 18 + random.nextInt(38)
                    pixels[i] = (alpha shl 24) or (grey shl 16) or (grey shl 8) or grey
                }
                val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
                val imageBitmap = bitmap.asImageBitmap()
                val brush = ShaderBrush(ImageShader(imageBitmap, TileMode.Repeated, TileMode.Repeated))
                cachedBrush = brush
                brush
            }
        }
    }
}

/**
 * Modifier extension to overlay a soft stippled noise grain on top of a composable.
 * Blends subtly with frosted glass and Haze backdrop blur.
 */
fun Modifier.softNoiseGrain(
    alpha: Float = 0.12f,
    blendMode: BlendMode = BlendMode.SrcOver
): Modifier = this.drawWithContent {
    drawContent()
    drawRect(
        brush = SoftNoiseTexture.getOrCreateBrush(),
        alpha = alpha,
        blendMode = blendMode
    )
}
