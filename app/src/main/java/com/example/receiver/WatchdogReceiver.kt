package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.data.LogCategory
import com.example.data.LogLevel
import com.example.data.LogRepository
import com.example.service.BluetoothBridgeService

/**
 * AlarmManager Watchdog that fires every 5 minutes to ensure BluetoothBridgeService
 * remains persistently alive even after aggressive OEM task killing or deep sleep.
 */
class WatchdogReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WatchdogReceiver"
        const val ACTION_WATCHDOG_PING = "com.aistudio.mtcebridge.WATCHDOG_PING"
        const val INTERVAL_MILLIS = 5 * 60 * 1000L // 5 minutes

        fun scheduleWatchdog(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WatchdogReceiver::class.java).apply {
                action = ACTION_WATCHDOG_PING
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, flags)

            val triggerAtMillis = SystemClock.elapsedRealtime() + INTERVAL_MILLIS

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                LogRepository.log(
                    tag = TAG,
                    message = "Watchdog armed (+5 minutes timer scheduled)",
                    category = LogCategory.WATCHDOG,
                    level = LogLevel.INFO
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed scheduling watchdog alarm", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val isAlive = BluetoothBridgeService.isRunning
        Log.d(TAG, "Watchdog ping triggered. Service alive: $isAlive")

        if (!isAlive) {
            LogRepository.log(
                tag = TAG,
                message = "Watchdog detected BluetoothBridgeService was STOPPED! Relaunching foreground service...",
                category = LogCategory.WATCHDOG,
                level = LogLevel.WARN
            )
            val serviceIntent = Intent(context, BluetoothBridgeService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Watchdog restart failed", e)
                LogRepository.log(
                    tag = TAG,
                    message = "Watchdog restart error: ${e.message}",
                    category = LogCategory.WATCHDOG,
                    level = LogLevel.ERROR
                )
            }
        } else {
            LogRepository.log(
                tag = TAG,
                message = "Watchdog health check OK (Service alive, uptime: ${BluetoothBridgeService.getFormattedUptime()})",
                category = LogCategory.WATCHDOG,
                level = LogLevel.SUCCESS
            )
        }

        // Re-arm for the next 5-minute cycle
        scheduleWatchdog(context)
    }
}
