package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.ProductStatus
import com.example.model.ScanRecord
import com.example.network.ApiClient
import com.example.network.ScanRecordDto
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

data class ScanStatistics(
    val totalScans: Int = 0,
    val verifiedCount: Int = 0,
    val warningCount: Int = 0,
    val invalidCount: Int = 0,
    val activeUsersCount: Int = 0,
    val scansByUser: Map<String, Int> = emptyMap()
)

class ScanRepository(private val context: Context) {
    private val TAG = "ScanRepository"

    private val _scans = MutableStateFlow<List<ScanRecord>>(emptyList())
    val scans: StateFlow<List<ScanRecord>> = _scans.asStateFlow()

    private val _stats = MutableStateFlow(ScanStatistics())
    val stats: StateFlow<ScanStatistics> = _stats.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    // Initial demo scans to populate history & dashboard on first launch
    private val initialDemoScans = listOf(
        ScanRecord(
            id = "scan_demo_1",
            userId = "demo_uid_scanner1",
            userName = "Arjun Verma",
            barcode = "8901234567890",
            productId = "prod_demo_1",
            productName = "Demo Verified Product",
            status = ProductStatus.VERIFIED,
            timestamp = System.currentTimeMillis() - 1000L * 60 * 25,
            message = "Product found in the registered product database."
        ),
        ScanRecord(
            id = "scan_demo_2",
            userId = "demo_uid_scanner2",
            userName = "Rohan Patel",
            barcode = "8901234567891",
            productId = "prod_demo_2",
            productName = "Demo Warning Product",
            status = ProductStatus.WARNING,
            timestamp = System.currentTimeMillis() - 1000L * 60 * 90,
            message = "Product was found, but requires additional verification."
        ),
        ScanRecord(
            id = "scan_demo_3",
            userId = "demo_uid_scanner1",
            userName = "Arjun Verma",
            barcode = "9998887776665",
            productId = "",
            productName = "Unknown Product",
            status = ProductStatus.INVALID,
            timestamp = System.currentTimeMillis() - 1000L * 60 * 180,
            message = "No matching product was found in the registered database."
        )
    )

    init {
        ApiClient.initialize(context)
        FirebaseManager.initialize(context)
        _scans.value = initialDemoScans
        computeStats(initialDemoScans)
        listenToFirestoreScans()
        CoroutineScope(Dispatchers.IO).launch {
            loadFromBackendApi()
        }
    }

    private suspend fun loadFromBackendApi() {
        try {
            val response = ApiClient.apiService.getScans()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                if (dtos.isNotEmpty()) {
                    val list = dtos.map { it.toDomain() }
                    _scans.value = list
                    computeStats(list)
                    Log.d(TAG, "Loaded ${list.size} scans from Backend API")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend API loadScans failed (offline or unreachable): ${e.message}")
        }
    }

    private fun listenToFirestoreScans() {
        val firestore = FirebaseManager.firestore ?: return
        try {
            listenerRegistration = firestore.collection("scans")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore scans listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.map { doc ->
                            ScanRecord.fromMap(doc.id, doc.data ?: emptyMap())
                        }
                        _scans.value = list
                        computeStats(list)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting up scans snapshot listener: ${e.message}")
        }
    }

    suspend fun saveScan(scan: ScanRecord): Result<ScanRecord> = withContext(Dispatchers.IO) {
        val id = if (scan.id.isEmpty()) "scan_" + System.currentTimeMillis() else scan.id
        val recordToSave = scan.copy(id = id)

        // 1. Submit to Backend API
        try {
            ApiClient.apiService.recordScan(ScanRecordDto.fromDomain(recordToSave))
            Log.d(TAG, "Saved scan to Backend API: $id")
        } catch (e: Exception) {
            Log.w(TAG, "Backend API recordScan failed: ${e.message}")
        }

        // 2. Try Firebase Firestore
        val firestore = FirebaseManager.firestore
        if (firestore != null) {
            try {
                firestore.collection("scans").document(id).set(recordToSave.toMap()).await()
                Log.d(TAG, "Saved scan to Firestore: $id")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save scan to Firestore: ${e.message}")
            }
        }

        // 3. Update local state
        val updated = listOf(recordToSave) + _scans.value.filterNot { it.id == id }
        _scans.value = updated
        computeStats(updated)

        Result.success(recordToSave)
    }

    private fun computeStats(records: List<ScanRecord>) {
        val total = records.size
        var verified = 0
        var warning = 0
        var invalid = 0
        val userScanCounts = mutableMapOf<String, Int>()

        for (rec in records) {
            when (rec.status) {
                ProductStatus.VERIFIED -> verified++
                ProductStatus.WARNING -> warning++
                ProductStatus.INVALID -> invalid++
            }
            val userKey = if (rec.userName.isNotBlank()) rec.userName else "Scanner"
            userScanCounts[userKey] = (userScanCounts[userKey] ?: 0) + 1
        }

        val activeUsers = userScanCounts.keys.size

        _stats.value = ScanStatistics(
            totalScans = total,
            verifiedCount = verified,
            warningCount = warning,
            invalidCount = invalid,
            activeUsersCount = activeUsers,
            scansByUser = userScanCounts
        )
    }
}
