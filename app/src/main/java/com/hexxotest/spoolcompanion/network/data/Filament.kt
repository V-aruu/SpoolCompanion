package com.hexxotest.spoolcompanion.network.data

import kotlinx.serialization.Serializable

// Raw filament model from Spoolman; used to build UI-friendly entries.
@Serializable
data class Filament(
    val color_hex: String = "00FFFFFF",
    val density: Double = 0.0,
    val diameter: Double = 0.0,
    val external_id: String = "",
    val extra: Extra,
    val id: Int = -1,
    val material: String = "",
    val name: String = "",
    val registered: String = "",
    val settings_bed_temp: Int = 0,
    val settings_extruder_temp: Int = 0,
    val spool_weight: Double = 0.0,
    val vendor: Vendor,
    val weight: Double= 0.0,
    val multi_color_hexes: String= "",
    val multi_color_direction: String= "",
)
