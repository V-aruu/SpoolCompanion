package com.hexxotest.spoolcompanion.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hexxotest.spoolcompanion.models.SpoolListEntry
import com.hexxotest.spoolcompanion.network.SpoolApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.toColorInt
import kotlin.math.roundToInt

class SpoolViewModel(spoolmanUrl: String) : ViewModel() {

    private val url = spoolmanUrl

    // Simple UI state machine for the Home screen.
    sealed interface UiState {
        data class Success(val spools: List<SpoolListEntry>) : UiState
        data object Error : UiState
        data object Loading : UiState
    }

    var currentUiState: UiState by mutableStateOf(UiState.Loading)
        private set

    // Tracks swipe-to-refresh progress without forcing the full-screen loading UI.
    var isRefreshing: Boolean by mutableStateOf(false)
        private set

    init {
        loadSpools(initialLoad = true)
    }

    fun refreshSpools() {
        loadSpools(initialLoad = false)
    }

    private fun loadSpools(initialLoad: Boolean) {
        viewModelScope.launch {
            val hadDataBeforeRefresh = currentUiState is UiState.Success
            if (initialLoad) {
                currentUiState = UiState.Loading
            } else {
                isRefreshing = true
            }
            try {
                currentUiState = UiState.Success(fetchSpools())
            } catch (e: Exception) {
                // Refresh failures keep existing list visible; initial load still shows error state.
                if (!hadDataBeforeRefresh) {
                    currentUiState = UiState.Error
                }
                Log.e("SpoolViewModel", "Failed to fetch spool list", e)
            } finally {
                isRefreshing = false
            }
        }
    }

    private suspend fun fetchSpools(): List<SpoolListEntry> = withContext(Dispatchers.IO) {
        // Fetch raw Spoolman models and map them into a UI-friendly list entry.
        val spoolApi = SpoolApi(baseUrl = url)
        val spools = spoolApi.retrofitService.getSpoolList()
        spools.map { spool ->
            // Prefer spool-specific initial weight; fall back to filament weight when absent.
            val totalWeight = when {
                spool.initial_weight > 0 -> spool.initial_weight
                spool.filament.weight > 0 -> spool.filament.weight
                else -> 0.0
            }
            // Remaining fraction is clamped to [0, 1] for UI rendering.
            val remainingFraction = if (totalWeight > 0) {
                (spool.remaining_weight / totalWeight).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
            // Format weights for UI; keep empty strings if the backend doesn't provide values.
            val totalWeightLabel = if (totalWeight > 0) {
                convertWeightDoubleToString(totalWeight)
            } else {
                ""
            }
            val remainingWeightLabel = if (spool.remaining_weight > 0) {
                convertWeightDoubleToString(spool.remaining_weight)
            } else {
                ""
            }
            SpoolListEntry(
                id = spool.id,
                filamentId = spool.filament.id,
                vendorName = spool.filament.vendor.name,
                name = spool.filament.name,
                // Keep the raw hex string so users can search by color code.
                colorHex = spool.filament.color_hex,
                color = parseSpoolmanColor(spool.filament.color_hex),
                material = spool.filament.material,
                weight = convertWeightDoubleToString(spool.filament.weight),
                diameter = spool.filament.diameter,
                comment = spool.comment,
                totalWeight = totalWeightLabel,
                remainingWeight = remainingWeightLabel,
                remainingFraction = remainingFraction,
                // Guard against empty multi-color strings which would cause an
                // IllegalArgumentException when converting "#" to a color.
                multiColors = if (spool.filament.multi_color_hexes.isBlank()) {
                    emptyList()
                } else {
                    spool.filament.multi_color_hexes.split(",")
                        .filter { it.isNotBlank() }
                        .map { parseSpoolmanColor(it) }
                },
                multiColorsDirection = spool.filament.multi_color_direction
            )
        }
    }

    private fun parseSpoolmanColor(rawColor: String): Color {
        val normalized = rawColor.trim().removePrefix("#")
        // Spoolman may return colors as RRGGBB + decimal transparency percent (0..100).
        if (normalized.length > 6 &&
            normalized.take(6).all { it.isHexDigit() } &&
            normalized.drop(6).all { it.isDigit() }
        ) {
            val rgb = normalized.take(6)
            val transparencyPercent = normalized.drop(6).toIntOrNull()
            if (transparencyPercent != null) {
                val clampedTransparency = transparencyPercent.coerceIn(0, 100)
                // API semantics: 0 = fully transparent, 100 = fully opaque.
                val opacity = clampedTransparency / 100f
                val alpha = (opacity * 255f).roundToInt().coerceIn(0, 255)
                val standardArgb = "${alpha.toString(16).padStart(2, '0')}$rgb"
                return Color("#$standardArgb".toColorInt())
            }
        }

        // Fall back to normal hex parsing (#RRGGBB or #AARRGGBB).
        return runCatching { Color("#$normalized".toColorInt()) }
            .getOrElse { Color.Transparent }
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private fun convertWeightDoubleToString(weight: Double): String {
        // Spoolman returns weight in grams; format for UI with g/kg units.
        // Weight var
        var weightInfo: Double = weight
        // Define unit 'g' default, 'kg' if > 1000 g
        var unit: String = "g"
        // Check if greater than a kilo
        if (weightInfo >= 1000) {
            weightInfo /= 1000.0
            unit = "kg"
        }
        // Return formatted string
        val weightStr: String = weightInfo.toString()
        return if (weightStr.contains(".0")) {
            "${weightStr.dropLast(2)} $unit"
        } else {
            "$weightStr $unit"
        }
    }

    companion object {
        fun provideFactory(spoolmanUrl: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SpoolViewModel(spoolmanUrl) as T
            }
        }
    }
}
