package com.example.xposed

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * LSPosed / Xposed hook module for MTCE / Rockchip RK3368 head units.
 *
 * MTCE head units handle Bluetooth externally via MCU serial commands, leaving
 * the standard Android BluetoothAdapter inactive or disconnected. Third-party apps
 * (such as DiPlay, CarPlay receivers, and Auto apps) query BluetoothAdapter APIs
 * and refuse to connect.
 *
 * This hook intercepts android.bluetooth.BluetoothAdapter and forces:
 * 1. isEnabled() -> true
 * 2. getState() -> BluetoothAdapter.STATE_ON (12)
 * 3. getConnectionState() -> BluetoothProfile.STATE_CONNECTED (2)
 * 4. getBondedDevices() -> Set containing spoofed MTCE BluetoothDevice (02:11:22:33:44:55)
 */
class BluetoothHook : IXposedHookLoadPackage {

    companion object {
        const val TAG = "MTCE_BT_HOOK"
        const val SPOOFED_MAC = "02:11:22:33:44:55"
        const val SPOOFED_NAME = "MTCE_BT"

        @Volatile
        private var cachedDummyDevice: BluetoothDevice? = null

        fun getOrCreateDummyDevice(): BluetoothDevice {
            cachedDummyDevice?.let { return it }
            val device = try {
                // Try standard adapter if available
                BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(SPOOFED_MAC)
            } catch (ignored: Throwable) {
                null
            } ?: run {
                // Fallback to reflection on hidden constructor BluetoothDevice(String)
                try {
                    val constructor = BluetoothDevice::class.java.getDeclaredConstructor(String::class.java)
                    constructor.isAccessible = true
                    constructor.newInstance(SPOOFED_MAC)
                } catch (t: Throwable) {
                    XposedBridge.log("[$TAG] Failed to instantiate dummy BluetoothDevice: ${t.message}")
                    null
                }
            }
            if (device != null) {
                cachedDummyDevice = device
                return device
            }
            throw IllegalStateException("Unable to create BluetoothDevice for $SPOOFED_MAC")
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Skip self
        if (lpparam.packageName == "com.aistudio.mtcebridge.rkbt") return

        XposedBridge.log("[$TAG] Initializing Bluetooth hook for package: ${lpparam.packageName}")

        try {
            val bluetoothAdapterClass = XposedHelpers.findClass("android.bluetooth.BluetoothAdapter", lpparam.classLoader)
                ?: return

            // 1. Hook isEnabled() -> always return true
            XposedHelpers.findAndHookMethod(
                bluetoothAdapterClass,
                "isEnabled",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = true
                        XposedBridge.log("[$TAG] Intercepted BluetoothAdapter.isEnabled() -> true for ${lpparam.packageName}")
                    }
                }
            )

            // 2. Hook getState() -> return BluetoothAdapter.STATE_ON
            XposedHelpers.findAndHookMethod(
                bluetoothAdapterClass,
                "getState",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = BluetoothAdapter.STATE_ON
                        XposedBridge.log("[$TAG] Intercepted BluetoothAdapter.getState() -> STATE_ON (12) for ${lpparam.packageName}")
                    }
                }
            )

            // 3. Hook getConnectionState() -> return BluetoothProfile.STATE_CONNECTED
            XposedHelpers.findAndHookMethod(
                bluetoothAdapterClass,
                "getConnectionState",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = BluetoothProfile.STATE_CONNECTED
                        XposedBridge.log("[$TAG] Intercepted BluetoothAdapter.getConnectionState() -> STATE_CONNECTED (2) for ${lpparam.packageName}")
                    }
                }
            )

            // 4. Hook getProfileConnectionState(int) -> return BluetoothProfile.STATE_CONNECTED
            XposedHelpers.findAndHookMethod(
                bluetoothAdapterClass,
                "getProfileConnectionState",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = BluetoothProfile.STATE_CONNECTED
                        XposedBridge.log("[$TAG] Intercepted BluetoothAdapter.getProfileConnectionState(${param.args[0]}) -> STATE_CONNECTED")
                    }
                }
            )

            // 5. Hook getBondedDevices() -> Set containing spoofed MTCE BluetoothDevice
            XposedHelpers.findAndHookMethod(
                bluetoothAdapterClass,
                "getBondedDevices",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val dummy = getOrCreateDummyDevice()
                            val set = HashSet<BluetoothDevice>()
                            // Preserve any existing devices if present
                            @Suppress("UNCHECKED_CAST")
                            val existing = param.result as? Set<BluetoothDevice>
                            if (existing != null) {
                                set.addAll(existing)
                            }
                            set.add(dummy)
                            param.result = set
                            XposedBridge.log("[$TAG] Injected dummy bonded device ($SPOOFED_MAC, $SPOOFED_NAME) for ${lpparam.packageName}")
                        } catch (t: Throwable) {
                            XposedBridge.log(t)
                        }
                    }
                }
            )

            // 6. Hook BluetoothDevice methods to ensure spoofed attributes
            val bluetoothDeviceClass = XposedHelpers.findClass("android.bluetooth.BluetoothDevice", lpparam.classLoader)
            if (bluetoothDeviceClass != null) {
                // Hook getName()
                XposedHelpers.findAndHookMethod(
                    bluetoothDeviceClass,
                    "getName",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val device = param.thisObject as? BluetoothDevice
                            if (device != null && device.address.equals(SPOOFED_MAC, ignoreCase = true)) {
                                param.result = SPOOFED_NAME
                            }
                        }
                    }
                )

                // Hook getBondState() -> BOND_BONDED
                XposedHelpers.findAndHookMethod(
                    bluetoothDeviceClass,
                    "getBondState",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val device = param.thisObject as? BluetoothDevice
                            if (device != null && device.address.equals(SPOOFED_MAC, ignoreCase = true)) {
                                param.result = BluetoothDevice.BOND_BONDED
                            }
                        }
                    }
                )
            }

        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Failed to install hooks on ${lpparam.packageName}: ${t.message}")
        }
    }
}
