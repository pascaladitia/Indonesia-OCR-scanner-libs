package com.pascal.ocr_libs.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class KTPModel(
    var nik: String? = null,
    var nama: String? = null,
    var tempatLahir: String? = null,
    var golDarah: String? = null,
    var tglLahir: String? = null,
    var jenisKelamin: String? = null,
    var alamat: String? = null,
    var rtrw: String? = null,
    var kelurahan: String? = null,
    var kecamatan: String? = null,
    var agama: String? = null,
    var statusPerkawinan: String? = null,
    var pekerjaan: String? = null,
    var kewarganegaraan: String? = null,
    var berlakuHingga: String? = null,
    var provinsi: String? = null,
    var kabKot: String? = null,
    var confidence: Int = 0,
    var bitmap: Bitmap? = null
) : Parcelable {
    fun toJson(): String = JSONObject(
        mapOf(
            "nik" to nik,
            "nama" to nama,
            "tempatLahir" to tempatLahir,
            "golDarah" to golDarah,
            "tglLahir" to tglLahir,
            "jenisKelamin" to jenisKelamin,
            "alamat" to alamat,
            "rtrw" to rtrw,
            "kelurahan" to kelurahan,
            "kecamatan" to kecamatan,
            "agama" to agama,
            "statusPerkawinan" to statusPerkawinan,
            "pekerjaan" to pekerjaan,
            "kewarganegaraan" to kewarganegaraan,
            "berlakuHingga" to berlakuHingga,
            "provinsi" to provinsi,
            "kabKot" to kabKot,
            "confidence" to confidence
        )
    ).toString()
}
