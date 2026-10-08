package com.example.chat.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chat.call.ActiveCallState
import com.example.chat.call.ZegoCallManager

@Composable
fun MinimizedCallBar(
    state: ActiveCallState,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00A884),
    isDark: Boolean = true
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                ZegoCallManager.restoreCall(context)
            },
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) Color(0xFF1F2C34) else Color(0xFFE2E8F0),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mic icon on the left
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.isMicMuted) Color(0x33E53935) else accentColor.copy(alpha = 0.20f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (state.isMicMuted) "Mic Muted" else "Mic Active",
                    tint = if (state.isMicMuted) Color(0xFFE53935) else accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // "📞 ContactName - Calling/status" in the middle (accent-colored text)
            val statusLabel = if (state.isConnected) state.formattedDuration else state.statusText
            Text(
                text = "📞 ${state.targetName.ifBlank { "Contact" }} - $statusLabel",
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Red End-call button on the right
            IconButton(
                onClick = {
                    ZegoCallManager.endCall()
                },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
