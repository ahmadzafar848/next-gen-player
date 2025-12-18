package com.ahmadzafartech.nextgenplayer.util

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.random.Random

fun gradientFromString(key: String): Brush {
    val seed = abs(key.hashCode())
    val random = Random(seed)

    // 🎨 Designer-approved ranges
    val hue = random.nextFloat() * 360f          // any hue
    val saturation = 0.35f + random.nextFloat() * 0.15f // 35–50%
    val lightness = 0.30f + random.nextFloat() * 0.10f  // 30–40%

    val c1 = Color.hsl(
        hue,
        saturation,
        lightness
    )

    val c2 = Color.hsl(
        (hue + 20f) % 360f,
        saturation * 0.9f,
        lightness + 0.04f
    )

    val c3 = Color.hsl(
        (hue + 40f) % 360f,
        saturation * 0.8f,
        lightness + 0.08f
    )

    return Brush.horizontalGradient(
        colors = listOf(c1, c2, c3)
    )
}
