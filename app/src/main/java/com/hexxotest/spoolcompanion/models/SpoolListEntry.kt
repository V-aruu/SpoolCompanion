package com.hexxotest.spoolcompanion.models

import androidx.compose.ui.graphics.Color

data class SpoolListEntry(
    val id: Int = -1,
    val filamentId: Int = -1,
    val comment: String = "",
    val color: Color = Color.Transparent,
    val vendorName: String = "",
    val name: String = "",
    val material: String = "",
    val diameter: Double = 0.0,
    val weight: String = ""
)
