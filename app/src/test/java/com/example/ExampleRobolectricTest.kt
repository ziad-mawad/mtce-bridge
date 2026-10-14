package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.bridge.BluetoothDeviceFactory
import com.example.bridge.MockBluetoothBroadcaster
import com.example.data.LogCategory
import com.example.data.LogRepository
import com.example.receiver.MtceIntentReceiver
import com.example.receiver.WatchdogReceiver
import com.example.xposed.BluetoothHook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies MTCE BT Bridge name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MTCE BT Bridge", appName)
    }

    @Test
    fun `bluetoothDeviceFactory creates device with pseudo MAC`() {
        val device = BluetoothDeviceFactory.createDevice("02:11:22:33:44:55", "MTCE_BT")
        assertNotNull(device)
        assertEquals("02:11:22:33:44:55", device.address)
    }

    @Test
    fun `mockBluetoothBroadcaster toggles connected state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MockBluetoothBroadcaster.broadcastConnectionState(context, connected = true)
        assertTrue(MockBluetoothBroadcaster.isMockConnected)

        MockBluetoothBroadcaster.broadcastConnectionState(context, connected = false)
        assertEquals(false, MockBluetoothBroadcaster.isMockConnected)
    }

    @Test
    fun `mtceIntentReceiver handles microntek report and logs to repository`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = MtceIntentReceiver()

        val intent = Intent("com.microntek.bt.report").apply {
            putExtra("connected", true)
            putExtra("state", 1)
            putExtra("device", "02:11:22:33:44:55")
        }

        receiver.onReceive(context, intent)

        val logs = LogRepository.logsFlow.value
        val foundMtceLog = logs.any { it.category == LogCategory.MTCE_INTENT && it.message.contains("com.microntek.bt.report") }
        assertTrue(foundMtceLog)
    }

    @Test
    fun `bluetoothHook companion creates dummy device`() {
        val dummy = BluetoothHook.getOrCreateDummyDevice()
        assertNotNull(dummy)
        assertEquals(BluetoothHook.SPOOFED_MAC, dummy.address)
    }
}
