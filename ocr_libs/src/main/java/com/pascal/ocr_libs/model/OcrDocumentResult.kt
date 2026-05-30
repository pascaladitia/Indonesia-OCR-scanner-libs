package com.pascal.ocr_libs.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

@Parcelize
data class OcrDocumentResult(
    val documentType: DocumentType,
    val rawText: String,
    val fields: List<OcrField>,
    val confidence: Float,
    val isLikelyDocument: Boolean,
    val errorMessage: String? = null
) : Parcelable {
    fun value(key: String): String? = fields.firstOrNull { it.key == key }?.value

    fun toJson(): String {
        val fieldArray = JSONArray()
        fields.forEach { fieldArray.put(it.toJson()) }
        return JSONObject(
            mapOf(
                "documentType" to documentType.name,
                "rawText" to rawText,
                "confidence" to confidence,
                "isLikelyDocument" to isLikelyDocument,
                "errorMessage" to errorMessage,
                "fields" to fieldArray
            )
        ).toString()
    }
}
