package com.example.scanner

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_QR_CODE
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var isLocked = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isLocked) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && !isLocked) {
                        for (barcode in barcodes) {
                            val value = barcode.rawValue
                            if (!value.isNullOrBlank()) {
                                isLocked = true
                                onBarcodeDetected(value)
                                break
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BarcodeAnalyzer", "Barcode scan error", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun reset() {
        isLocked = false
    }
}
