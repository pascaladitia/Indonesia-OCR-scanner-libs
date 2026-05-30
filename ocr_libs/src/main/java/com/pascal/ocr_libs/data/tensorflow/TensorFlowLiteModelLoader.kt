package com.pascal.ocr_libs.data.tensorflow

import android.content.Context
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object TensorFlowLiteModelLoader {
    const val EAST_TEXT_DETECTOR_ASSET = "models/east_text_detector.tflite"
    const val KERAS_OCR_RECOGNIZER_ASSET = "models/keras_ocr_recognizer.tflite"

    fun fromAsset(context: Context, assetPath: String): MappedByteBuffer {
        val descriptor = context.assets.openFd(assetPath)
        FileInputStream(descriptor.fileDescriptor).use { inputStream ->
            return inputStream.channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
        }
    }

    fun bundledTextDetector(context: Context): MappedByteBuffer =
        fromAsset(context, EAST_TEXT_DETECTOR_ASSET)

    fun bundledTextRecognizer(context: Context): MappedByteBuffer =
        fromAsset(context, KERAS_OCR_RECOGNIZER_ASSET)
}
