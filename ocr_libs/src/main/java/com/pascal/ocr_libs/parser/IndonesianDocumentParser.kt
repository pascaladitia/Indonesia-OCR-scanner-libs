package com.pascal.ocr_libs.parser

import com.pascal.ocr_libs.domain.model.OcrText
import com.pascal.ocr_libs.domain.parser.DocumentParser
import com.pascal.ocr_libs.model.DocumentType
import com.pascal.ocr_libs.model.KTPModel
import com.pascal.ocr_libs.model.OcrDocumentResult
import com.pascal.ocr_libs.model.OcrField
import java.util.Locale

object IndonesianDocumentParser : DocumentParser {
    override fun parse(text: OcrText, requestedType: DocumentType): OcrDocumentResult {
        val rawText = text.rawText.trim()
        val lines = text.lines.map { it.cleanLine() }.filter { it.isNotBlank() }

        if (rawText.isBlank()) {
            return OcrDocumentResult(
                documentType = if (requestedType == DocumentType.AUTO) DocumentType.KTP else requestedType,
                rawText = rawText,
                fields = emptyList(),
                confidence = 0f,
                isLikelyDocument = false,
                errorMessage = "No readable text was detected in the image."
            )
        }

        val type = if (requestedType == DocumentType.AUTO) detectType(rawText, lines) else requestedType
        return when (type) {
            DocumentType.KTP, DocumentType.AUTO -> parseKtp(rawText, lines)
            DocumentType.KK -> parseKk(rawText, lines)
            DocumentType.STNK -> parseStnk(rawText, lines)
            DocumentType.PLATE -> parsePlate(rawText)
            DocumentType.SIM -> parseSim(rawText, lines)
        }
    }

    fun toKtpModel(result: OcrDocumentResult): KTPModel = KTPModel(
        nik = result.value("nik"),
        nama = result.value("nama"),
        tempatLahir = result.value("tempat_lahir"),
        golDarah = result.value("gol_darah"),
        tglLahir = result.value("tanggal_lahir"),
        jenisKelamin = result.value("jenis_kelamin"),
        alamat = result.value("alamat"),
        rtrw = result.value("rt_rw"),
        kelurahan = result.value("kelurahan"),
        kecamatan = result.value("kecamatan"),
        agama = result.value("agama"),
        statusPerkawinan = result.value("status_perkawinan"),
        pekerjaan = result.value("pekerjaan"),
        kewarganegaraan = result.value("kewarganegaraan"),
        berlakuHingga = result.value("berlaku_hingga"),
        provinsi = result.value("provinsi"),
        kabKot = result.value("kab_kota"),
        confidence = (result.confidence * 100).toInt()
    )

    private fun parseKtp(rawText: String, lines: List<String>): OcrDocumentResult {
        val fields = linkedMapOf<String, OcrField>()
        fun add(key: String, label: String, value: String?, confidence: Float = 0.78f) {
            value.cleanedValue()?.let { fields[key] = OcrField(key, label, it, confidence) }
        }

        add("provinsi", "Province", lines.firstStarting("PROVINSI")?.removeLabel("PROVINSI"), 0.86f)
        add("kab_kota", "City/Regency", lines.firstOrNull { it.contains("KOTA") || it.contains("KABUPATEN") || it.contains("JAKARTA") }, 0.82f)
        add("nik", "NIK", findNik(rawText), 0.92f)
        add("nama", "Full Name", lines.valueAfterLabel("NAMA"), 0.82f)

        val birth = lines.valueAfterAnyLabel("TEMPAT/TGL LAHIR", "TEMPAT TG Lahir", "TEMPAT LAHIR", "TGL LAHIR")
            ?: lines.firstOrNull { DATE_REGEX.containsMatchIn(it) && it.contains(",") }
        birth?.let {
            val date = DATE_REGEX.find(it)?.value?.normalizeDate()
            add("tanggal_lahir", "Birth Date", date, 0.84f)
            add("tempat_lahir", "Birth Place", it.replace(DATE_REGEX, "").replace(",", "").trim().filterLetters(), 0.78f)
        }

        add("jenis_kelamin", "Gender", GENDER_REGEX.find(rawText.uppercase(Locale.US))?.value?.normalizeGender(), 0.86f)
        add("gol_darah", "Blood Type", lines.valueAfterAnyLabel("GOL. DARAH", "GOL DARAH")?.take(3)?.filterBloodGroup(), 0.72f)
        add("alamat", "Address", lines.valueAfterLabel("ALAMAT"), 0.78f)
        add("rt_rw", "RT/RW", RT_RW_REGEX.find(rawText)?.value, 0.84f)
        add("kelurahan", "Village/Subdistrict", lines.valueAfterAnyLabel("KEL/DESA", "KEL", "DESA"), 0.75f)
        add("kecamatan", "District", lines.valueAfterLabel("KECAMATAN"), 0.75f)
        add("agama", "Religion", lines.valueAfterLabel("AGAMA")?.normalizeReligion(), 0.8f)
        add("status_perkawinan", "Marital Status", lines.valueAfterLabel("STATUS PERKAWINAN")?.normalizeMaritalStatus(), 0.76f)
        add("pekerjaan", "Occupation", lines.valueAfterLabel("PEKERJAAN"), 0.74f)
        add("kewarganegaraan", "Nationality", lines.valueAfterAnyLabel("KEWARGANEGARAAN", "KEWARGA NEGARAAN")?.normalizeCitizenship(), 0.78f)
        add("berlaku_hingga", "Valid Until", lines.valueAfterAnyLabel("BERLAKU HINGGA", "BERIAKU HINGGA"), 0.72f)

        return result(DocumentType.KTP, rawText, fields.values.toList(), 15)
    }

    private fun parseKk(rawText: String, lines: List<String>): OcrDocumentResult {
        val fields = linkedMapOf<String, OcrField>()
        fun add(key: String, label: String, value: String?, confidence: Float = 0.76f) {
            value.cleanedValue()?.let { fields[key] = OcrField(key, label, it, confidence) }
        }

        add("nomor_kk", "Family Card Number", findLongNumber(rawText, 16), 0.9f)
        add("kepala_keluarga", "Head of Family", lines.valueAfterAnyLabel("NAMA KEPALA KELUARGA", "KEPALA KELUARGA"), 0.8f)
        add("alamat", "Address", lines.valueAfterLabel("ALAMAT"), 0.75f)
        add("rt_rw", "RT/RW", RT_RW_REGEX.find(rawText)?.value, 0.8f)
        add("desa_kelurahan", "Village/Subdistrict", lines.valueAfterAnyLabel("DESA/KELURAHAN", "KELURAHAN", "DESA"), 0.75f)
        add("kecamatan", "District", lines.valueAfterLabel("KECAMATAN"), 0.75f)
        add("kab_kota", "City/Regency", lines.valueAfterAnyLabel("KABUPATEN/KOTA", "KABUPATEN", "KOTA"), 0.75f)
        add("provinsi", "Province", lines.valueAfterLabel("PROVINSI"), 0.75f)
        add("kode_pos", "Postal Code", "\\b\\d{5}\\b".toRegex().find(rawText)?.value, 0.72f)

        return result(DocumentType.KK, rawText, fields.values.toList(), 9)
    }

    private fun parseStnk(rawText: String, lines: List<String>): OcrDocumentResult {
        val fields = linkedMapOf<String, OcrField>()
        fun add(key: String, label: String, value: String?, confidence: Float = 0.76f) {
            value.cleanedValue()?.let { fields[key] = OcrField(key, label, it, confidence) }
        }

        add("nomor_polisi", "License Plate Number", PLATE_REGEX.find(rawText.uppercase(Locale.US))?.value?.compactSpaces(), 0.86f)
        add("nama_pemilik", "Owner Name", lines.valueAfterAnyLabel("NAMA PEMILIK", "NAMA"), 0.76f)
        add("alamat", "Address", lines.valueAfterLabel("ALAMAT"), 0.72f)
        add("merk", "Brand", lines.valueAfterAnyLabel("MERK", "MEREK"), 0.76f)
        add("tipe", "Model/Type", lines.valueAfterLabel("TYPE") ?: lines.valueAfterLabel("TIPE"), 0.74f)
        add("jenis", "Vehicle Type", lines.valueAfterLabel("JENIS"), 0.74f)
        add("tahun", "Year", "\\b(19|20)\\d{2}\\b".toRegex().find(rawText)?.value, 0.72f)
        add("warna", "Color", lines.valueAfterLabel("WARNA"), 0.72f)
        add("nomor_rangka", "Chassis Number", lines.valueAfterAnyLabel("NO RANGKA", "NOMOR RANGKA"), 0.74f)
        add("nomor_mesin", "Engine Number", lines.valueAfterAnyLabel("NO MESIN", "NOMOR MESIN"), 0.74f)

        return result(DocumentType.STNK, rawText, fields.values.toList(), 10)
    }

    private fun parsePlate(rawText: String): OcrDocumentResult {
        val plate = PLATE_REGEX.find(rawText.uppercase(Locale.US).replace("\n", " "))?.value?.compactSpaces()
        val fields = plate?.let { listOf(OcrField("nomor_polisi", "License Plate Number", it, 0.9f)) }.orEmpty()
        return result(DocumentType.PLATE, rawText, fields, 1)
    }

    private fun parseSim(rawText: String, lines: List<String>): OcrDocumentResult {
        val fields = linkedMapOf<String, OcrField>()
        fun add(key: String, label: String, value: String?, confidence: Float = 0.76f) {
            value.cleanedValue()?.let { fields[key] = OcrField(key, label, it, confidence) }
        }

        add("jenis_sim", "License Class", "\\bSIM\\s*[A-C]\\b".toRegex().find(rawText.uppercase(Locale.US))?.value?.compactSpaces(), 0.84f)
        add("nomor_sim", "Driver License Number", findLongNumber(rawText, 12), 0.84f)
        add("nama", "Full Name", lines.valueAfterLabel("NAMA"), 0.78f)
        add("tempat_lahir", "Birth Place", lines.valueAfterAnyLabel("TEMPAT/TGL LAHIR", "TEMPAT LAHIR")?.replace(DATE_REGEX, "")?.replace(",", ""), 0.72f)
        add("tanggal_lahir", "Birth Date", DATE_REGEX.find(rawText)?.value?.normalizeDate(), 0.76f)
        add("alamat", "Address", lines.valueAfterLabel("ALAMAT"), 0.72f)
        add("pekerjaan", "Occupation", lines.valueAfterLabel("PEKERJAAN"), 0.72f)
        add("berlaku_hingga", "Valid Until", lines.valueAfterAnyLabel("BERLAKU SAMPAI", "BERLAKU HINGGA", "VALID UNTIL") ?: DATE_REGEX.findAll(rawText).lastOrNull()?.value?.normalizeDate(), 0.72f)

        return result(DocumentType.SIM, rawText, fields.values.toList(), 8)
    }

    private fun detectType(rawText: String, lines: List<String>): DocumentType {
        val upper = rawText.uppercase(Locale.US)
        return when {
            upper.contains("KARTU KELUARGA") || upper.contains("NOMOR KARTU KELUARGA") -> DocumentType.KK
            upper.contains("SURAT TANDA NOMOR KENDARAAN") || upper.contains("STNK") || upper.contains("NO RANGKA") -> DocumentType.STNK
            upper.contains("SURAT IZIN MENGEMUDI") || Regex("\\bSIM\\s*[A-C]\\b").containsMatchIn(upper) -> DocumentType.SIM
            lines.size <= 4 && PLATE_REGEX.containsMatchIn(upper) -> DocumentType.PLATE
            upper.contains("PROVINSI") || upper.contains("NIK") || upper.contains("KARTU TANDA PENDUDUK") -> DocumentType.KTP
            PLATE_REGEX.containsMatchIn(upper) -> DocumentType.PLATE
            else -> DocumentType.KTP
        }
    }

    private fun result(type: DocumentType, rawText: String, fields: List<OcrField>, expectedFields: Int): OcrDocumentResult {
        val confidence = (fields.size.toFloat() / expectedFields).coerceIn(0f, 1f)
        return OcrDocumentResult(
            documentType = type,
            rawText = rawText,
            fields = fields,
            confidence = confidence,
            isLikelyDocument = fields.isNotEmpty() && confidence >= 0.2f
        )
    }

    private fun findNik(rawText: String): String? = findLongNumber(rawText, 16)

    private fun findLongNumber(rawText: String, length: Int): String? {
        return rawText
            .replace('O', '0')
            .replace('o', '0')
            .replace('I', '1')
            .replace('l', '1')
            .replace('S', '5')
            .let { "\\b\\d{$length}\\b".toRegex().find(it)?.value }
    }

    private fun List<String>.firstStarting(label: String): String? =
        firstOrNull { it.uppercase(Locale.US).startsWith(label.uppercase(Locale.US)) }

    private fun List<String>.valueAfterLabel(label: String): String? =
        valueAfterAnyLabel(label)

    private fun List<String>.valueAfterAnyLabel(vararg labels: String): String? {
        val normalizedLabels = labels.map { it.uppercase(Locale.US) }
        forEachIndexed { index, line ->
            val upper = line.uppercase(Locale.US)
            normalizedLabels.firstOrNull { upper.contains(it) }?.let { label ->
                val inline = line.removeLabel(label)
                if (inline.isNotBlank()) return inline
                return getOrNull(index + 1)
            }
        }
        return null
    }

    private fun String.removeLabel(label: String): String =
        replace(label, "", ignoreCase = true).replace(":", "").trim()

    private fun String?.cleanedValue(): String? = this
        ?.replace(Regex("\\s+"), " ")
        ?.replace(" :", ":")
        ?.trim(' ', ':', '-', '.')
        ?.takeIf { it.isNotBlank() }

    private fun String.cleanLine(): String = replace(Regex("\\s+"), " ").trim()
    private fun String.compactSpaces(): String = replace(Regex("\\s+"), " ").trim()
    private fun String.filterLetters(): String = replace(Regex("[^A-Za-z .'-]"), " ").compactSpaces()
    private fun String.filterBloodGroup(): String = uppercase(Locale.US).replace("8", "B").replace("0", "O").replace("4", "A").take(2)
    private fun String.normalizeDate(): String = replace("/", "-").replace(".", "-")
    private fun String.normalizeGender(): String = if (contains("PEREMPUAN") || contains("WANITA")) "PEREMPUAN" else "LAKI-LAKI"
    private fun String.normalizeCitizenship(): String = if (uppercase(Locale.US).contains("WN")) "WNI" else this
    private fun String.normalizeReligion(): String {
        val upper = uppercase(Locale.US)
        return when {
            upper.contains("ISLAM") -> "ISLAM"
            upper.contains("KRISTEN") -> "KRISTEN"
            upper.contains("KATHOLIK") || upper.contains("KATOLIK") -> "KATHOLIK"
            upper.contains("HINDU") -> "HINDU"
            upper.contains("BUDDHA") || upper.contains("BUDHA") -> "BUDHA"
            upper.contains("KONGHUCU") || upper.contains("KONGHUCHU") -> "KONGHUCHU"
            else -> this
        }
    }

    private fun String.normalizeMaritalStatus(): String {
        val upper = uppercase(Locale.US)
        return when {
            upper.contains("BELUM") -> "BELUM KAWIN"
            upper.contains("CERAI HIDUP") -> "CERAI HIDUP"
            upper.contains("CERAI") -> "CERAI MATI"
            upper.contains("KAWIN") -> "KAWIN"
            else -> this
        }
    }

    private val DATE_REGEX = Regex("\\b\\d{2}[-/.]\\d{2}[-/.]\\d{4}\\b")
    private val RT_RW_REGEX = Regex("\\b\\d{3}\\s*/\\s*\\d{3}\\b")
    private val GENDER_REGEX = Regex("LAKI-LAKI|LAKILAKI|PEREMPUAN|WANITA|PRIA|LAKI")
    private val PLATE_REGEX = Regex("\\b[A-Z]{1,2}\\s?\\d{1,4}\\s?[A-Z]{0,3}\\b")
}
