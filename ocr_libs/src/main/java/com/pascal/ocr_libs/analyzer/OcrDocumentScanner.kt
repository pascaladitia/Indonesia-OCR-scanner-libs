package com.pascal.ocr_libs.analyzer

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.pascal.ocr_libs.data.mlkit.MlKitOcrEngine
import com.pascal.ocr_libs.domain.engine.OcrEngine
import com.pascal.ocr_libs.domain.parser.DocumentParser
import com.pascal.ocr_libs.domain.usecase.ScanDocumentUseCase
import com.pascal.ocr_libs.model.DocumentType
import com.pascal.ocr_libs.model.OcrDocumentResult
import com.pascal.ocr_libs.parser.IndonesianDocumentParser

class OcrDocumentScanner(
    private val ocrEngine: OcrEngine = MlKitOcrEngine(),
    parser: DocumentParser = IndonesianDocumentParser
) {
    private val scanDocument = ScanDocumentUseCase(ocrEngine, parser)

    suspend fun scanBitmap(
        bitmap: Bitmap,
        documentType: DocumentType = DocumentType.AUTO,
        rotationDegrees: Int = 0
    ): OcrDocumentResult {
        return scanDocument.fromBitmap(bitmap, documentType, rotationDegrees)
    }

    suspend fun scanImageProxy(
        imageProxy: ImageProxy,
        documentType: DocumentType = DocumentType.AUTO
    ): OcrDocumentResult {
        return scanDocument.fromImageProxy(imageProxy, documentType)
    }

    fun close() {
        ocrEngine.close()
    }
}
