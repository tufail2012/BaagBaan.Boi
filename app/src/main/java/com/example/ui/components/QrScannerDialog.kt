package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.getSectionAccentColor
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun QrScannerDialog(
    onDismissRequest: () -> Unit,
    onQrScanned: (String) -> Unit,
    customPaletteColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkMode()
    val scanQrAccent = getSectionAccentColor("Scan QR", customPaletteColor = customPaletteColor)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isScanningActive by remember { mutableStateOf(true) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(3000)
            errorMessage = null
            isScanningActive = true
        }
    }

    BackHandler(onBack = onDismissRequest)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("qr_scanner_dialog")
    ) {
        if (hasCameraPermission) {
            val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

            DisposableEffect(Unit) {
                onDispose {
                    cameraExecutor.shutdown()
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                        val barcodeScanner = BarcodeScanning.getClient()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && isScanningActive) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    val rawValue = barcode.rawValue
                                                    if (!rawValue.isNullOrBlank()) {
                                                        isScanningActive = false
                                                        if (rawValue.startsWith("baagbaanboi://record")) {
                                                            onQrScanned(rawValue)
                                                        } else {
                                                            errorMessage = "Not a valid booking code"
                                                        }
                                                        break
                                                    }
                                                }
                                            }
                                            .addOnFailureListener {
                                                // Continue analyzing next frames
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Viewfinder Reticle Overlay with punched-out spotlight
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                val boxSize = size.minDimension * 0.65f
                val left = (size.width - boxSize) / 2f
                val top = (size.height - boxSize) / 2f
                val cornerLength = 40.dp.toPx()
                val strokeWidth = 4.dp.toPx()
                val cornerRadius = 14.dp.toPx()
                val primaryColor = scanQrAccent

                // Solid, dimmed, palette-tinted scrim over entire screen
                drawRect(
                    color = Color(0xFF070B14).copy(alpha = 0.84f)
                )
                drawRect(
                    color = scanQrAccent.copy(alpha = 0.16f)
                )

                // Punch cutout over the viewfinder so raw camera is ONLY visible here
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    blendMode = BlendMode.Clear
                )

                // Viewfinder Reticle Border
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.6f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Top-Left Corner
                drawLine(
                    color = primaryColor,
                    start = Offset(left, top + cornerLength),
                    end = Offset(left, top + cornerRadius),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(left + cornerRadius, top),
                    end = Offset(left + cornerLength, top),
                    strokeWidth = strokeWidth
                )

                // Top-Right Corner
                drawLine(
                    color = primaryColor,
                    start = Offset(left + boxSize - cornerLength, top),
                    end = Offset(left + boxSize - cornerRadius, top),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(left + boxSize, top + cornerRadius),
                    end = Offset(left + boxSize, top + cornerLength),
                    strokeWidth = strokeWidth
                )

                // Bottom-Left Corner
                drawLine(
                    color = primaryColor,
                    start = Offset(left, top + boxSize - cornerLength),
                    end = Offset(left, top + boxSize - cornerRadius),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(left + cornerRadius, top + boxSize),
                    end = Offset(left + cornerLength, top + boxSize),
                    strokeWidth = strokeWidth
                )

                // Bottom-Right Corner
                drawLine(
                    color = primaryColor,
                    start = Offset(left + boxSize - cornerLength, top + boxSize),
                    end = Offset(left + boxSize - cornerRadius, top + boxSize),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(left + boxSize, top + boxSize - cornerLength),
                    end = Offset(left + boxSize, top + boxSize - cornerRadius),
                    strokeWidth = strokeWidth
                )
            }
        } else {
            // Permission request fallback card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCardBackground(
                            cornerRadius = 20.dp,
                            accentColor = scanQrAccent,
                            isDark = true
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = scanQrAccent,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Camera Permission Required",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "To scan receipt QR codes directly into booking records, please grant camera access.",
                            fontSize = 14.sp,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = scanQrAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Top Bar - Wide Pill-Shaped Glass Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .glassCardBackground(
                    isDark = isDark,
                    accentColor = scanQrAccent,
                    shape = RoundedCornerShape(percent = 50)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(scanQrAccent.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = scanQrAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Scan Receipt QR",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Align QR code inside viewfinder",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Flashlight Toggle (Separate Glass Circle Container)
                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            cameraControl?.enableTorch(isFlashOn)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .glassCardBackground(
                                isDark = true,
                                accentColor = if (isFlashOn) Color(0xFFFFD54F) else Color.White,
                                shape = CircleShape
                            )
                            .testTag("toggle_torch_button")
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Torch",
                            tint = if (isFlashOn) Color(0xFFFFD54F) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Close Button (Separate Glass Circle Container)
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .glassCardBackground(
                                isDark = true,
                                accentColor = Color.White,
                                shape = CircleShape
                            )
                            .testTag("close_qr_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Scanner",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Error Banner (Animated)
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCardBackground(
                        cornerRadius = 14.dp,
                        accentColor = Color(0xFFEF4444),
                        isDark = true
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFCA5A5)
                    )
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
