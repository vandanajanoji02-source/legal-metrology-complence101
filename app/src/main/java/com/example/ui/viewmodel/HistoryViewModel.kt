package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InspectionRepository
import com.example.data.ScanRepository
import com.example.model.Inspection
import com.example.model.ProductStatus
import com.example.model.ScanRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    private val scanRepository: ScanRepository,
    private val inspectionRepository: InspectionRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<ProductStatus?>(null)
    val selectedStatusFilter: StateFlow<ProductStatus?> = _selectedStatusFilter.asStateFlow()

    val filteredScans: StateFlow<List<ScanRecord>> = combine(
        scanRepository.scans,
        _searchQuery,
        _selectedStatusFilter
    ) { scans, query, filter ->
        scans.filter { scan ->
            val matchesQuery = query.isBlank() ||
                scan.productName.contains(query, ignoreCase = true) ||
                scan.barcode.contains(query, ignoreCase = true) ||
                scan.userName.contains(query, ignoreCase = true)

            val matchesFilter = filter == null || scan.status == filter

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val filteredInspections: StateFlow<List<Inspection>> = combine(
        inspectionRepository?.inspections ?: MutableStateFlow(emptyList()),
        _searchQuery,
        _selectedStatusFilter
    ) { inspections, query, filter ->
        inspections.filter { inspection ->
            val matchesQuery = query.isBlank() ||
                inspection.inspectionId.contains(query, ignoreCase = true) ||
                inspection.productName.contains(query, ignoreCase = true) ||
                inspection.barcode.contains(query, ignoreCase = true) ||
                inspection.brand.contains(query, ignoreCase = true) ||
                inspection.inspectorName.contains(query, ignoreCase = true)

            val matchesFilter = filter == null || inspection.status == filter

            matchesQuery && matchesFilter
        }.sortedByDescending { it.createdAt }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(status: ProductStatus?) {
        _selectedStatusFilter.value = status
    }
}
