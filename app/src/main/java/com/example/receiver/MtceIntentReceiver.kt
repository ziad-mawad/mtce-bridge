package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.bridge.MockBluetoothBroadcaster
import com.example.data.LogCategory
import com.example.data.LogLevel
import com.example.data.LogRepository
import com.example.service.BluetoothBridgeService

/**
 * Intercepts proprietary Microntek (MTCE / Rockchip RK3368) Bluetooth intents
 * and forwards standard Android Bluetooth broadcasts for third-party apps (e.g. CarPlay/DiPlay).
 */
class MtceIntentReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MtceReceiver"

        val MTCE_ACTIONS = listOf(
            "com.microntek.bt.report",
            "com.microntek.btstate",
            "com.microntek.bt.power",
            "com.microntek.bt.connect",
            "com.microntek.bt.connected",
            "com.microntek.bt.disconnected",
            "com.microntek.bt.pair",
            "com.microntek.bt.device",
            "com.microntek.bt.phone",
            "com.microntek.bt.music",
            "com.microntek.canbus.report",
            "com.microntek.sync",
            "com.microntek.bootcheck"
        )
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        // Extract and format all extras for debugging
        val extrasMap = mutableMapOf<String, String>()
        val bundle = intent.extras
        if (bundle != null) {
            for (key in bundle.keySet()) {
                val value = bundle.get(key)
                extrasMap[key] = value?.toString() ?: "null"
            }
        }

        BluetoothBridgeService.incrementIntentCount()

        LogRepository.log(
            tag = TAG,
            message = "Caught MTCE Intent: $action [Extras: ${extrasMap.size}]",
            category = LogCategory.MTCE_INTENT,
            level = LogLevel.INFO,
            extras = extrasMap
        )

        Log.d(TAG, "MTCE intent received: $action extras=$extrasMap")

        // Parse connection state from common MTCE intent patterns
        val isConnectedEvent = when {
            action.equals("com.microntek.bt.connected", ignoreCase = true) -> true
            action.equals("com.microntek.bt.disconnected", ignoreCase = true) -> false
            action.contains("connect", ignoreCase = true) -> {
                val state = intent.getIntExtra("state", -1)
                val connect = intent.getBooleanExtra("connect", false)
                val status = intent.getStringExtra("status")
                state == 1 || state == 2 || connect || status?.equals("connected", ignoreCase = true) == true
            }
            action.equals("com.microntek.btstate", ignoreCase = true) -> {
                val state = intent.getIntExtra("state", -1)
                state == 1 || state == 2 || intent.getBooleanExtra("connected", false)
            }
            action.equals("com.microntek.bt.report", ignoreCase = true) -> {
                // Check common report extras
                val connected = intent.getBooleanExtra("connected", false)
                val state = intent.getIntExtra("state", -1)
                connected || state == 1 || state == 2 || intent.hasExtra("device")
            }
            else -> false
        }

        if (isConnectedEvent) {
            LogRepository.log(
                tag = TAG,
                message = "MTCE connection event detected! Forwarding Android Bluetooth broadcasts with spoofed MAC...",
                category = LogCategory.MTCE_INTENT,
                level = LogLevel.SUCCESS
            )
            MockBluetoothBroadcaster.broadcastConnectionState(context, connected = true)
        } else if (action.contains("disconnect", ignoreCase = true)) {
            LogRepository.log(
                tag = TAG,
                message = "MTCE disconnection detected. Forwarding Bluetooth DISCONNECTED state.",
                category = LogCategory.MTCE_INTENT,
                level = LogLevel.WARN
            )
            MockBluetoothBroadcaster.broadcastConnectionState(context, connected = false)
        }
    }
}
