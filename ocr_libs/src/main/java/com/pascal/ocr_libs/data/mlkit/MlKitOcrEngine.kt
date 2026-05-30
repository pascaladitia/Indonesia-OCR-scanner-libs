package com.pascal.ocr_libs.data.mlkit

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pascal.ocr_libs.domain.engine.OcrEngine
import com.pascal.ocr_libs.domain.model.OcrText
import kotlinx.coroutines.tasks.await

class MlKitOcrEngine : OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeBitmap(bitmap: Bitmap, rotationDegrees: Int): OcrText {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        return recognizer.process(image).await().toOcrText()
    }

    @SuppressLint("UnsafeOptInUsageError")
    @OptIn(ExperimentalGetImage::class)
    override suspend fun recognizeImageProxy(imageProxy: ImageProxy): OcrText {
        val mediaImage = imageProxy.image ?: return OcrText(rawText = "", lines = emptyList())
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        return recognizer.process(image).await().toOcrText()
    }

    override fun close() {
        recognizer.close()
    }
}
