package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.LogCategory
import com.example.data.LogLevel
import com.example.data.LogRepository
import com.example.service.BluetoothBridgeService

/**
 * Boot receiver listening to Android BOOT_COMPLETED, MTCE head unit QUICKBOOT_POWERON,
 * and related boot checkpoints to automatically start BluetoothBridgeService and arm watchdog.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "Head unit boot event received: $action")

        LogRepository.log(
            tag = TAG,
            message = "Boot event detected: $action. Initiating background bridge and watchdog...",
            category = LogCategory.ROOT_SYSTEM,
            level = LogLevel.SUCCESS
        )

        val serviceIntent = Intent(context, BluetoothBridgeService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service on boot", e)
            LogRepository.log(
                tag = TAG,
                message = "Failed to launch service on boot: ${e.message}",
                category = LogCategory.ROOT_SYSTEM,
                level = LogLevel.ERROR
            )
        }

        // Arm the 5-minute watchdog
        WatchdogReceiver.scheduleWatchdog(context)
    }
}
