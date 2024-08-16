package com.hexxotest.spoolcompanion.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hexxotest.spoolcompanion.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolCompanionApp(nfcTagViewModel: NfcTagViewModel) {

    val context = LocalContext.current

    var showSettingsDialog by remember { mutableStateOf(false) }

    val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val spoolmanUrlFromPrefs = sharedPrefs.getString("spoolman_url", "")
    val spoolmanUrl = remember { mutableStateOf(spoolmanUrlFromPrefs) }

    var spoolViewModel: SpoolViewModel? = null

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(stringResource(id = R.string.top_app_bar))
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (spoolmanUrlFromPrefs.toString().isNotEmpty()) {
                spoolViewModel = spoolViewModel ?: SpoolViewModel(spoolmanUrlFromPrefs.toString())
                HomeScreen(
                    uiState = spoolViewModel!!.currentUiState,
                    nfcTagViewModel = nfcTagViewModel
                )
            } else {
                NoUrlApp()
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            spoolmanUrl = spoolmanUrl,
            onDismiss = { showSettingsDialog = false },
            onConfirm = {
                sharedPrefs.edit {
                    putString("spoolman_url", spoolmanUrl.value)
                    apply()
                }
                showSettingsDialog = false
            }
        )
    }

}

@Composable
fun SettingsDialog(
    spoolmanUrl: MutableState<String?>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {

    AlertDialog(
        title = { Text(text = "Settings") },
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(
                onClick = { onConfirm() })
            {
                Text(text = "OK")
            }
        },
        dismissButton = {
            Button(
                onClick = { onDismiss() })
            {
                Text(text = "Cancel")
            }
        },
        text = {
            Column {
                Text(
                    text = "Spoolman URL:",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = spoolmanUrl.value ?: "",
                    onValueChange = { spoolmanUrl.value = it },
                    maxLines = 1,
                    singleLine = true,
                    placeholder = { Text(text = "http://0.0.0.0:7912/") })
            }
        }
    )
}

@Composable
fun NoNfcApp() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            modifier = Modifier
                .size(128.dp),
            painter = painterResource(id = R.drawable.ic_nfc),
            contentDescription = "NFC Error",
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.inversePrimary)
        )
        Text(
            text = stringResource(R.string.nfc_error),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun NoUrlApp() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Icon(
            modifier = Modifier
                .size(128.dp),
            imageVector = Icons.Filled.Settings,
            contentDescription = "No url for spoolman",
            tint = MaterialTheme.colorScheme.inversePrimary
        )
        Text(
            text = stringResource(R.string.no_url_error),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7A)
@Composable
fun DefaultPreview() {
    NoUrlApp()
}