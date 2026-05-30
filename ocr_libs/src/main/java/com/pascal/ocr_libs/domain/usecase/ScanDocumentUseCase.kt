package com.pascal.ocr_libs.domain.usecase

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.pascal.ocr_libs.domain.engine.OcrEngine
import com.pascal.ocr_libs.domain.parser.DocumentParser
import com.pascal.ocr_libs.model.DocumentType
import com.pascal.ocr_libs.model.OcrDocumentResult

class ScanDocumentUseCase(
    private val ocrEngine: OcrEngine,
    private val parser: DocumentParser
) {
    suspend fun fromBitmap(
        bitmap: Bitmap,
        documentType: DocumentType = DocumentType.AUTO,
        rotationDegrees: Int = 0
    ): OcrDocumentResult {
        return parser.parse(ocrEngine.recognizeBitmap(bitmap, rotationDegrees), documentType)
    }

    suspend fun fromImageProxy(
        imageProxy: ImageProxy,
        documentType: DocumentType = DocumentType.AUTO
    ): OcrDocumentResult {
        return parser.parse(ocrEngine.recognizeImageProxy(imageProxy), documentType)
    }
}
