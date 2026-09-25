package com.example.compliance

/**
 * Encapsulates the overall result of evaluating a product package against Legal Metrology rules.
 */
data class ComplianceResult(
    val overallStatus: ComplianceStatus,
    val checks: List<ComplianceCheck>,
    val passedCount: Int = checks.count { it.status == ComplianceStatus.PASS },
    val failedCount: Int = checks.count { it.status == ComplianceStatus.FAIL },
    val reviewCount: Int = checks.count { it.status == ComplianceStatus.REVIEW },
    val ruleSource: String = "Legal Metrology (Packaged Commodities) Rules, 2011",
    val ruleVersion: String = "MVP-2026",
    val disclaimer: String =
        "Preliminary automated screening. Final compliance determination " +
        "requires inspector verification and applicable legal provisions."
) {
    // Backward compatibility accessors
    val checksPassed: Int get() = passedCount
    val confirmedViolations: Int get() = failedCount
    val needsReview: Int get() = reviewCount
}
