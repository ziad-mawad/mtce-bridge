package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.bridge.BluetoothDeviceFactory
import com.example.bridge.MockBluetoothBroadcaster
import com.example.data.LogCategory
import com.example.data.LogLevel
import com.example.data.LogRepository
import com.example.receiver.MtceIntentReceiver
import com.example.receiver.WatchdogReceiver
import java.util.concurrent.atomic.AtomicInteger

/**
 * Foreground persistent bridge service running on MTCE Rockchip RK3368 car head units.
 * Bridges MCU serial events to Android Bluetooth stack with START_STICKY and PRIORITY_LOW notification.
 */
class BluetoothBridgeService : Service() {

    companion object {
        private const val TAG = "BridgeService"
        private const val CHANNEL_ID = "mtce_bridge_service_channel"
        private const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.aistudio.mtcebridge.START"
        const val ACTION_STOP = "com.aistudio.mtcebridge.STOP"
        const val ACTION_TOGGLE_MOCK = "com.aistudio.mtcebridge.TOGGLE_MOCK"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var serviceStartTimeMillis: Long = 0
            private set

        private val intentCounter = AtomicInteger(0)

        fun incrementIntentCount() = intentCounter.incrementAndGet()
        fun getIntentCount(): Int = intentCounter.get()

        fun getFormattedUptime(): String {
            if (!isRunning || serviceStartTimeMillis == 0L) return "Stopped"
            val diffSec = (System.currentTimeMillis() - serviceStartTimeMillis) / 1000
            val hours = diffSec / 3600
            val minutes = (diffSec % 3600) / 60
            val seconds = diffSec % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    private var mtceReceiver: MtceIntentReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Creating BluetoothBridgeService")
        createNotificationChannel()

        // Register dynamic MTCE Intent receiver for all proprietary Microntek actions
        mtceReceiver = MtceIntentReceiver()
        val filter = IntentFilter().apply {
            MtceIntentReceiver.MTCE_ACTIONS.forEach { addAction(it) }
            // Additional wildcard / generic car intents
            addAction("com.microntek.irkeyDown")
            addAction("com.microntek.irkeyUp")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mtceReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(mtceReceiver, filter)
            }
            LogRepository.log(
                tag = TAG,
                message = "Registered MTCE Receiver for ${MtceIntentReceiver.MTCE_ACTIONS.size} Microntek actions",
                category = LogCategory.ROOT_SYSTEM,
                level = LogLevel.SUCCESS
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error registering MTCE receiver", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            LogRepository.log(
                tag = TAG,
                message = "Service stop requested by user.",
                category = LogCategory.ROOT_SYSTEM,
                level = LogLevel.WARN
            )
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_TOGGLE_MOCK) {
            val newState = !MockBluetoothBroadcaster.isMockConnected
            MockBluetoothBroadcaster.broadcastConnectionState(this, newState)
            updateNotification()
            return START_STICKY
        }

        if (!isRunning) {
            isRunning = true
            serviceStartTimeMillis = System.currentTimeMillis()
            LogRepository.log(
                tag = TAG,
                message = "BluetoothBridgeService started in Foreground (START_STICKY active)",
                category = LogCategory.ROOT_SYSTEM,
                level = LogLevel.SUCCESS
            )
        }

        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground with type, falling back", e)
            startForeground(NOTIFICATION_ID, notification)
        }

        // Arm the 5-minute Watchdog alarm
        WatchdogReceiver.scheduleWatchdog(this)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MTCE Bluetooth Bridge Status"
            val descriptionText = "Displays active background bridge status for car head unit MCU"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, BluetoothBridgeService::class.java).apply {
            action = ACTION_TOGGLE_MOCK
        }
        val pToggle = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (MockBluetoothBroadcaster.isMockConnected) {
            "Simulating Connected: ${BluetoothDeviceFactory.currentMacAddress} (MTCE_BT)"
        } else {
            "Listening for Microntek MCU events... (Intents: ${intentCounter.get()})"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MTCE Bluetooth Bridge Active")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pOpenApp)
            .addAction(
                0,
                if (MockBluetoothBroadcaster.isMockConnected) "Mock Disconnect" else "Mock Connect",
                pToggle
            )
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        mtceReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (ignored: Exception) {}
        }
        LogRepository.log(
            tag = TAG,
            message = "BluetoothBridgeService destroyed. Triggering watchdog reschedule...",
            category = LogCategory.ROOT_SYSTEM,
            level = LogLevel.WARN
        )
        // Ensure watchdog is primed to revive if killed unexpectedly
        WatchdogReceiver.scheduleWatchdog(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
