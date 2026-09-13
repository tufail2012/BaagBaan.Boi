package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {
    private const val PREFS_NAME = "app_crash_reporting_prefs"
    private const val KEY_HAS_PENDING_CRASH = "key_has_pending_crash"
    private const val KEY_LAST_CRASH_TIMESTAMP = "key_last_crash_timestamp"
    private const val CRASH_FILE_NAME = "last_crash.txt"
    private const val ARCHIVED_CRASH_FILE_NAME = "last_crash_archived.txt"

    @Volatile
    var currentScreenName: String? = null

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildCrashReport(context, thread, throwable)

                // Save synchronously to plain-text file in internal app storage
                val crashFile = File(context.filesDir, CRASH_FILE_NAME)
                crashFile.writeText(report)

                // Update SharedPreferences flag synchronously
                val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_HAS_PENDING_CRASH, true)
                    .putLong(KEY_LAST_CRASH_TIMESTAMP, System.currentTimeMillis())
                    .commit()

                Log.e("CrashReporter", "Uncaught exception persisted to $CRASH_FILE_NAME:\n$report")
            } catch (e: Throwable) {
                // Defensive: never throw from uncaught exception handler
                Log.e("CrashReporter", "Failed to persist crash trace", e)
            } finally {
                // Delegate to original handler so Android's normal crash lifecycle remains intact
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun getPendingCrashReport(context: Context): String? {
        if (!BuildConfig.DEBUG) {
            return null
        }
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hasPending = prefs.getBoolean(KEY_HAS_PENDING_CRASH, false)
            val crashFile = File(context.filesDir, CRASH_FILE_NAME)
            if (hasPending && crashFile.exists()) {
                val content = crashFile.readText()
                if (content.isNotBlank()) content else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to read crash report", e)
            null
        }
    }

    fun clearPendingCrashReport(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_HAS_PENDING_CRASH, false).apply()

            val crashFile = File(context.filesDir, CRASH_FILE_NAME)
            if (crashFile.exists()) {
                val archiveFile = File(context.filesDir, ARCHIVED_CRASH_FILE_NAME)
                if (archiveFile.exists()) {
                    archiveFile.delete()
                }
                crashFile.renameTo(archiveFile)
            }
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to clear pending crash report", e)
        }
    }

    // Backwards-compatible aliases
    fun getSavedCrashTrace(context: Context): String? = getPendingCrashReport(context)
    fun clearSavedCrashTrace(context: Context) = clearPendingCrashReport(context)

    private fun buildCrashReport(context: Context, thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date())

        val exceptionClass = throwable.javaClass.name
        val message = throwable.message ?: "(null message)"
        val directCause = throwable.cause?.let { "${it.javaClass.name}: ${it.message ?: "(null)"}" } ?: "None"

        // Capture full nested causes chain
        val nestedCausesBuilder = StringBuilder()
        var currentCause: Throwable? = throwable.cause
        var depth = 1
        while (currentCause != null) {
            nestedCausesBuilder.append("[$depth] ${currentCause.javaClass.name}: ${currentCause.message ?: "(null)"}\n")
            val causeSw = StringWriter()
            val causePw = PrintWriter(causeSw)
            currentCause.printStackTrace(causePw)
            nestedCausesBuilder.append(causeSw.toString())
            nestedCausesBuilder.append("\n----------------------------------------\n")
            currentCause = currentCause.cause
            depth++
        }
        val nestedCauses = if (nestedCausesBuilder.isNotEmpty()) nestedCausesBuilder.toString().trim() else "None"

        val appVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
            "${packageInfo.versionName} ($vCode)"
        } catch (_: Throwable) {
            "Unknown"
        }

        val detectedScreen = currentScreenName ?: detectScreenFromStackTrace(stackTrace)

        return buildString {
            appendLine("Baagbaan Crash Diagnostic")
            appendLine()
            if (!detectedScreen.isNullOrBlank()) {
                appendLine("Screen:")
                appendLine(detectedScreen)
                appendLine()
            }
            appendLine("Timestamp:")
            appendLine(timeStr)
            appendLine()
            appendLine("Thread:")
            appendLine(thread.name)
            appendLine()
            appendLine("Exception:")
            appendLine(exceptionClass)
            appendLine()
            appendLine("Message:")
            appendLine(message)
            appendLine()
            appendLine("Cause:")
            appendLine(directCause)
            appendLine()
            appendLine("Stack Trace:")
            appendLine(stackTrace)
            appendLine()
            appendLine("Caused By:")
            appendLine(nestedCauses)
            appendLine()
            appendLine("App Version:")
            appendLine(appVersion)
            appendLine()
            appendLine("Android Version:")
            appendLine(Build.VERSION.RELEASE)
            appendLine()
            appendLine("SDK:")
            appendLine(Build.VERSION.SDK_INT.toString())
            appendLine()
            appendLine("Device:")
            appendLine(Build.MANUFACTURER)
            appendLine(Build.MODEL)
        }
    }

    fun detectScreenFromStackTrace(stackTrace: String): String? {
        return when {
            stackTrace.contains("FarmerRecordsScreen") || stackTrace.contains("RecordsLiquidGlass") -> "Records"
            stackTrace.contains("drawBackdrop") || stackTrace.contains("LayerBackdrop") || stackTrace.contains("com.kyant.backdrop") -> "Records / Liquid Glass Backdrop"
            stackTrace.contains("FarmerFormScreen") -> "New Entry (FarmerFormScreen)"
            stackTrace.contains("AgriBottomNav") -> "Bottom Navigation"
            stackTrace.contains("AgriHeader") -> "Header / Profile Menu"
            else -> null
        }
    }
}

private fun extractField(content: String, header: String): String? {
    val lines = content.lines()
    val index = lines.indexOfFirst { it.trim().equals(header.trim(), ignoreCase = true) }
    if (index != -1 && index + 1 < lines.size) {
        val result = lines[index + 1].trim()
        if (result.isNotBlank()) return result
    }
    return null
}

@Composable
fun CrashReportScreen(
    context: Context,
    trace: String,
    onDismiss: () -> Unit
) {
    val detectedScreen = remember(trace) {
        extractField(trace, "Screen:") ?: CrashReporter.detectScreenFromStackTrace(trace)
    }
    val detectedException = remember(trace) {
        extractField(trace, "Exception:") ?: "Throwable"
    }
    val detectedMessage = remember(trace) {
        extractField(trace, "Message:") ?: "Uncaught runtime exception"
    }

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
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Crash Diagnostic",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Something went wrong",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Crash Diagnostic Report",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!detectedScreen.isNullOrBlank()) {
                        Row {
                            Text(
                                text = "Screen: ",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = detectedScreen,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row {
                        Text(
                            text = "Exception: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = detectedException,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF87171),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "Message:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = detectedMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Stack trace:",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Full Diagnostic & Stack Trace Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617))
            ) {
                val verticalScroll = rememberScrollState()
                val horizontalScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = trace,
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Copy Error and Continue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Baagbaan Crash Diagnostic", trace)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Full diagnostic copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_crash_error_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Error",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Error", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        CrashReporter.clearPendingCrashReport(context)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("continue_from_crash_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Continue", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
