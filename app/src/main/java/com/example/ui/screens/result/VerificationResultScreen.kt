package com.example.ui.screens.result

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.model.ProductStatus
import com.example.model.ScanRecord
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationResultScreen(
    product: Product?,
    scanRecord: ScanRecord,
    evidenceImages: List<String> = emptyList(),
    onProceedToViolation: () -> Unit,
    onCompleteInspection: () -> Unit,
    onScanAnother: () -> Unit,
    onViewHistory: () -> Unit,
    onBack: () -> Unit
) {
    val status = scanRecord.status
    val (bannerBg, bannerFg, statusIcon, statusLabel) = when (status) {
        ProductStatus.VERIFIED -> Quadruple(
            StatusVerifiedContainer,
            StatusVerifiedText,
            Icons.Default.CheckCircle,
            "✓ VERIFIED / AUTHENTIC"
        )
        ProductStatus.WARNING -> Quadruple(
            StatusWarningContainer,
            StatusWarningText,
            Icons.Default.Warning,
            "⚠ WARNING / FLAGGED"
        )
        ProductStatus.INVALID -> Quadruple(
            StatusInvalidContainer,
            StatusInvalidText,
            Icons.Default.Close,
            "✕ INVALID / UNREGISTERED"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Verification Result",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onScanAnother) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Another")
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
            // Massive Status Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verification_status_banner"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = bannerBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(bannerFg.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = bannerFg,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = statusLabel,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = bannerFg,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = scanRecord.message.ifBlank { status.defaultMessage },
                        style = MaterialTheme.typography.bodyMedium,
                        color = bannerFg,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bannerFg.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Barcode: ${scanRecord.barcode}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = bannerFg
                        )
                    }
                }
            }

            // Attached Evidence Images (if captured in step 1)
            if (evidenceImages.isNotEmpty()) {
                Text(
                    text = "ATTACHED EVIDENCE PHOTOS (${evidenceImages.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(evidenceImages) { path ->
                        val bitmap = remember(path) {
                            try {
                                BitmapFactory.decodeFile(path)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Evidence Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Product Details Section
            Text(
                text = "PRODUCT SPECIFICATIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(
                        label = "Product Name",
                        value = product?.name ?: scanRecord.productName
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    DetailRow(
                        label = "Brand",
                        value = product?.brand?.ifBlank { "Unregistered Brand" } ?: "Unregistered Brand"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    DetailRow(
                        label = "Category",
                        value = product?.category?.ifBlank { "General" } ?: "General"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    DetailRow(
                        label = "Manufacturer",
                        value = product?.manufacturer?.ifBlank { "Unverified Third Party" } ?: "Unverified Third Party"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    DetailRow(
                        label = "Manufacturing Date",
                        value = product?.manufacturingDate?.ifBlank { "N/A" } ?: "N/A"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    DetailRow(
                        label = "Expiry Date",
                        value = product?.expiryDate?.ifBlank { "N/A" } ?: "N/A"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    DetailRow(
                        label = "Inspecting Officer",
                        value = scanRecord.userName
                    )

                    if (product?.description?.isNotBlank() == true) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Security & Regulatory Notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = product.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Primary Workflow Decisions
            Spacer(modifier = Modifier.height(4.dp))

            if (status == ProductStatus.VERIFIED) {
                // If verified, standard completion is primary
                Button(
                    onClick = onCompleteInspection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("complete_inspection_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Inspection & Save Report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = onProceedToViolation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("flag_violation_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = StatusWarning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Flag Potential Defect / Violation", color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                // If WARNING or INVALID, reporting violation is the primary action
                Button(
                    onClick = onProceedToViolation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("record_violation_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status == ProductStatus.INVALID) StatusInvalid else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record Violation & Evidence (Required)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = onCompleteInspection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save Inspection As-is Without Violation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(
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
