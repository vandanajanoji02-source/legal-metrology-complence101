package com.example.ui.screens.inspection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocr.ProductOcrResult
import com.example.ui.theme.StatusVerified
import com.example.ui.theme.StatusVerifiedContainer
import com.example.ui.theme.StatusVerifiedText
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.theme.StatusWarningText
import com.example.ui.viewmodel.InspectionViewModel
import com.example.ui.viewmodel.OcrProcessState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrResultsScreen(
    viewModel: InspectionViewModel,
    onContinueNextStep: () -> Unit,
    onRetakePhotos: () -> Unit,
    onBack: () -> Unit
) {
    val ocrState by viewModel.ocrProcessState.collectAsState()
    val ocrResult by viewModel.ocrResult.collectAsState()
    val inspectionId by viewModel.currentInspectionId.collectAsState()
    val capturedImages by viewModel.capturedImages.collectAsState()

    var showRawText by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<Pair<String, String?>?>(null) } // fieldName to currentValue

    // Edit Field Dialog
    if (editingField != null) {
        val (fieldName, currentValue) = editingField!!
        var tempValue by remember { mutableStateOf(currentValue ?: "") }

        AlertDialog(
            onDismissRequest = { editingField = null },
            title = { Text("Edit $fieldName", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Update the OCR extracted information for $fieldName:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        label = { Text(fieldName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = fieldName != "Manufacturer Address"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateOcrField(fieldName, tempValue)
                        editingField = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.updateOcrField(fieldName, null)
                        editingField = null
                    }
                ) {
                    Text("Clear Field", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "OCR Results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$inspectionId • Step 2 of 4",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (val state = ocrState) {
            is OcrProcessState.Analyzing -> {
                // --- Analyzing / Progress Loading View ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "Analyzing Product Photos...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analyzing photo ${state.currentPhoto} of ${state.totalPhotos} with Google ML Kit...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (state.totalPhotos > 0) state.currentPhoto.toFloat() / state.totalPhotos.toFloat() else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Processing on device • Zero external servers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            is OcrProcessState.Empty -> {
                // --- Empty / Unreadable View ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(StatusWarningContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "No Readable Text Detected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tips for best OCR results:\n• Ensure good lighting and hold device steady\n• Avoid glare and angled reflections on shiny packaging\n• Keep text clear and parallel to camera",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onRetakePhotos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try Again / Retake Photos", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onContinueNextStep,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Continue Manually", fontWeight = FontWeight.Bold)
                    }
                }
            }

            else -> {
                // --- OCR Results Review Screen ---
                val result = ocrResult ?: ProductOcrResult()
                val analyzedCount = if (result.processedPhotoCount > 0) result.processedPhotoCount else capturedImages.size
                val totalCount = if (result.totalPhotoCount > 0) result.totalPhotoCount else capturedImages.size

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Summary Banner Card
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.DocumentScanner,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "On-Device ML Kit OCR",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(StatusVerifiedContainer)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Verified Local",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusVerifiedText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (totalCount > 1) {
                                        "$analyzedCount of $totalCount photos analyzed successfully"
                                    } else {
                                        "Analyzed 1 product photo"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Tap any field or edit button to correct any label values before proceeding.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Section Header: Detected Information
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Detected Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Legal Metrology Rules",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Field 1: Product Name
                    item {
                        OcrFieldCard(
                            label = "Product Name",
                            value = result.productName,
                            confidence = result.confidenceMap["Product Name"],
                            onEdit = { editingField = "Product Name" to result.productName }
                        )
                    }

                    // Field 2: Brand
                    item {
                        OcrFieldCard(
                            label = "Brand",
                            value = result.brand,
                            confidence = result.confidenceMap["Brand"],
                            onEdit = { editingField = "Brand" to result.brand }
                        )
                    }

                    // Field 3: MRP
                    item {
                        OcrFieldCard(
                            label = "MRP (Maximum Retail Price)",
                            value = result.mrp,
                            confidence = result.confidenceMap["MRP"],
                            onEdit = { editingField = "MRP" to result.mrp }
                        )
                    }

                    // Field 4: Manufacturing Date
                    item {
                        OcrFieldCard(
                            label = "Manufacturing Date",
                            value = result.manufacturingDate,
                            confidence = result.confidenceMap["Manufacturing Date"],
                            onEdit = { editingField = "Manufacturing Date" to result.manufacturingDate }
                        )
                    }

                    // Field 5: Expiry Date
                    item {
                        OcrFieldCard(
                            label = "Expiry Date",
                            value = result.expiryDate,
                            confidence = result.confidenceMap["Expiry Date"],
                            onEdit = { editingField = "Expiry Date" to result.expiryDate }
                        )
                    }

                    // Field 6: Net Quantity
                    item {
                        OcrFieldCard(
                            label = "Net Quantity",
                            value = result.netQuantity,
                            confidence = result.confidenceMap["Net Quantity"],
                            onEdit = { editingField = "Net Quantity" to result.netQuantity }
                        )
                    }

                    // Field 7: Batch / Lot Number
                    item {
                        OcrFieldCard(
                            label = "Batch / Lot Number",
                            value = result.batchNumber,
                            confidence = result.confidenceMap["Batch Number"],
                            onEdit = { editingField = "Batch Number" to result.batchNumber }
                        )
                    }

                    // Field 8: Manufacturer
                    item {
                        OcrFieldCard(
                            label = "Manufacturer",
                            value = result.manufacturer,
                            confidence = result.confidenceMap["Manufacturer"],
                            onEdit = { editingField = "Manufacturer" to result.manufacturer }
                        )
                    }

                    // Field 9: Manufacturer Address
                    item {
                        OcrFieldCard(
                            label = "Manufacturer Address",
                            value = result.manufacturerAddress,
                            confidence = if (result.manufacturerAddress != null) "Detected" else "Not detected",
                            onEdit = { editingField = "Manufacturer Address" to result.manufacturerAddress }
                        )
                    }

                    // Field 10: License / Registration Number
                    item {
                        OcrFieldCard(
                            label = "License / Registration Number",
                            value = result.licenseNumber,
                            confidence = result.confidenceMap["License Number"],
                            onEdit = { editingField = "License Number" to result.licenseNumber }
                        )
                    }

                    // Expandable Raw OCR Text Viewer
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRawText = !showRawText },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "View Raw OCR Extracted Text",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showRawText) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                }

                                AnimatedVisibility(visible = showRawText) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = result.rawText.ifBlank { "No raw text detected." },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onContinueNextStep,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("ocr_continue_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = "Continue to Next Step →",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onRetakePhotos,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retake / Add Photos", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrFieldCard(
    label: String,
    value: String?,
    confidence: String?,
    onEdit: () -> Unit
) {
    val isDetected = !value.isNullOrBlank()
    val statusText = confidence ?: if (isDetected) "Detected" else "Not detected"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDetected) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (statusText) {
                                    "Detected" -> StatusVerifiedContainer
                                    "Edited" -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (statusText) {
                                "Detected" -> StatusVerifiedText
                                "Edited" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isDetected) {
                    Text(
                        text = value!!,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "Not detected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit $label",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
