package com.hexxotest.spoolcompanion.network.data

import kotlinx.serialization.Serializable

@Serializable
data class SpoolItem(
    val archived: Boolean = false,
    val extra: Extra,
    val filament: Filament,
    val id: Int = -1,
    val initial_weight: Double = 0.0,
    val registered: String = "",
    val remaining_length: Double = 0.0,
    val remaining_weight: Double = 0.0,
    val spool_weight: Double = 0.0,
    val used_length: Double = 0.0,
    val used_weight: Double = 0.0,
    val comment: String = ""
)