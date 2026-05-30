package com.pascal.ocr_libs.data.tensorflow

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.pascal.ocr_libs.domain.engine.OcrEngine
import com.pascal.ocr_libs.domain.model.OcrText
import java.nio.ByteBuffer

class TensorFlowLiteOcrEngine(
    modelBuffer: ByteBuffer,
    private val adapter: TensorFlowLiteOcrAdapter
) : OcrEngine {
    private val interpreter = TensorFlowLiteInterpreter(modelBuffer)

    override suspend fun recognizeBitmap(bitmap: Bitmap, rotationDegrees: Int): OcrText {
        return adapter.recognize(interpreter, bitmap, rotationDegrees)
    }

    override suspend fun recognizeImageProxy(imageProxy: ImageProxy): OcrText {
        throw UnsupportedOperationException(
            "TensorFlowLiteOcrEngine reads Bitmap input. Convert the ImageProxy frame to Bitmap before scanning, or provide a custom OcrEngine implementation for live frames."
        )
    }

    override fun close() {
        interpreter.close()
    }
}
