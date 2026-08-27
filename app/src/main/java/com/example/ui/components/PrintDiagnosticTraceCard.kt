package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.util.PrintDiagnosticTrace

@Composable
fun PrintDiagnosticTraceCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = isAppInDarkMode(),
    title: String = "Print Diagnostic Trace"
) {
    val logs = PrintDiagnosticTrace.logs
    if (logs.isEmpty()) return

    val context = LocalContext.current
    var isCollapsed by remember { mutableStateOf(false) }

    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val headerColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("print_diagnostic_trace_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isCollapsed = !isCollapsed },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = headerColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$title (${logs.size} events)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = headerColor
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Print Diagnostic Trace", PrintDiagnosticTrace.getTraceString())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Print trace copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp).testTag("copy_print_trace_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Print Trace",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { PrintDiagnosticTrace.clear() },
                    modifier = Modifier.size(28.dp).testTag("clear_print_trace_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Trace",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Icon(
                    imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isCollapsed) "Expand" else "Collapse",
                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (!isCollapsed) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF020617) else Color(0xFFF1F5F9))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = logs.joinToString("\n"),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth().testTag("print_diagnostic_trace_text")
                )
            }
        }
    }
}
