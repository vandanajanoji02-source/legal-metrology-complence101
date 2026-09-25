package com.example.ui.screens.inspection

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compliance.ComplianceCheck
import com.example.compliance.ComplianceResult
import com.example.compliance.ComplianceStatus
import com.example.ui.theme.*

import com.example.ui.viewmodel.InspectionFlowState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplianceResultScreen(
    result: ComplianceResult,
    inspectionId: String = "",
    flowState: InspectionFlowState = InspectionFlowState.Idle,
    onSubmitInspection: () -> Unit,
    onViewReport: () -> Unit = {},
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LEGAL METROLOGY COMPLIANCE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        if (inspectionId.isNotBlank()) {
                            Text(
                                text = "$inspectionId • Automated Rule Screening",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Feedback Banners based on flowState
                    when (flowState) {
                        is InspectionFlowState.Completed -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusVerifiedContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusVerified),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusVerifiedText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Inspection submitted successfully",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StatusVerifiedText
                                    )
                                }
                            }
                        }
                        is InspectionFlowState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusInvalidContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusInvalid),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = StatusInvalidText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = flowState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusInvalidText,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = result.disclaimer,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val isSubmitting = flowState is InspectionFlowState.Submitting
                    val isCompleted = flowState is InspectionFlowState.Completed
                    val isError = flowState is InspectionFlowState.Error

                    Button(
                        onClick = {
                            if (isCompleted) {
                                onViewReport()
                            } else {
                                onSubmitInspection()
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) StatusVerified else MaterialTheme.colorScheme.primary
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
                                text = "Submitting...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View Inspection Report",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isError) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Retry Submission",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Submit Inspection",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overall Status Banner
            item { OverallStatusCard(result = result) }

            // Summary Metrics Chips
            item { SummaryRow(result = result) }

            // Rule Authority / Source Tag
            item { RuleSourceBadge(result = result) }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rule Compliance Evaluation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${result.checks.size} Checks Evaluated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Every Individual Rule Card
            items(result.checks) { check ->
                ComplianceCheckCard(check = check)
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

// =============================================================================
// Overall Status Card
// =============================================================================

@Composable
private fun OverallStatusCard(result: ComplianceResult) {
    val bgColor: Color
    val borderColor: Color
    val textColor: Color
    val icon: ImageVector
    val label: String
    val subtitle: String

    when (result.overallStatus) {
        ComplianceStatus.PASS -> {
            bgColor = StatusVerifiedContainer
            borderColor = StatusVerified
            textColor = StatusVerifiedText
            icon = Icons.Default.CheckCircle
            label = "PASS"
            subtitle = "All required declarations detected and verified"
        }
        ComplianceStatus.FAIL -> {
            bgColor = StatusInvalidContainer
            borderColor = StatusInvalid
            textColor = StatusInvalidText
            icon = Icons.Default.Cancel
            label = "FAIL"
            subtitle = "${result.failedCount} confirmed violation(s) detected"
        }
        ComplianceStatus.REVIEW -> {
            bgColor = StatusWarningContainer
            borderColor = StatusWarning
            textColor = StatusWarningText
            icon = Icons.Default.Info
            label = "REVIEW"
            subtitle = "${result.reviewCount} declaration(s) require inspector physical verification"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = borderColor,
                modifier = Modifier.size(48.dp)
            )
            Column {
                Text(
                    text = "Overall Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }
        }
    }
}

// =============================================================================
// Summary Row
// =============================================================================

@Composable
private fun SummaryRow(result: ComplianceResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(
            modifier = Modifier.weight(1f),
            count = result.passedCount,
            label = "Passed",
            color = StatusVerified,
            bgColor = StatusVerifiedContainer
        )
        SummaryChip(
            modifier = Modifier.weight(1f),
            count = result.failedCount,
            label = "Violations",
            color = StatusInvalid,
            bgColor = StatusInvalidContainer
        )
        SummaryChip(
            modifier = Modifier.weight(1f),
            count = result.reviewCount,
            label = "Needs Review",
            color = StatusWarning,
            bgColor = StatusWarningContainer
        )
    }
}

@Composable
private fun SummaryChip(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    color: Color,
    bgColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

// =============================================================================
// Rule Source Badge
// =============================================================================

@Composable
private fun RuleSourceBadge(result: ComplianceResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Gavel,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = result.ruleSource,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Version: ${result.ruleVersion} — Package declaration rules evaluation",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// =============================================================================
// Individual Rule Check Card
// =============================================================================

@Composable
private fun ComplianceCheckCard(check: ComplianceCheck) {
    val bgColor: Color
    val borderColor: Color
    val textColor: Color
    val icon: ImageVector

    when (check.status) {
        ComplianceStatus.PASS -> {
            bgColor = StatusVerifiedContainer
            borderColor = StatusVerified
            textColor = StatusVerifiedText
            icon = Icons.Default.CheckCircle
        }
        ComplianceStatus.FAIL -> {
            bgColor = StatusInvalidContainer
            borderColor = StatusInvalid
            textColor = StatusInvalidText
            icon = Icons.Default.Cancel
        }
        ComplianceStatus.REVIEW -> {
            bgColor = StatusWarningContainer
            borderColor = StatusWarning
            textColor = StatusWarningText
            icon = Icons.Default.Warning
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = check.status.name,
                tint = borderColor,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header: Field Name & Rule Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = check.fieldName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = check.ruleCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Detected Value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detected: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = check.detectedValue ?: "Not detected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (check.detectedValue != null) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (check.detectedValue == null) FontStyle.Italic else FontStyle.Normal,
                        color = if (check.detectedValue != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Status line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Status: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ComplianceStatusBadge(status = check.status)
                }

                // Reason line
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reason: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.9f)
                    )
                    Text(
                        text = check.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComplianceStatusBadge(status: ComplianceStatus) {
    val label: String
    val color: Color
    val bg: Color
    when (status) {
        ComplianceStatus.PASS -> {
            label = "PASS"
            color = StatusVerifiedText
            bg = StatusVerified.copy(alpha = 0.15f)
        }
        ComplianceStatus.FAIL -> {
            label = "FAIL"
            color = StatusInvalidText
            bg = StatusInvalid.copy(alpha = 0.15f)
        }
        ComplianceStatus.REVIEW -> {
            label = "REVIEW"
            color = StatusWarningText
            bg = StatusWarning.copy(alpha = 0.15f)
        }
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 11.sp
        )
    }
}
