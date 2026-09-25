package com.example.ocr

/**
 * Data model holding on-device OCR results and extracted product label fields.
 * Unextracted fields default to null (displayed as "Not detected" in the UI).
 */
data class ProductOcrResult(
    val productName: String? = null,
    val brand: String? = null,
    val mrp: String? = null,
    val manufacturingDate: String? = null,
    val expiryDate: String? = null,
    val batchNumber: String? = null,
    val netQuantity: String? = null,
    val manufacturer: String? = null,
    val manufacturerAddress: String? = null,
    val licenseNumber: String? = null,
    val rawText: String = "",
    val processedPhotoCount: Int = 0,
    val totalPhotoCount: Int = 0,
    val confidenceMap: Map<String, String> = emptyMap()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "productName" to productName,
            "brand" to brand,
            "mrp" to mrp,
            "manufacturingDate" to manufacturingDate,
            "expiryDate" to expiryDate,
            "batchNumber" to batchNumber,
            "netQuantity" to netQuantity,
            "manufacturer" to manufacturer,
            "manufacturerAddress" to manufacturerAddress,
            "licenseNumber" to licenseNumber,
            "rawText" to rawText,
            "processedPhotoCount" to processedPhotoCount,
            "totalPhotoCount" to totalPhotoCount,
            "confidenceMap" to confidenceMap
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ProductOcrResult {
            return ProductOcrResult(
                productName = map["productName"] as? String,
                brand = map["brand"] as? String,
                mrp = map["mrp"] as? String,
                manufacturingDate = map["manufacturingDate"] as? String,
                expiryDate = map["expiryDate"] as? String,
                batchNumber = map["batchNumber"] as? String,
                netQuantity = map["netQuantity"] as? String,
                manufacturer = map["manufacturer"] as? String,
                manufacturerAddress = map["manufacturerAddress"] as? String,
                licenseNumber = map["licenseNumber"] as? String,
                rawText = map["rawText"] as? String ?: "",
                processedPhotoCount = (map["processedPhotoCount"] as? Number)?.toInt() ?: 0,
                totalPhotoCount = (map["totalPhotoCount"] as? Number)?.toInt() ?: 0,
                confidenceMap = (map["confidenceMap"] as? Map<String, String>) ?: emptyMap()
            )
        }
    }
}
