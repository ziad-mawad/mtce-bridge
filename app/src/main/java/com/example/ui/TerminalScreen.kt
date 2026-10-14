package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bridge.BluetoothDeviceFactory
import com.example.data.LogCategory
import com.example.data.LogEntry
import com.example.data.LogLevel
import com.example.data.LogRepository
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    val allLogs by LogRepository.logsFlow.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf(LogCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredLogs = remember(allLogs, selectedCategory, searchQuery) {
        allLogs.filter { log ->
            val matchesCategory = (selectedCategory == LogCategory.ALL || log.category == selectedCategory)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                log.message.contains(searchQuery, ignoreCase = true) ||
                log.tag.contains(searchQuery, ignoreCase = true) ||
                (log.extras?.entries?.any { "${it.key}=${it.value}".contains(searchQuery, ignoreCase = true) } == true)
            }
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .padding(16.dp)
    ) {

        // Control header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LIVE MTCE TELEMETRY TERMINAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${filteredLogs.size} events captured",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val text = LogRepository.getAllLogsAsText()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("MTCE_BT_Logs", text)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied logs to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_logs_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Logs", tint = NeonCyan)
                }

                IconButton(
                    onClick = { LogRepository.clear() },
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Clear Logs", tint = CockpitAmber)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search actions, extras (e.g. bt.report, connected)...", fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("terminal_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CockpitSurface,
                unfocusedContainerColor = CockpitSurface,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CockpitBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                LogCategory.ALL to "ALL",
                LogCategory.MTCE_INTENT to "MTCE MCU",
                LogCategory.BROADCAST to "BROADCASTS",
                LogCategory.WATCHDOG to "WATCHDOG",
                LogCategory.ROOT_SYSTEM to "SYSTEM / ROOT",
                LogCategory.HOOK to "XPOSED HOOKS"
            ).forEach { (category, label) ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                        selectedLabelColor = NeonCyan,
                        containerColor = CockpitSurface,
                        labelColor = TextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == category,
                        borderColor = CockpitBorder,
                        selectedBorderColor = NeonCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Inject Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val intent = Intent("com.microntek.bt.report").apply {
                        putExtra("connected", true)
                        putExtra("state", 1)
                        putExtra("device", BluetoothDeviceFactory.currentMacAddress)
                        putExtra("caller", "MCU_SIMULATOR")
                    }
                    context.sendBroadcast(intent)
                },
                modifier = Modifier.weight(1f).testTag("inject_bt_report_btn")
            ) {
                Text("+ Inject bt.report", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent("com.microntek.btstate").apply {
                        putExtra("state", 2)
                        putExtra("connected", true)
                        putExtra("type", "A2DP_HFP")
                    }
                    context.sendBroadcast(intent)
                },
                modifier = Modifier.weight(1f).testTag("inject_btstate_btn")
            ) {
                Text("+ Inject btstate", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Log Entries List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CockpitSurface)
                    .border(1.dp, CockpitBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No log events matching current filters.\nEvents from MCU and OS broadcasts will stream here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF060A10))
                    .border(1.dp, CockpitBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp)
                    .testTag("terminal_logs_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    LogItemRow(log)
                }
            }
        }
    }
}

@Composable
fun LogItemRow(log: LogEntry) {
    var isExpanded by remember { mutableStateOf(false) }

    val levelColor = when (log.level) {
        LogLevel.SUCCESS -> CockpitEmerald
        LogLevel.WARN -> CockpitAmber
        LogLevel.ERROR -> CockpitCrimson
        LogLevel.INFO -> NeonCyan
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CockpitSurface)
            .clickable {
                if (!log.extras.isNullOrEmpty()) {
                    isExpanded = !isExpanded
                }
            }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.formattedTime,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(levelColor.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = log.tag,
                    fontSize = 10.sp,
                    color = levelColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!log.extras.isNullOrEmpty()) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = log.message,
            color = TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        // Expanded Intent Extras
        if (isExpanded && !log.extras.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(6.dp)
            ) {
                Text(
                    text = "INTENT EXTRAS DUMP:",
                    fontSize = 10.sp,
                    color = CockpitAmber,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                log.extras.forEach { (k, v) ->
                    Text(
                        text = "• $k: $v",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
            }
        }
    }
}
