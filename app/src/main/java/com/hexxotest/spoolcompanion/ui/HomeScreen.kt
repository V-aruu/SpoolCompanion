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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
    ElevatedCard(
        onClick = {
            nfcTagViewModel.isDialogShown = true
            nfcTagViewModel.spoolId = spool.id
            nfcTagViewModel.filamentId = spool.filamentId
        },
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = MaterialTheme.shapes.small),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(12.dp)
        ) {
            Box(
                modifier = modifier
                    .size(24.dp)
                    .background(spool.color)
                    .border(1.dp, Color.Black)
            )
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
                        painter = painterResource(id = R.drawable.ic_contactless),
                        contentDescription = "NFC",
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.inversePrimary)
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
    SpoolList(spools = spoolList, nfcTagViewModel = NfcTagViewModel())
}