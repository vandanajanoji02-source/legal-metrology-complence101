package com.example.ui.screens.inspection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Inspection
import com.example.model.ProductStatus
import com.example.model.ViolationSeverity
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.StatusInvalid
import com.example.ui.theme.StatusInvalidContainer
import com.example.ui.theme.StatusInvalidText
import com.example.ui.theme.StatusVerified
import com.example.ui.theme.StatusVerifiedContainer
import com.example.ui.theme.StatusVerifiedText
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.theme.StatusWarningText
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionReportScreen(
    inspection: Inspection,
    onBack: () -> Unit,
    onReturnHome: () -> Unit
) {
    val context = LocalContext.current
    var previewImageDialogPath by remember { mutableStateOf<String?>(null) }

    val status = inspection.status
    val (bannerBg, bannerFg, statusIcon, statusLabel) = when (status) {
        ProductStatus.VERIFIED -> Quadruple(
            StatusVerifiedContainer,
            StatusVerifiedText,
            Icons.Default.CheckCircle,
            "VERIFIED COMPLIANT"
        )
        ProductStatus.WARNING -> Quadruple(
            StatusWarningContainer,
            StatusWarningText,
            Icons.Default.Warning,
            "FLAGGED WITH WARNING"
        )
        ProductStatus.INVALID -> Quadruple(
            StatusInvalidContainer,
            StatusInvalidText,
            Icons.Default.Close,
            "VIOLATION / INVALID"
        )
    }

    fun shareReport() {
        val summary = buildString {
            appendLine("══════════════════════════════════════════")
            appendLine("SMARTVERIFY OFFICIAL INSPECTION DOSSIER")
            appendLine("══════════════════════════════════════════")
            appendLine("Inspection ID: ${inspection.inspectionId}")
            appendLine("Status: ${inspection.status.title}")
            appendLine("Date/Time: ${formatTimestamp(inspection.createdAt)}")
            appendLine("Inspector: ${inspection.inspectorName} (${inspection.inspectorId})")
            appendLine("------------------------------------------")
            appendLine("PRODUCT SPECIFICATIONS:")
            appendLine("• Name: ${inspection.productName}")
            appendLine("• Barcode: ${inspection.barcode}")
            appendLine("• Brand: ${inspection.brand.ifBlank { "N/A" }}")
            appendLine("• Category: ${inspection.category.ifBlank { "N/A" }}")
            appendLine("• Manufacturer: ${inspection.manufacturer.ifBlank { "N/A" }}")
            appendLine("• Mfg Date: ${inspection.manufacturingDate.ifBlank { "N/A" }}")
            appendLine("• Expiry Date: ${inspection.expiryDate.ifBlank { "N/A" }}")
            appendLine("------------------------------------------")
            if (inspection.violations.isNotEmpty()) {
                appendLine("RECORDED VIOLATIONS (${inspection.violations.size}):")
                inspection.violations.forEachIndexed { idx, viol ->
                    appendLine("${idx + 1}. [${viol.severity.name}] ${viol.type}")
                    if (viol.description.isNotBlank()) {
                        appendLine("   Details: ${viol.description}")
                    }
                }
                appendLine("------------------------------------------")
            }
            if (inspection.notes.isNotBlank()) {
                appendLine("INSPECTOR NOTES & DIRECTIVES:")
                appendLine(inspection.notes)
                appendLine("------------------------------------------")
            }
            appendLine("Evidence Photos Attached: ${inspection.imageUrls.size}")
            appendLine("Digital Signature: SHA256-${(inspection.inspectionId + inspection.createdAt).hashCode()}")
            appendLine("══════════════════════════════════════════")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "SmartVerify Inspection Report - ${inspection.inspectionId}")
            putExtra(Intent.EXTRA_TEXT, summary)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Inspection Report")
        context.startActivity(shareIntent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inspection Dossier",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { shareReport() },
                        modifier = Modifier.testTag("share_report_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Report")
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
            // Dossier Top Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = bannerBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(bannerFg.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = bannerFg,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = statusLabel,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = bannerFg
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bannerFg.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Inspection ID", inspection.inspectionId))
                                    Toast.makeText(context, "Copied ${inspection.inspectionId}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = inspection.inspectionId,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = bannerFg
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy ID",
                                    tint = bannerFg,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Timestamp: ${formatTimestamp(inspection.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = bannerFg.copy(alpha = 0.85f)
                    )
                }
            }

            // Inspecting Officer Details
            Text(
                text = "INSPECTOR INFORMATION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportFieldRow(label = "Field Officer", value = inspection.inspectorName)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ReportFieldRow(label = "Officer ID", value = inspection.inspectorId)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ReportFieldRow(label = "Enforcement Unit", value = "Standards & Anti-Counterfeit Taskforce")
                }
            }

            // Product Specifications
            Text(
                text = "PRODUCT DATA & VERIFICATION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportFieldRow(label = "Product Name", value = inspection.productName)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ReportFieldRow(label = "Barcode", value = inspection.barcode)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ReportFieldRow(label = "Brand", value = inspection.brand.ifBlank { "Unregistered" })
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ReportFieldRow(label = "Category", value = inspection.category.ifBlank { "General Merchandise" })
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ReportFieldRow(label = "Manufacturer", value = inspection.manufacturer.ifBlank { "Third Party" })
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ReportFieldRow(label = "Mfg Date", value = inspection.manufacturingDate.ifBlank { "N/A" })
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ReportFieldRow(label = "Expiry Date", value = inspection.expiryDate.ifBlank { "N/A" })
                }
            }

            // Violations Section (if any)
            if (inspection.violations.isNotEmpty()) {
                Text(
                    text = "RECORDED STATUTORY VIOLATIONS (${inspection.violations.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatusInvalid
                )

                inspection.violations.forEachIndexed { index, violation ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. ${violation.type}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (violation.severity) {
                                                ViolationSeverity.HIGH -> StatusInvalidContainer
                                                ViolationSeverity.MEDIUM -> StatusWarningContainer
                                                ViolationSeverity.LOW -> MaterialTheme.colorScheme.primaryContainer
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = violation.severity.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (violation.severity) {
                                            ViolationSeverity.HIGH -> StatusInvalidText
                                            ViolationSeverity.MEDIUM -> StatusWarningText
                                            ViolationSeverity.LOW -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                            }

                            if (violation.description.isNotBlank()) {
                                Text(
                                    text = violation.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Evidence Photos Gallery
            Text(
                text = "CAPTURED EVIDENCE PHOTOS (${inspection.imageUrls.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (inspection.imageUrls.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No evidence photos were captured during this inspection session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(inspection.imageUrls) { path ->
                        val bitmap = remember(path) {
                            try {
                                BitmapFactory.decodeFile(path)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { previewImageDialogPath = path }
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Evidence Full Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Inspector Observation Notes
            if (inspection.notes.isNotBlank()) {
                Text(
                    text = "OFFICIAL INSPECTOR REMARKS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = inspection.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Bottom Actions
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { shareReport() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share / Export Official Dossier", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onReturnHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Return to Inspector Home", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Full Screen Image Dialog
        if (previewImageDialogPath != null) {
            val path = previewImageDialogPath!!
            val bitmap = remember(path) {
                try {
                    BitmapFactory.decodeFile(path)
                } catch (e: Exception) {
                    null
                }
            }

            AlertDialog(
                onDismissRequest = { previewImageDialogPath = null },
                title = { Text("Evidence Photo Review") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Dossier Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Unable to preview image")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { previewImageDialogPath = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
private fun ReportFieldRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
