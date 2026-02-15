package com.hexxotest.spoolcompanion.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hexxotest.spoolcompanion.R
import com.hexxotest.spoolcompanion.models.SpoolListEntry
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Main Screen selector: switch between Spool list, loading animation or error message
fun HomeScreen(
    uiState: SpoolViewModel.UiState,
    modifier: Modifier = Modifier,
    nfcTagViewModel: NfcTagViewModel,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    selectedSort: SortOption,
    isSortAscending: Boolean
) {
    // Show an error dialog for NFC write failures.
    val writeErrorMessage = nfcTagViewModel.writeErrorMessage
    if (writeErrorMessage != null) {
        WriteErrorDialog(
            message = writeErrorMessage,
            onDismiss = { nfcTagViewModel.writeErrorMessage = null }
        )
    }
    // Show a read dialog when a tag is scanned outside of write mode.
    val readTagInfo = nfcTagViewModel.readTagInfo
    if (readTagInfo != null) {
        val spool = (uiState as? SpoolViewModel.UiState.Success)
            ?.spools
            ?.firstOrNull { it.id == readTagInfo.spoolId }
        ReadTagDialog(
            readTagInfo = readTagInfo,
            spool = spool,
            onDismiss = { nfcTagViewModel.readTagInfo = null }
        )
    }
    when (uiState) {
        is SpoolViewModel.UiState.Success -> {
            var isSearchEnabled by rememberSaveable { mutableStateOf(false) }
            var searchQuery by rememberSaveable { mutableStateOf("") }
            // Recompute filtered rows as the user types; when disabled we keep full list.
            val filteredSpools = remember(uiState.spools, isSearchEnabled, searchQuery) {
                if (!isSearchEnabled) {
                    uiState.spools
                } else {
                    uiState.spools.filter { it.matchesSearch(searchQuery) }
                }
            }
            // Apply selected sorting mode over the filtered set.
            val sortedSpools = remember(filteredSpools, selectedSort, isSortAscending) {
                filteredSpools.sortedWith(spoolComparator(selectedSort, isSortAscending))
            }
            val spoolListState = rememberLazyListState()
            // Changing sort mode should reveal the new order from the top.
            LaunchedEffect(selectedSort, isSortAscending) {
                spoolListState.scrollToItem(0)
            }
            // NFC dialog is driven by shared view-model state so it can be dismissed after a write.
            if (nfcTagViewModel.isDialogShown) {
                WriteNfcDialog(nfcTagViewModel)
            }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isSearchEnabled) {
                            // Place the search field directly under the top bar with a small gap.
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                placeholder = { Text(text = "Search spools...") },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                                contentDescription = "Clear search"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        SpoolList(
                            spools = sortedSpools,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp),
                            nfcTagViewModel = nfcTagViewModel,
                            selectedSort = selectedSort,
                            listState = spoolListState
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            isSearchEnabled = !isSearchEnabled
                            // Disabling search returns the full list immediately.
                            if (!isSearchEnabled) {
                                searchQuery = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isSearchEnabled) {
                                    android.R.drawable.ic_menu_close_clear_cancel
                                } else {
                                    android.R.drawable.ic_menu_search
                                }
                            ),
                            contentDescription = if (isSearchEnabled) {
                                "Disable search"
                            } else {
                                "Enable search"
                            }
                        )
                    }
                }
            }
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
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .padding(12.dp)
                    .fillMaxWidth()
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
                    .weight(1f)
            ) {
                Text(
                    text = "${spool.vendorName} - ${spool.name} " +
                            "(${spool.material}, ${spool.diameter} mm, ${spool.weight})",
                    // Keep the header on one line so the ID stays on the same row.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (spool.comment.isNotEmpty()) {
                    Text(
                        text = spool.comment,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
                // Subtle label color: blend text toward the card surface so it reads softly.
                val spoolIdColor = lerp(
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.surface,
                    0.7f
                )
                Text(
                    // Show spool ID on the right for quick identification.
                    text = "#${spool.id}",
                    fontStyle = FontStyle.Italic,
                    color = spoolIdColor
                )
            }
            // Remaining filament indicator (percentage of initial/total weight).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Subtle default color; switch to burgundy when remaining < 5%.
                val remainingColor = if (spool.remainingFraction < 0.05f) {
                    Color(0xFFB01D2A)
                } else {
                    // Use a lighter blend in light theme so the bar stays visible.
                    val blendRatio = if (isSystemInDarkTheme()) 0.5f else 0.05f
                    lerp(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.surface,
                        blendRatio
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(spool.remainingFraction)
                        .background(remainingColor)
                )
            }
        }
    }

}

@Composable
private fun SpoolList(
    spools: List<SpoolListEntry>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    nfcTagViewModel: NfcTagViewModel,
    selectedSort: SortOption = SortOption.REMAINING,
    listState: LazyListState = rememberLazyListState()
) {
    // Lazy list to efficiently handle large numbers of spools.
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(all = 4.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(spools, key = { _, item -> item.id }) { index, item ->
            // Insert a subtle visual break whenever the sorting group changes.
            val currentGroup = item.groupLabel(selectedSort)
            val previousGroup = if (index > 0) spools[index - 1].groupLabel(selectedSort) else null
            val isNewGroup = index == 0 || currentGroup != previousGroup

            if (isNewGroup && currentGroup != null) {
                if (index > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = currentGroup,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp)
                )
            }
            SpoolEntry(spool = item, nfcTagViewModel = nfcTagViewModel)
        }
    }
}

enum class SortOption(val label: String) {
    REMAINING("Remaining"),
    NAME("Name"),
    VENDOR("Vendor"),
    ID("ID"),
    COLOR("Color")
}

private fun spoolComparator(selectedSort: SortOption, isAscending: Boolean): Comparator<SpoolListEntry> {
    val comparator = when (selectedSort) {
        SortOption.REMAINING -> compareBy<SpoolListEntry> { it.remainingFraction }
            .thenBy { it.name.lowercase() }
            .thenBy { it.id }
        SortOption.NAME -> compareBy<SpoolListEntry> { it.name.lowercase() }
            .thenBy { it.vendorName.lowercase() }
            .thenBy { it.id }
        SortOption.VENDOR -> compareBy<SpoolListEntry> { it.vendorName.lowercase() }
            .thenBy { it.name.lowercase() }
            .thenBy { it.id }
        SortOption.ID -> compareBy<SpoolListEntry> { it.id }
        SortOption.COLOR -> compareBy<SpoolListEntry> { colorFamilyRank(it.colorHex) }
            .thenBy { it.colorHex.lowercase() }
            .thenBy { it.name.lowercase() }
            .thenBy { it.id }
    }
    return if (isAscending) comparator else comparator.reversed()
}

private fun SpoolListEntry.groupLabel(selectedSort: SortOption): String? =
    when (selectedSort) {
        SortOption.REMAINING -> remainingGroupLabel(remainingFraction)
        SortOption.NAME -> name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        SortOption.VENDOR -> vendorName.ifBlank { "Unknown vendor" }
        // ID sorting is inherently unique per row, so extra group labels add noise.
        SortOption.ID -> null
        SortOption.COLOR -> colorFamilyLabel(colorHex)
    }

private fun remainingGroupLabel(fraction: Float): String {
    val percent = (fraction.coerceIn(0f, 1f) * 100f).roundToInt()
    return when {
        percent < 10 -> "0-9%"
        percent < 25 -> "10-24%"
        percent < 50 -> "25-49%"
        percent < 75 -> "50-74%"
        else -> "75-100%"
    }
}

private fun colorFamilyLabel(rawHex: String): String {
    val normalized = rawHex.trim().removePrefix("#")
    if (normalized.length < 6) return "Unknown color"
    val rgb = normalized.take(6)
    val r = rgb.substring(0, 2).toIntOrNull(16) ?: return "Unknown color"
    val g = rgb.substring(2, 4).toIntOrNull(16) ?: return "Unknown color"
    val b = rgb.substring(4, 6).toIntOrNull(16) ?: return "Unknown color"

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    if (max < 32) return "Black"
    if (delta < 12 && max > 220) return "White"
    if (delta < 12) return "Gray"

    val hue = hueDegrees(r, g, b)
    return when {
        hue < 20f -> "Red"
        hue < 45f -> "Orange"
        hue < 70f -> "Yellow"
        hue < 155f -> "Green"
        hue < 200f -> "Cyan"
        hue < 255f -> "Blue"
        hue < 300f -> "Purple"
        hue < 340f -> "Pink"
        else -> "Red"
    }
}

private fun colorFamilyRank(rawHex: String): Int =
    when (colorFamilyLabel(rawHex)) {
        "Black" -> 0
        "Gray" -> 1
        "White" -> 2
        "Red" -> 3
        "Orange" -> 4
        "Yellow" -> 5
        "Green" -> 6
        "Cyan" -> 7
        "Blue" -> 8
        "Purple" -> 9
        "Pink" -> 10
        else -> 11
    }

private fun hueDegrees(r: Int, g: Int, b: Int): Float {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val delta = max - min
    if (delta == 0f) return 0f
    val rawHue = when (max) {
        rf -> ((gf - bf) / delta) % 6f
        gf -> ((bf - rf) / delta) + 2f
        else -> ((rf - gf) / delta) + 4f
    } * 60f
    return if (rawHue < 0f) rawHue + 360f else rawHue
}

private fun SpoolListEntry.matchesSearch(rawQuery: String): Boolean {
    val query = rawQuery.trim()
    if (query.isEmpty()) {
        return true
    }
    // Search only requested fields: name, vendor, color, material and comment.
    val searchable = listOf(
        name,
        vendorName,
        colorHex,
        material,
        comment
    ).joinToString(separator = " ").normalizeSearch()
    // Split by spaces so multi-word input narrows results incrementally while typing.
    return query.normalizeSearch()
        .split(" ")
        .filter { it.isNotBlank() }
        .all { token -> searchable.fuzzyContains(token) }
}

private fun String.normalizeSearch(): String =
    trim().lowercase()

private fun String.fuzzyContains(token: String): Boolean {
    if (contains(token)) {
        return true
    }
    // Fuzzy fallback: match token as an ordered subsequence.
    var queryIndex = 0
    for (char in this) {
        if (queryIndex < token.length && char == token[queryIndex]) {
            queryIndex++
            if (queryIndex == token.length) {
                return true
            }
        }
    }
    return false
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

@Composable
fun WriteErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        title = { Text(text = "NFC Write Failed") },
        text = { Text(text = message) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "OK")
            }
        }
    )
}

@Composable
fun ReadTagDialog(
    readTagInfo: ReadTagInfo,
    spool: SpoolListEntry?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Large header with a color swatch matching the filament.
                val headerColor = spool?.color ?: MaterialTheme.colorScheme.surfaceVariant
                val headerBorder = spool?.color?.subtleBorderVariant()
                    ?: MaterialTheme.colorScheme.outlineVariant
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (spool != null &&
                        spool.multiColors.isNotEmpty() &&
                        spool.multiColors.any { it != Color.Transparent }
                    ) {
                        // Render multi-color filaments using the same direction logic as the card.
                        val direction = spool.multiColorsDirection.trim().lowercase()
                        val isLongitudinal = direction == "longitudinal"
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, headerBorder)
                        ) {
                            if (isLongitudinal) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    spool.multiColors.forEach { c ->
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp, 24.dp)
                                                .background(c)
                                        )
                                    }
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    spool.multiColors.forEach { c ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp, 48.dp)
                                                .background(c)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (spool != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(headerColor)
                                .border(1.dp, headerBorder)
                        )
                    } else {
                        // Unknown tag: show NFC-off icon instead of a color swatch.
                        Image(
                            modifier = Modifier.size(48.dp),
                            painter = painterResource(id = R.drawable.ic_nfc_off),
                            contentDescription = "Unknown NFC tag",
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = spool?.let { "${it.vendorName} - ${it.name}" } ?: "Unknown tag",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (spool != null) {
                            Text(
                                text = "Spool #${spool.id}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Slightly blended label color for field descriptions.
                val labelColor = lerp(
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.surface,
                    0.4f
                )
                // Always show tag UID, even if the payload cannot be parsed.
                Row {
                    Text(text = "Tag ID:", color = labelColor)
                    Text(text = readTagInfo.tagId, modifier = Modifier.padding(start = 8.dp))
                }

                if (spool != null) {
                    // Rich details from the loaded spool list.
                    Row {
                        Text(text = "Spool ID:", color = labelColor)
                        Text(text = "#${spool.id}", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row {
                        Text(text = "Filament ID:", color = labelColor)
                        Text(text = "#${spool.filamentId}", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row {
                        Text(text = "Material:", color = labelColor)
                        Text(text = spool.material, modifier = Modifier.padding(start = 8.dp))
                    }
                    Row {
                        Text(text = "Diameter:", color = labelColor)
                        Text(text = "${spool.diameter} mm", modifier = Modifier.padding(start = 8.dp))
                    }
                    if (spool.totalWeight.isNotEmpty()) {
                        Row {
                            Text(text = "Spool weight:", color = labelColor)
                            Text(text = spool.totalWeight, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    if (spool.remainingWeight.isNotEmpty()) {
                        val remainingPercent = (spool.remainingFraction * 100).roundToInt()
                        Row {
                            Text(text = "Remaining:", color = labelColor)
                            Text(
                                text = "${spool.remainingWeight} (${remainingPercent}%)",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                } else {
                    // Fallback when the spool list is unavailable or the tag is unknown.
                    if (!readTagInfo.rawText.isNullOrBlank()) {
                        Row {
                            Text(text = "Raw payload:", color = labelColor)
                            Text(text = readTagInfo.rawText, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TextButton(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onClick = onDismiss
                    ) {
                        Text(stringResource(id = R.string.dismiss))
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
