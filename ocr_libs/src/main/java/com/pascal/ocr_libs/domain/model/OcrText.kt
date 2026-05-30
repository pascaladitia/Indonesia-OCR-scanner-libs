package com.pascal.ocr_libs.domain.model

data class OcrText(
    val rawText: String,
    val lines: List<String>
)
