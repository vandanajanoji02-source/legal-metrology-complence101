package com.example.model

data class ScanRecord(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val barcode: String = "",
    val productId: String = "",
    val productName: String = "",
    val status: ProductStatus = ProductStatus.INVALID,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "userName" to userName,
            "barcode" to barcode,
            "productId" to productId,
            "productName" to productName,
            "status" to status.name,
            "timestamp" to timestamp,
            "message" to message
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): ScanRecord {
            return ScanRecord(
                id = id,
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "Scanner",
                barcode = map["barcode"] as? String ?: "",
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "Unknown Product",
                status = ProductStatus.fromString(map["status"] as? String),
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                message = map["message"] as? String ?: ""
            )
        }
    }
}
