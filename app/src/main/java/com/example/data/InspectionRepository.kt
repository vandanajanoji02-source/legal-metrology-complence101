package com.example.data

import android.content.Context
import android.util.Log
import com.example.compliance.ComplianceResult
import com.example.model.Inspection
import com.example.model.ProductStatus
import com.example.model.Violation
import com.example.model.ViolationSeverity
import com.example.network.ApiClient
import com.example.network.InspectionDto
import com.example.network.NetworkConfig
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

data class InspectionStats(
    val totalInspections: Int = 0,
    val todayInspections: Int = 0,
    val verifiedCount: Int = 0,
    val warningCount: Int = 0,
    val violationCount: Int = 0
)

class InspectionRepository(private val context: Context) {
    private val TAG = "InspectionRepository"

    private val _inspections = MutableStateFlow<List<Inspection>>(emptyList())
    val inspections: StateFlow<List<Inspection>> = _inspections.asStateFlow()

    private val _stats = MutableStateFlow(InspectionStats())
    val stats: StateFlow<InspectionStats> = _stats.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    // Realistic seed demo inspections for SIH presentation
    private val initialDemoInspections = listOf(
        Inspection(
            inspectionId = "INSP-2026-8812",
            inspectorId = "INSP-IND-8821",
            inspectorName = "Arjun Verma",
            productId = "prod_demo_1",
            barcode = "8901234567890",
            productName = "Demo Verified Product",
            brand = "SIH Demo",
            category = "Electronics",
            manufacturer = "SIH Certified Tech Labs",
            manufacturingDate = "2026-01-15",
            expiryDate = "2028-01-15",
            status = ProductStatus.VERIFIED,
            violations = emptyList(),
            notes = "Standard packaging inspected. Holographic security seal intact and batch code verified against national database.",
            imageUrls = emptyList(),
            createdAt = System.currentTimeMillis() - 1000L * 60 * 45
        ),
        Inspection(
            inspectionId = "INSP-2026-8790",
            inspectorId = "INSP-IND-8821",
            inspectorName = "Arjun Verma",
            productId = "prod_demo_2",
            barcode = "8901234567891",
            productName = "Demo Warning Product",
            brand = "SIH Demo",
            category = "Pharmaceuticals",
            manufacturer = "MediCorp Healthcare Ltd",
            manufacturingDate = "2025-03-10",
            expiryDate = "2026-10-01",
            status = ProductStatus.WARNING,
            violations = listOf(
                Violation(
                    id = "viol_101",
                    type = "Incorrect Labeling",
                    severity = ViolationSeverity.MEDIUM,
                    description = "Storage temperature guidance label smudged; near expiry alert flagged for shelf monitoring.",
                    evidenceImages = emptyList()
                )
            ),
            notes = "Retailer advised to pull affected batch from main display shelf for secondary audit.",
            imageUrls = emptyList(),
            createdAt = System.currentTimeMillis() - 1000L * 60 * 120
        ),
        Inspection(
            inspectionId = "INSP-2026-8755",
            inspectorId = "INSP-IND-8821",
            inspectorName = "Arjun Verma",
            productId = "prod_demo_3",
            barcode = "8901234567892",
            productName = "Demo Invalid Product",
            brand = "SIH Demo",
            category = "Consumer Goods",
            manufacturer = "Unverified Third Party",
            manufacturingDate = "2024-05-20",
            expiryDate = "2025-05-20",
            status = ProductStatus.INVALID,
            violations = listOf(
                Violation(
                    id = "viol_102",
                    type = "Suspected Counterfeit",
                    severity = ViolationSeverity.HIGH,
                    description = "Barcode matches revoked certification. Missing mandatory manufacturer registration ID.",
                    evidenceImages = emptyList()
                ),
                Violation(
                    id = "viol_103",
                    type = "Expired Product",
                    severity = ViolationSeverity.HIGH,
                    description = "Product exceeded shelf life by 16 months.",
                    evidenceImages = emptyList()
                )
            ),
            notes = "Stock confiscated under Section 14 (Counterfeiting & Public Safety Act). Report submitted to Central Vigilance.",
            imageUrls = emptyList(),
            createdAt = System.currentTimeMillis() - 1000L * 60 * 240
        )
    )

    init {
        ApiClient.initialize(context)
        FirebaseManager.initialize(context)
        _inspections.value = initialDemoInspections
        computeStats(initialDemoInspections)
        listenToFirestoreInspections()
        CoroutineScope(Dispatchers.IO).launch {
            loadFromBackendApi()
        }
    }

    private suspend fun loadFromBackendApi() {
        try {
            val response = ApiClient.apiService.getInspections()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                if (dtos.isNotEmpty()) {
                    val list = dtos.map { it.toDomain() }
                    _inspections.value = list
                    computeStats(list)
                    Log.d(TAG, "Loaded ${list.size} inspections from Backend API")
                }
            }

            // Also check dashboard stats
            val statsResponse = ApiClient.apiService.getDashboardStats()
            if (statsResponse.isSuccessful && statsResponse.body() != null) {
                val statDto = statsResponse.body()!!
                _stats.value = InspectionStats(
                    totalInspections = statDto.resolvedTotal,
                    todayInspections = statDto.todayInspections ?: _stats.value.todayInspections,
                    verifiedCount = statDto.resolvedVerified,
                    warningCount = statDto.resolvedWarning,
                    violationCount = statDto.resolvedViolations
                )
                Log.d(TAG, "Loaded dashboard stats from Backend API: ${_stats.value}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend API load inspections failed (server offline or unreachable): ${e.message}")
        }
    }

    private fun listenToFirestoreInspections() {
        val firestore = FirebaseManager.firestore ?: return
        try {
            listenerRegistration = firestore.collection("inspections")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore inspections listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.map { doc ->
                            Inspection.fromMap(doc.id, doc.data ?: emptyMap())
                        }
                        _inspections.value = list
                        computeStats(list)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting up inspections snapshot listener: ${e.message}")
        }
    }

    suspend fun submitInspection(
        inspection: Inspection,
        complianceResult: ComplianceResult? = null
    ): Result<Inspection> = withContext(Dispatchers.IO) {
        val id = if (inspection.inspectionId.isEmpty()) {
            "INSP-2026-" + (1000 + (System.currentTimeMillis() % 9000))
        } else {
            inspection.inspectionId
        }
        var recordToSave = inspection.copy(inspectionId = id)
        val baseUrl = NetworkConfig.getBaseUrl(context)
        val submitUrl = if (baseUrl.endsWith("/")) "${baseUrl}inspections" else "$baseUrl/inspections"

        Log.i(TAG, "SUBMIT_START: Submitting inspection $id")
        Log.i(TAG, "SUBMIT_URL: $submitUrl")

        val dto = InspectionDto.fromDomain(recordToSave, complianceResult)

        // 1. Submit to Backend API
        try {
            val response = ApiClient.apiService.submitInspection(dto)
            val code = response.code()
            if (response.isSuccessful) {
                val saveResp = response.body()
                recordToSave = recordToSave.copy(isSynced = true)
                Log.i(TAG, "SUBMIT_SUCCESS: HTTP $code - ${saveResp?.message ?: "Inspection saved successfully"} (id: ${saveResp?.inspectionId ?: id})")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Empty error response"
                Log.e(TAG, "SUBMIT_HTTP_ERROR: HTTP $code - $errorBody")
                return@withContext Result.failure(Exception("HTTP $code: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "SUBMIT_NETWORK_ERROR: Network failure contacting $submitUrl - ${e.message}", e)
            return@withContext Result.failure(Exception("Failed to connect to $submitUrl: ${e.message}", e))
        }

        // 2. Save to Firebase Firestore if available (with timeout so it never blocks)
        val firestore = FirebaseManager.firestore
        if (firestore != null) {
            try {
                withTimeoutOrNull(2500L) {
                    firestore.collection("inspections").document(id).set(recordToSave.toMap()).await()
                    Log.d(TAG, "Saved inspection to Firestore: $id")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore write warning (bypassed): ${e.message}")
            }
        }

        // 3. Update local state
        val updated = listOf(recordToSave) + _inspections.value.filterNot { it.inspectionId == id }
        _inspections.value = updated
        computeStats(updated)

        Result.success(recordToSave)
    }

    suspend fun saveInspection(inspection: Inspection): Result<Inspection> {
        return submitInspection(inspection, null)
    }

    fun getInspectionById(id: String): Inspection? {
        return _inspections.value.find { it.inspectionId == id }
    }

    private fun computeStats(records: List<Inspection>) {
        val total = records.size
        var verified = 0
        var warning = 0
        var violation = 0
        var todayCount = 0

        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        for (rec in records) {
            when (rec.status) {
                ProductStatus.VERIFIED -> {
                    if (rec.violations.isEmpty()) verified++ else violation++
                }
                ProductStatus.WARNING -> warning++
                ProductStatus.INVALID -> violation++
            }

            val itemCal = Calendar.getInstance().apply { timeInMillis = rec.createdAt }
            if (itemCal.get(Calendar.YEAR) == currentYear && itemCal.get(Calendar.DAY_OF_YEAR) == currentDay) {
                todayCount++
            }
        }

        _stats.value = InspectionStats(
            totalInspections = total,
            todayInspections = todayCount,
            verifiedCount = verified,
            warningCount = warning,
            violationCount = violation
        )
    }
}
