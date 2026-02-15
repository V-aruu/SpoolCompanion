package com.hexxotest.spoolcompanion.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

    // Persisted settings live in SharedPreferences for now (DataStore key exists but is unused).
    val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val spoolmanUrlFromPrefs = sharedPrefs.getString("spoolman_url", "")
    val spoolmanUrl = remember { mutableStateOf(spoolmanUrlFromPrefs) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    // Restore sort preference on app launch; fall back to Remaining when value is missing/invalid.
    var selectedSort by remember {
        mutableStateOf(sharedPrefs.readSortOption())
    }
    var isSortAscending by remember {
        mutableStateOf(sharedPrefs.getBoolean(KEY_SORT_ASCENDING, true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Text(stringResource(id = R.string.top_app_bar))
                },
                actions = {
                    IconButton(onClick = { isSortMenuExpanded = true }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    selectedSort = option
                                    sharedPrefs.edit { putString(KEY_SORT_OPTION, option.name) }
                                    isSortMenuExpanded = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = {
                        isSortAscending = !isSortAscending
                        sharedPrefs.edit { putBoolean(KEY_SORT_ASCENDING, isSortAscending) }
                    }) {
                        Icon(
                            painter = painterResource(
                                id = if (isSortAscending) {
                                    android.R.drawable.arrow_up_float
                                } else {
                                    android.R.drawable.arrow_down_float
                                }
                            ),
                            contentDescription = if (isSortAscending) {
                                "Sort ascending"
                            } else {
                                "Sort descending"
                            },
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
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
                // Key the ViewModel by URL so changing the server creates a fresh instance.
                val spoolViewModel: SpoolViewModel = viewModel(
                    key = spoolmanUrlFromPrefs,
                    factory = SpoolViewModel.provideFactory(spoolmanUrlFromPrefs.toString())
                )
                HomeScreen(
                    uiState = spoolViewModel.currentUiState,
                    nfcTagViewModel = nfcTagViewModel,
                    // Swipe down on the list to request a fresh API fetch.
                    isRefreshing = spoolViewModel.isRefreshing,
                    onRefresh = { spoolViewModel.refreshSpools() },
                    selectedSort = selectedSort,
                    isSortAscending = isSortAscending
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
                // Save the cleaned base URL; SpoolApi appends "/api/v1/".
                sharedPrefs.edit {
                    putString("spoolman_url", spoolmanUrl.value)
                    apply()
                }
                showSettingsDialog = false
            }
        )
    }

}

private const val KEY_SORT_OPTION = "sort_option"
private const val KEY_SORT_ASCENDING = "sort_ascending"

private fun android.content.SharedPreferences.readSortOption(): SortOption {
    val stored = getString(KEY_SORT_OPTION, SortOption.ID.name)
    return SortOption.entries.firstOrNull { it.name == stored } ?: SortOption.ID
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
                onClick = {
                    // Normalize user input to avoid trailing slashes in the base URL.
                    val cleaned = (spoolmanUrl.value ?: "").trim().removeSuffix("/")
                    spoolmanUrl.value = cleaned
                    onConfirm()
                })
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
            painter = painterResource(id = R.drawable.ic_settings_filled),
            contentDescription = "No url for spoolman",
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.no_url_error),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_8)
@Composable
fun DefaultPreview() {
    NoUrlApp()
}
