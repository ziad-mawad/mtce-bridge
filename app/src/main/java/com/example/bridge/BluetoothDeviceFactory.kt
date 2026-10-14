package com.example.bridge

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.data.LogCategory
import com.example.data.LogLevel
import com.example.data.LogRepository

/**
 * Constructs pseudo-BluetoothDevice objects using standard APIs or hidden reflection constructors.
 * Critical for MTCE / Rockchip RK3368 car head units where standard Bluetooth stack is dormant.
 */
object BluetoothDeviceFactory {
    private const val TAG = "DeviceFactory"
    const val DEFAULT_MAC = "02:11:22:33:44:55"
    const val DEFAULT_NAME = "MTCE_BT"

    @Volatile
    var currentMacAddress: String = DEFAULT_MAC

    @Volatile
    var currentDeviceName: String = DEFAULT_NAME

    fun createDevice(
        macAddress: String = currentMacAddress,
        deviceName: String = currentDeviceName
    ): BluetoothDevice {
        val cleanMac = macAddress.trim().uppercase()

        // 1. Try standard adapter first
        val deviceFromAdapter: BluetoothDevice? = try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.getRemoteDevice(cleanMac)
        } catch (e: Throwable) {
            Log.w(TAG, "Standard BluetoothAdapter getRemoteDevice failed: ${e.message}")
            null
        }

        if (deviceFromAdapter != null) {
            LogRepository.log(
                tag = TAG,
                message = "Created BluetoothDevice via standard adapter: $cleanMac ($deviceName)",
                category = LogCategory.BROADCAST,
                level = LogLevel.INFO
            )
            return deviceFromAdapter
        }

        // 2. Reflection fallback: hidden constructor BluetoothDevice(String address)
        try {
            val constructor = BluetoothDevice::class.java.getDeclaredConstructor(String::class.java)
            constructor.isAccessible = true
            val device = constructor.newInstance(cleanMac)

            LogRepository.log(
                tag = TAG,
                message = "Created BluetoothDevice via reflection constructor: $cleanMac ($deviceName)",
                category = LogCategory.BROADCAST,
                level = LogLevel.SUCCESS
            )
            return device
        } catch (t: Throwable) {
            Log.e(TAG, "Reflection instantiation of BluetoothDevice failed", t)
            LogRepository.log(
                tag = TAG,
                message = "Failed to construct BluetoothDevice for $cleanMac: ${t.message}",
                category = LogCategory.BROADCAST,
                level = LogLevel.ERROR
            )
            throw RuntimeException("Failed to construct dummy BluetoothDevice for $cleanMac", t)
        }
    }
}
