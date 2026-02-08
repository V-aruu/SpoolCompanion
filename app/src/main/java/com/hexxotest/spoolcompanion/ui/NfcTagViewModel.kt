package com.hexxotest.spoolcompanion.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class NfcTagViewModel : ViewModel() {

    // Selected spool ID for the next NFC write. -1 indicates "none selected".
    var spoolId by mutableIntStateOf(-1)

    // Selected filament ID for the next NFC write. -1 indicates "none selected".
    var filamentId by mutableIntStateOf(-1)

    // Controls visibility of the "approach tag" dialog.
    var isDialogShown by mutableStateOf(false)

}
