package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.ProductRepository
import com.example.data.ScanRepository
import com.example.model.Product
import com.example.model.ProductStatus
import com.example.model.ScanRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanProcessState {
    object Idle : ScanProcessState
    data class Processing(val barcode: String) : ScanProcessState
    data class ResultReady(
        val product: Product?,
        val scanRecord: ScanRecord
    ) : ScanProcessState
    data class Error(val message: String) : ScanProcessState
}

class ScannerViewModel(
    private val productRepository: ProductRepository,
    private val scanRepository: ScanRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanProcessState>(ScanProcessState.Idle)
    val scanState: StateFlow<ScanProcessState> = _scanState.asStateFlow()

    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    fun toggleTorch() {
        _torchEnabled.value = !_torchEnabled.value
    }

    fun processBarcode(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return

        // If already processing, skip
        if (_scanState.value is ScanProcessState.Processing) return

        _scanState.value = ScanProcessState.Processing(trimmed)

        viewModelScope.launch {
            try {
                val currentUser = authRepository.currentUser.value
                val userId = currentUser?.id ?: "guest_scanner"
                val userName = currentUser?.name ?: "Scanner Agent"

                val product = productRepository.findByBarcode(trimmed)

                val (status, message, prodName, prodId) = if (product != null) {
                    Quadruple(
                        product.status,
                        product.status.defaultMessage,
                        product.name,
                        product.id
                    )
                } else {
                    Quadruple(
                        ProductStatus.INVALID,
                        "No matching product was found in the registered database.",
                        "Unregistered Product",
                        ""
                    )
                }

                val scanRecord = ScanRecord(
                    id = "scan_" + System.currentTimeMillis(),
                    userId = userId,
                    userName = userName,
                    barcode = trimmed,
                    productId = prodId,
                    productName = prodName,
                    status = status,
                    timestamp = System.currentTimeMillis(),
                    message = message
                )

                // Save to Firestore & local state
                scanRepository.saveScan(scanRecord)

                _scanState.value = ScanProcessState.ResultReady(product, scanRecord)
            } catch (e: Exception) {
                _scanState.value = ScanProcessState.Error("Failed to verify product: ${e.message}")
            }
        }
    }

    fun resetScanner() {
        _scanState.value = ScanProcessState.Idle
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
