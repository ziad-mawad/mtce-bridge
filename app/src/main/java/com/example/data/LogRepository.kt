package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

enum class LogCategory {
    ALL,
    MTCE_INTENT,
    BROADCAST,
    WATCHDOG,
    ROOT_SYSTEM,
    HOOK
}

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}

data class LogEntry(
    val id: Long = System.currentTimeMillis() + (Math.random() * 1000).toLong(),
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val category: LogCategory,
    val level: LogLevel = LogLevel.INFO,
    val extras: Map<String, String>? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}

object LogRepository {
    private const val MAX_LOGS = 500
    private val deque = ConcurrentLinkedDeque<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    init {
        log(
            tag = "INIT",
            message = "MTCE BT Bridge Log Engine initialized. Ready to intercept Rockchip / MTCE serial MCU events.",
            category = LogCategory.ROOT_SYSTEM,
            level = LogLevel.SUCCESS
        )
    }

    fun log(
        tag: String,
        message: String,
        category: LogCategory,
        level: LogLevel = LogLevel.INFO,
        extras: Map<String, String>? = null
    ) {
        val entry = LogEntry(
            tag = tag,
            message = message,
            category = category,
            level = level,
            extras = extras
        )
        deque.addFirst(entry)
        while (deque.size > MAX_LOGS) {
            deque.removeLast()
        }
        _logsFlow.value = deque.toList()
    }

    fun clear() {
        deque.clear()
        _logsFlow.value = emptyList()
        log(
            tag = "TERMINAL",
            message = "Logs cleared by user.",
            category = LogCategory.ROOT_SYSTEM,
            level = LogLevel.INFO
        )
    }

    fun getAllLogsAsText(): String {
        return deque.toList().reversed().joinToString("\n") { entry ->
            val extrasText = if (!entry.extras.isNullOrEmpty()) {
                " | Extras: " + entry.extras.entries.joinToString(", ") { "${it.key}=${it.value}" }
            } else ""
            "[${entry.formattedTime}] [${entry.category}] [${entry.tag}] ${entry.message}$extrasText"
        }
    }
}
