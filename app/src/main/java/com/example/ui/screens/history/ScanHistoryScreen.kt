package com.example.ui.screens.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Inspection
import com.example.model.ProductStatus
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.StatusInvalidContainer
import com.example.ui.theme.StatusInvalidText
import com.example.ui.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onSelectInspection: (Inspection) -> Unit
) {
    val inspections by viewModel.filteredInspections.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedStatusFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inspection Dossier History",
                        fontWeight = FontWeight.Bold
                    )
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
        ) {
            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search by ID, product, barcode, inspector...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_history_input")
            )

            // Status Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { viewModel.onFilterSelected(null) },
                        label = { Text("All (${inspections.size})") },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == ProductStatus.VERIFIED,
                        onClick = {
                            viewModel.onFilterSelected(
                                if (selectedFilter == ProductStatus.VERIFIED) null else ProductStatus.VERIFIED
                            )
                        },
                        label = { Text("Verified") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == ProductStatus.WARNING,
                        onClick = {
                            viewModel.onFilterSelected(
                                if (selectedFilter == ProductStatus.WARNING) null else ProductStatus.WARNING
                            )
                        },
                        label = { Text("Warning") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == ProductStatus.INVALID,
                        onClick = {
                            viewModel.onFilterSelected(
                                if (selectedFilter == ProductStatus.INVALID) null else ProductStatus.INVALID
                            )
                        },
                        label = { Text("Violations / Invalid") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Inspections List
            if (inspections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No inspection records match your filter criteria",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(inspections, key = { it.inspectionId }) { inspection ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectInspection(inspection) }
                                .testTag("inspection_history_card_${inspection.inspectionId}"),
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = inspection.productName,
                                        style = MaterialTheme.typography.titleMedium,
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
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (inspection.imageUrls.isNotEmpty()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.PhotoCamera,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "${inspection.imageUrls.size}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

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
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Officer: ${inspection.inspectorName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatTimestamp(inspection.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
