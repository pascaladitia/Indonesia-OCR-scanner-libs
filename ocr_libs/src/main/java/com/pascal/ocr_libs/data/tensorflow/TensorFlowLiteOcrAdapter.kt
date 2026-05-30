package com.pascal.ocr_libs.data.tensorflow

import android.graphics.Bitmap
import com.pascal.ocr_libs.domain.model.OcrText

interface TensorFlowLiteOcrAdapter {
    fun recognize(interpreter: TensorFlowLiteInterpreter, bitmap: Bitmap, rotationDegrees: Int = 0): OcrText
}
