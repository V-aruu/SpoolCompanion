package com.hexxotest.spoolcompanion.models

import androidx.compose.ui.graphics.Color

// UI-friendly model derived from the Spoolman API response.
data class SpoolListEntry(
    // IDs are used when writing NFC tags.
    val id: Int = -1,
    val filamentId: Int = -1,
    val comment: String = "",
    val color: Color = Color.Transparent,
    val vendorName: String = "",
    val name: String = "",
    val material: String = "",
    val diameter: Double = 0.0,
    // Pre-formatted weight string (e.g., "750 g" or "1 kg") for direct UI display.
    val weight: String = "",
    // Fraction of filament remaining on the spool (0.0..1.0).
    val remainingFraction: Float = 0f,
    val multiColors: List<Color> = listOf(Color.Transparent, Color.Transparent),
    // Direction hint from the API (e.g., "longitudinal").
    val multiColorsDirection: String = ""
)
