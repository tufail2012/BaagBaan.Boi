package com.example.chat.ui.attachments

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LocalMorphMenuBackdrop
import com.example.ui.components.ThemedIcon
import com.example.ui.components.isGlassSupported
import com.example.ui.components.navLiquidGlass
import com.example.ui.theme.GlassStyle
import com.example.ui.theme.LocalLiquidGlassConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickDocument: () -> Unit,
    onShareLocation: () -> Unit,
    onPickContact: () -> Unit,
    isDark: Boolean = true,
    panelModifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val config = LocalLiquidGlassConfig.current
    val backdrop = LocalMorphMenuBackdrop.current
    val glassOn = isGlassSupported() && config.chatGlassEnabled
    val surfaceOpacity = config.surfaceOpacity.coerceIn(0f, 1f)
    val fallbackFill = if (config.surfaceTintColor != Color.Unspecified) {
        config.surfaceTintColor.copy(alpha = surfaceOpacity)
    } else {
        if (isDark) Color(0x991E293B) else Color(0xB3FFFFFF)
    }
    val edgeBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.05f)
        )
    )

    val sheetSurfaceModifier = when {
        panelModifier != Modifier -> panelModifier
        glassOn && backdrop != null -> Modifier
            .navLiquidGlass(shape = sheetShape, backdrop = backdrop)
            .border(0.5.dp, edgeBrush, sheetShape)
            .clip(sheetShape)
        glassOn -> Modifier
            .clip(sheetShape)
            .then(
                when (config.glassStyle) {
                    GlassStyle.LIQUID -> Modifier.background(fallbackFill, sheetShape)
                    GlassStyle.BLUR, GlassStyle.TRANSPARENT -> Modifier
                }
            )
            .border(0.5.dp, edgeBrush, sheetShape)
        else -> Modifier
            .clip(sheetShape)
            .background(if (isDark) Color(0x991E293B) else Color(0xB3FFFFFF), sheetShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), sheetShape)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(sheetSurfaceModifier)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) Color(0xFF4A5568) else Color(0xFFCBD5E1))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Share Content",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                )

                // Row 1: Gallery, Camera, Document
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Default.PhotoLibrary,
                        label = "Gallery",
                        gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
                        isDark = isDark,
                        onClick = {
                            onDismiss()
                            onPickGallery()
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.CameraAlt,
                        label = "Camera",
                        gradient = listOf(Color(0xFFEC4899), Color(0xFFBE185D)),
                        isDark = isDark,
                        onClick = {
                            onDismiss()
                            onTakePhoto()
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.Description,
                        label = "Document",
                        gradient = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                        isDark = isDark,
                        onClick = {
                            onDismiss()
                            onPickDocument()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Row 2: Location, Contact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        gradient = listOf(Color(0xFF10B981), Color(0xFF047857)),
                        isDark = isDark,
                        onClick = {
                            onDismiss()
                            onShareLocation()
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.ContactPhone,
                        label = "Contact",
                        gradient = listOf(Color(0xFF06B6D4), Color(0xFF0E7490)),
                        isDark = isDark,
                        onClick = {
                            onDismiss()
                            onPickContact()
                        }
                    )
                    // Spacer item to keep grid aligned with Row 1
                    Box(modifier = Modifier.size(72.dp))
                }
            }
        }
    }
}

@Composable
private fun AttachmentOptionItem(
    icon: ImageVector,
    label: String,
    gradient: List<Color>,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            ThemedIcon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                keepTint = true,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
        )
    }
}
