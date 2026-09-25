package com.example.network

import com.example.compliance.ComplianceResult
import com.example.compliance.ComplianceStatus
import com.example.model.Inspection
import com.example.model.Product
import com.example.model.ProductStatus
import com.example.model.ScanRecord
import com.example.model.Violation
import com.example.model.ViolationSeverity
import com.example.ocr.ProductOcrResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class ComplianceCheckDto(
    @Json(name = "ruleCode") val ruleCode: String = "",
    @Json(name = "fieldName") val fieldName: String = "",
    @Json(name = "status") val status: String = "",
    @Json(name = "detectedValue") val detectedValue: String? = null,
    @Json(name = "confidence") val confidence: Double = 0.0,
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class InspectionSaveResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "inspectionId") val inspectionId: String = "",
    @Json(name = "message") val message: String = ""
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "barcode") val barcode: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "brand") val brand: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "manufacturer") val manufacturer: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "manufacturing_date") val manufacturingDateSnake: String? = null,
    @Json(name = "manufacturingDate") val manufacturingDateCamel: String? = null,
    @Json(name = "expiry_date") val expiryDateSnake: String? = null,
    @Json(name = "expiryDate") val expiryDateCamel: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "created_at") val createdAtSnake: Long? = null,
    @Json(name = "createdAt") val createdAtCamel: Long? = null
) {
    fun toDomain(): Product {
        val resolvedMfd = manufacturingDateCamel ?: manufacturingDateSnake ?: ""
        val resolvedExp = expiryDateCamel ?: expiryDateSnake ?: ""
        val resolvedCreatedAt = createdAtCamel ?: createdAtSnake ?: System.currentTimeMillis()
        val resolvedStatus = ProductStatus.fromString(status)

        return Product(
            id = id ?: "prod_${System.currentTimeMillis()}",
            barcode = barcode ?: "",
            name = name ?: "Unnamed Product",
            brand = brand ?: "",
            category = category ?: "",
            manufacturer = manufacturer ?: "",
            description = description ?: "",
            manufacturingDate = resolvedMfd,
            expiryDate = resolvedExp,
            status = resolvedStatus,
            createdAt = resolvedCreatedAt
        )
    }

    companion object {
        fun fromDomain(product: Product): ProductDto {
            return ProductDto(
                id = product.id,
                barcode = product.barcode,
                name = product.name,
                brand = product.brand,
                category = product.category,
                manufacturer = product.manufacturer,
                description = product.description,
                manufacturingDateCamel = product.manufacturingDate,
                manufacturingDateSnake = product.manufacturingDate,
                expiryDateCamel = product.expiryDate,
                expiryDateSnake = product.expiryDate,
                status = product.status.name,
                createdAtCamel = product.createdAt,
                createdAtSnake = product.createdAt
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class ViolationDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "severity") val severity: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "evidence_images") val evidenceImagesSnake: List<String>? = null,
    @Json(name = "evidenceImages") val evidenceImagesCamel: List<String>? = null
) {
    fun toDomain(): Violation {
        val images = evidenceImagesCamel ?: evidenceImagesSnake ?: emptyList()
        return Violation(
            id = id ?: "viol_${System.currentTimeMillis()}",
            type = type ?: "Other",
            severity = ViolationSeverity.fromString(severity),
            description = description ?: "",
            evidenceImages = images
        )
    }

    companion object {
        fun fromDomain(violation: Violation): ViolationDto {
            return ViolationDto(
                id = violation.id,
                type = violation.type,
                severity = violation.severity.name,
                description = violation.description,
                evidenceImagesCamel = violation.evidenceImages,
                evidenceImagesSnake = violation.evidenceImages
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class InspectionDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "inspectionId") val inspectionIdCamel: String? = null,
    @Json(name = "inspection_id") val inspectionIdSnake: String? = null,
    @Json(name = "inspectorId") val inspectorIdCamel: String? = null,
    @Json(name = "inspector_id") val inspectorIdSnake: String? = null,
    @Json(name = "inspectorName") val inspectorNameCamel: String? = null,
    @Json(name = "inspector_name") val inspectorNameSnake: String? = null,
    @Json(name = "productId") val productIdCamel: String? = null,
    @Json(name = "product_id") val productIdSnake: String? = null,
    @Json(name = "barcode") val barcode: String? = null,
    @Json(name = "productName") val productNameCamel: String? = null,
    @Json(name = "product_name") val productNameSnake: String? = null,
    @Json(name = "brand") val brand: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "manufacturer") val manufacturer: String? = null,
    @Json(name = "manufacturingDate") val manufacturingDateCamel: String? = null,
    @Json(name = "manufacturing_date") val manufacturingDateSnake: String? = null,
    @Json(name = "expiryDate") val expiryDateCamel: String? = null,
    @Json(name = "expiry_date") val expiryDateSnake: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "overallStatus") val overallStatus: String? = null,
    @Json(name = "mrp") val mrp: String? = null,
    @Json(name = "netQuantity") val netQuantity: String? = null,
    @Json(name = "batchNumber") val batchNumber: String? = null,
    @Json(name = "manufacturerAddress") val manufacturerAddress: String? = null,
    @Json(name = "licenseNumber") val licenseNumber: String? = null,
    @Json(name = "inspectionDate") val inspectionDate: String? = null,
    @Json(name = "complianceChecks") val complianceChecks: List<ComplianceCheckDto>? = null,
    @Json(name = "violations") val violations: List<ViolationDto>? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "imageUrls") val imageUrlsCamel: List<String>? = null,
    @Json(name = "image_urls") val imageUrlsSnake: List<String>? = null,
    @Json(name = "createdAt") val createdAtCamel: Long? = null,
    @Json(name = "created_at") val createdAtSnake: Long? = null,
    @Json(name = "isSynced") val isSyncedCamel: Boolean? = null,
    @Json(name = "is_synced") val isSyncedSnake: Boolean? = null
) {
    fun toDomain(): Inspection {
        val resolvedId = inspectionIdCamel ?: inspectionIdSnake ?: id ?: "INSP-2026-${(1000..9999).random()}"
        val resolvedInspectorId = inspectorIdCamel ?: inspectorIdSnake ?: ""
        val resolvedInspectorName = inspectorNameCamel ?: inspectorNameSnake ?: "Inspector"
        val resolvedProductId = productIdCamel ?: productIdSnake ?: ""
        val resolvedProductName = productNameCamel ?: productNameSnake ?: "Product"
        val resolvedMfd = manufacturingDateCamel ?: manufacturingDateSnake ?: ""
        val resolvedExp = expiryDateCamel ?: expiryDateSnake ?: ""
        val resolvedImages = imageUrlsCamel ?: imageUrlsSnake ?: emptyList()
        val resolvedCreatedAt = createdAtCamel ?: createdAtSnake ?: System.currentTimeMillis()
        val resolvedStatus = when {
            overallStatus != null -> when (overallStatus.trim().uppercase()) {
                "PASS" -> ProductStatus.VERIFIED
                "REVIEW" -> ProductStatus.WARNING
                "FAIL" -> ProductStatus.INVALID
                else -> ProductStatus.fromString(status)
            }
            else -> ProductStatus.fromString(status)
        }
        val domainViolations = violations?.map { it.toDomain() } ?: emptyList()

        val parsedOcr = ProductOcrResult(
            productName = resolvedProductName,
            brand = brand ?: "",
            mrp = mrp,
            manufacturingDate = resolvedMfd,
            expiryDate = resolvedExp,
            batchNumber = batchNumber,
            netQuantity = netQuantity,
            manufacturer = manufacturer ?: "",
            manufacturerAddress = manufacturerAddress,
            licenseNumber = licenseNumber
        )

        return Inspection(
            inspectionId = resolvedId,
            inspectorId = resolvedInspectorId,
            inspectorName = resolvedInspectorName,
            productId = resolvedProductId,
            barcode = barcode ?: "",
            productName = resolvedProductName,
            brand = brand ?: "",
            category = category ?: "",
            manufacturer = manufacturer ?: "",
            manufacturingDate = resolvedMfd,
            expiryDate = resolvedExp,
            status = resolvedStatus,
            violations = domainViolations,
            notes = notes ?: "",
            imageUrls = resolvedImages,
            createdAt = resolvedCreatedAt,
            isSynced = isSyncedCamel ?: isSyncedSnake ?: true,
            ocrResult = parsedOcr
        )
    }

    companion object {
        fun fromDomain(
            inspection: Inspection,
            complianceResult: ComplianceResult? = null
        ): InspectionDto {
            val ocr = inspection.ocrResult
            val resolvedOverallStatus = complianceResult?.overallStatus?.name
                ?: when (inspection.status) {
                    ProductStatus.VERIFIED -> "PASS"
                    ProductStatus.WARNING -> "REVIEW"
                    ProductStatus.INVALID -> "FAIL"
                }

            val checksDtoList = complianceResult?.checks?.map { check ->
                ComplianceCheckDto(
                    ruleCode = check.ruleCode,
                    fieldName = check.fieldName,
                    status = check.status.name,
                    detectedValue = check.detectedValue,
                    confidence = check.confidence,
                    reason = check.reason
                )
            } ?: emptyList()

            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(inspection.createdAt))

            return InspectionDto(
                id = inspection.inspectionId,
                inspectionIdCamel = inspection.inspectionId,
                inspectionIdSnake = inspection.inspectionId,
                inspectorIdCamel = inspection.inspectorId,
                inspectorIdSnake = inspection.inspectorId,
                inspectorNameCamel = inspection.inspectorName,
                inspectorNameSnake = inspection.inspectorName,
                productIdCamel = inspection.productId,
                productIdSnake = inspection.productId,
                barcode = inspection.barcode,
                productNameCamel = inspection.productName,
                productNameSnake = inspection.productName,
                brand = inspection.brand,
                category = inspection.category,
                manufacturer = inspection.manufacturer,
                manufacturingDateCamel = inspection.manufacturingDate,
                manufacturingDateSnake = inspection.manufacturingDate,
                expiryDateCamel = inspection.expiryDate,
                expiryDateSnake = inspection.expiryDate,
                status = resolvedOverallStatus,
                overallStatus = resolvedOverallStatus,
                mrp = ocr?.mrp,
                netQuantity = ocr?.netQuantity,
                batchNumber = ocr?.batchNumber,
                manufacturerAddress = ocr?.manufacturerAddress,
                licenseNumber = ocr?.licenseNumber,
                inspectionDate = formattedDate,
                complianceChecks = checksDtoList,
                violations = inspection.violations.map { ViolationDto.fromDomain(it) },
                notes = inspection.notes,
                imageUrlsCamel = inspection.imageUrls,
                imageUrlsSnake = inspection.imageUrls,
                createdAtCamel = inspection.createdAt,
                createdAtSnake = inspection.createdAt,
                isSyncedCamel = inspection.isSynced,
                isSyncedSnake = inspection.isSynced
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class ScanRecordDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "userId") val userIdCamel: String? = null,
    @Json(name = "user_id") val userIdSnake: String? = null,
    @Json(name = "userName") val userNameCamel: String? = null,
    @Json(name = "user_name") val userNameSnake: String? = null,
    @Json(name = "barcode") val barcode: String? = null,
    @Json(name = "productId") val productIdCamel: String? = null,
    @Json(name = "product_id") val productIdSnake: String? = null,
    @Json(name = "productName") val productNameCamel: String? = null,
    @Json(name = "product_name") val productNameSnake: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "timestamp") val timestamp: Long? = null,
    @Json(name = "message") val message: String? = null
) {
    fun toDomain(): ScanRecord {
        return ScanRecord(
            id = id ?: "scan_${System.currentTimeMillis()}",
            userId = userIdCamel ?: userIdSnake ?: "",
            userName = userNameCamel ?: userNameSnake ?: "Scanner",
            barcode = barcode ?: "",
            productId = productIdCamel ?: productIdSnake ?: "",
            productName = productNameCamel ?: productNameSnake ?: "Product",
            status = ProductStatus.fromString(status),
            timestamp = timestamp ?: System.currentTimeMillis(),
            message = message ?: ""
        )
    }

    companion object {
        fun fromDomain(scan: ScanRecord): ScanRecordDto {
            return ScanRecordDto(
                id = scan.id,
                userIdCamel = scan.userId,
                userIdSnake = scan.userId,
                userNameCamel = scan.userName,
                userNameSnake = scan.userName,
                barcode = scan.barcode,
                productIdCamel = scan.productId,
                productIdSnake = scan.productId,
                productNameCamel = scan.productName,
                productNameSnake = scan.productName,
                status = scan.status.name,
                timestamp = scan.timestamp,
                message = scan.message
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class DashboardStatsDto(
    @Json(name = "totalInspections") val totalInspectionsCamel: Int? = null,
    @Json(name = "total_inspections") val totalInspectionsSnake: Int? = null,
    @Json(name = "completed") val completed: Int? = null,
    @Json(name = "pending") val pending: Int? = null,
    @Json(name = "failed") val failed: Int? = null,
    @Json(name = "totalViolations") val totalViolationsCamel: Int? = null,
    @Json(name = "total_violations") val totalViolationsSnake: Int? = null,
    @Json(name = "openViolations") val openViolationsCamel: Int? = null,
    @Json(name = "open_violations") val openViolationsSnake: Int? = null,
    @Json(name = "inspectors") val inspectors: Int? = null,
    @Json(name = "todayInspections") val todayInspections: Int? = null,
    @Json(name = "verifiedCount") val verifiedCount: Int? = null,
    @Json(name = "warningCount") val warningCount: Int? = null,
    @Json(name = "violationCount") val violationCount: Int? = null
) {
    val resolvedTotal: Int
        get() = totalInspectionsCamel ?: totalInspectionsSnake ?: (completed ?: 0) + (pending ?: 0) + (failed ?: 0)

    val resolvedViolations: Int
        get() = violationCount ?: totalViolationsCamel ?: totalViolationsSnake ?: openViolationsCamel ?: openViolationsSnake ?: failed ?: 0

    val resolvedVerified: Int
        get() = verifiedCount ?: completed ?: 0

    val resolvedWarning: Int
        get() = warningCount ?: pending ?: 0
}

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean? = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: T? = null,
    @Json(name = "items") val items: List<T>? = null,
    @Json(name = "products") val products: List<ProductDto>? = null,
    @Json(name = "inspections") val inspections: List<InspectionDto>? = null,
    @Json(name = "scans") val scans: List<ScanRecordDto>? = null
)
