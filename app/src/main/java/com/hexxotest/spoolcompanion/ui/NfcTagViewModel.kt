package com.hexxotest.spoolcompanion.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

class NfcTagViewModel(application: Application) : AndroidViewModel(application) {

    // Selected spool ID for the next NFC write. -1 indicates "none selected".
    var spoolId by mutableIntStateOf(-1)

    // Selected filament ID for the next NFC write. -1 indicates "none selected".
    var filamentId by mutableIntStateOf(-1)

    // Controls visibility of the "approach tag" dialog.
    var isDialogShown by mutableStateOf(false)

    // Holds data from the last scanned tag (when not writing) to show a read dialog.
    var readTagInfo by mutableStateOf<ReadTagInfo?>(null)

    // Error message for NFC write failures (shown in a dialog).
    var writeErrorMessage by mutableStateOf<String?>(null)

    // Spoolman URL. This is a computed property to ensure it's always fresh.
    val spoolmanUrl: String?
        get() {
            val sharedPrefs = getApplication<Application>().getSharedPreferences("settings", Context.MODE_PRIVATE)
            return sharedPrefs.getString("spoolman_url", null)
        }
    val addUrlToNfc: Boolean
        get() {
            val sharedPrefs = getApplication<Application>().getSharedPreferences("settings", Context.MODE_PRIVATE)
            return sharedPrefs.getBoolean("spoolman_nfc_url", false)
        }
}

// Parsed tag payload + tag UID for the read dialog.
data class ReadTagInfo(
    val tagId: String,
    val spoolId: Int?,
    val filamentId: Int?,
    val rawText: String?
)
