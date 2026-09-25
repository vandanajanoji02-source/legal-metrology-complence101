package com.example.model

enum class ViolationSeverity(val title: String) {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH");

    companion object {
        fun fromString(value: String?): ViolationSeverity {
            return when (value?.uppercase()?.trim()) {
                "HIGH" -> HIGH
                "MEDIUM" -> MEDIUM
                else -> LOW
            }
        }
    }
}

data class Violation(
    val id: String = "",
    val type: String = "",
    val severity: ViolationSeverity = ViolationSeverity.MEDIUM,
    val description: String = "",
    val evidenceImages: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "type" to type,
            "severity" to severity.name,
            "description" to description,
            "evidenceImages" to evidenceImages
        )
    }

    companion object {
        val PREDEFINED_TYPES = listOf(
            "Missing MRP",
            "Incorrect Labeling",
            "Expired Product",
            "Missing Manufacturer Details",
            "Incorrect Quantity",
            "Barcode Mismatch",
            "Suspected Counterfeit",
            "Tampered Seal / Packaging",
            "Unapproved Batch",
            "Other"
        )

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Violation {
            return Violation(
                id = map["id"] as? String ?: "",
                type = map["type"] as? String ?: "Other",
                severity = ViolationSeverity.fromString(map["severity"] as? String),
                description = map["description"] as? String ?: "",
                evidenceImages = (map["evidenceImages"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }
    }
}
