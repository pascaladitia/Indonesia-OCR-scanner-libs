# Indonesia OCR Scanner

Reusable Android OCR library for Indonesian documents: KTP, KK, STNK, license plates, and Indonesian driver licenses. The production library lives in `ocr_libs`; the `app` module is only a Jetpack Compose demo.

The project is structured with clean architecture boundaries:

```text
ocr_libs
├── analyzer        Public scanner and CameraX analyzer APIs
├── domain         Engine contracts, parser contracts, use cases, pure models
├── data/mlkit     Default on-device text recognition engine
├── data/tensorflow TensorFlow Lite OCR engine bridge
├── model          Public result models
└── parser         Indonesian document parsers
```

## Highlights

- Supports KTP, KK, STNK, license plate, and SIM document parsing.
- Supports `AUTO` document type detection.
- Scans from `Bitmap`.
- Supports automatic live scan with CameraX `ImageAnalysis`.
- Uses a clean `OcrEngine` abstraction, so OCR engines can be swapped.
- Includes a TensorFlow Lite engine bridge for custom `.tflite` OCR models.
- Bundles TensorFlow Lite OCR sample models under `ocr_libs/src/main/assets/models/`.
- Ships with ML Kit as the default ready-to-run OCR engine.
- Returns structured fields, confidence score, raw text, and document type.
- Includes a polished Compose demo app.

## Important Accuracy Note

No mobile OCR library can guarantee perfect results on every phone and every blurry image. Accuracy depends on camera quality, motion blur, glare, crop, lighting, font variation, and document damage.

This library is designed to be production-friendly: OCR runs on-device, document parsing is isolated, and TensorFlow Lite can be plugged in when you have a trained OCR model.

## Modules

```text
app       Demo application
ocr_libs  Reusable Android library
```

Public package:

```kotlin
com.pascal.ocr_libs
```

## Installation

### Local Project

Add the library module in `settings.gradle.kts`:

```kotlin
include(":ocr_libs")
```

Add the dependency in your app module:

```kotlin
dependencies {
    implementation(project(":ocr_libs"))
}
```

### GitHub / JitPack

After publishing the repository and creating a release tag such as `v1.0.0`, add JitPack:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Dependency:

```kotlin
dependencies {
    implementation("com.github.pascaladitia:indonesia-OCR-scanner-libs:v1.0.0")
}
```

The library module is configured with Maven publishing metadata:

```text
groupId    com.github.pascal
artifactId ocr-libs
version    1.0.0
```

Change these values before publishing under your own GitHub account.

## Permissions

For live scan and camera capture:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

For gallery access on older Android versions:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

## Basic Bitmap Usage

```kotlin
val scanner = OcrDocumentScanner()

val result = scanner.scanBitmap(
    bitmap = bitmap,
    documentType = DocumentType.AUTO
)

result.fields.forEach { field ->
    Log.d("OCR", "${field.label}: ${field.value}")
}
```

Manual document type:

```kotlin
scanner.scanBitmap(bitmap, DocumentType.KTP)
scanner.scanBitmap(bitmap, DocumentType.KK)
scanner.scanBitmap(bitmap, DocumentType.STNK)
scanner.scanBitmap(bitmap, DocumentType.PLATE)
scanner.scanBitmap(bitmap, DocumentType.SIM)
```

## CameraX Live Scan

```kotlin
val analysis = ImageAnalysis.Builder()
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()

analysis.setAnalyzer(
    cameraExecutor,
    OcrImageAnalyzer(
        documentType = DocumentType.AUTO,
        minConfidenceToAutoCapture = 0.35f,
        onDocumentDetected = { result ->
            // Update UI on the main thread.
        },
        onError = { throwable ->
            // Show error message.
        }
    )
)
```

`OcrImageAnalyzer` throttles OCR reads to keep performance reasonable on lower-end devices. When a likely document is detected and the confidence passes the threshold, `onDocumentDetected` is called.

## TensorFlow Lite Usage

TensorFlow Lite is included as an engine bridge. A real OCR `.tflite` model must define its own input and output contract, so the library exposes an adapter interface:

To keep Android 16 / 16 KB page-size compatibility, the demo app does not package TensorFlow Lite native runtime libraries by default. The `.tflite` model files are bundled in the library, and apps that want to execute them can add a TensorFlow Lite runtime version that is 16 KB page-size compatible.

Bundled model assets:

```text
models/east_text_detector.tflite
models/keras_ocr_recognizer.tflite
```

These are the TensorFlow Lite OCR example detector and recognizer models. The official TensorFlow OCR reference describes the pipeline as two stages: text detection first, then text recognition on cropped text regions.

```kotlin
class MyTensorFlowOcrAdapter : TensorFlowLiteOcrAdapter {
    override fun recognize(
        interpreter: TensorFlowLiteInterpreter,
        bitmap: Bitmap,
        rotationDegrees: Int
    ): OcrText {
        // 1. Resize and normalize bitmap according to your model.
        // 2. Run interpreter.run(...) or runForMultipleInputsOutputs(...).
        // 3. Decode model output into raw text and lines.
        return OcrText(
            rawText = "...",
            lines = listOf("...")
        )
    }
}
```

Create a scanner with TensorFlow Lite:

```kotlin
val modelByteBuffer = TensorFlowLiteModelLoader.fromAsset(
    context = context,
    assetPath = TensorFlowLiteModelLoader.KERAS_OCR_RECOGNIZER_ASSET
)

val scanner = OcrDocumentScanner(
    ocrEngine = TensorFlowLiteOcrEngine(
        modelBuffer = modelByteBuffer,
        adapter = MyTensorFlowOcrAdapter()
    )
)

val result = scanner.scanBitmap(bitmap, DocumentType.AUTO)
```

This design keeps the parser independent from the OCR engine. You can use ML Kit, TensorFlow Lite, or any custom engine that implements `OcrEngine`.

For a full TensorFlow OCR pipeline, use both bundled models:

```kotlin
val detector = TensorFlowLiteModelLoader.bundledTextDetector(context)
val recognizer = TensorFlowLiteModelLoader.bundledTextRecognizer(context)
```

The library provides the model files and interpreter bridge; production-quality TensorFlow OCR still requires pipeline code for detection decoding, perspective correction, crop extraction, recognizer decoding, and text line ordering. ML Kit remains the default engine because it already handles that full OCR pipeline on-device.

## Result Model

```kotlin
data class OcrDocumentResult(
    val documentType: DocumentType,
    val rawText: String,
    val fields: List<OcrField>,
    val confidence: Float,
    val isLikelyDocument: Boolean,
    val errorMessage: String?
)
```

Get a field by key:

```kotlin
val nik = result.value("nik")
val fullName = result.value("nama")
val plateNumber = result.value("nomor_polisi")
```

Export JSON:

```kotlin
val json = result.toJson()
```

## Supported Fields

KTP:

- `nik`
- `nama`
- `tempat_lahir`
- `tanggal_lahir`
- `jenis_kelamin`
- `gol_darah`
- `alamat`
- `rt_rw`
- `kelurahan`
- `kecamatan`
- `agama`
- `status_perkawinan`
- `pekerjaan`
- `kewarganegaraan`
- `berlaku_hingga`
- `provinsi`
- `kab_kota`

KK:

- `nomor_kk`
- `kepala_keluarga`
- `alamat`
- `rt_rw`
- `desa_kelurahan`
- `kecamatan`
- `kab_kota`
- `provinsi`
- `kode_pos`

STNK:

- `nomor_polisi`
- `nama_pemilik`
- `alamat`
- `merk`
- `tipe`
- `jenis`
- `tahun`
- `warna`
- `nomor_rangka`
- `nomor_mesin`

License plate:

- `nomor_polisi`

SIM:

- `jenis_sim`
- `nomor_sim`
- `nama`
- `tempat_lahir`
- `tanggal_lahir`
- `alamat`
- `pekerjaan`
- `berlaku_hingga`

## Demo App

Run:

```bash
./gradlew :app:assembleDebug
```

Install:

```bash
./gradlew :app:installDebug
```

Demo flow:

1. Select a document type or `Auto`.
2. Use `Automatic Live Scan`, `Gallery`, or `Camera`.
3. The parsed document fields appear below the preview.

## Accuracy Tips

- Use bright, even lighting.
- Avoid plastic glare and reflections.
- Keep the document inside the camera guide.
- Avoid extreme tilt and motion blur.
- Use manual document type if auto-detection is wrong.
- Store `rawText` while debugging parser behavior for real-world documents.

## Privacy

The default OCR flow runs on-device. It does not upload document images or OCR text to a server. If you add a cloud fallback or LLM-based parser, get explicit user consent and document the data handling policy clearly.

## Release Checklist

- Rename the repository to a public-friendly name, for example `Indonesia-OCR-Scanner`.
- Update Maven `groupId`, `artifactId`, and version.
- Add screenshots of the demo app.
- Add a license file, such as Apache-2.0 or MIT.
- Use only dummy sample images, never real identity documents.
- Run `./gradlew :app:assembleDebug :ocr_libs:assembleRelease` before release.

## License

Add a `LICENSE` file before publishing. Apache-2.0 or MIT is recommended for an open-source Android library.
