package com.example.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {
    const val CRASH_LOG_FILE_NAME = "crash_log.txt"

    @Volatile
    var currentScreenName: String? = null

    /**
     * Installs the uncaught exception handler as early as possible.
     * Preserves and invokes the previous/default handler after logging.
     */
    fun install(context: Context) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("RECORDS_DEBUG", "Uncaught exception intercepted in thread: ${thread.name}", throwable)
                val crashReport = buildCrashReport(context, thread, throwable)
                writeCrashLog(context, crashReport)
                Log.e("CRASH_DIAGNOSTIC", "Crash log written to storage:\n$crashReport")
            } catch (e: Throwable) {
                // Defensive: never throw from the uncaught exception handler
                Log.e("CRASH_DIAGNOSTIC", "Failed to write crash log", e)
            } finally {
                // Always call the previous default handler to keep Android crash lifecycle intact
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
        Log.d("CRASH_DIAGNOSTIC", "Uncaught exception handler installed successfully")
    }

    /**
     * Synchronously writes crash report to:
     * 1. context.getExternalFilesDir(null)/"crash_log.txt"
     * 2. context.filesDir/"crash_log.txt"
     */
    fun writeCrashLog(context: Context, content: String) {
        // 1. Internal storage
        try {
            val internalFile = File(context.filesDir, CRASH_LOG_FILE_NAME)
            internalFile.writeText(content)
            Log.d("CRASH_DIAGNOSTIC", "Wrote crash log to internal: ${internalFile.absolutePath}")
        } catch (t: Throwable) {
            Log.e("CRASH_DIAGNOSTIC", "Error writing internal crash_log.txt", t)
        }

        // 2. External app files storage
        try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null) {
                val externalFile = File(externalDir, CRASH_LOG_FILE_NAME)
                externalFile.writeText(content)
                Log.d("CRASH_DIAGNOSTIC", "Wrote crash log to external: ${externalFile.absolutePath}")
            }
        } catch (t: Throwable) {
            Log.e("CRASH_DIAGNOSTIC", "Error writing external crash_log.txt", t)
        }
    }

    /**
     * Checks whether crash_log.txt exists in either internal or external storage and is not empty.
     */
    fun hasCrashLog(context: Context): Boolean {
        return readCrashLog(context) != null
    }

    /**
     * Reads the crash log from internal or external storage.
     */
    fun readCrashLog(context: Context): String? {
        // Check internal filesDir
        try {
            val internalFile = File(context.filesDir, CRASH_LOG_FILE_NAME)
            if (internalFile.exists() && internalFile.length() > 0) {
                val text = internalFile.readText()
                if (text.isNotBlank()) return text
            }
        } catch (t: Throwable) {
            Log.e("CRASH_DIAGNOSTIC", "Failed to read internal crash_log.txt", t)
        }

        // Check external filesDir
        try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null) {
                val externalFile = File(externalDir, CRASH_LOG_FILE_NAME)
                if (externalFile.exists() && externalFile.length() > 0) {
                    val text = externalFile.readText()
                    if (text.isNotBlank()) return text
                }
            }
        } catch (t: Throwable) {
            Log.e("CRASH_DIAGNOSTIC", "Failed to read external crash_log.txt", t)
        }

        return null
    }

    /**
     * Deletes crash_log.txt from both internal and external storage.
     */
    fun clearCrashLog(context: Context) {
        try {
            val internalFile = File(context.filesDir, CRASH_LOG_FILE_NAME)
            if (internalFile.exists()) {
                internalFile.delete()
            }
        } catch (t: Throwable) {
            Log.e("CRASH_DIAGNOSTIC", "Failed to delete internal crash_log.txt", t)
        }

        try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null) {
                val externalFile = File(externalDir, CRASH_LOG_FILE_NAME)
                if (externalFile.exists()) {
                    externalFile.delete()
                }
            }
        } catch (t: Throwable) {
            Log.e("CRASH_DIAGNOSTIC", "Failed to delete external crash_log.txt", t)
        }
    }

    /**
     * Explicit recording helper for try/catch diagnostics around critical entry points.
     */
    fun recordExplicitCrash(throwable: Throwable, contextTag: String) {
        try {
            val context = com.example.AgriApplication.instance.applicationContext
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.getDefault()).format(Date())

            val report = buildString {
                appendLine("========================================")
                appendLine("BAAGBAAN EXPLICIT CAUGHT CRASH")
                appendLine("========================================")
                appendLine("Context Tag: $contextTag")
                appendLine("Timestamp: $timeStr")
                appendLine("Thread: ${Thread.currentThread().name} (id: ${Thread.currentThread().id})")
                appendLine("Exception: ${throwable.javaClass.name}")
                appendLine("Message: ${throwable.message ?: "(null message)"}")
                appendLine()
                appendLine("Cause:")
                appendLine("${throwable.cause?.javaClass?.name ?: "None"}: ${throwable.cause?.message ?: ""}")
                appendLine()
                appendLine("Complete Stack Trace:")
                appendLine(stackTrace)
                appendLine()
                appendLine("Environment Metadata:")
                appendLine("Android Version: ${Build.VERSION.RELEASE}")
                appendLine("SDK Version: ${Build.VERSION.SDK_INT}")
                appendLine("App Version: ${getAppVersion(context)}")
                appendLine("Device Model: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("========================================")
            }
            writeCrashLog(context, report)
        } catch (t: Throwable) {
            Log.e("CRASH_DIAGNOSTIC", "Failed to record explicit crash", t)
        }
    }

    /**
     * Checks Android 11+ historical process exit reasons for process kills or native crashes (SIGSEGV/RenderThread).
     */
    fun checkHistoricalExitReasons(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Thread({
                try {
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                    val exitReasons = activityManager?.getHistoricalProcessExitReasons(context.packageName, 0, 3)
                    val lastExit = exitReasons?.firstOrNull()

                    if (lastExit != null) {
                        val reasonDesc = when (lastExit.reason) {
                            ApplicationExitInfo.REASON_CRASH -> "CRASH (Java/Kotlin uncaught exception)"
                            ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE (Native signal/SIGSEGV/SIGBUS/RenderNode)"
                            ApplicationExitInfo.REASON_ANR -> "ANR (Application Not Responding)"
                            ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY (Killed by Low Memory Killer)"
                            ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED (Killed by OS signal)"
                            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
                            ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
                            ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
                            else -> "Reason Code: ${lastExit.reason}"
                        }
                        Log.d("LIFECYCLE_DEBUG", "Previous Process Exit: $reasonDesc, status=${lastExit.status}, desc=${lastExit.description}")

                        // If previous exit was a native crash or abnormal termination and no crash log was written by Java handler
                        if ((lastExit.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                             lastExit.reason == ApplicationExitInfo.REASON_SIGNALED ||
                             (lastExit.reason == ApplicationExitInfo.REASON_CRASH && !hasCrashLog(context)))) {
                            val traceText = try {
                                lastExit.traceInputStream?.bufferedReader()?.use { it.readText() } ?: ""
                            } catch (_: Throwable) { "" }

                            val exitReport = buildString {
                                appendLine("========================================")
                                appendLine("BAAGBAAN PROCESS EXIT DIAGNOSTIC")
                                appendLine("========================================")
                                appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date(lastExit.timestamp))}")
                                appendLine("Exit Reason: $reasonDesc")
                                appendLine("Exit Status: ${lastExit.status}")
                                appendLine("Exit Description: ${lastExit.description ?: "None"}")
                                appendLine("Process Importance: ${lastExit.importance}")
                                appendLine("PSS Memory: ${lastExit.pss} kB, RSS: ${lastExit.rss} kB")
                                if (traceText.isNotBlank()) {
                                    appendLine()
                                    appendLine("Native Trace / Tombstone:")
                                    appendLine(traceText)
                                }
                                appendLine()
                                appendLine("Android Version: ${Build.VERSION.RELEASE}")
                                appendLine("SDK Version: ${Build.VERSION.SDK_INT}")
                                appendLine("App Version: ${getAppVersion(context)}")
                                appendLine("Device Model: ${Build.MANUFACTURER} ${Build.MODEL}")
                                appendLine("========================================")
                            }
                            writeCrashLog(context, exitReport)
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("LIFECYCLE_DEBUG", "Failed to inspect historical exit reasons", t)
                }
            }, "HistoricalExitChecker").start()
        }
    }

    private fun buildCrashReport(context: Context, thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.getDefault()).format(Date())
        val exceptionClass = throwable.javaClass.name
        val message = throwable.message ?: "(null message)"

        // Build complete cause chain
        val causeChainBuilder = StringBuilder()
        var currentCause: Throwable? = throwable.cause
        var depth = 1
        while (currentCause != null) {
            causeChainBuilder.append("[$depth] ${currentCause.javaClass.name}: ${currentCause.message ?: "(null message)"}\n")
            val causeSw = StringWriter()
            val causePw = PrintWriter(causeSw)
            currentCause.printStackTrace(causePw)
            causeChainBuilder.append(causeSw.toString())
            causeChainBuilder.append("\n----------------------------------------\n")
            currentCause = currentCause.cause
            depth++
        }
        val causeChain = if (causeChainBuilder.isNotEmpty()) causeChainBuilder.toString().trim() else "None"

        val appVersion = getAppVersion(context)

        return buildString {
            appendLine("========================================")
            appendLine("BAAGBAAN CRASH DIAGNOSTIC REPORT")
            appendLine("========================================")
            appendLine("Timestamp: $timeStr")
            appendLine("Current Screen: ${currentScreenName ?: "Unknown"}")
            appendLine("Current Thread: ${thread.name} (id: ${thread.id})")
            appendLine("Exception Class: $exceptionClass")
            appendLine("Exception Message: $message")
            appendLine()
            appendLine("Complete Cause Chain:")
            appendLine(causeChain)
            appendLine()
            appendLine("Complete Stack Trace:")
            appendLine(stackTrace)
            appendLine()
            appendLine("Device & Environment:")
            appendLine("Android Version: ${Build.VERSION.RELEASE}")
            appendLine("SDK Version: ${Build.VERSION.SDK_INT}")
            appendLine("App Version: $appVersion")
            appendLine("Device Model: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Board: ${Build.BOARD}, Hardware: ${Build.HARDWARE}")
            appendLine("========================================")
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
            "${packageInfo.versionName} ($versionCode)"
        } catch (_: Throwable) {
            "Unknown"
        }
    }
}

/**
 * Full-screen Crash Diagnostic UI displayed before normal app interaction.
 */
@Composable
fun CrashDiagnosticScreen(
    context: Context,
    crashLog: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.18f), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Crash Diagnostic",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Crash Diagnostic",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "An uncaught runtime issue was captured",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main log scrollable card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617))
            ) {
                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = crashLog,
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.5.sp,
                        lineHeight = 16.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: COPY, CLEAR LOG, CONTINUE TO APP
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // COPY button
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Baagbaan Crash Log", crashLog)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Crash log copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("crash_diagnostic_copy_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // CLEAR LOG button
                    OutlinedButton(
                        onClick = {
                            CrashReporter.clearCrashLog(context)
                            Toast.makeText(context, "Crash log cleared", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("crash_diagnostic_clear_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Log",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CLEAR LOG", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }

                // CONTINUE TO APP button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("crash_diagnostic_continue_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("CONTINUE TO APP", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Continue",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
