package com.pascal.ocr_libs.analyzer

import android.graphics.Bitmap
import com.pascal.ocr_libs.model.DocumentType
import com.pascal.ocr_libs.model.KTPModel
import com.pascal.ocr_libs.parser.IndonesianDocumentParser
import kotlinx.coroutines.runBlocking

@Deprecated(
    message = "Use OcrDocumentScanner.scanBitmap(bitmap, DocumentType.KTP).",
    replaceWith = ReplaceWith("OcrDocumentScanner().scanBitmap(image, DocumentType.KTP)")
)
class OCRforEKTPLibrary {
    fun scanEKTP(image: Bitmap): KTPModel = runBlocking {
        val result = OcrDocumentScanner().scanBitmap(image, DocumentType.KTP)
        IndonesianDocumentParser.toKtpModel(result)
    }
}
