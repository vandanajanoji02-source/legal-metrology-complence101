package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ScannerHome : Screen("scanner_home")
    object InspectorProfile : Screen("inspector_profile")
    object InspectionCapture : Screen("inspection_capture")
    object OcrResults : Screen("ocr_results")
    object BarcodeScanner : Screen("barcode_scanner")
    object VerificationResult : Screen("verification_result")
    object ViolationEntry : Screen("violation_entry")
    object InspectionReport : Screen("inspection_report")
    object ScanHistory : Screen("scan_history")
    object AdminDashboard : Screen("admin_dashboard")
    object AdminProducts : Screen("admin_products")
    object AdminScans : Screen("admin_scans")
    object ComplianceResult : Screen("compliance_result")
}
