package com.hexxotest.spoolcompanion

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.hexxotest.spoolcompanion.ui.NfcTagViewModel
import com.hexxotest.spoolcompanion.ui.NoNfcApp
import com.hexxotest.spoolcompanion.ui.ReadTagInfo
import com.hexxotest.spoolcompanion.ui.SpoolCompanionApp
import com.hexxotest.spoolcompanion.ui.theme.SpoolCompanionTheme

// DataStore setup for future settings persistence (currently SharedPreferences is used in UI).
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    // Shared view-model holding the current NFC write selection + dialog state.
    private val nfcTagViewModel: NfcTagViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        // Init methods
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // NFC adapter may be null on devices without NFC hardware.
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Content handling
        setContent {
            SpoolCompanionTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    if (nfcAdapter == null) {
                        // Fallback UI when NFC is not available on the device.
                        NoNfcApp()
                    } else {
                        SpoolCompanionApp(nfcTagViewModel)
                    }
                }
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this)
    }

    public override fun onResume() {
        super.onResume()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null) {
            // Foreground dispatch keeps NFC intents routed to this activity while it's visible.
            val intent = Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_MUTABLE
            )
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.w("INTENT", intent.action.toString())
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            // Only write when the user explicitly initiated the flow (dialog visible).
            val tag: Tag = IntentCompat.getParcelableExtra(
                intent,
                NfcAdapter.EXTRA_TAG,
                Parcelable::class.java
            ) as Tag
            if (nfcTagViewModel.isDialogShown) {
                val url = nfcTagViewModel.spoolmanUrl
                if (!url.isNullOrBlank()) {
                    Log.w(
                        "NFC",
                        "SPOOL:${nfcTagViewModel.spoolId} | FILAMENT:${nfcTagViewModel.filamentId} | SPOOLMANURL:${url}"
                    )
                } else {
                    Log.w(
                        "NFC",
                        "SPOOL:${nfcTagViewModel.spoolId} | FILAMENT:${nfcTagViewModel.filamentId}"
                    )
                }
                
                // Payload format is a two-line text record consumed by the companion tool.
                val records = mutableListOf<NdefRecord>()
                records.add(NdefRecord.createTextRecord(
                    null,
                    "SPOOL:${nfcTagViewModel.spoolId}\nFILAMENT:${nfcTagViewModel.filamentId}"
                ))
                if (!url.isNullOrBlank() && nfcTagViewModel.spoolId != -1) {
                    try {
                        val spoolmanDeepLink = "$url/spool/show/${nfcTagViewModel.spoolId}"
                        records.add(NdefRecord.createUri(spoolmanDeepLink))
                    } catch (e: IllegalArgumentException) {
                        Log.e("NFC", "Failed to create URI record for: $url/spool/view/${nfcTagViewModel.spoolId}", e)
                    }
                }
                val msg = NdefMessage(records.toTypedArray())

                // Some tags are not NDEF-formatted; guard against null tech.
                val ndef = Ndef.get(tag)
                if (ndef == null) {
                    nfcTagViewModel.isDialogShown = false
                    nfcTagViewModel.writeErrorMessage = "This tag does not support NDEF."
                    return
                }
                try {
                    ndef.use {
                        it.connect()
                        it.writeNdefMessage(msg)
                    }
                    // Close dialog after a successful write.
                    nfcTagViewModel.isDialogShown = false
                } catch (e: Exception) {
                    // Surface write errors (e.g., read-only or insufficient capacity).
                    nfcTagViewModel.isDialogShown = false
                    nfcTagViewModel.writeErrorMessage =
                        e.message ?: "Failed to write NFC tag."
                }
            } else {
                // Otherwise, interpret the tag payload and surface it in a read dialog.
                val tagId = formatTagId(tag)
                val payload = readTagText(intent, tag)
                val (spoolId, filamentId) = parseTagPayload(payload)
                nfcTagViewModel.readTagInfo = ReadTagInfo(
                    tagId = tagId,
                    spoolId = spoolId,
                    filamentId = filamentId,
                    rawText = payload
                )
            }
        }
    }

    // Try to read a text payload from the NFC intent or cached NDEF message.
    private fun readTagText(intent: Intent, tag: Tag): String? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        val message = rawMessages
            ?.firstOrNull()
            ?.let { it as? NdefMessage }
            ?: Ndef.get(tag)?.cachedNdefMessage
        return message?.records?.firstNotNullOfOrNull { it.toText() }
    }

    // Parse the two-line payload format into spool + filament IDs.
    private fun parseTagPayload(payload: String?): Pair<Int?, Int?> {
        if (payload.isNullOrBlank()) return Pair(null, null)
        var spoolId: Int? = null
        var filamentId: Int? = null
        payload.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("SPOOL:", ignoreCase = true)) {
                spoolId = trimmed.substringAfter(":").trim().toIntOrNull()
            }
            if (trimmed.startsWith("FILAMENT:", ignoreCase = true)) {
                filamentId = trimmed.substringAfter(":").trim().toIntOrNull()
            }
        }
        return Pair(spoolId, filamentId)
    }

    // Decode a standard NFC text record (RTD_TEXT).
    private fun NdefRecord.toText(): String? {
        if (tnf != NdefRecord.TNF_WELL_KNOWN || !type.contentEquals(NdefRecord.RTD_TEXT)) {
            return null
        }
        val data = payload
        if (data.isEmpty()) return null
        val languageCodeLength = data[0].toInt() and 0x3F
        return String(
            data,
            1 + languageCodeLength,
            data.size - 1 - languageCodeLength,
            Charsets.UTF_8
        )
    }

    // Format the NFC tag UID as a hex string for display.
    private fun formatTagId(tag: Tag): String =
        tag.id.joinToString("") { "%02X".format(it) }

}
