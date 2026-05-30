package com.pascal.ocr_libs.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class OCRResultModel(
    val isSuccess: Boolean,
    val errorMessage: String?,
    val imagePath: String? = null,
    val ktp: KTPModel
) : Parcelable {
    fun toJson(): String = JSONObject(
        mapOf(
            "isSuccess" to isSuccess,
            "errorMessage" to errorMessage,
            "imagePath" to imagePath,
            "ktp" to JSONObject(ktp.toJson())
        )
    ).toString()
}
