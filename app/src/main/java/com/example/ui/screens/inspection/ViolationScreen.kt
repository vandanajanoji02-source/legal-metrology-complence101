package com.example.ui.screens.inspection

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Violation
import com.example.model.ViolationSeverity
import com.example.ui.theme.StatusInvalid
import com.example.ui.theme.StatusInvalidContainer
import com.example.ui.theme.StatusInvalidText
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.theme.StatusWarningText
import com.example.ui.viewmodel.InspectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViolationScreen(
    viewModel: InspectionViewModel,
    onFinalizeInspection: () -> Unit,
    onBack: () -> Unit
) {
    val inspectionId by viewModel.currentInspectionId.collectAsState()
    val product by viewModel.currentProduct.collectAsState()
    val barcode by viewModel.scannedBarcode.collectAsState()
    val capturedImages by viewModel.capturedImages.collectAsState()
    val violations by viewModel.violations.collectAsState()
    val inspectorNotes by viewModel.inspectorNotes.collectAsState()

    var selectedType by remember { mutableStateOf("Incorrect Labeling") }
    var selectedSeverity by remember { mutableStateOf(ViolationSeverity.MEDIUM) }
    var descriptionInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf(inspectorNotes) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val violationTypes = listOf(
        "Missing MRP",
        "Incorrect Labeling",
        "Expired Product",
        "Missing Manufacturer Details",
        "Incorrect Quantity",
        "Barcode Mismatch",
        "Suspected Counterfeit",
        "Tampered Seal / Packaging",
        "Unapproved Batch",
        "Other Non-Compliance"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Record Violation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$inspectionId • ${product?.name ?: barcode}",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning Banner Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StatusInvalidContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(StatusInvalid.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = StatusInvalidText,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Violation / Defect Reporting",
                            fontWeight = FontWeight.Bold,
                            color = StatusInvalidText,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Attach evidence and specify statutory violation parameters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusInvalidText
                        )
                    }
                }
            }

            // Section 1: Violation Type Dropdown
            Text(
                text = "VIOLATION CLASSIFICATION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = typeDropdownExpanded,
                onExpandedChange = { typeDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Violation Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )

                ExposedDropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false }
                ) {
                    violationTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedType = type
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Section 2: Severity Level Selector
            Text(
                text = "SEVERITY ASSESSMENT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LOW
                SeverityPill(
                    title = "LOW",
                    subtitle = "Minor Labeling",
                    isSelected = selectedSeverity == ViolationSeverity.LOW,
                    activeColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedSeverity = ViolationSeverity.LOW }
                )

                // MEDIUM
                SeverityPill(
                    title = "MEDIUM",
                    subtitle = "MRP / Expiry Risk",
                    isSelected = selectedSeverity == ViolationSeverity.MEDIUM,
                    activeColor = StatusWarning,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedSeverity = ViolationSeverity.MEDIUM }
                )

                // HIGH
                SeverityPill(
                    title = "HIGH",
                    subtitle = "Counterfeit / Safety",
                    isSelected = selectedSeverity == ViolationSeverity.HIGH,
                    activeColor = StatusInvalid,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedSeverity = ViolationSeverity.HIGH }
                )
            }

            // Section 3: Violation Description
            Text(
                text = "DETAILED FINDINGS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text("Violation Details & Specifics") },
                placeholder = { Text("e.g. Batch sticker altered; missing mandatory FSSAI/BIS registration number on carton.") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Section 4: Attached Evidence Images Strip
            Text(
                text = "SUPPORTING EVIDENCE IMAGES (${capturedImages.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (capturedImages.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No images attached. Captured photos from previous step will automatically be linked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Evidence Thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Section 5: Inspector Observation Notes
            Text(
                text = "OVERALL INSPECTOR NOTES / REGULATORY ACTION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = notesInput,
                onValueChange = {
                    notesInput = it
                    viewModel.setInspectorNotes(it)
                },
                label = { Text("Enforcement & Seizure Notes") },
                placeholder = { Text("e.g. Notice issued to vendor. Stock segregated pending lab verification.") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Recorded Violations List (if any already added)
            if (violations.isNotEmpty()) {
                Text(
                    text = "RECORDED VIOLATIONS IN THIS DOSSIER (${violations.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                violations.forEach { viol ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = viol.type,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (viol.severity) {
                                                    ViolationSeverity.HIGH -> StatusInvalidContainer
                                                    ViolationSeverity.MEDIUM -> StatusWarningContainer
                                                    ViolationSeverity.LOW -> MaterialTheme.colorScheme.primaryContainer
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = viol.severity.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (viol.severity) {
                                                ViolationSeverity.HIGH -> StatusInvalidText
                                                ViolationSeverity.MEDIUM -> StatusWarningText
                                                ViolationSeverity.LOW -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                }
                                if (viol.description.isNotBlank()) {
                                    Text(
                                        text = viol.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.removeViolation(viol.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action 1: Add More Violations
            OutlinedButton(
                onClick = {
                    val desc = descriptionInput.ifBlank { "Violation recorded for $selectedType" }
                    viewModel.addViolation(selectedType, selectedSeverity, desc, capturedImages)
                    descriptionInput = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("+ Record Additional Violation")
            }

            // Action 2: Save & Finalize Inspection
            Button(
                onClick = {
                    if (descriptionInput.isNotBlank() || violations.isEmpty()) {
                        val desc = descriptionInput.ifBlank { "Violation detected: $selectedType ($selectedSeverity severity)" }
                        viewModel.addViolation(selectedType, selectedSeverity, desc, capturedImages)
                    }
                    viewModel.setInspectorNotes(notesInput)
                    viewModel.submitInspection {
                        onFinalizeInspection()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("finalize_inspection_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Violation & Generate Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SeverityPill(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) activeColor else Color.LightGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
