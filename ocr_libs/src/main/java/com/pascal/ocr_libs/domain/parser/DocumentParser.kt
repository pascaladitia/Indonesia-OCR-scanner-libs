package com.pascal.ocr_libs.domain.parser

import com.pascal.ocr_libs.domain.model.OcrText
import com.pascal.ocr_libs.model.DocumentType
import com.pascal.ocr_libs.model.OcrDocumentResult

interface DocumentParser {
    fun parse(text: OcrText, requestedType: DocumentType = DocumentType.AUTO): OcrDocumentResult
}
