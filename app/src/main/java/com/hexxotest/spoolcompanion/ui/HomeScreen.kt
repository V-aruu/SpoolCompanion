package com.hexxotest.spoolcompanion.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.res.colorResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hexxotest.spoolcompanion.R
import com.hexxotest.spoolcompanion.models.SpoolListEntry

@Composable
// Main Screen selector : switch between Spool list, loading animation or error message
fun HomeScreen(
    uiState: SpoolViewModel.UiState,
    modifier: Modifier = Modifier,
    nfcTagViewModel: NfcTagViewModel
) {
    when (uiState) {
        is SpoolViewModel.UiState.Success -> {
            // NFC dialog is driven by shared view-model state so it can be dismissed after a write.
            if (nfcTagViewModel.isDialogShown) {
                WriteNfcDialog(nfcTagViewModel)
            }
            SpoolList(
                spools = uiState.spools,
                modifier = modifier.fillMaxSize(),
                nfcTagViewModel = nfcTagViewModel
            )
        }
        is SpoolViewModel.UiState.Error -> ErrorScreen()
        is SpoolViewModel.UiState.Loading -> LoadingScreen()
    }
}

@Composable
fun LoadingScreen() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Large spinner to signal network fetch in progress.
        CircularProgressIndicator(
            modifier = Modifier
                .size(128.dp),
            trackColor = MaterialTheme.colorScheme.inversePrimary
        )
        Text(
            text = stringResource(R.string.loading),
            modifier = Modifier
                .padding(top = 16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorScreen() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Friendly error state for network/parse failures.
        Image(
            modifier = Modifier
                .size(128.dp),
            painter = painterResource(id = R.drawable.ic_cloud_off),
            contentDescription = "Network Error",
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.inversePrimary)
        )
        Text(
            text = stringResource(R.string.loading_failed),
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SpoolEntry(
    spool: SpoolListEntry,
    modifier: Modifier = Modifier,
    nfcTagViewModel: NfcTagViewModel
) {
    // Use a lighter gray shadow in dark mode so it stands out on the dark background
    val shadowColor = if (isSystemInDarkTheme()) Color(0xFF555555) else DefaultShadowColor
    OutlinedCard(
        onClick = {
            // Selection drives the NFC write flow (dialog + payload in MainActivity).
            nfcTagViewModel.isDialogShown = true
            nfcTagViewModel.spoolId = spool.id
            nfcTagViewModel.filamentId = spool.filamentId
        },
        // Use a dedicated gray background for the card container
        // Use gray_800 only for dark theme; keep default surface for light theme
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSystemInDarkTheme())
                colorResource(id = R.color.gray_800)
            else
                MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = MaterialTheme.shapes.small,
                ambientColor = shadowColor,
                spotColor = shadowColor),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(12.dp)
        ) {
            // Border color is derived from the primary filament color for subtle contrast.
            val borderColor = spool.color.subtleBorderVariant()
            // Show single color or multicolor filament.
            if (spool.multiColors.isNotEmpty() && spool.multiColors.any { it != Color.Transparent }) {
                // Spoolman provides a direction hint for multi-color filaments.
                val direction = spool.multiColorsDirection.trim().lowercase()
                val isLongitudinal = direction == "longitudinal"

                Box(
                    modifier = modifier
                        .size(24.dp)
                        .border(1.dp, borderColor)
                ) {
                    if (isLongitudinal) {
                        // Longitudinal: top/bottom (stacked vertically).
                        Column(modifier = Modifier.fillMaxSize()) {
                            spool.multiColors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp, 12.dp)
                                        .background(c)
                                )
                            }
                        }
                    } else {
                        // Coaxial (default): left/right (side-by-side).
                        Row(modifier = Modifier.fillMaxSize()) {
                            spool.multiColors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(12.dp, 24.dp)
                                        .background(c)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = modifier
                        .size(24.dp)
                        .background(spool.color)
                        .border(1.dp, borderColor)
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = "${spool.vendorName} - ${spool.name} " +
                            "(${spool.material}, ${spool.diameter} mm, ${spool.weight})",
                )
                if (spool.comment.isNotEmpty()) {
                    Text(
                        text = spool.comment,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }

}

@Composable
fun SpoolList(
    spools: List<SpoolListEntry>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    nfcTagViewModel: NfcTagViewModel
) {
    // Lazy list to efficiently handle large numbers of spools.
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 4.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(spools, itemContent = { item ->
            SpoolEntry(spool = item, nfcTagViewModel = nfcTagViewModel)
        })
    }
}

@Composable
fun WriteNfcDialog(
    nfcTagViewModel: NfcTagViewModel
) {
    if (nfcTagViewModel.isDialogShown) {
        // This dialog is shown while waiting for an NFC tag to be detected.
        Dialog(
            onDismissRequest = {
                nfcTagViewModel.isDialogShown = false
            }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Image(
                        modifier = Modifier
                            .size(128.dp),
                        painter = painterResource(id = R.drawable.ic_contactless_filled),
                        contentDescription = "NFC",
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "Approach an NFC tag...",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        TextButton(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onClick = {
                                nfcTagViewModel.isDialogShown = false
                            }
                        ) {
                            Text(stringResource(id = R.string.dismiss))
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true, device = Devices.PIXEL_7A,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun ListPreview() {
    val spoolList = listOf(
        SpoolListEntry(
            name = "Jetpack Blue",
            vendorName = "MyVendor",
            comment = "My comment about this specific filament",
            material = "ABS",
            diameter = 1.75,
            weight = "1 kg",
            color = Color.Blue
        ),
        SpoolListEntry(
            name = "Compose Red",
            vendorName = "MainVendor",
            comment = "",
            material = "ABS",
            diameter = 1.75,
            weight = "1 kg",
            color = Color.Red
        )
    )
    SpoolList(spools = spoolList, nfcTagViewModel = viewModel())
}
