package com.example.ui.screens.inspection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.viewmodel.InspectionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionImageCaptureScreen(
    viewModel: InspectionViewModel,
    onSubmitEvidence: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val inspectionId by viewModel.currentInspectionId.collectAsState()
    val capturedImages by viewModel.capturedImages.collectAsState()

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
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var photoNotes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var previewImageDialogPath by remember { mutableStateOf<String?>(null) }

    // Gallery Picker launcher for picking stored photos
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        for (uri in uris) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val targetFile = File(context.cacheDir, "evidence_import_${System.currentTimeMillis()}_${(100..999).random()}.jpg")
                val outputStream = FileOutputStream(targetFile)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.addCapturedImage(targetFile.absolutePath)
            } catch (e: Exception) {
                Log.e("ImageCaptureScreen", "Import failed: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Product Photos & Evidence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = inspectionId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            cameraControl?.cameraControl?.enableTorch(!isTorchOn)
                            isTorchOn = !isTorchOn
                        }
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isTorchOn) Color(0xFFFACC15) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Camera Viewfinder Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
            ) {
                if (hasCameraPermission) {
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

                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    imageCapture = capture

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        capture
                                    )
                                    cameraControl = cam
                                } catch (e: Exception) {
                                    Log.e("ImageCaptureScreen", "Camera binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        }
                    )

                    // Shutter button & Gallery import button
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery import
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "Pick from Gallery",
                                tint = Color.White
                            )
                        }

                        // Capture Shutter Button
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(enabled = !isCapturing) {
                                    val capture = imageCapture
                                    if (capture != null) {
                                        isCapturing = true
                                        val photoFile = File(
                                            context.cacheDir,
                                            "evidence_${System.currentTimeMillis()}.jpg"
                                        )
                                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                        capture.takePicture(
                                            outputOptions,
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                    isCapturing = false
                                                    viewModel.addCapturedImage(photoFile.absolutePath)
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    isCapturing = false
                                                    Log.e("ImageCaptureScreen", "Photo capture failed", exception)
                                                }
                                            }
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Capture Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        // Placeholder spacer to keep shutter centered
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera Permission Required",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Captured Images Thumbnail Gallery
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAPTURED EVIDENCE (${capturedImages.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (capturedImages.isNotEmpty()) {
                        Text(
                            text = "Tap to review / add note",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (capturedImages.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Capture or import photos using the camera controls above",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(capturedImages) { path ->
                            val bitmap = remember(path) {
                                try {
                                    BitmapFactory.decodeFile(path)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            val note = photoNotes[path]

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { previewImageDialogPath = path }
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Evidence Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }

                                // Optional note label badge at bottom of thumbnail
                                if (!note.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = note,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Delete thumbnail badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .clickable {
                                            viewModel.removeCapturedImage(path)
                                            photoNotes = photoNotes - path
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button: Submit Evidence →
            Button(
                onClick = {
                    if (capturedImages.isNotEmpty() && !isSubmitting) {
                        isSubmitting = true
                        coroutineScope.launch {
                            // Associate optional photo notes with current inspection
                            if (photoNotes.isNotEmpty()) {
                                val formattedNotes = photoNotes.entries
                                    .filter { it.value.isNotBlank() }
                                    .joinToString("\n") { (path, note) ->
                                        "• Photo (${File(path).name}): $note"
                                    }
                                if (formattedNotes.isNotBlank()) {
                                    val existing = viewModel.inspectorNotes.value
                                    viewModel.setInspectorNotes(
                                        if (existing.isBlank()) formattedNotes else "$existing\n$formattedNotes"
                                    )
                                }
                            }
                            delay(400)
                            isSubmitting = false
                            onSubmitEvidence()
                        }
                    }
                },
                enabled = capturedImages.isNotEmpty() && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 16.dp)
                    .testTag("submit_evidence_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Submitting Evidence...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Submit Evidence →",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Full-screen Image Review Dialog
        if (previewImageDialogPath != null) {
            val path = previewImageDialogPath!!
            var currentNote by remember(path) { mutableStateOf(photoNotes[path] ?: "") }
            val quickSuggestions = listOf(
                "Front label",
                "MRP label",
                "Expiry date",
                "Barcode",
                "Damaged packaging"
            )
            val bitmap = remember(path) {
                try {
                    BitmapFactory.decodeFile(path)
                } catch (e: Exception) {
                    null
                }
            }

            AlertDialog(
                onDismissRequest = {
                    if (currentNote.isNotBlank()) {
                        photoNotes = photoNotes + (path to currentNote)
                    }
                    previewImageDialogPath = null
                },
                title = { Text("Evidence Photo Review", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Full Review",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Unable to render image preview")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = File(path).name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Optional Note Input
                        OutlinedTextField(
                            value = currentNote,
                            onValueChange = { currentNote = it },
                            label = { Text("Optional Note / Description", fontSize = 12.sp) },
                            placeholder = { Text("e.g. Front label, MRP...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick suggestion pills
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickSuggestions) { suggestion ->
                                SuggestionChip(
                                    onClick = { currentNote = suggestion },
                                    label = { Text(suggestion, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (currentNote.isNotBlank()) {
                                photoNotes = photoNotes + (path to currentNote)
                            } else {
                                photoNotes = photoNotes - path
                            }
                            previewImageDialogPath = null
                        }
                    ) {
                        Text("Done")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                viewModel.removeCapturedImage(path)
                                photoNotes = photoNotes - path
                                previewImageDialogPath = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(
                            onClick = {
                                viewModel.removeCapturedImage(path)
                                photoNotes = photoNotes - path
                                previewImageDialogPath = null
                            }
                        ) {
                            Text("Retake", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    }
}
