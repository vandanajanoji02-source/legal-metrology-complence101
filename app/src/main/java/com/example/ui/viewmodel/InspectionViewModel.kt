package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.compliance.ComplianceResult
import com.example.compliance.ComplianceStatus
import com.example.data.AuthRepository
import com.example.data.InspectionRepository
import com.example.data.ProductRepository
import com.example.data.ScanRepository
import com.example.model.Inspection
import com.example.model.Product
import com.example.model.ProductStatus
import com.example.model.ScanRecord
import com.example.model.Violation
import com.example.model.ViolationSeverity
import com.example.ocr.OcrService
import com.example.ocr.ProductFieldExtractor
import com.example.ocr.ProductOcrResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OcrProcessState {
    object Idle : OcrProcessState
    data class Analyzing(val currentPhoto: Int, val totalPhotos: Int) : OcrProcessState
    data class Success(val result: ProductOcrResult) : OcrProcessState
    data class Empty(val message: String) : OcrProcessState
    data class Error(val message: String) : OcrProcessState
}

sealed interface InspectionFlowState {
    object Idle : InspectionFlowState
    object InProgress : InspectionFlowState
    object VerifyingBarcode : InspectionFlowState
    data class VerificationReady(val product: Product?, val status: ProductStatus) : InspectionFlowState
    object Submitting : InspectionFlowState
    data class Completed(val inspection: Inspection) : InspectionFlowState
    data class Error(val message: String) : InspectionFlowState
}

class InspectionViewModel(
    private val inspectionRepository: InspectionRepository,
    private val productRepository: ProductRepository,
    private val scanRepository: ScanRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _flowState = MutableStateFlow<InspectionFlowState>(InspectionFlowState.Idle)
    val flowState: StateFlow<InspectionFlowState> = _flowState.asStateFlow()

    private val _ocrProcessState = MutableStateFlow<OcrProcessState>(OcrProcessState.Idle)
    val ocrProcessState: StateFlow<OcrProcessState> = _ocrProcessState.asStateFlow()

    private val _ocrResult = MutableStateFlow<ProductOcrResult?>(null)
    val ocrResult: StateFlow<ProductOcrResult?> = _ocrResult.asStateFlow()

    private val _currentInspectionId = MutableStateFlow(generateInspectionId())
    val currentInspectionId: StateFlow<String> = _currentInspectionId.asStateFlow()

    private val _capturedImages = MutableStateFlow<List<String>>(emptyList())
    val capturedImages: StateFlow<List<String>> = _capturedImages.asStateFlow()

    private val _scannedBarcode = MutableStateFlow("")
    val scannedBarcode: StateFlow<String> = _scannedBarcode.asStateFlow()

    private val _currentProduct = MutableStateFlow<Product?>(null)
    val currentProduct: StateFlow<Product?> = _currentProduct.asStateFlow()

    private val _verificationStatus = MutableStateFlow(ProductStatus.VERIFIED)
    val verificationStatus: StateFlow<ProductStatus> = _verificationStatus.asStateFlow()

    private val _violations = MutableStateFlow<List<Violation>>(emptyList())
    val violations: StateFlow<List<Violation>> = _violations.asStateFlow()

    private val _inspectorNotes = MutableStateFlow("")
    val inspectorNotes: StateFlow<String> = _inspectorNotes.asStateFlow()

    private val _activeInspectionForReport = MutableStateFlow<Inspection?>(null)
    val activeInspectionForReport: StateFlow<Inspection?> = _activeInspectionForReport.asStateFlow()

    fun startNewInspection() {
        _currentInspectionId.value = generateInspectionId()
        _capturedImages.value = emptyList()
        _scannedBarcode.value = ""
        _currentProduct.value = null
        _verificationStatus.value = ProductStatus.VERIFIED
        _violations.value = emptyList()
        _inspectorNotes.value = ""
        _ocrResult.value = null
        _ocrProcessState.value = OcrProcessState.Idle
        _flowState.value = InspectionFlowState.InProgress
    }

    fun addCapturedImage(filePath: String) {
        if (filePath.isNotBlank() && !_capturedImages.value.contains(filePath)) {
            _capturedImages.value = _capturedImages.value + filePath
        }
    }

    fun removeCapturedImage(filePath: String) {
        _capturedImages.value = _capturedImages.value.filterNot { it == filePath }
    }

    fun setInspectorNotes(notes: String) {
        _inspectorNotes.value = notes
    }

    fun processBarcode(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return

        // Prevent duplicate scanning during the same inspection session
        if (_scannedBarcode.value == trimmed && _currentProduct.value != null) {
            _flowState.value = InspectionFlowState.VerificationReady(_currentProduct.value, _verificationStatus.value)
            return
        }

        _scannedBarcode.value = trimmed
        _flowState.value = InspectionFlowState.VerifyingBarcode

        viewModelScope.launch {
            try {
                val product = productRepository.findByBarcode(trimmed)
                _currentProduct.value = product
                val status = product?.status ?: ProductStatus.INVALID
                _verificationStatus.value = status

                _flowState.value = InspectionFlowState.VerificationReady(product, status)
            } catch (e: Exception) {
                _flowState.value = InspectionFlowState.Error("Product verification failed: ${e.message}")
            }
        }
    }

    fun addViolation(
        type: String,
        severity: ViolationSeverity,
        description: String,
        evidenceImages: List<String> = emptyList()
    ) {
        val violation = Violation(
            id = "viol_" + System.currentTimeMillis(),
            type = type.ifBlank { "Other" },
            severity = severity,
            description = description,
            evidenceImages = evidenceImages.ifEmpty { _capturedImages.value }
        )
        _violations.value = _violations.value + violation
    }

    fun removeViolation(violationId: String) {
        _violations.value = _violations.value.filterNot { it.id == violationId }
    }

    fun runOcrOnCapturedPhotos(onComplete: (ProductOcrResult) -> Unit = {}) {
        val photos = _capturedImages.value
        if (photos.isEmpty()) {
            _ocrProcessState.value = OcrProcessState.Empty("No photos captured to analyze.")
            return
        }

        _ocrProcessState.value = OcrProcessState.Analyzing(currentPhoto = 1, totalPhotos = photos.size)

        viewModelScope.launch {
            try {
                val results = OcrService.processMultipleImages(photos) { current, total ->
                    _ocrProcessState.value = OcrProcessState.Analyzing(currentPhoto = current, totalPhotos = total)
                }

                val validResults = results.filter { it.isSuccess && it.rawText.isNotBlank() }
                val combinedRawText = validResults.joinToString("\n\n---\n\n") { it.rawText }

                if (combinedRawText.isBlank()) {
                    val emptyResult = ProductOcrResult(
                        rawText = "",
                        processedPhotoCount = 0,
                        totalPhotoCount = photos.size
                    )
                    _ocrResult.value = emptyResult
                    _ocrProcessState.value = OcrProcessState.Empty("No readable text detected in submitted photos.")
                    onComplete(emptyResult)
                    return@launch
                }

                val extracted = ProductFieldExtractor.extract(
                    combinedRawText = combinedRawText,
                    individualTexts = results.map { it.rawText }
                )

                _ocrResult.value = extracted
                _ocrProcessState.value = OcrProcessState.Success(extracted)
                onComplete(extracted)
            } catch (e: Exception) {
                _ocrProcessState.value = OcrProcessState.Error("OCR analysis error: ${e.message}")
            }
        }
    }

    fun updateOcrResult(updated: ProductOcrResult) {
        _ocrResult.value = updated
    }

    fun updateOcrField(fieldName: String, newValue: String?) {
        val current = _ocrResult.value ?: ProductOcrResult()
        val cleanVal = newValue?.trim()?.ifBlank { null }
        val newConfidence = current.confidenceMap.toMutableMap()
        newConfidence[fieldName] = if (cleanVal != null) "Edited" else "Not detected"

        val updated = when (fieldName) {
            "Product Name" -> current.copy(productName = cleanVal, confidenceMap = newConfidence)
            "Brand" -> current.copy(brand = cleanVal, confidenceMap = newConfidence)
            "MRP" -> current.copy(mrp = cleanVal, confidenceMap = newConfidence)
            "Manufacturing Date" -> current.copy(manufacturingDate = cleanVal, confidenceMap = newConfidence)
            "Expiry Date" -> current.copy(expiryDate = cleanVal, confidenceMap = newConfidence)
            "Batch Number" -> current.copy(batchNumber = cleanVal, confidenceMap = newConfidence)
            "Net Quantity" -> current.copy(netQuantity = cleanVal, confidenceMap = newConfidence)
            "Manufacturer" -> current.copy(manufacturer = cleanVal, confidenceMap = newConfidence)
            "Manufacturer Address" -> current.copy(manufacturerAddress = cleanVal, confidenceMap = newConfidence)
            "License Number" -> current.copy(licenseNumber = cleanVal, confidenceMap = newConfidence)
            else -> current
        }
        _ocrResult.value = updated
    }

    fun submitInspection(
        complianceResult: ComplianceResult? = null,
        onSuccess: (Inspection) -> Unit = {}
    ) {
        _flowState.value = InspectionFlowState.Submitting

        viewModelScope.launch {
            try {
                val currentUser = authRepository.currentUser.value
                val inspectorId = currentUser?.inspectorId ?: currentUser?.id ?: "INSP-FIELD"
                val inspectorName = currentUser?.name ?: "Field Inspector"

                val product = _currentProduct.value
                val ocr = _ocrResult.value
                val barcode = _scannedBarcode.value.ifBlank { product?.barcode ?: "UNSCANNED" }
                val prodName = product?.name?.ifBlank { null } ?: ocr?.productName ?: "Product $barcode"
                val prodBrand = product?.brand?.ifBlank { null } ?: ocr?.brand ?: ""
                val prodMfg = product?.manufacturer?.ifBlank { null } ?: ocr?.manufacturer ?: ""
                val prodMfgDate = product?.manufacturingDate?.ifBlank { null } ?: ocr?.manufacturingDate ?: ""
                val prodExpDate = product?.expiryDate?.ifBlank { null } ?: ocr?.expiryDate ?: ""

                val finalStatus = when {
                    complianceResult != null -> when (complianceResult.overallStatus) {
                        ComplianceStatus.PASS -> ProductStatus.VERIFIED
                        ComplianceStatus.REVIEW -> ProductStatus.WARNING
                        ComplianceStatus.FAIL -> ProductStatus.INVALID
                    }
                    _violations.value.isNotEmpty() && _verificationStatus.value == ProductStatus.VERIFIED -> ProductStatus.WARNING
                    else -> _verificationStatus.value
                }

                val inspection = Inspection(
                    inspectionId = _currentInspectionId.value,
                    inspectorId = inspectorId,
                    inspectorName = inspectorName,
                    productId = product?.id ?: "",
                    barcode = barcode,
                    productName = prodName,
                    brand = prodBrand,
                    category = product?.category ?: "",
                    manufacturer = prodMfg,
                    manufacturingDate = prodMfgDate,
                    expiryDate = prodExpDate,
                    status = finalStatus,
                    violations = _violations.value,
                    notes = _inspectorNotes.value,
                    imageUrls = _capturedImages.value,
                    createdAt = System.currentTimeMillis(),
                    isSynced = true,
                    ocrResult = ocr
                )

                // Save to Inspection Repository via submitInspection
                val result = inspectionRepository.submitInspection(inspection, complianceResult)
                if (result.isSuccess) {
                    val savedInspection = result.getOrThrow()

                    // Also record in Scan Repository for historical compatibility
                    val scanRecord = ScanRecord(
                        id = "scan_" + System.currentTimeMillis(),
                        userId = inspectorId,
                        userName = inspectorName,
                        barcode = barcode,
                        productId = product?.id ?: "",
                        productName = prodName,
                        status = finalStatus,
                        timestamp = System.currentTimeMillis(),
                        message = if (_violations.value.isNotEmpty()) {
                            "Violations recorded: ${_violations.value.size}"
                        } else {
                            finalStatus.defaultMessage
                        }
                    )
                    scanRepository.saveScan(scanRecord)

                    _activeInspectionForReport.value = savedInspection
                    _flowState.value = InspectionFlowState.Completed(savedInspection)
                    onSuccess(savedInspection)
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Failed to submit inspection"
                    _flowState.value = InspectionFlowState.Error(err)
                }
            } catch (e: Exception) {
                _flowState.value = InspectionFlowState.Error("Submission failed: ${e.message}")
            }
        }
    }

    fun selectInspectionForReport(inspection: Inspection) {
        _activeInspectionForReport.value = inspection
    }

    fun reset() {
        _flowState.value = InspectionFlowState.Idle
    }

    companion object {
        fun generateInspectionId(): String {
            val randomNum = (1000..9999).random()
            return "INSP-2026-$randomNum"
        }
    }
}
