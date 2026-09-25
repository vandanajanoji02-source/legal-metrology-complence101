package com.example.ui.screens.scanner

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InspectionStats
import com.example.model.AppUser
import com.example.model.Inspection
import com.example.network.ApiClient
import com.example.network.NetworkConfig
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.StatusInvalid
import com.example.ui.theme.StatusInvalidContainer
import com.example.ui.theme.StatusInvalidText
import com.example.ui.theme.StatusVerified
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerHomeScreen(
    user: AppUser,
    stats: InspectionStats,
    recentInspections: List<Inspection>,
    onStartNewInspection: () -> Unit,
    onOpenQuickScanner: () -> Unit,
    onViewHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    onSelectInspection: (Inspection) -> Unit,
    onSimulateScan: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var backendUrl by remember { mutableStateOf(NetworkConfig.getBaseUrl(context)) }
    var isCheckingApi by remember { mutableStateOf(false) }
    var isApiConnected by remember { mutableStateOf<Boolean?>(null) }
    var showUrlEditDialog by remember { mutableStateOf(false) }
    var tempUrl by remember { mutableStateOf(backendUrl) }

    LaunchedEffect(backendUrl) {
        isCheckingApi = true
        isApiConnected = ApiClient.isServerReachable()
        isCheckingApi = false
    }

    if (showUrlEditDialog) {
        AlertDialog(
            onDismissRequest = { showUrlEditDialog = false },
            title = { Text("Configure Backend API") },
            text = {
                Column {
                    Text(
                        "Set the API Base URL for the app to communicate with. For local Android emulator testing, use http://10.0.2.2:5000/api/",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("API Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        NetworkConfig.setBaseUrl(context, tempUrl)
                        backendUrl = NetworkConfig.getBaseUrl(context)
                        showUrlEditDialog = false
                    }
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        NetworkConfig.resetToDefault(context)
                        backendUrl = NetworkConfig.getBaseUrl(context)
                        showUrlEditDialog = false
                    }
                ) {
                    Text("Reset Default")
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
                            text = "SmartVerify",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Field Officer Inspector Portal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onViewHistory,
                        modifier = Modifier.testTag("history_nav_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Inspection History")
                    }
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.testTag("profile_nav_button")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Inspector Profile")
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Inspector Welcome & Identification Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenProfile),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Welcome on Duty,",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = user.name.ifBlank { "Field Inspector" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = user.inspectorId,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(StatusVerified)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Online • Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Backend API Connectivity Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            tempUrl = backendUrl
                            showUrlEditDialog = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCheckingApi -> MaterialTheme.colorScheme.surfaceContainerHighest
                                            isApiConnected == true -> StatusVerified.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isCheckingApi -> Icons.Default.CloudSync
                                        isApiConnected == true -> Icons.Default.CloudDone
                                        else -> Icons.Default.CloudOff
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isCheckingApi -> MaterialTheme.colorScheme.primary
                                        isApiConnected == true -> StatusVerified
                                        else -> MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Backend API",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when {
                                                    isCheckingApi -> MaterialTheme.colorScheme.surfaceVariant
                                                    isApiConnected == true -> StatusVerified.copy(alpha = 0.2f)
                                                    else -> MaterialTheme.colorScheme.errorContainer
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isCheckingApi -> "Checking..."
                                                isApiConnected == true -> "Connected"
                                                else -> "Offline Fallback"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isCheckingApi -> MaterialTheme.colorScheme.onSurfaceVariant
                                                isApiConnected == true -> StatusVerified
                                                else -> MaterialTheme.colorScheme.error
                                            }
                                        )
                                    }
                                }
                                Text(
                                    text = backendUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isCheckingApi = true
                                    isApiConnected = ApiClient.isServerReachable()
                                    isCheckingApi = false
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Connection",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Primary Action 1: "START NEW INSPECTION" (High-Impact Button)
            item {
                Button(
                    onClick = onStartNewInspection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .testTag("start_new_inspection_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "START NEW INSPECTION",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Multi-Photo Evidence + Barcode Verification",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Primary Action 2: "QUICK BARCODE SCAN"
            item {
                OutlinedButton(
                    onClick = onOpenQuickScanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("quick_scan_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Quick Barcode Reader",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                }
            }

            // Key Inspection Metrics
            item {
                Text(
                    text = "TODAY'S ENFORCEMENT METRICS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Today",
                        value = stats.todayInspections.toString(),
                        icon = Icons.Default.Shield,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Verified",
                        value = stats.verifiedCount.toString(),
                        icon = Icons.Default.CheckCircle,
                        accentColor = StatusVerified,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Flagged",
                        value = stats.violationCount.toString(),
                        icon = Icons.Default.ReportProblem,
                        accentColor = StatusInvalid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Demo Barcode Trigger (Essential for Emulator / Hackathon Demo Reliability)
            item {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚡ Instant Barcode Simulation (Demo)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Simulate barcode lookups directly for presentation testing:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSimulateScan("8901234567890") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("simulate_verified_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Verified", fontSize = 12.sp, color = StatusVerified)
                            }

                            OutlinedButton(
                                onClick = { onSimulateScan("8901234567891") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("simulate_warning_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Warning", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                            }

                            OutlinedButton(
                                onClick = { onSimulateScan("8901234567892") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("simulate_invalid_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Invalid", fontSize = 12.sp, color = StatusInvalid)
                            }
                        }
                    }
                }
            }

            // Recent Inspections Feed
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT INSPECTION REPORTS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onViewHistory) {
                        Text("View All", fontSize = 13.sp)
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (recentInspections.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No inspection records recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentInspections.take(4)) { inspection ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectInspection(inspection) }
                            .testTag("recent_inspection_card_${inspection.inspectionId}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inspection.productName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusBadge(status = inspection.status)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ID: ${inspection.inspectionId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (inspection.violations.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(StatusInvalidContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${inspection.violations.size} Violations",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusInvalidText
                                        )
                                    }
                                }
                            }

                            Text(
                                text = formatTimestamp(inspection.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
