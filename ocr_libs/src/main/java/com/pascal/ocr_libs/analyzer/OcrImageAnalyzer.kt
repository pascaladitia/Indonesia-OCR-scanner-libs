package com.pascal.ocr_libs.analyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.pascal.ocr_libs.model.DocumentType
import com.pascal.ocr_libs.model.OcrDocumentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OcrImageAnalyzer(
    private val documentType: DocumentType = DocumentType.AUTO,
    private val minConfidenceToAutoCapture: Float = 0.35f,
    private val analyzeIntervalMillis: Long = 900L,
    private val scanner: OcrDocumentScanner = OcrDocumentScanner(),
    private val onDocumentDetected: (OcrDocumentResult) -> Unit,
    private val onError: (Throwable) -> Unit = {}
) : ImageAnalysis.Analyzer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var busy = false
    @Volatile private var lastAnalyzedAt = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (busy || now - lastAnalyzedAt < analyzeIntervalMillis) {
            image.close()
            return
        }

        busy = true
        lastAnalyzedAt = now
        scope.launch {
            try {
                val result = scanner.scanImageProxy(image, documentType)
                if (result.isLikelyDocument && result.confidence >= minConfidenceToAutoCapture) {
                    onDocumentDetected(result)
                }
            } catch (throwable: Throwable) {
                onError(throwable)
            } finally {
                image.close()
                busy = false
            }
        }
    }
}
