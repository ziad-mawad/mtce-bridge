package com.example.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNotifications
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system.RootUtil
import com.example.ui.theme.CockpitAmber
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitCrimson
import com.example.ui.theme.CockpitEmerald
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun PermissionsScreen(
    permissionState: PermissionState,
    onRefreshPermissions: () -> Unit,
    onRequestRuntimePermissions: () -> Unit,
    onRequestRootCheck: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isInstallingSystemApp by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }
    val installLogs = remember { mutableStateListOf<String>() }
    var installSuccess by remember { mutableStateOf<Boolean?>(null) }
    var showRebootConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header summary
        Card(
            modifier = Modifier.fillMaxWidth().testTag("permissions_summary_card"),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SYSTEM BYPASS & PRIVILEGE MANAGEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "MTCE Android car head units require persistent system-level hooks and bypasses to intercept external Bluetooth serial packets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            // Sequential launch
                            if (!permissionState.batteryOptimizationIgnored) {
                                PermissionHelper.requestBatteryOptimization(context)
                            } else if (!permissionState.overlayGranted) {
                                PermissionHelper.requestOverlayPermission(context)
                            } else if (!permissionState.writeSettingsGranted) {
                                PermissionHelper.requestWriteSettingsPermission(context)
                            } else if (!permissionState.bluetoothGranted) {
                                onRequestRuntimePermissions()
                            } else {
                                onRequestRootCheck()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("run_wizard_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF00363D))
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Permission Wizard", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRefreshPermissions,
                        modifier = Modifier.testTag("refresh_permissions_button")
                    ) {
                        Text("Refresh")
                    }
                }
            }
        }

        // Section: "Make System App" (Core Requirement 1)
        Card(
            modifier = Modifier.fillMaxWidth().testTag("make_system_app_card"),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (permissionState.isSystemApp) CockpitEmerald else CockpitAmber
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = "System App",
                            tint = if (permissionState.isSystemApp) CockpitEmerald else CockpitAmber
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privileged System App (/system/priv-app)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    StatusBadge(
                        status = if (permissionState.isSystemApp) "INSTALLED" else "USER APP",
                        isSuccess = permissionState.isSystemApp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Requires root (su). Remounts /system rw, copies APK to /system/priv-app/MtceBtBridge, sets permissions to 644, and allows native BLUETOOTH_PRIVILEGED respect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            showInstallDialog = true
                            installLogs.clear()
                            installSuccess = null
                            isInstallingSystemApp = true
                            scope.launch {
                                val success = RootUtil.makeSystemApp(context) { logLine ->
                                    installLogs.add(logLine)
                                }
                                installSuccess = success
                                isInstallingSystemApp = false
                                onRefreshPermissions()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("make_system_app_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (permissionState.isSystemApp) CockpitSurfaceVariant else CockpitAmber,
                            contentColor = if (permissionState.isSystemApp) TextPrimary else Color.Black
                        )
                    ) {
                        Text(
                            text = if (permissionState.isSystemApp) "Re-install System App" else "Make System App (SU)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { showRebootConfirmDialog = true },
                        modifier = Modifier.testTag("reboot_device_button")
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reboot")
                    }
                }
            }
        }

        // Section: System Bypasses & Permissions Checklist
        Text(
            text = "INDIVIDUAL SYSTEM BYPASSES",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // 1. Root Access
        PermissionItemCard(
            title = "Root Access (su)",
            description = "Provides superuser capability to remount filesystem and control hardware bridging.",
            icon = Icons.Default.LockOpen,
            isGranted = permissionState.hasRootAccess,
            actionLabel = "Request SU",
            testTag = "perm_root_card",
            buttonTestTag = "request_su_button",
            onAction = onRequestRootCheck
        )

        // 2. Battery Optimization
        PermissionItemCard(
            title = "Ignore Battery Optimizations",
            description = "Prevents Android Doze / PowerManager from terminating the bridge during vehicle idle.",
            icon = Icons.Default.BatteryAlert,
            isGranted = permissionState.batteryOptimizationIgnored,
            actionLabel = "Bypass Doze",
            testTag = "perm_battery_card",
            buttonTestTag = "bypass_battery_button",
            onAction = { PermissionHelper.requestBatteryOptimization(context) }
        )

        // 3. Overlay Permission (SYSTEM_ALERT_WINDOW)
        PermissionItemCard(
            title = "Draw Over Other Apps",
            description = "Permits bridge status overlays and high-priority background activity launching.",
            icon = Icons.Default.Layers,
            isGranted = permissionState.overlayGranted,
            actionLabel = "Grant Overlay",
            testTag = "perm_overlay_card",
            buttonTestTag = "grant_overlay_button",
            onAction = { PermissionHelper.requestOverlayPermission(context) }
        )

        // 4. Modify System Settings (WRITE_SETTINGS)
        PermissionItemCard(
            title = "Modify System Settings",
            description = "Enables writing system Bluetooth flags and car configuration properties.",
            icon = Icons.Default.SettingsApplications,
            isGranted = permissionState.writeSettingsGranted,
            actionLabel = "Grant Settings",
            testTag = "perm_write_settings_card",
            buttonTestTag = "grant_settings_button",
            onAction = { PermissionHelper.requestWriteSettingsPermission(context) }
        )

        // 5. Bluetooth Runtime Permissions
        PermissionItemCard(
            title = "Bluetooth & Location Stack",
            description = "Grants standard Android Bluetooth connectivity, scan, and admin privileges.",
            icon = Icons.Default.Security,
            isGranted = permissionState.bluetoothGranted,
            actionLabel = "Grant BT",
            testTag = "perm_bt_card",
            buttonTestTag = "grant_bt_button",
            onAction = onRequestRuntimePermissions
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // "Make System App" Progress Dialog
    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isInstallingSystemApp) showInstallDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isInstallingSystemApp) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NeonCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Installing to /system/priv-app...", color = TextPrimary)
                    } else if (installSuccess == true) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CockpitEmerald)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Installation Succeeded", color = CockpitEmerald)
                    } else {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = CockpitCrimson)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Installation Failed", color = CockpitCrimson)
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = "Script Execution Log:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (installLogs.isEmpty()) "Executing mount, copy, and chmod scripts..." else installLogs.joinToString("\n"),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                }
            },
            confirmButton = {
                if (!isInstallingSystemApp) {
                    if (installSuccess == true) {
                        Button(
                            onClick = {
                                showInstallDialog = false
                                scope.launch { RootUtil.rebootDevice() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CockpitEmerald, contentColor = Color.Black)
                        ) {
                            Text("Reboot Head Unit Now")
                        }
                    } else {
                        Button(onClick = { showInstallDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                }
            },
            dismissButton = {
                if (!isInstallingSystemApp && installSuccess == true) {
                    TextButton(onClick = { showInstallDialog = false }) {
                        Text("Reboot Later")
                    }
                }
            },
            containerColor = CockpitSurface
        )
    }

    // Reboot Confirmation Dialog
    if (showRebootConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRebootConfirmDialog = false },
            title = { Text("Reboot Head Unit?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Rebooting will restart the Android subsystem and reload /system/priv-app permissions.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRebootConfirmDialog = false
                        scope.launch { RootUtil.rebootDevice() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CockpitAmber, contentColor = Color.Black)
                ) {
                    Text("Confirm Reboot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = CockpitSurface
        )
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    testTag: String,
    buttonTestTag: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) CockpitEmerald.copy(alpha = 0.15f) else CockpitAmber.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) CockpitEmerald else CockpitAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isGranted) {
                StatusBadge(status = "ACTIVE", isSuccess = true)
            } else {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.testTag(buttonTestTag)
                ) {
                    Text(actionLabel, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, isSuccess: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSuccess) CockpitEmerald.copy(alpha = 0.2f) else CockpitAmber.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            color = if (isSuccess) CockpitEmerald else CockpitAmber,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}
