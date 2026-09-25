package com.example.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.scanner.BarcodeAnalyzer
import com.example.ui.viewmodel.ScanProcessState
import com.example.ui.viewmodel.ScannerViewModel
import java.util.concurrent.Executors

@Composable
fun CameraScannerScreen(
    viewModel: ScannerViewModel,
    headerSubtitle: String = "",
    onBarcodeScanned: (String) -> Unit,
    onSkip: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanState by viewModel.scanState.collectAsState()

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
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var cameraControl: Camera? by remember { mutableStateOf(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualBarcodeInput by remember { mutableStateOf("") }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeAnalyzer = remember {
        BarcodeAnalyzer { barcode ->
            onBarcodeScanned(barcode)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Live CameraX Preview
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, barcodeAnalyzer)
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = cam
                        } catch (exc: Exception) {
                            Log.e("CameraScannerScreen", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Scanning Viewfinder Overlay with Darkened Cutout & Animated Laser
            ScannerViewfinderOverlay()
        } else {
            // Permission Denied State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "SmartVerify needs camera permission to scan product barcodes and serial tags.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission")
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { showManualInputDialog = true }
                ) {
                    Text("Or Enter Barcode Manually", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("close_scanner_button")
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Align Barcode in Frame",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (headerSubtitle.isNotBlank()) {
                    Text(
                        text = headerSubtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onSkip != null) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.testTag("top_skip_barcode_button")
                    ) {
                        Text("Skip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = {
                        cameraControl?.cameraControl?.enableTorch(!isTorchOn)
                        isTorchOn = !isTorchOn
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("torch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = if (isTorchOn) Color(0xFFFACC15) else Color.White
                    )
                }
            }
        }

        // Bottom Controls Bar (Manual Entry & Formats Supported)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Supports EAN-13, EAN-8, UPC-A, UPC-E, Code 128, QR",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showManualInputDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("manual_barcode_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.22f)
                    )
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                if (onSkip != null) {
                    Button(
                        onClick = onSkip,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("skip_barcode_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Skip Scan →", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Loading indicator if processing
        if (scanState is ScanProcessState.Processing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Verifying Barcode...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Querying Smart Registry Database",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Manual Barcode Input Dialog (Essential for testing & presentation)
        if (showManualInputDialog) {
            AlertDialog(
                onDismissRequest = { showManualInputDialog = false },
                title = { Text("Manual Barcode Lookup") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Type a barcode number to test verification lookup:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = manualBarcodeInput,
                            onValueChange = { manualBarcodeInput = it },
                            placeholder = { Text("e.g. 8901234567890") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Demo suggestions: 8901234567890 (Verified), 8901234567891 (Warning), 8901234567892 (Invalid)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val code = manualBarcodeInput.trim()
                            if (code.isNotEmpty()) {
                                showManualInputDialog = false
                                onBarcodeScanned(code)
                            }
                        }
                    ) {
                        Text("Lookup")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualInputDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ScannerViewfinderOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_animation"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val boxWidthPx = widthPx * 0.76f
        val boxHeightPx = boxWidthPx * 0.72f

        val left = (widthPx - boxWidthPx) / 2f
        val top = (heightPx - boxHeightPx) / 2f - 40f
        val right = left + boxWidthPx
        val bottom = top + boxHeightPx

        val cornerLength = 40f
        val strokeWidth = 8f

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw darkened semi-transparent background
            drawRect(
                color = Color.Black.copy(alpha = 0.55f),
                size = size
            )

            // Clear cutout for viewfinder
            val cutoutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        radiusX = 16f,
                        radiusY = 16f
                    )
                )
            }
            drawPath(
                path = cutoutPath,
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )

            // Draw Corner Guides (Emerald Green for Dark Green theme)
            val cornerColor = Color(0xFF34D399)

            // Top-left
            drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth, StrokeCap.Round)
            drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth, StrokeCap.Round)

            // Top-right
            drawLine(cornerColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth, StrokeCap.Round)
            drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth, StrokeCap.Round)

            // Bottom-left
            drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth, StrokeCap.Round)
            drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth, StrokeCap.Round)

            // Bottom-right
            drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth, StrokeCap.Round)
            drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth, StrokeCap.Round)

            // Draw Animated Scanning Laser Line
            val laserY = top + (bottom - top) * laserPosition
            drawLine(
                color = Color(0xFFF43F5E),
                start = Offset(left + 8f, laserY),
                end = Offset(right - 8f, laserY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}
