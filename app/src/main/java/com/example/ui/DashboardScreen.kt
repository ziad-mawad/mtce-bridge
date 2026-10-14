package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bridge.BluetoothDeviceFactory
import com.example.bridge.MockBluetoothBroadcaster
import com.example.data.LogCategory
import com.example.data.LogLevel
import com.example.data.LogRepository
import com.example.service.BluetoothBridgeService
import com.example.ui.theme.CockpitAmber
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitEmerald
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    permissionState: PermissionState,
    onNavigateToPermissions: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val context = LocalContext.current
    var isServiceRunning by remember { mutableStateOf(BluetoothBridgeService.isRunning) }
    var isMockConnected by remember { mutableStateOf(MockBluetoothBroadcaster.isMockConnected) }
    var currentMac by remember { mutableStateOf(BluetoothDeviceFactory.currentMacAddress) }
    var currentName by remember { mutableStateOf(BluetoothDeviceFactory.currentDeviceName) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Live uptime ticker
    var uptimeText by remember { mutableStateOf(BluetoothBridgeService.getFormattedUptime()) }
    var processedCount by remember { mutableIntStateOf(BluetoothBridgeService.getIntentCount()) }

    LaunchedEffect(Unit) {
        while (true) {
            isServiceRunning = BluetoothBridgeService.isRunning
            isMockConnected = MockBluetoothBroadcaster.isMockConnected
            uptimeText = BluetoothBridgeService.getFormattedUptime()
            processedCount = BluetoothBridgeService.getIntentCount()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Head Unit Vehicle Telemetry Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("telemetry_banner_card"),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning) CockpitEmerald.copy(alpha = 0.2f) else CockpitAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Car Status",
                                tint = if (isServiceRunning) CockpitEmerald else CockpitAmber
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MTCE / RK3368 BRIDGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isServiceRunning) "SERVICE ACTIVE" else "SERVICE STOPPED",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isServiceRunning) CockpitEmerald else CockpitAmber,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Main Service Toggle
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { start ->
                            if (start) {
                                val intent = Intent(context, BluetoothBridgeService::class.java)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                isServiceRunning = true
                            } else {
                                val intent = Intent(context, BluetoothBridgeService::class.java).apply {
                                    action = BluetoothBridgeService.ACTION_STOP
                                }
                                context.startService(intent)
                                isServiceRunning = false
                            }
                        },
                        modifier = Modifier.testTag("service_toggle_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CockpitEmerald,
                            checkedTrackColor = CockpitEmerald.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CockpitSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Telemetry metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricPill(label = "UPTIME", value = uptimeText)
                    MetricPill(label = "MCU INTENTS", value = "$processedCount")
                    MetricPill(
                        label = "ROOT / PRIV-APP",
                        value = if (permissionState.isSystemApp) "PRIV-APP" else if (permissionState.hasRootAccess) "ROOT OK" else "NORMAL"
                    )
                }
            }
        }

        // Mock Bluetooth Connection Switch Card (Core Requirement 5)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mock_bt_toggle_card"),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isMockConnected) CockpitEmerald else CockpitBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMockConnected) CockpitEmerald.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMockConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                contentDescription = "Bluetooth Status",
                                tint = if (isMockConnected) CockpitEmerald else NeonCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mock BT Connection",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isMockConnected) "Broadcasting STATE_CONNECTED" else "Broadcasting DISCONNECTED",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isMockConnected) CockpitEmerald else TextMuted
                            )
                        }
                    }

                    // Toggle switch for manual triggering without MCU
                    Switch(
                        checked = isMockConnected,
                        onCheckedChange = { connected ->
                            isMockConnected = connected
                            MockBluetoothBroadcaster.broadcastConnectionState(context, connected)
                        },
                        modifier = Modifier.testTag("mock_bt_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CockpitEmerald,
                            checkedTrackColor = CockpitEmerald.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CockpitSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Broadcasts ACTION_CONNECTION_STATE_CHANGED and ACL_CONNECTED with spoofed MAC to satisfy third-party CarPlay / DiPlay handshakes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Spoofed Bluetooth Device Card (Core Requirement 3)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("spoofed_device_card"),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPOOFED BLUETOOTH DEVICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(36.dp).testTag("edit_spoofed_device_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Spoofed Device",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CockpitSurfaceVariant)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PSEUDO-MAC ADDRESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = currentMac,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            color = CockpitAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DEVICE NAME",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = currentName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reflected into dummy BluetoothDevice extra passed in all OS broadcasts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // Manual Quick Action Triggers (Bench Testing Suite)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_actions_card"),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BENCH TEST BROADCAST SUITE",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Trigger simulated OS broadcasts or MCU intents to test third-party apps without the car MCU:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            MockBluetoothBroadcaster.broadcastConnectionState(context, true)
                        },
                        modifier = Modifier.testTag("broadcast_acl_connected_btn")
                    ) {
                        Text("ACL Connected")
                    }

                    OutlinedButton(
                        onClick = {
                            MockBluetoothBroadcaster.broadcastConnectionState(context, false)
                        },
                        modifier = Modifier.testTag("broadcast_acl_disconnected_btn")
                    ) {
                        Text("ACL Disconnected")
                    }

                    OutlinedButton(
                        onClick = {
                            // Send simulated MTCE Intent
                            val intent = Intent("com.microntek.bt.report").apply {
                                putExtra("connected", true)
                                putExtra("state", 1)
                                putExtra("device", currentMac)
                            }
                            context.sendBroadcast(intent)
                            LogRepository.log(
                                tag = "BENCH_SIM",
                                message = "Injected test intent com.microntek.bt.report (state=1, connected=true)",
                                category = LogCategory.MTCE_INTENT,
                                level = LogLevel.INFO
                            )
                        },
                        modifier = Modifier.testTag("simulate_mcu_report_btn")
                    ) {
                        Text("Simulate com.microntek.bt.report")
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent("com.microntek.btstate").apply {
                                putExtra("state", 2)
                                putExtra("connected", true)
                            }
                            context.sendBroadcast(intent)
                            LogRepository.log(
                                tag = "BENCH_SIM",
                                message = "Injected test intent com.microntek.btstate (state=2)",
                                category = LogCategory.MTCE_INTENT,
                                level = LogLevel.INFO
                            )
                        },
                        modifier = Modifier.testTag("simulate_mcu_btstate_btn")
                    ) {
                        Text("Simulate com.microntek.btstate")
                    }
                }
            }
        }

        // Watchdog & Boot Persistence Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("watchdog_info_card"),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Watchdog",
                        tint = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "5-Min AlarmManager Watchdog",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fires every 5m via ELAPSED_REALTIME_WAKEUP to revive bridge service if OEM killers terminate it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Edit MAC/Device Dialog
    if (showEditDialog) {
        var tempMac by remember { mutableStateOf(currentMac) }
        var tempName by remember { mutableStateOf(currentName) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("Configure Spoofed Device", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "CarPlay / DiPlay validates MAC address structure (AA:BB:CC:DD:EE:FF).",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = tempMac,
                        onValueChange = { tempMac = it },
                        label = { Text("Pseudo-MAC Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_mac_field")
                    )
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Device Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_name_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        BluetoothDeviceFactory.currentMacAddress = tempMac.trim().uppercase()
                        BluetoothDeviceFactory.currentDeviceName = tempName.trim()
                        currentMac = BluetoothDeviceFactory.currentMacAddress
                        currentName = BluetoothDeviceFactory.currentDeviceName
                        showEditDialog = false
                    },
                    modifier = Modifier.testTag("save_device_dialog_btn")
                ) {
                    Text("Save & Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = CockpitSurface
        )
    }
}

@Composable
fun MetricPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CockpitSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}
