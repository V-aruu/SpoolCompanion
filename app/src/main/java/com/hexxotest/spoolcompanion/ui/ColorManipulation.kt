package com.hexxotest.spoolcompanion.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Utility extensions for generating subtle border contrast from a base color.
fun Color.darken(factor: Float = 0.92f): Color =
    copy(
        red = (red * factor).coerceIn(0f, 1f),
        green = (green * factor).coerceIn(0f, 1f),
        blue = (blue * factor).coerceIn(0f, 1f),
        alpha = 1f
    )

fun Color.lighten(amount: Float = 0.08f): Color =
    copy(
        red = (red + (1f - red) * amount).coerceIn(0f, 1f),
        green = (green + (1f - green) * amount).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * amount).coerceIn(0f, 1f),
        alpha = 1f
    )

fun Color.subtleBorderVariant(
    luminanceThreshold: Float = 0.5f,
    lightenAmount: Float = 0.25f,
    darkenFactor: Float = 0.75f
): Color {
    // If the color is dark, lighten the border; otherwise darken it.
    val isDark = luminance() < luminanceThreshold
    return if (isDark) lighten(lightenAmount) else darken(darkenFactor)
}
