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
import com.hexxotest.spoolcompanion.ui.SpoolCompanionApp
import com.hexxotest.spoolcompanion.ui.theme.SpoolCompanionTheme

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    private val nfcTagViewModel: NfcTagViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        // Init methods
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // NFC
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
            val tag: Tag = IntentCompat.getParcelableExtra(
                intent,
                NfcAdapter.EXTRA_TAG,
                Parcelable::class.java
            ) as Tag
            if (nfcTagViewModel.isDialogShown) {
                Log.w(
                    "NFC",
                    "SPOOL:${nfcTagViewModel.spoolId} | FILAMENT:${nfcTagViewModel.filamentId}"
                )
                val msg = NdefMessage(
                    NdefRecord.createTextRecord(
                        null,
                        "SPOOL:${nfcTagViewModel.spoolId}\nFILAMENT:${nfcTagViewModel.filamentId}"
                    )
                )
                Ndef.get(tag).use {
                    it.connect()
                    it.writeNdefMessage(msg)
                }
                nfcTagViewModel.isDialogShown = false
            }
        }
    }

}
