package com.pascal.ocr_libs.data.mlkit

import com.google.mlkit.vision.text.Text
import com.pascal.ocr_libs.domain.model.OcrText

internal fun Text.toOcrText(): OcrText {
    return OcrText(
        rawText = text.trim(),
        lines = textBlocks
            .flatMap { it.lines }
            .map { line -> line.text.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotBlank() }
    )
}
