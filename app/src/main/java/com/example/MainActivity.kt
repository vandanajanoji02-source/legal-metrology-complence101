package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AuthRepository
import com.example.data.InspectionRepository
import com.example.data.ProductRepository
import com.example.data.ScanRepository
import com.example.model.Inspection
import com.example.network.ApiClient
import com.example.model.ProductStatus
import com.example.model.ScanRecord
import com.example.model.UserRole
import com.example.ui.navigation.Screen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AdminProductsScreen
import com.example.ui.screens.admin.AdminScansScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.history.ScanHistoryScreen
import com.example.ui.screens.inspection.InspectionImageCaptureScreen
import com.example.ui.screens.inspection.InspectionReportScreen
import com.example.ui.screens.inspection.OcrResultsScreen
import com.example.ui.screens.inspection.ComplianceResultScreen
import com.example.ui.screens.inspection.ViolationScreen
import com.example.compliance.ComplianceRuleEngine
import com.example.ui.screens.profile.InspectorProfileScreen
import com.example.ui.screens.result.VerificationResultScreen
import com.example.ui.screens.scanner.CameraScannerScreen
import com.example.ui.screens.scanner.ScannerHomeScreen
import com.example.ui.theme.SmartVerifyTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.InspectionViewModel
import com.example.ui.viewmodel.ScanProcessState
import com.example.ui.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ApiClient.initialize(applicationContext)

        val authRepository = AuthRepository(applicationContext)
        val productRepository = ProductRepository(applicationContext)
        val scanRepository = ScanRepository(applicationContext)
        val inspectionRepository = InspectionRepository(applicationContext)

        setContent {
            SmartVerifyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartVerifyApp(
                        authRepository = authRepository,
                        productRepository = productRepository,
                        scanRepository = scanRepository,
                        inspectionRepository = inspectionRepository
                    )
                }
            }
        }
    }
}

@Composable
fun SmartVerifyApp(
    authRepository: AuthRepository,
    productRepository: ProductRepository,
    scanRepository: ScanRepository,
    inspectionRepository: InspectionRepository
) {
    val navController = rememberNavController()

    val authViewModel = remember { AuthViewModel(authRepository) }
    val scannerViewModel = remember { ScannerViewModel(productRepository, scanRepository, authRepository) }
    val inspectionViewModel = remember {
        InspectionViewModel(inspectionRepository, productRepository, scanRepository, authRepository)
    }
    val historyViewModel = remember { HistoryViewModel(scanRepository, inspectionRepository) }
    val adminViewModel = remember { AdminViewModel(productRepository, scanRepository) }

    val currentUser by authViewModel.currentUser.collectAsState()
    val inspectionStats by inspectionRepository.stats.collectAsState()
    val recentInspections by inspectionRepository.inspections.collectAsState()
    val scanProcessState by scannerViewModel.scanState.collectAsState()
    val capturedImages by inspectionViewModel.capturedImages.collectAsState()
    val currentInspectionId by inspectionViewModel.currentInspectionId.collectAsState()
    val activeInspectionForReport by inspectionViewModel.activeInspectionForReport.collectAsState()
    val currentInspectionProduct by inspectionViewModel.currentProduct.collectAsState()
    val currentScannedBarcode by inspectionViewModel.scannedBarcode.collectAsState()

    val startDestination = if (currentUser == null) {
        Screen.Login.route
    } else if (currentUser?.role == UserRole.ADMIN) {
        Screen.AdminDashboard.route
    } else {
        Screen.ScannerHome.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- 1. Login Screen ---
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { user ->
                    if (user.role == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.ScannerHome.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // --- 2. Inspector Home Screen ---
        composable(Screen.ScannerHome.route) {
            val user = currentUser
            if (user == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                ScannerHomeScreen(
                    user = user,
                    stats = inspectionStats,
                    recentInspections = recentInspections,
                    onStartNewInspection = {
                        inspectionViewModel.startNewInspection()
                        navController.navigate(Screen.InspectionCapture.route)
                    },
                    onOpenQuickScanner = {
                        scannerViewModel.resetScanner()
                        navController.navigate(Screen.BarcodeScanner.route)
                    },
                    onViewHistory = {
                        navController.navigate(Screen.ScanHistory.route)
                    },
                    onOpenProfile = {
                        navController.navigate(Screen.InspectorProfile.route)
                    },
                    onSelectInspection = { inspection ->
                        inspectionViewModel.selectInspectionForReport(inspection)
                        navController.navigate(Screen.InspectionReport.route)
                    },
                    onSimulateScan = { barcode ->
                        inspectionViewModel.startNewInspection()
                        inspectionViewModel.processBarcode(barcode)
                        scannerViewModel.processBarcode(barcode)
                        navController.navigate(Screen.VerificationResult.route)
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // --- 3. Inspector Profile Screen ---
        composable(Screen.InspectorProfile.route) {
            val user = currentUser
            if (user != null) {
                InspectorProfileScreen(
                    user = user,
                    totalInspections = inspectionStats.totalInspections,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // --- 4. Multi-Image Evidence Capture Screen ---
        composable(Screen.InspectionCapture.route) {
            InspectionImageCaptureScreen(
                viewModel = inspectionViewModel,
                onSubmitEvidence = {
                    inspectionViewModel.runOcrOnCapturedPhotos()
                    navController.navigate(Screen.OcrResults.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- 4b. On-Device ML Kit OCR Results Screen ---
        composable(Screen.OcrResults.route) {
            OcrResultsScreen(
                viewModel = inspectionViewModel,
                onContinueNextStep = {
                    // Always go to Compliance Result first
                    navController.navigate(Screen.ComplianceResult.route)
                },
                onRetakePhotos = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- 4c. Compliance Result Screen ---
        composable(Screen.ComplianceResult.route) {
            val ocrResult by inspectionViewModel.ocrResult.collectAsState()
            val flowState by inspectionViewModel.flowState.collectAsState()
            val complianceResult = remember(ocrResult) {
                ComplianceRuleEngine.evaluate(
                    ocrResult = ocrResult ?: com.example.ocr.ProductOcrResult(),
                    isImported = false
                )
            }
            ComplianceResultScreen(
                result = complianceResult,
                inspectionId = currentInspectionId,
                flowState = flowState,
                onSubmitInspection = {
                    inspectionViewModel.submitInspection(complianceResult)
                },
                onViewReport = {
                    navController.navigate(Screen.InspectionReport.route) {
                        popUpTo(Screen.ComplianceResult.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- 5. Barcode Scanner Screen (CameraX + ML Kit) ---
        composable(Screen.BarcodeScanner.route) {
            CameraScannerScreen(
                viewModel = scannerViewModel,
                headerSubtitle = "$currentInspectionId • Step 2 of 3",
                onBarcodeScanned = { barcode ->
                    inspectionViewModel.processBarcode(barcode)
                    scannerViewModel.processBarcode(barcode)
                    navController.navigate(Screen.VerificationResult.route) {
                        popUpTo(Screen.BarcodeScanner.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.VerificationResult.route) {
                        popUpTo(Screen.BarcodeScanner.route) { inclusive = true }
                    }
                },
                onClose = {
                    navController.popBackStack()
                }
            )
        }

        // --- 6. Product Verification Result Screen ---
        composable(Screen.VerificationResult.route) {
            val currentState = scanProcessState
            val (product, scanRecord) = when {
                currentInspectionProduct != null -> {
                    val p = currentInspectionProduct!!
                    val rec = ScanRecord(
                        id = "scan_" + System.currentTimeMillis(),
                        userId = currentUser?.inspectorId ?: "inspector",
                        userName = currentUser?.name ?: "Inspector",
                        barcode = currentScannedBarcode.ifBlank { p.barcode },
                        productId = p.id,
                        productName = p.name,
                        status = p.status,
                        timestamp = System.currentTimeMillis(),
                        message = p.status.defaultMessage
                    )
                    Pair(p, rec)
                }
                currentState is ScanProcessState.ResultReady -> {
                    Pair(currentState.product, currentState.scanRecord)
                }
                else -> {
                    val fallback = ScanRecord(
                        id = "scan_demo",
                        userId = currentUser?.inspectorId ?: "scanner",
                        userName = currentUser?.name ?: "Scanner",
                        barcode = currentScannedBarcode.ifBlank { "8901234567890" },
                        productId = "prod_demo_1",
                        productName = "Demo Verified Product",
                        status = ProductStatus.VERIFIED,
                        timestamp = System.currentTimeMillis(),
                        message = "Product found in the registered product database."
                    )
                    Pair(null, fallback)
                }
            }

            VerificationResultScreen(
                product = product,
                scanRecord = scanRecord,
                evidenceImages = capturedImages,
                onProceedToViolation = {
                    navController.navigate(Screen.ViolationEntry.route)
                },
                onCompleteInspection = {
                    inspectionViewModel.submitInspection {
                        navController.navigate(Screen.InspectionReport.route) {
                            popUpTo(Screen.VerificationResult.route) { inclusive = true }
                        }
                    }
                },
                onScanAnother = {
                    inspectionViewModel.startNewInspection()
                    scannerViewModel.resetScanner()
                    navController.navigate(Screen.BarcodeScanner.route) {
                        popUpTo(Screen.VerificationResult.route) { inclusive = true }
                    }
                },
                onViewHistory = {
                    navController.navigate(Screen.ScanHistory.route)
                },
                onBack = {
                    val dest = if (currentUser?.role == UserRole.ADMIN) Screen.AdminDashboard.route else Screen.ScannerHome.route
                    navController.navigate(dest) {
                        popUpTo(Screen.VerificationResult.route) { inclusive = true }
                    }
                }
            )
        }

        // --- 7. Violation Entry Screen ---
        composable(Screen.ViolationEntry.route) {
            ViolationScreen(
                viewModel = inspectionViewModel,
                onFinalizeInspection = {
                    navController.navigate(Screen.InspectionReport.route) {
                        popUpTo(Screen.ViolationEntry.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- 8. Inspection Report Screen ---
        composable(Screen.InspectionReport.route) {
            val reportToDisplay = activeInspectionForReport ?: recentInspections.firstOrNull() ?: Inspection(
                inspectionId = currentInspectionId,
                inspectorId = currentUser?.inspectorId ?: "INSP-IND-8821",
                inspectorName = currentUser?.name ?: "Arjun Verma",
                productName = "Demo Inspection Record",
                barcode = "8901234567890",
                status = ProductStatus.VERIFIED,
                notes = "Inspection completed successfully.",
                createdAt = System.currentTimeMillis()
            )

            InspectionReportScreen(
                inspection = reportToDisplay,
                onBack = {
                    navController.popBackStack()
                },
                onReturnHome = {
                    val dest = if (currentUser?.role == UserRole.ADMIN) Screen.AdminDashboard.route else Screen.ScannerHome.route
                    navController.navigate(dest) {
                        popUpTo(Screen.ScannerHome.route) { inclusive = true }
                    }
                }
            )
        }

        // --- 9. Inspection History Screen ---
        composable(Screen.ScanHistory.route) {
            ScanHistoryScreen(
                viewModel = historyViewModel,
                onBack = { navController.popBackStack() },
                onSelectInspection = { inspection ->
                    inspectionViewModel.selectInspectionForReport(inspection)
                    navController.navigate(Screen.InspectionReport.route)
                }
            )
        }

        // --- Admin Screens (Existing compatibility) ---
        composable(Screen.AdminDashboard.route) {
            val user = currentUser
            if (user == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                AdminDashboardScreen(
                    user = user,
                    viewModel = adminViewModel,
                    onNavigateProducts = {
                        navController.navigate(Screen.AdminProducts.route)
                    },
                    onNavigateScans = {
                        navController.navigate(Screen.AdminScans.route)
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.AdminProducts.route) {
            AdminProductsScreen(
                viewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminScans.route) {
            AdminScansScreen(
                viewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
