package com.example.compliance

import com.example.ocr.ProductOcrResult

/**
 * Legal Metrology Compliance Rule Engine
 *
 * Evaluates extracted on-device OCR fields ([ProductOcrResult]) against the Legal
 * Metrology (Packaged Commodities) Rules, 2011 standard declaration requirements.
 *
 * MVP Rule Set:
 * ──────────────
 *   LM-PC-001  Manufacturer / Packer / Importer Name & Address
 *   LM-PC-002  Net Quantity declaration with standard units
 *   LM-PC-003  Maximum Retail Price (MRP) inclusive of all taxes
 *   LM-PC-004  Consumer Care / Helpline contact details
 *   LM-PC-005  Product Name / Generic or common denomination
 *   LM-PC-006  Country of Origin (Mandatory for imported packaged commodities)
 *   LM-PC-DATE-MFG  Manufacturing / Packaging Date
 *   LM-PC-DATE-EXP  Expiry / Use By / Best Before Date
 *
 * Evaluation Rules:
 * ──────────────────
 *   - PASS   : Declaration is confidently detected and requirement is satisfied.
 *   - FAIL   : Mandatory declaration is confirmed missing or violated.
 *   - REVIEW : OCR extraction is uncertain, ambiguous, or requires physical inspector verification.
 *
 * Date fields and low-confidence readings are NEVER auto-failed to avoid false legal infractions.
 */
object ComplianceRuleEngine {

    private const val STATUS_DETECTED = "Detected"
    private const val STATUS_NEEDS_REVIEW = "Needs review"

    private val COUNTRY_ORIGIN_PATTERNS = listOf(
        Regex("""(?i)\b(?:MADE IN|PRODUCT OF|IMPORTED FROM)\s*[:\-]?\s*([A-Za-z]{3,25})"""),
        Regex("""(?i)\bCOUNTRY OF ORIGIN\s*[:\-]\s*([A-Za-z]{3,25})"""),
        Regex("""(?i)\bCOUNTRY OF ORIGIN\b(?!\s+(?:DECLARATION|RULES|REQUIREMENT|IS|WAS|NOT))""")
    )

    /**
     * Evaluate the given [ocrResult] and generate a comprehensive [ComplianceResult].
     *
     * @param ocrResult Extracted label fields from on-device ML Kit OCR.
     * @param isImported Whether the product is classified/verified as an imported commodity.
     */
    fun evaluate(
        ocrResult: ProductOcrResult,
        isImported: Boolean = false
    ): ComplianceResult {
        val checks = mutableListOf<ComplianceCheck>()

        // 1. LM-PC-001 — Manufacturer / Packer / Importer Details
        checks += evaluateRequiredField(
            ruleCode = "LM-PC-001",
            fieldName = "Manufacturer",
            value = ocrResult.manufacturer,
            ocrStatus = ocrResult.confidenceMap["Manufacturer"],
            missingReason = "Manufacturer/packer declaration not detected on label",
            reviewReason = "Manufacturer details detected with partial confidence — requires inspector review"
        )

        // 2. LM-PC-002 — Net Quantity
        checks += evaluateRequiredField(
            ruleCode = "LM-PC-002",
            fieldName = "Net Quantity",
            value = ocrResult.netQuantity,
            ocrStatus = ocrResult.confidenceMap["Net Quantity"],
            missingReason = "Net quantity declaration not detected on label",
            reviewReason = "Net quantity detected but requires inspector verification"
        )

        // 3. LM-PC-003 — Maximum Retail Price (MRP)
        checks += evaluateRequiredField(
            ruleCode = "LM-PC-003",
            fieldName = "MRP",
            value = ocrResult.mrp,
            ocrStatus = ocrResult.confidenceMap["MRP"],
            missingReason = "Maximum Retail Price (MRP) not detected on label",
            reviewReason = "MRP detected with low confidence — requires inspector verification"
        )

        // 4. LM-PC-004 — Consumer Care Details
        // Consumer care details on packages are often in very fine print; if absent via OCR, mark as REVIEW
        checks += evaluateConsumerCareField(
            ruleCode = "LM-PC-004",
            fieldName = "Consumer Care",
            ocrStatus = ocrResult.confidenceMap["Consumer Care"],
            rawText = ocrResult.rawText
        )

        // 5. LM-PC-005 — Product / Generic Name
        checks += evaluateRequiredField(
            ruleCode = "LM-PC-005",
            fieldName = "Product Name",
            value = ocrResult.productName,
            ocrStatus = ocrResult.confidenceMap["Product Name"],
            missingReason = "Product/generic name not detected on label",
            reviewReason = "Product name detected with partial confidence — verify against physical packaging"
        )

        // 6. LM-PC-006 — Country of Origin (Only applicable for imported goods)
        if (isImported) {
            checks += evaluateCountryOfOrigin(ocrResult.rawText)
        }

        // 7. Date Fields (Always REVIEW when not detected, never auto-fail)
        checks += evaluateDateField(
            ruleCode = "LM-PC-DATE-MFG",
            fieldName = "Manufacturing Date",
            value = ocrResult.manufacturingDate,
            ocrStatus = ocrResult.confidenceMap["Manufacturing Date"]
        )

        checks += evaluateDateField(
            ruleCode = "LM-PC-DATE-EXP",
            fieldName = "Expiry / Best Before",
            value = ocrResult.expiryDate,
            ocrStatus = ocrResult.confidenceMap["Expiry Date"]
        )

        val passedCount = checks.count { it.status == CheckStatus.PASS }
        val failedCount = checks.count { it.status == CheckStatus.FAIL }
        val reviewCount = checks.count { it.status == CheckStatus.REVIEW }

        val overallStatus = when {
            failedCount > 0 -> OverallStatus.FAIL
            reviewCount > 0 -> OverallStatus.REVIEW
            else -> OverallStatus.PASS
        }

        return ComplianceResult(
            overallStatus = overallStatus,
            checks = checks,
            passedCount = passedCount,
            failedCount = failedCount,
            reviewCount = reviewCount
        )
    }

    // -------------------------------------------------------------------------
    // Helper Evaluation Functions
    // -------------------------------------------------------------------------

    private fun evaluateRequiredField(
        ruleCode: String,
        fieldName: String,
        value: String?,
        ocrStatus: String?,
        missingReason: String,
        reviewReason: String
    ): ComplianceCheck {
        if (value.isNullOrBlank()) {
            return ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.FAIL,
                detectedValue = null,
                reason = missingReason,
                confidence = 0.85
            )
        }

        // Safety Guard 1: Reject nutritional terminology as valid manufacturer
        if (fieldName == "Manufacturer") {
            val up = value.uppercase()
            val hasNutrition = up.contains("NUTRITION") || up.contains("MUTRITION") ||
                    up.contains("APORN") || up.contains("PROTEIN") || up.contains("CARBOHYDRATE") ||
                    up.contains("FAT") || up.contains("SUGAR") || up.contains("ENERGY") ||
                    up.contains("SERVING")
            if (hasNutrition) {
                return ComplianceCheck(
                    ruleCode = ruleCode,
                    fieldName = fieldName,
                    status = CheckStatus.REVIEW,
                    detectedValue = value,
                    reason = "Manufacturer candidate contains nutritional or unverified text — physical inspection required",
                    confidence = 0.40
                )
            }
        }

        // Safety Guard 2: Reject ambiguous MRP values (e.g. single digit ₹1 or invalid formats)
        if (fieldName == "MRP") {
            val isAmbiguous = value == "₹1" || value == "1" || value.equals("Not", ignoreCase = true) ||
                    !value.startsWith("₹") || value.none { it.isDigit() }
            if (isAmbiguous) {
                return ComplianceCheck(
                    ruleCode = ruleCode,
                    fieldName = fieldName,
                    status = CheckStatus.REVIEW,
                    detectedValue = value,
                    reason = "MRP value is ambiguous or incomplete — physical verification required",
                    confidence = 0.40
                )
            }
        }

        // Safety Guard 3: Reject "Not" or "Not detected" as valid values
        if (value.equals("Not", ignoreCase = true) || value.equals("Not detected", ignoreCase = true) || value.equals("USE", ignoreCase = true)) {
            return ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.REVIEW,
                detectedValue = null,
                reason = reviewReason,
                confidence = 0.40
            )
        }

        return when (ocrStatus) {
            STATUS_DETECTED -> ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.PASS,
                detectedValue = value,
                reason = "$fieldName declaration detected and legible",
                confidence = 0.90
            )
            STATUS_NEEDS_REVIEW -> ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.REVIEW,
                detectedValue = value,
                reason = reviewReason,
                confidence = 0.60
            )
            else -> ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.REVIEW,
                detectedValue = value,
                reason = reviewReason,
                confidence = 0.50
            )
        }
    }

    private fun evaluateConsumerCareField(
        ruleCode: String,
        fieldName: String,
        ocrStatus: String?,
        rawText: String
    ): ComplianceCheck {
        val hasConsumerCareInRaw = rawText.contains("CONSUMER CARE", ignoreCase = true) ||
                rawText.contains("CUSTOMER CARE", ignoreCase = true) ||
                rawText.contains("HELPLINE", ignoreCase = true) ||
                rawText.contains("TOLL FREE", ignoreCase = true)

        return when {
            ocrStatus == STATUS_DETECTED || hasConsumerCareInRaw -> ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.PASS,
                detectedValue = "Present",
                reason = "Consumer care contact details detected on label",
                confidence = 0.85
            )
            ocrStatus == STATUS_NEEDS_REVIEW -> ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.REVIEW,
                detectedValue = "Partially detected",
                reason = "Consumer care details present but require visual confirmation",
                confidence = 0.60
            )
            else -> ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.REVIEW,
                detectedValue = null,
                reason = "Consumer care contact not detected in OCR — verify on physical packaging",
                confidence = 0.50
            )
        }
    }

    private fun evaluateCountryOfOrigin(rawText: String): ComplianceCheck {
        var matchFound = false
        var countryName: String? = null

        for (pattern in COUNTRY_ORIGIN_PATTERNS) {
            val match = pattern.find(rawText)
            if (match != null) {
                matchFound = true
                if (match.groupValues.size > 1 && match.groupValues[1].isNotBlank()) {
                    countryName = match.groupValues[1].trim()
                }
                break
            }
        }

        return if (matchFound) {
            ComplianceCheck(
                ruleCode = "LM-PC-006",
                fieldName = "Country of Origin",
                status = CheckStatus.PASS,
                detectedValue = countryName ?: "Present",
                reason = "Country of origin declaration detected for imported commodity",
                confidence = 0.85
            )
        } else {
            ComplianceCheck(
                ruleCode = "LM-PC-006",
                fieldName = "Country of Origin",
                status = CheckStatus.FAIL,
                detectedValue = null,
                reason = "Country of origin is mandatory for imported goods under Rule 6(1)(e) — not detected",
                confidence = 0.90
            )
        }
    }

    private fun evaluateDateField(
        ruleCode: String,
        fieldName: String,
        value: String?,
        ocrStatus: String?
    ): ComplianceCheck {
        return when {
            ocrStatus == STATUS_DETECTED && !value.isNullOrBlank() -> ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.PASS,
                detectedValue = value,
                reason = "$fieldName detected — verify against physical batch",
                confidence = 0.85
            )
            ocrStatus == STATUS_NEEDS_REVIEW && !value.isNullOrBlank() -> ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.REVIEW,
                detectedValue = value,
                reason = "$fieldName partially detected — inspector must verify",
                confidence = 0.55
            )
            else -> ComplianceCheck(
                ruleCode = ruleCode,
                fieldName = fieldName,
                status = CheckStatus.REVIEW,
                detectedValue = null,
                reason = "$fieldName not legible via OCR — inspector must check label directly",
                confidence = 0.40
            )
        }
    }
}
