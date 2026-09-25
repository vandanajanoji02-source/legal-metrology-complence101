package com.example.compliance

/**
 * Compliance evaluation status for individual checks and overall determination.
 * - PASS: Declaration confidently detected and requirement satisfied.
 * - FAIL: Requirement is confidently missing or violated.
 * - REVIEW: OCR/extraction is uncertain, ambiguous, or applicability cannot be determined.
 */
enum class ComplianceStatus {
    PASS,
    FAIL,
    REVIEW
}

typealias CheckStatus = ComplianceStatus
typealias OverallStatus = ComplianceStatus

/**
 * Represents the evaluation outcome for a single Legal Metrology declaration rule.
 */
data class ComplianceCheck(
    val ruleCode: String,
    val fieldName: String,
    val status: ComplianceStatus,
    val detectedValue: String?,
    val confidence: Double,
    val reason: String
)
