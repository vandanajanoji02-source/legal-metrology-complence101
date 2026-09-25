package com.example.compliance

import com.example.ocr.ProductOcrResult
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ComplianceRuleEngine:
 * 1. Fully valid product → PASS
 * 2. Missing required field → FAIL
 * 3. Uncertain OCR → REVIEW
 * 4. Imported product without country of origin → FAIL
 * 5. Mixed PASS + REVIEW → REVIEW
 * 6. Any FAIL → FAIL
 */
class ComplianceRuleEngineTest {

    // -------------------------------------------------------------------------
    // 1. Fully valid product -> PASS
    // -------------------------------------------------------------------------
    @Test
    fun `test 1 fully valid product returns PASS overall status`() {
        val ocrResult = ProductOcrResult(
            productName = "Good Day Butter Cookies",
            brand = "Britannia",
            mrp = "₹50",
            netQuantity = "100 g",
            manufacturer = "Britannia Industries Ltd",
            manufacturerAddress = "Plot 14, Whitefield Road, Bengaluru - 560066",
            licenseNumber = "10015042000123",
            manufacturingDate = "01/2026",
            expiryDate = "07/2026",
            rawText = "CONSUMER CARE: 1800-103-1413",
            confidenceMap = mapOf(
                "MRP" to "Detected",
                "Net Quantity" to "Detected",
                "Manufacturer" to "Detected",
                "Product Name" to "Detected",
                "Consumer Care" to "Detected",
                "Manufacturing Date" to "Detected",
                "Expiry Date" to "Detected"
            )
        )

        val result = ComplianceRuleEngine.evaluate(ocrResult, isImported = false)

        assertEquals(ComplianceStatus.PASS, result.overallStatus)
        assertEquals(0, result.failedCount)
        assertTrue(result.passedCount >= 5)

        // All mandatory rules must be PASS
        val mrp = result.checks.first { it.ruleCode == "LM-PC-003" }
        assertEquals(ComplianceStatus.PASS, mrp.status)
        assertEquals("₹50", mrp.detectedValue)

        val netQty = result.checks.first { it.ruleCode == "LM-PC-002" }
        assertEquals(ComplianceStatus.PASS, netQty.status)

        val mfg = result.checks.first { it.ruleCode == "LM-PC-001" }
        assertEquals(ComplianceStatus.PASS, mfg.status)

        val name = result.checks.first { it.ruleCode == "LM-PC-005" }
        assertEquals(ComplianceStatus.PASS, name.status)
    }

    // -------------------------------------------------------------------------
    // 2. Missing required field -> FAIL
    // -------------------------------------------------------------------------
    @Test
    fun `test 2 missing required field returns FAIL`() {
        val ocrResult = ProductOcrResult(
            productName = "Coconut Crunchy Cookies",
            mrp = "₹10",
            netQuantity = "68 g",
            manufacturer = null, // Missing mandatory manufacturer
            rawText = "",
            confidenceMap = mapOf(
                "MRP" to "Detected",
                "Net Quantity" to "Detected",
                "Product Name" to "Detected",
                "Manufacturer" to "Not detected"
            )
        )

        val result = ComplianceRuleEngine.evaluate(ocrResult, isImported = false)

        assertEquals(ComplianceStatus.FAIL, result.overallStatus)
        assertTrue(result.failedCount >= 1)

        val mfgCheck = result.checks.first { it.ruleCode == "LM-PC-001" }
        assertEquals(ComplianceStatus.FAIL, mfgCheck.status)
        assertNull(mfgCheck.detectedValue)
        assertTrue(mfgCheck.reason.isNotBlank())
    }

    @Test
    fun `test 2 missing MRP returns FAIL`() {
        val ocrResult = ProductOcrResult(
            productName = "Some Product",
            mrp = null, // Missing MRP
            netQuantity = "100 g",
            manufacturer = "ABC Foods Ltd",
            rawText = "",
            confidenceMap = mapOf(
                "MRP" to "Not detected",
                "Net Quantity" to "Detected",
                "Manufacturer" to "Detected",
                "Product Name" to "Detected"
            )
        )

        val result = ComplianceRuleEngine.evaluate(ocrResult, isImported = false)

        assertEquals(ComplianceStatus.FAIL, result.overallStatus)
        val mrpCheck = result.checks.first { it.ruleCode == "LM-PC-003" }
        assertEquals(ComplianceStatus.FAIL, mrpCheck.status)
    }

    // -------------------------------------------------------------------------
    // 3. Uncertain OCR -> REVIEW
    // -------------------------------------------------------------------------
    @Test
    fun `test 3 uncertain OCR returns REVIEW`() {
        val ocrResult = ProductOcrResult(
            productName = "Partial Name",
            mrp = "₹10",
            netQuantity = "68 g",
            manufacturer = "Partial Mfg",
            rawText = "",
            confidenceMap = mapOf(
                "MRP" to "Detected",
                "Net Quantity" to "Detected",
                "Manufacturer" to "Needs review", // Uncertain OCR
                "Product Name" to "Needs review", // Uncertain OCR
                "Consumer Care" to "Not detected"
            )
        )

        val result = ComplianceRuleEngine.evaluate(ocrResult, isImported = false)

        assertEquals(ComplianceStatus.REVIEW, result.overallStatus)
        assertEquals(0, result.failedCount)
        assertTrue(result.reviewCount >= 1)

        val mfgCheck = result.checks.first { it.ruleCode == "LM-PC-001" }
        assertEquals(ComplianceStatus.REVIEW, mfgCheck.status)
    }

    // -------------------------------------------------------------------------
    // 4. Imported product without country of origin -> FAIL
    // -------------------------------------------------------------------------
    @Test
    fun `test 4 imported product without country of origin returns FAIL`() {
        val ocrResult = ProductOcrResult(
            productName = "Imported Biscuits",
            mrp = "₹150",
            netQuantity = "200 g",
            manufacturer = "Foreign Foods Co",
            rawText = "Some label text without origin declaration",
            confidenceMap = mapOf(
                "MRP" to "Detected",
                "Net Quantity" to "Detected",
                "Manufacturer" to "Detected",
                "Product Name" to "Detected",
                "Consumer Care" to "Detected"
            )
        )

        val result = ComplianceRuleEngine.evaluate(ocrResult, isImported = true)

        val cooCheck = result.checks.firstOrNull { it.ruleCode == "LM-PC-006" }
        assertNotNull("Country of origin check must exist for imported products", cooCheck)
        assertEquals(ComplianceStatus.FAIL, cooCheck!!.status)
        assertEquals(ComplianceStatus.FAIL, result.overallStatus)
    }

    @Test
    fun `test 4 imported product with country of origin passes`() {
        val ocrResult = ProductOcrResult(
            productName = "Imported Chocolates",
            mrp = "₹200",
            netQuantity = "100 g",
            manufacturer = "Swiss Chocolates AG",
            rawText = "Made in Switzerland. Imported by ABC Trading.",
            confidenceMap = mapOf(
                "MRP" to "Detected",
                "Net Quantity" to "Detected",
                "Manufacturer" to "Detected",
                "Product Name" to "Detected",
                "Consumer Care" to "Detected"
            )
        )

        val result = ComplianceRuleEngine.evaluate(ocrResult, isImported = true)

        val cooCheck = result.checks.firstOrNull { it.ruleCode == "LM-PC-006" }
        assertNotNull(cooCheck)
        assertEquals(ComplianceStatus.PASS, cooCheck!!.status)
    }

    // -------------------------------------------------------------------------
    // 5. Mixed PASS + REVIEW -> REVIEW
    // -------------------------------------------------------------------------
    @Test
    fun `test 5 mixed PASS plus REVIEW returns REVIEW overall status`() {
        val ocrResult = ProductOcrResult(
            productName = "Indian Snacks",
            mrp = "₹10",
            netQuantity = "68 g",
            manufacturer = "Local Snacks Pvt Ltd",
            manufacturingDate = null, // Missing date field -> REVIEW
            expiryDate = null,        // Missing date field -> REVIEW
            rawText = "Some text",
            confidenceMap = mapOf(
                "MRP" to "Detected",
                "Net Quantity" to "Detected",
                "Manufacturer" to "Detected",
                "Product Name" to "Detected",
                "Consumer Care" to "Detected",
                "Manufacturing Date" to "Not detected",
                "Expiry Date" to "Not detected"
            )
        )

        val result = ComplianceRuleEngine.evaluate(ocrResult, isImported = false)

        assertEquals(ComplianceStatus.REVIEW, result.overallStatus)
        assertEquals(0, result.failedCount)
        assertTrue(result.passedCount > 0)
        assertTrue(result.reviewCount > 0)
    }

    // -------------------------------------------------------------------------
    // 6. Any FAIL -> FAIL
    // -------------------------------------------------------------------------
    @Test
    fun `test 6 any FAIL results in FAIL overall status even with PASS and REVIEW`() {
        val ocrResult = ProductOcrResult(
            productName = "Test Product",
            mrp = null, // FAIL: Missing MRP
            netQuantity = "100 g", // PASS
            manufacturer = "Test Mfg", // REVIEW
            rawText = "",
            confidenceMap = mapOf(
                "MRP" to "Not detected",
                "Net Quantity" to "Detected",
                "Manufacturer" to "Needs review",
                "Product Name" to "Detected"
            )
        )

        val result = ComplianceRuleEngine.evaluate(ocrResult, isImported = false)

        assertEquals(ComplianceStatus.FAIL, result.overallStatus)
        assertTrue(result.failedCount >= 1)
    }

    @Test
    fun `test domestic product without country of origin does NOT fail`() {
        val ocrResult = ProductOcrResult(
            productName = "Indian Snacks",
            mrp = "₹10",
            netQuantity = "68 g",
            manufacturer = "Local Snacks Pvt Ltd",
            manufacturingDate = "01/2026",
            expiryDate = "01/2027",
            rawText = "Some label text without any country mention",
            confidenceMap = mapOf(
                "MRP" to "Detected",
                "Net Quantity" to "Detected",
                "Manufacturer" to "Detected",
                "Product Name" to "Detected",
                "Consumer Care" to "Detected",
                "Manufacturing Date" to "Detected",
                "Expiry Date" to "Detected"
            )
        )

        val result = ComplianceRuleEngine.evaluate(ocrResult, isImported = false)

        val cooCheck = result.checks.firstOrNull { it.ruleCode == "LM-PC-006" }
        assertNull("Country of origin rule must NOT be evaluated for domestic products", cooCheck)
        assertEquals(0, result.failedCount)
        assertEquals(ComplianceStatus.PASS, result.overallStatus)
    }

    @Test
    fun `test result contains required metadata fields`() {
        val result = ComplianceRuleEngine.evaluate(ProductOcrResult())
        assertTrue(result.ruleSource.contains("Legal Metrology"))
        assertTrue(result.ruleVersion.isNotBlank())
        assertTrue(result.disclaimer.contains("Preliminary"))
        assertTrue(result.checks.isNotEmpty())
    }
}
