package com.example.system

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.LogCategory
import com.example.data.LogLevel
import com.example.data.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

data class CommandResult(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>
) {
    val isSuccess: Boolean get() = exitCode == 0
    val outputText: String get() = (stdout + stderr).joinToString("\n")
}

object RootUtil {
    private const val TAG = "RootUtil"

    fun isPrivilegedSystemApp(context: Context): Boolean {
        val path = context.applicationInfo.sourceDir ?: ""
        return path.startsWith("/system/priv-app") || path.startsWith("/product/priv-app")
    }

    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        val result = executeSu("id")
        val hasRoot = result.isSuccess && result.stdout.any { it.contains("uid=0") }
        LogRepository.log(
            tag = TAG,
            message = if (hasRoot) "Root access confirmed: uid=0(root)" else "Root not available or denied: ${result.outputText}",
            category = LogCategory.ROOT_SYSTEM,
            level = if (hasRoot) LogLevel.SUCCESS else LogLevel.WARN
        )
        hasRoot
    }

    suspend fun executeSu(command: String): CommandResult = withContext(Dispatchers.IO) {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes(command + "\n")
                os.writeBytes("exit\n")
                os.flush()
            }

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { stdout.add(it) }
                }
            }

            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { stderr.add(it) }
                }
            }

            val exitCode = process.waitFor()
            CommandResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed: $command", e)
            CommandResult(-1, stdout, listOf(e.message ?: "Execution error"))
        } finally {
            process?.destroy()
        }
    }

    /**
     * Executes the system priv-app installation routine for MTCE / Rockchip RK3368 head units.
     * Copies APK into /system/priv-app/MtceBtBridge, sets 644 permissions, and syncs.
     */
    suspend fun makeSystemApp(context: Context, onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val sourceApk = context.applicationInfo.sourceDir
        if (sourceApk.isNullOrBlank()) {
            onProgress("Error: Could not locate source APK path.")
            return@withContext false
        }

        val targetDir = "/system/priv-app/MtceBtBridge"
        val targetApk = "$targetDir/MtceBtBridge.apk"

        onProgress("Current APK location: $sourceApk")
        LogRepository.log(
            tag = TAG,
            message = "Starting 'Make System App' installation routine...",
            category = LogCategory.ROOT_SYSTEM,
            level = LogLevel.INFO
        )

        val script = StringBuilder().apply {
            // Remount system rw (handling Android 9 and RK3368 variants)
            append("mount -o rw,remount /system 2>/dev/null || mount -o rw,remount / 2>/dev/null || mount -o rw,remount /system_root 2>/dev/null\n")
            // Create target folder
            append("mkdir -p $targetDir\n")
            // Copy APK
            append("cp \"$sourceApk\" \"$targetApk\"\n")
            // Set permissions
            append("chmod 755 $targetDir\n")
            append("chmod 644 \"$targetApk\"\n")
            append("chown -R root:root $targetDir\n")
            // SELinux context
            append("chcon -R u:object_r:system_file:s0 $targetDir 2>/dev/null || true\n")
            // Sync & remount ro
            append("sync\n")
            append("mount -o ro,remount /system 2>/dev/null || true\n")
            append("echo 'INSTALL_COMPLETE'\n")
        }.toString()

        val result = executeSu(script)
        for (line in result.stdout) {
            onProgress("[STDOUT] $line")
        }
        for (line in result.stderr) {
            onProgress("[STDERR] $line")
        }

        val success = result.isSuccess && result.stdout.any { it.contains("INSTALL_COMPLETE") }
        if (success) {
            LogRepository.log(
                tag = TAG,
                message = "Successfully installed to $targetApk with 644 permissions. Device reboot required to apply BLUETOOTH_PRIVILEGED.",
                category = LogCategory.ROOT_SYSTEM,
                level = LogLevel.SUCCESS
            )
            onProgress("SUCCESS: Installed into /system/priv-app/! BLUETOOTH_PRIVILEGED will take effect upon reboot.")
        } else {
            LogRepository.log(
                tag = TAG,
                message = "Make System App script failed. Exit code: ${result.exitCode}. Output: ${result.outputText}",
                category = LogCategory.ROOT_SYSTEM,
                level = LogLevel.ERROR
            )
            onProgress("FAILED: Could not write to /system. Verify root / magisk permissions.")
        }
        success
    }

    suspend fun rebootDevice(): CommandResult = withContext(Dispatchers.IO) {
        LogRepository.log(
            tag = TAG,
            message = "Initiating device reboot via SU...",
            category = LogCategory.ROOT_SYSTEM,
            level = LogLevel.WARN
        )
        executeSu("reboot || /system/bin/reboot")
    }
}
