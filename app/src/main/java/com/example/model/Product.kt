package com.example.model

data class Product(
    val id: String = "",
    val barcode: String = "",
    val name: String = "",
    val brand: String = "",
    val category: String = "",
    val manufacturer: String = "",
    val description: String = "",
    val manufacturingDate: String = "",
    val expiryDate: String = "",
    val status: ProductStatus = ProductStatus.VERIFIED,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "barcode" to barcode,
            "name" to name,
            "brand" to brand,
            "category" to category,
            "manufacturer" to manufacturer,
            "description" to description,
            "manufacturingDate" to manufacturingDate,
            "expiryDate" to expiryDate,
            "status" to status.name,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Product {
            return Product(
                id = id,
                barcode = map["barcode"] as? String ?: "",
                name = map["name"] as? String ?: "",
                brand = map["brand"] as? String ?: "",
                category = map["category"] as? String ?: "",
                manufacturer = map["manufacturer"] as? String ?: "",
                description = map["description"] as? String ?: "",
                manufacturingDate = map["manufacturingDate"] as? String ?: "",
                expiryDate = map["expiryDate"] as? String ?: "",
                status = ProductStatus.fromString(map["status"] as? String),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
