package com.example.ui.screens.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppUser
import com.example.model.ProductStatus
import com.example.model.ScanRecord
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.StatusInvalid
import com.example.ui.theme.StatusVerified
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    user: AppUser,
    viewModel: AdminViewModel,
    onNavigateProducts: () -> Unit,
    onNavigateScans: () -> Unit,
    onLogout: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val allScans by viewModel.allScans.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Admin Control Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SIH ADMIN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        Text(
                            text = user.name.ifBlank { "System Administrator" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.seedDemoProducts() },
                        modifier = Modifier.testTag("admin_seed_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Seed Demo Products")
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("admin_logout_button")
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
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on dashboard */ },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateProducts,
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Products") },
                    label = { Text("Products") },
                    modifier = Modifier.testTag("nav_products_tab")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateScans,
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "All Scans") },
                    label = { Text("Scans") },
                    modifier = Modifier.testTag("nav_scans_tab")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Live Analytics Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SYSTEM OVERVIEW & KPIS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusVerified)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Firestore Live Sync",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusVerified
                                )
                            }
                        }

                        // Top 2 Big KPI cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Total Scans",
                                value = stats.totalScans.toString(),
                                icon = Icons.Default.QrCodeScanner,
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Active Users",
                                value = stats.activeUsersCount.toString(),
                                icon = Icons.Default.Group,
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 3 Status KPIs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Verified",
                                value = stats.verifiedCount.toString(),
                                icon = Icons.Default.CheckCircle,
                                accentColor = StatusVerified,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Warning",
                                value = stats.warningCount.toString(),
                                icon = Icons.Default.Warning,
                                accentColor = StatusWarning,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Invalid",
                                value = stats.invalidCount.toString(),
                                icon = Icons.Default.Close,
                                accentColor = StatusInvalid,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Visual Status Distribution Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PRODUCT VERIFICATION BREAKDOWN",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val total = stats.totalScans.coerceAtLeast(1)
                        val verifiedPercent = (stats.verifiedCount.toFloat() / total * 100).toInt()
                        val warningPercent = (stats.warningCount.toFloat() / total * 100).toInt()
                        val invalidPercent = (stats.invalidCount.toFloat() / total * 100).toInt()

                        StatusProgressBar(
                            label = "Verified Products",
                            count = stats.verifiedCount,
                            percent = verifiedPercent,
                            color = StatusVerified
                        )

                        StatusProgressBar(
                            label = "Warning Products (Require Inspection)",
                            count = stats.warningCount,
                            percent = warningPercent,
                            color = StatusWarning
                        )

                        StatusProgressBar(
                            label = "Invalid / Unregistered Barcodes",
                            count = stats.invalidCount,
                            percent = invalidPercent,
                            color = StatusInvalid
                        )
                    }
                }
            }

            // Scans by User Section (Multi-user capability demonstration)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SCANS BY FIELD AGENTS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (stats.scansByUser.isEmpty()) {
                            Text(
                                text = "No user activity yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            stats.scansByUser.forEach { (user, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = user,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$count scans",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }

            // Recent Audit Scans Preview
            item {
                Text(
                    text = "RECENT SCAN AUDIT STREAM",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(allScans.take(5)) { scan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                text = scan.productName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            StatusBadge(status = scan.status)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Barcode: ${scan.barcode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = scan.userName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = formatTimestamp(scan.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StatusProgressBar(
    label: String,
    count: Int,
    percent: Int,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count ($percent%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
