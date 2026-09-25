package com.example.model

enum class ProductStatus(
    val title: String,
    val defaultMessage: String
) {
    VERIFIED(
        title = "VERIFIED",
        defaultMessage = "Product found in the registered product database."
    ),
    WARNING(
        title = "WARNING",
        defaultMessage = "Product was found, but requires additional verification."
    ),
    INVALID(
        title = "INVALID / NOT FOUND",
        defaultMessage = "No matching product was found in the registered database."
    );

    companion object {
        fun fromString(value: String?): ProductStatus {
            return when (value?.uppercase()?.trim()) {
                "VERIFIED" -> VERIFIED
                "WARNING" -> WARNING
                else -> INVALID
            }
        }
    }
}
