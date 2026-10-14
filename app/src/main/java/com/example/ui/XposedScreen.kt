package com.example.ui

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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LogCategory
import com.example.data.LogLevel
import com.example.data.LogRepository
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

@Composable
fun XposedScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Module Header
        Card(
            modifier = Modifier.fillMaxWidth().testTag("xposed_module_info_card"),
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
                                .background(NeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Extension, contentDescription = null, tint = NeonCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "LSPOSED / XPOSED MODULE",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "BluetoothHook Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = CockpitEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    StatusBadge(status = "XPOSED READY", isSuccess = true)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Declared in AndroidManifest.xml (xposedmodule=true, minversion=82) and loaded via assets/xposed_init targeting BluetoothAdapter framework methods.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Active Interception Rules
        Text(
            text = "HOOKED FRAMEWORK METHODS",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        HookSpecCard(
            signature = "BluetoothAdapter.isEnabled()",
            behavior = "Forces return true across target apps",
            retVal = "true"
        )

        HookSpecCard(
            signature = "BluetoothAdapter.getState()",
            behavior = "Forces standard Android Bluetooth state to ON",
            retVal = "STATE_ON (12)"
        )

        HookSpecCard(
            signature = "BluetoothAdapter.getConnectionState()",
            behavior = "Reports standard profile state as connected",
            retVal = "STATE_CONNECTED (2)"
        )

        HookSpecCard(
            signature = "BluetoothAdapter.getBondedDevices()",
            behavior = "Injects Set containing spoofed MTCE BluetoothDevice",
            retVal = "Set<BluetoothDevice> [02:11:22:33:44:55]"
        )

        HookSpecCard(
            signature = "BluetoothDevice.getBondState()",
            behavior = "Reports spoofed device as bonded",
            retVal = "BOND_BONDED (12)"
        )

        // Setup Instructions
        Card(
            modifier = Modifier.fillMaxWidth().testTag("xposed_guide_card"),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HOW TO ENABLE IN LSPOSED",
                    style = MaterialTheme.typography.labelSmall,
                    color = CockpitAmber,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Open LSPosed / EdXposed Manager on the MTCE head unit.\n" +
                           "2. Go to Modules tab and toggle 'MTCE BT Bridge' ON.\n" +
                           "3. In the module scope, select target apps (e.g. DiPlay, CarLink, Android Auto, System Framework).\n" +
                           "4. Soft reboot the head unit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HookSpecCard(signature: String, behavior: String, retVal: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = signature,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = behavior,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Replaces return: ",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = retVal,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CockpitEmerald,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
