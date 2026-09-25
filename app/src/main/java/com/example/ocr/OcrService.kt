package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

data class OcrTextLine(
    val text: String,
    val confidence: Float? = null
)

data class OcrTextBlock(
    val text: String,
    val lines: List<OcrTextLine> = emptyList()
)

data class OcrImageResult(
    val imagePath: String,
    val rawText: String,
    val blocks: List<OcrTextBlock> = emptyList(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

object OcrService {
    private const val TAG = "OcrService"
    private const val MAX_IMAGE_DIMENSION = 1600

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Process a single image file on-device using ML Kit Text Recognition.
     */
    suspend fun processImage(imagePath: String): OcrImageResult = withContext(Dispatchers.Default) {
        val file = File(imagePath)
        if (!file.exists() || file.length() == 0L) {
            return@withContext OcrImageResult(
                imagePath = imagePath,
                rawText = "",
                isSuccess = false,
                errorMessage = "File not found or empty"
            )
        }

        var bitmap: Bitmap? = null
        try {
            // 1. Lightweight preprocessing: orientation correction and safe downsampling
            bitmap = loadPreprocessedBitmap(file)
            if (bitmap == null) {
                return@withContext OcrImageResult(
                    imagePath = imagePath,
                    rawText = "",
                    isSuccess = false,
                    errorMessage = "Failed to decode image bitmap"
                )
            }

            // 2. Prepare ML Kit InputImage
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // 3. Run ML Kit text recognition asynchronously
            val mlKitText: Text = recognizeTextAsync(inputImage)

            // 4. Map ML Kit Text structure
            val textBlocks = mlKitText.textBlocks.map { block ->
                val lines = block.lines.map { line ->
                    val lineConfidence = line.elements.mapNotNull { it.confidence }.average().toFloat().takeIf { !it.isNaN() }
                    OcrTextLine(
                        text = line.text,
                        confidence = lineConfidence
                    )
                }
                OcrTextBlock(
                    text = block.text,
                    lines = lines
                )
            }

            Log.d(TAG, "Successfully processed $imagePath - recognized ${mlKitText.text.length} chars")
            OcrImageResult(
                imagePath = imagePath,
                rawText = mlKitText.text,
                blocks = textBlocks,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "OCR recognition failed for $imagePath: ${e.message}", e)
            OcrImageResult(
                imagePath = imagePath,
                rawText = "",
                isSuccess = false,
                errorMessage = e.message
            )
        } finally {
            bitmap?.recycle()
        }
    }

    /**
     * Process multiple images sequentially with progress reporting.
     */
    suspend fun processMultipleImages(
        imagePaths: List<String>,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<OcrImageResult> = withContext(Dispatchers.Default) {
        val results = mutableListOf<OcrImageResult>()
        val total = imagePaths.size

        imagePaths.forEachIndexed { index, path ->
            onProgress(index + 1, total)
            val result = processImage(path)
            results.add(result)
        }

        results
    }

    private suspend fun recognizeTextAsync(inputImage: InputImage): Text =
        suspendCancellableCoroutine { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    if (continuation.isActive) {
                        continuation.resume(text)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }

    /**
     * Decode bitmap safely with sub-sampling if needed, and apply EXIF rotation.
     */
    private fun loadPreprocessedBitmap(file: File): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth <= 0 || origHeight <= 0) return null

        var sampleSize = 1
        val maxDim = max(origWidth, origHeight)
        if (maxDim > MAX_IMAGE_DIMENSION) {
            sampleSize = (maxDim.toFloat() / MAX_IMAGE_DIMENSION).toInt().coerceAtLeast(1)
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

        // Check EXIF orientation
        val rotationAngle = getExifOrientationDegrees(file)
        if (rotationAngle != 0) {
            val matrix = Matrix().apply { postRotate(rotationAngle.toFloat()) }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated != decoded) {
                decoded.recycle()
            }
            return rotated
        }

        return decoded
    }

    private fun getExifOrientationDegrees(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
}
