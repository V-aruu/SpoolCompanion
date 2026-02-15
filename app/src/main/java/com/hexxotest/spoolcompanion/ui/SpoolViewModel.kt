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
import kotlinx.coroutines.launch
import java.io.IOException
import androidx.core.graphics.toColorInt

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

    init {
        getSpools()
    }

    private fun getSpools() {
        viewModelScope.launch {
            currentUiState = try {
                // Fetch raw Spoolman models and map them into a UI-friendly list entry.
                val spoolApi = SpoolApi(baseUrl = url)
                val spools = spoolApi.retrofitService.getSpoolList()
                val entries = spools.map { spool ->
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
                        color = Color("#${spool.filament.color_hex}".toColorInt()),
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
                                .map { Color("#${it}".toColorInt()) }
                        },
                        multiColorsDirection = spool.filament.multi_color_direction
                    )
                }
                UiState.Success(entries)
            } catch (e: Exception) {
                // Any failure (network, parsing, etc.) surfaces as a generic error state.
                UiState.Error
            }
        }
    }

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
