package com.pascal.ocr_libs.domain.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.pascal.ocr_libs.domain.model.OcrText

interface OcrEngine {
    suspend fun recognizeBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): OcrText
    suspend fun recognizeImageProxy(imageProxy: ImageProxy): OcrText
    fun close()
}
