package com.example

import com.example.model.AppUser
import com.example.model.Inspection
import com.example.model.Product
import com.example.model.ProductStatus
import com.example.model.ScanRecord
import com.example.model.UserRole
import com.example.model.Violation
import com.example.model.ViolationSeverity
import com.example.ui.viewmodel.InspectionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartVerifyUnitTest {

    @Test
    fun `product status parsing works as expected`() {
        assertEquals(ProductStatus.VERIFIED, ProductStatus.fromString("VERIFIED"))
        assertEquals(ProductStatus.VERIFIED, ProductStatus.fromString("verified"))
        assertEquals(ProductStatus.WARNING, ProductStatus.fromString("WARNING"))
        assertEquals(ProductStatus.WARNING, ProductStatus.fromString("warning"))
        assertEquals(ProductStatus.INVALID, ProductStatus.fromString("INVALID"))
        assertEquals(ProductStatus.INVALID, ProductStatus.fromString("unknown_status"))
        assertEquals(ProductStatus.INVALID, ProductStatus.fromString(null))
    }

    @Test
    fun `violation severity parsing works correctly`() {
        assertEquals(ViolationSeverity.HIGH, ViolationSeverity.fromString("HIGH"))
        assertEquals(ViolationSeverity.HIGH, ViolationSeverity.fromString("high"))
        assertEquals(ViolationSeverity.MEDIUM, ViolationSeverity.fromString("MEDIUM"))
        assertEquals(ViolationSeverity.MEDIUM, ViolationSeverity.fromString("medium"))
        assertEquals(ViolationSeverity.LOW, ViolationSeverity.fromString("LOW"))
        assertEquals(ViolationSeverity.LOW, ViolationSeverity.fromString("low"))
        assertEquals(ViolationSeverity.LOW, ViolationSeverity.fromString(null))
    }

    @Test
    fun `user role parsing works correctly`() {
        assertEquals(UserRole.ADMIN, UserRole.fromString("ADMIN"))
        assertEquals(UserRole.ADMIN, UserRole.fromString("admin"))
        assertEquals(UserRole.SCANNER, UserRole.fromString("SCANNER"))
        assertEquals(UserRole.SCANNER, UserRole.fromString("scanner"))
        assertEquals(UserRole.SCANNER, UserRole.fromString(null))
        assertEquals(UserRole.SCANNER, UserRole.fromString("other"))
    }

    @Test
    fun `product model serialization and deserialization`() {
        val product = Product(
            id = "prod-101",
            barcode = "8901234567890",
            name = "Bharat Immunize Polio Vaccine 5ml",
            brand = "Bharat Pharma",
            category = "Vaccine",
            manufacturer = "Bharat Biotech Ltd",
            description = "National Immunization Schedule standard vial",
            manufacturingDate = "2026-01-15",
            expiryDate = "2027-01-15",
            status = ProductStatus.VERIFIED,
            createdAt = 1700000000000L
        )

        val map = product.toMap()
        assertEquals("prod-101", map["id"])
        assertEquals("8901234567890", map["barcode"])
        assertEquals("VERIFIED", map["status"])

        val parsed = Product.fromMap("prod-101", map)
        assertEquals(product.id, parsed.id)
        assertEquals(product.barcode, parsed.barcode)
        assertEquals(product.name, parsed.name)
        assertEquals(product.brand, parsed.brand)
        assertEquals(product.category, parsed.category)
        assertEquals(product.status, parsed.status)
        assertEquals(product.createdAt, parsed.createdAt)
    }

    @Test
    fun `violation model serialization and deserialization`() {
        val violation = Violation(
            id = "viol-99",
            type = "Missing MRP",
            severity = ViolationSeverity.HIGH,
            description = "MRP label completely absent from container.",
            evidenceImages = listOf("/cache/img1.jpg", "/cache/img2.jpg")
        )

        val map = violation.toMap()
        assertEquals("viol-99", map["id"])
        assertEquals("Missing MRP", map["type"])
        assertEquals("HIGH", map["severity"])

        val parsed = Violation.fromMap(map)
        assertEquals(violation.id, parsed.id)
        assertEquals(violation.type, parsed.type)
        assertEquals(violation.severity, parsed.severity)
        assertEquals(violation.description, parsed.description)
        assertEquals(2, parsed.evidenceImages.size)
    }

    @Test
    fun `inspection model serialization and deserialization`() {
        val inspection = Inspection(
            inspectionId = "INSP-2026-9901",
            inspectorId = "INSP-IND-8821",
            inspectorName = "Arjun Verma",
            productId = "prod-101",
            barcode = "8901234567890",
            productName = "Bharat Immunize Polio Vaccine 5ml",
            brand = "Bharat Pharma",
            category = "Vaccine",
            manufacturer = "Bharat Biotech Ltd",
            status = ProductStatus.WARNING,
            violations = listOf(
                Violation(
                    id = "v-1",
                    type = "Incorrect Labeling",
                    severity = ViolationSeverity.MEDIUM,
                    description = "Smudged batch number"
                )
            ),
            notes = "Re-examination scheduled within 48h.",
            imageUrls = listOf("/cache/ev1.jpg"),
            createdAt = 1700000000000L,
            isSynced = true
        )

        val map = inspection.toMap()
        assertEquals("INSP-2026-9901", map["inspectionId"])
        assertEquals("Arjun Verma", map["inspectorName"])
        assertEquals("WARNING", map["status"])

        val parsed = Inspection.fromMap("INSP-2026-9901", map)
        assertEquals(inspection.inspectionId, parsed.inspectionId)
        assertEquals(inspection.inspectorId, parsed.inspectorId)
        assertEquals(inspection.inspectorName, parsed.inspectorName)
        assertEquals(inspection.productName, parsed.productName)
        assertEquals(inspection.status, parsed.status)
        assertEquals(1, parsed.violations.size)
        assertEquals("Incorrect Labeling", parsed.violations[0].type)
        assertEquals(inspection.notes, parsed.notes)
        assertEquals(inspection.createdAt, parsed.createdAt)
    }

    @Test
    fun `inspection id format matches pattern`() {
        val id = InspectionViewModel.generateInspectionId()
        assertTrue(id.startsWith("INSP-2026-"))
        assertEquals(14, id.length)
    }

    @Test
    fun `scan record serialization and deserialization`() {
        val scan = ScanRecord(
            id = "scan-001",
            userId = "user-123",
            userName = "Dr. Sharma",
            barcode = "8901234567890",
            productId = "prod-101",
            productName = "Bharat Immunize Polio Vaccine 5ml",
            status = ProductStatus.VERIFIED,
            timestamp = 1700000050000L,
            message = "Scan verified successfully"
        )

        val map = scan.toMap()
        assertEquals("scan-001", map["id"])
        assertEquals("user-123", map["userId"])
        assertEquals("VERIFIED", map["status"])

        val parsed = ScanRecord.fromMap("scan-001", map)
        assertEquals(scan.id, parsed.id)
        assertEquals(scan.userId, parsed.userId)
        assertEquals(scan.userName, parsed.userName)
        assertEquals(scan.barcode, parsed.barcode)
        assertEquals(scan.productId, parsed.productId)
        assertEquals(scan.status, parsed.status)
        assertEquals(scan.message, parsed.message)
    }

    @Test
    fun `app user model serialization and deserialization`() {
        val user = AppUser(
            id = "admin-01",
            email = "admin@sih.gov.in",
            name = "System Admin",
            role = UserRole.ADMIN,
            inspectorId = "ADMIN-HQ-001",
            badgeNumber = "BADGE-0001",
            jurisdiction = "National Directorate - HQ",
            department = "Standards Bureau"
        )

        val map = user.toMap()
        assertEquals("admin-01", map["id"])
        assertEquals("ADMIN", map["role"])
        assertEquals("ADMIN-HQ-001", map["inspectorId"])

        val parsed = AppUser.fromMap("admin-01", map)
        assertEquals(user.id, parsed.id)
        assertEquals(user.email, parsed.email)
        assertEquals(user.name, parsed.name)
        assertEquals(user.role, parsed.role)
        assertEquals(user.inspectorId, parsed.inspectorId)
        assertEquals(user.badgeNumber, parsed.badgeNumber)
    }

    @Test
    fun `network default base url points to emulator 10_0_2_2 port 5000`() {
        assertEquals("http://10.0.2.2:5000/api/", com.example.network.NetworkConfig.DEFAULT_BASE_URL)
        assertEquals("http://10.0.2.2:5000/api/", com.example.network.NetworkConfig.getBaseUrl(null))
    }

    @Test
    fun `product dto to domain and from domain mapping`() {
        val domainProduct = Product(
            id = "prod_net_1",
            barcode = "8901234567890",
            name = "API Verified Product",
            brand = "Test Brand",
            category = "Electronics",
            manufacturer = "Test Mfg Ltd",
            description = "Tested via backend API",
            manufacturingDate = "2026-01-01",
            expiryDate = "2027-01-01",
            status = ProductStatus.VERIFIED,
            createdAt = 1700000000000L
        )

        val dto = com.example.network.ProductDto.fromDomain(domainProduct)
        assertEquals("prod_net_1", dto.id)
        assertEquals("8901234567890", dto.barcode)
        assertEquals("API Verified Product", dto.name)
        assertEquals("VERIFIED", dto.status)

        val convertedBack = dto.toDomain()
        assertEquals(domainProduct.id, convertedBack.id)
        assertEquals(domainProduct.barcode, convertedBack.barcode)
        assertEquals(domainProduct.name, convertedBack.name)
        assertEquals(domainProduct.status, convertedBack.status)
        assertEquals(domainProduct.manufacturingDate, convertedBack.manufacturingDate)
        assertEquals(domainProduct.expiryDate, convertedBack.expiryDate)
    }

    @Test
    fun `inspection dto to domain and from domain mapping`() {
        val domainInspection = Inspection(
            inspectionId = "INSP-2026-9999",
            inspectorId = "INSP-001",
            inspectorName = "Officer Test",
            productId = "prod_net_1",
            barcode = "8901234567890",
            productName = "API Verified Product",
            brand = "Test Brand",
            category = "Electronics",
            manufacturer = "Test Mfg Ltd",
            status = ProductStatus.WARNING,
            violations = listOf(
                Violation(
                    id = "viol_1",
                    type = "Incorrect Labeling",
                    severity = ViolationSeverity.MEDIUM,
                    description = "Missing batch number"
                )
            ),
            notes = "Test notes",
            createdAt = 1700000000000L,
            isSynced = true
        )

        val dto = com.example.network.InspectionDto.fromDomain(domainInspection)
        assertEquals("INSP-2026-9999", dto.inspectionIdCamel)
        assertEquals(1, dto.violations?.size)
        assertEquals("Incorrect Labeling", dto.violations?.first()?.type)

        val convertedBack = dto.toDomain()
        assertEquals(domainInspection.inspectionId, convertedBack.inspectionId)
        assertEquals(domainInspection.inspectorName, convertedBack.inspectorName)
        assertEquals(domainInspection.status, convertedBack.status)
        assertEquals(1, convertedBack.violations.size)
        assertEquals(ViolationSeverity.MEDIUM, convertedBack.violations[0].severity)
    }

    @Test
    fun `dashboard stats dto calculations resolve correctly`() {
        val statsDto = com.example.network.DashboardStatsDto(
            completed = 10,
            pending = 4,
            failed = 2,
            openViolationsCamel = 6
        )

        assertEquals(16, statsDto.resolvedTotal)
        assertEquals(10, statsDto.resolvedVerified)
        assertEquals(4, statsDto.resolvedWarning)
        assertEquals(6, statsDto.resolvedViolations)
    }

    @Test
    fun `inspection dto with compliance result conversion maps all fields`() {
        val ocr = com.example.ocr.ProductOcrResult(
            productName = "Amul Taaza Milk 500ml",
            brand = "Amul",
            mrp = "Rs. 27.00",
            netQuantity = "500 ml",
            manufacturingDate = "18/09/2026",
            expiryDate = "20/09/2026",
            batchNumber = "B901",
            manufacturer = "Gujarat Cooperative Milk Marketing Federation Ltd",
            manufacturerAddress = "Anand, Gujarat - 388001",
            licenseNumber = "10014021000001"
        )
        val inspection = Inspection(
            inspectionId = "INSP-2026-7788",
            inspectorId = "INSP-FIELD-09",
            inspectorName = "Priya Nair",
            barcode = "8901262010053",
            productName = "Amul Taaza Milk 500ml",
            brand = "Amul",
            status = ProductStatus.VERIFIED,
            createdAt = 1773800000000L,
            ocrResult = ocr
        )
        val complianceResult = com.example.compliance.ComplianceResult(
            overallStatus = com.example.compliance.ComplianceStatus.PASS,
            checks = listOf(
                com.example.compliance.ComplianceCheck(
                    ruleCode = "LM-PC-001",
                    fieldName = "Manufacturer",
                    status = com.example.compliance.ComplianceStatus.PASS,
                    detectedValue = "Gujarat Cooperative Milk Marketing Federation Ltd",
                    confidence = 0.95,
                    reason = "Manufacturer verified"
                ),
                com.example.compliance.ComplianceCheck(
                    ruleCode = "LM-PC-002",
                    fieldName = "Net Quantity",
                    status = com.example.compliance.ComplianceStatus.PASS,
                    detectedValue = "500 ml",
                    confidence = 0.99,
                    reason = "Standard unit detected"
                )
            ),
            passedCount = 2,
            failedCount = 0,
            reviewCount = 0,
            disclaimer = "Automated rule screening"
        )

        val dto = com.example.network.InspectionDto.fromDomain(inspection, complianceResult)

        assertEquals("INSP-2026-7788", dto.inspectionIdCamel)
        assertEquals("PASS", dto.overallStatus)
        assertEquals("PASS", dto.status)
        assertEquals("Rs. 27.00", dto.mrp)
        assertEquals("500 ml", dto.netQuantity)
        assertEquals("B901", dto.batchNumber)
        assertEquals("Anand, Gujarat - 388001", dto.manufacturerAddress)
        assertEquals("10014021000001", dto.licenseNumber)
        assertEquals(2, dto.complianceChecks?.size)
        assertEquals("LM-PC-001", dto.complianceChecks?.get(0)?.ruleCode)
        assertEquals("PASS", dto.complianceChecks?.get(0)?.status)

        // Check reverse conversion
        val backToDomain = dto.toDomain()
        assertEquals("INSP-2026-7788", backToDomain.inspectionId)
        assertEquals(ProductStatus.VERIFIED, backToDomain.status)
        assertEquals("Rs. 27.00", backToDomain.ocrResult?.mrp)
        assertEquals("500 ml", backToDomain.ocrResult?.netQuantity)
    }
}
