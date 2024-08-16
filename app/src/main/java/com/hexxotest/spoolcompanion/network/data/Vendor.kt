package com.hexxotest.spoolcompanion.network.data

import kotlinx.serialization.Serializable

@Serializable
data class Vendor(
    val external_id: String = "",
    val extra: Extra,
    val id: Int = -1,
    val name: String = "",
    val registered: String = ""
)