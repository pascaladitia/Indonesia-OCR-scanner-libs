package com.pascal.ocr_libs.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class OcrField(
    val key: String,
    val label: String,
    val value: String,
    val confidence: Float = 0.75f
) : Parcelable {
    fun toJson(): JSONObject = JSONObject(
        mapOf(
            "key" to key,
            "label" to label,
            "value" to value,
            "confidence" to confidence
        )
    )
}
