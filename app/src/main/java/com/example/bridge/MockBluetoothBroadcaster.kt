package com.example.bridge

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.LogCategory
import com.example.data.LogLevel
import com.example.data.LogRepository

/**
 * Manages dispatching mock Android Bluetooth broadcasts to trick third-party apps
 * (CarPlay dongles, DiPlay, AutoLink, media players) into seeing an active connection.
 */
object MockBluetoothBroadcaster {
    private const val TAG = "MockBroadcaster"

    @Volatile
    var isMockConnected: Boolean = false
        private set

    fun broadcastConnectionState(
        context: Context,
        connected: Boolean,
        macAddress: String = BluetoothDeviceFactory.currentMacAddress,
        deviceName: String = BluetoothDeviceFactory.currentDeviceName
    ) {
        isMockConnected = connected
        val dummyDevice = try {
            BluetoothDeviceFactory.createDevice(macAddress, deviceName)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot create dummy device for broadcast", e)
            return
        }

        val targetState = if (connected) BluetoothProfile.STATE_CONNECTED else BluetoothProfile.STATE_DISCONNECTED
        val prevState = if (connected) BluetoothProfile.STATE_DISCONNECTED else BluetoothProfile.STATE_CONNECTED

        // 1. BluetoothAdapter.ACTION_STATE_CHANGED (ensure BT is reported ON)
        val stateChangedIntent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
            putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON)
            putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_TURNING_ON)
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        sendBroadcastSafe(context, stateChangedIntent, "BluetoothAdapter.ACTION_STATE_CHANGED (STATE_ON)")

        // 2. BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED
        val connIntent = Intent(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED).apply {
            putExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, targetState)
            putExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE, prevState)
            putExtra(BluetoothDevice.EXTRA_DEVICE, dummyDevice)
            putExtra(BluetoothDevice.EXTRA_NAME, deviceName)
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        sendBroadcastSafe(
            context,
            connIntent,
            "BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> ${if (connected) "CONNECTED" else "DISCONNECTED"}"
        )

        // 3. ACL Connection broadcast
        val aclAction = if (connected) {
            BluetoothDevice.ACTION_ACL_CONNECTED
        } else {
            BluetoothDevice.ACTION_ACL_DISCONNECTED
        }
        val aclIntent = Intent(aclAction).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, dummyDevice)
            putExtra(BluetoothDevice.EXTRA_NAME, deviceName)
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        sendBroadcastSafe(context, aclIntent, aclAction)

        // 4. Bond state broadcast (if connecting, simulate BOND_BONDED)
        if (connected) {
            val bondIntent = Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, dummyDevice)
                putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED)
                putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_BONDING)
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            }
            sendBroadcastSafe(context, bondIntent, "BluetoothDevice.ACTION_BOND_STATE_CHANGED (BONDED)")
        }

        // 5. Headset (HFP) & A2DP profile connection broadcasts
        val hfpIntent = Intent(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED).apply {
            putExtra(BluetoothProfile.EXTRA_STATE, targetState)
            putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState)
            putExtra(BluetoothDevice.EXTRA_DEVICE, dummyDevice)
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        sendBroadcastSafe(context, hfpIntent, "BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED")

        val a2dpIntent = Intent(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED).apply {
            putExtra(BluetoothProfile.EXTRA_STATE, targetState)
            putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState)
            putExtra(BluetoothDevice.EXTRA_DEVICE, dummyDevice)
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        sendBroadcastSafe(context, a2dpIntent, "BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED")

        LogRepository.log(
            tag = TAG,
            message = "Dispatched full Bluetooth mock broadcast suite [State: ${if (connected) "CONNECTED" else "DISCONNECTED"}, Device: ${dummyDevice.address}]",
            category = LogCategory.BROADCAST,
            level = if (connected) LogLevel.SUCCESS else LogLevel.WARN,
            extras = mapOf(
                "MAC" to dummyDevice.address,
                "Device" to deviceName,
                "State" to if (connected) "CONNECTED (2)" else "DISCONNECTED (0)"
            )
        )
    }

    private fun sendBroadcastSafe(context: Context, intent: Intent, description: String) {
        try {
            context.sendBroadcast(intent)
            LogRepository.log(
                tag = TAG,
                message = "Sent broadcast: $description",
                category = LogCategory.BROADCAST,
                level = LogLevel.INFO
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending broadcast $description", e)
            LogRepository.log(
                tag = TAG,
                message = "Error sending $description: ${e.message}",
                category = LogCategory.BROADCAST,
                level = LogLevel.ERROR
            )
        }
    }
}
