package com.hexxotest.spoolcompanion.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class NfcTagViewModel : ViewModel() {

    var spoolId by mutableIntStateOf(-1)

    var filamentId by mutableIntStateOf(-1)

    var isDialogShown by mutableStateOf(false)

}