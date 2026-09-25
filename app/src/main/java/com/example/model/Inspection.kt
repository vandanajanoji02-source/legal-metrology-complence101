package com.example.model

import com.example.ocr.ProductOcrResult

data class Inspection(
    val inspectionId: String = "",
    val inspectorId: String = "",
    val inspectorName: String = "",
    val productId: String = "",
    val barcode: String = "",
    val productName: String = "",
    val brand: String = "",
    val category: String = "",
    val manufacturer: String = "",
    val manufacturingDate: String = "",
    val expiryDate: String = "",
    val status: ProductStatus = ProductStatus.VERIFIED,
    val violations: List<Violation> = emptyList(),
    val notes: String = "",
    val imageUrls: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val ocrResult: ProductOcrResult? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "inspectionId" to inspectionId,
            "inspectorId" to inspectorId,
            "inspectorName" to inspectorName,
            "productId" to productId,
            "barcode" to barcode,
            "productName" to productName,
            "brand" to brand,
            "category" to category,
            "manufacturer" to manufacturer,
            "manufacturingDate" to manufacturingDate,
            "expiryDate" to expiryDate,
            "status" to status.name,
            "violations" to violations.map { it.toMap() },
            "notes" to notes,
            "imageUrls" to imageUrls,
            "createdAt" to createdAt,
            "isSynced" to isSynced,
            "ocrResult" to ocrResult?.toMap()
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): Inspection {
            val violationsList = (map["violations"] as? List<*>)
                ?.mapNotNull { it as? Map<String, Any?> }
                ?.map { Violation.fromMap(it) }
                ?: emptyList()

            val ocrData = (map["ocrResult"] as? Map<String, Any?>)?.let {
                ProductOcrResult.fromMap(it)
            }

            return Inspection(
                inspectionId = (map["inspectionId"] as? String)?.ifBlank { id } ?: id,
                inspectorId = map["inspectorId"] as? String ?: "",
                inspectorName = map["inspectorName"] as? String ?: "Inspector",
                productId = map["productId"] as? String ?: "",
                barcode = map["barcode"] as? String ?: "",
                productName = map["productName"] as? String ?: "Product",
                brand = map["brand"] as? String ?: "",
                category = map["category"] as? String ?: "",
                manufacturer = map["manufacturer"] as? String ?: "",
                manufacturingDate = map["manufacturingDate"] as? String ?: "",
                expiryDate = map["expiryDate"] as? String ?: "",
                status = ProductStatus.fromString(map["status"] as? String),
                violations = violationsList,
                notes = map["notes"] as? String ?: "",
                imageUrls = (map["imageUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isSynced = map["isSynced"] as? Boolean ?: true,
                ocrResult = ocrData
            )
        }
    }
}
