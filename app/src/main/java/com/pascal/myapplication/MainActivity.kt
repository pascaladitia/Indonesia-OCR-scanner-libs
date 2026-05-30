package com.pascal.myapplication

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pascal.myapplication.ui.theme.OCRKTPComposeTheme
import com.pascal.ocr_libs.analyzer.OcrDocumentScanner
import com.pascal.ocr_libs.analyzer.OcrImageAnalyzer
import com.pascal.ocr_libs.model.DocumentType
import com.pascal.ocr_libs.model.OcrDocumentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val scanner = OcrDocumentScanner()
    private var pendingCameraUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT
            )
        )
        setContent {
            OCRKTPComposeTheme(dynamicColor = false) {
                OcrDemoScreen()
            }
        }
    }

    override fun onDestroy() {
        scanner.close()
        super.onDestroy()
    }

    @Composable
    private fun OcrDemoScreen() {
        var documentType by remember { mutableStateOf(DocumentType.AUTO) }
        var result by remember { mutableStateOf<OcrDocumentResult?>(null) }
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        var liveScanEnabled by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                liveScanEnabled = false
                scanUri(it, documentType) { loadedBitmap, scanResult, error ->
                    bitmap = loadedBitmap
                    result = scanResult
                    errorMessage = error
                    isLoading = false
                }
                isLoading = true
            }
        }

        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = pendingCameraUri
            if (success && uri != null) {
                liveScanEnabled = false
                scanUri(uri, documentType) { loadedBitmap, scanResult, error ->
                    bitmap = loadedBitmap
                    result = scanResult
                    errorMessage = error
                    isLoading = false
                }
                isLoading = true
            } else {
                isLoading = false
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                liveScanEnabled = true
                errorMessage = null
            } else {
                errorMessage = "Camera permission is required for automatic live scanning."
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF6F8FB)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Header(
                        documentType = documentType,
                        onDocumentTypeChanged = {
                            documentType = it
                            result = null
                            bitmap = null
                        }
                    )
                }

                item {
                    ActionPanel(
                        liveScanEnabled = liveScanEnabled,
                        onLiveScanClick = {
                            if (hasCameraPermission()) {
                                liveScanEnabled = !liveScanEnabled
                                errorMessage = null
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onGalleryClick = { galleryLauncher.launch("image/*") },
                        onCameraClick = {
                            val outputUri = createImageUri()
                            pendingCameraUri = outputUri
                            isLoading = true
                            cameraLauncher.launch(outputUri)
                        }
                    )
                }

                if (liveScanEnabled) {
                    item {
                        LiveCameraCard(
                            documentType = documentType,
                            onResult = {
                                result = it
                                bitmap = null
                                errorMessage = null
                            },
                            onError = { errorMessage = it }
                        )
                    }
                }

                if (isLoading) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                errorMessage?.let { message ->
                    item { StatusCard(message = message, isError = true) }
                }

                bitmap?.let { selectedBitmap ->
                    item {
                        ImagePreview(bitmap = selectedBitmap)
                    }
                }

                result?.let { scanResult ->
                    item {
                        ResultSummary(result = scanResult)
                    }
                    items(scanResult.fields, key = { it.key }) { field ->
                        FieldRow(label = field.label, value = field.value)
                    }
                    item {
                        RawTextCard(rawText = scanResult.rawText)
                    }
                } ?: item {
                    StatusCard(message = "Choose an image or enable live scan to read KTP, KK, STNK, license plates, or driver licenses.", isError = false)
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Header(
        documentType: DocumentType,
        onDocumentTypeChanged: (DocumentType) -> Unit
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Indonesia OCR Scanner",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF17212B)
            )
            Text(
                text = "OCR library demo for KTP, KK, STNK, license plates, and Indonesian driver licenses.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF52606D)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocumentType.entries.forEach { type ->
                    FilterChip(
                        selected = documentType == type,
                        onClick = { onDocumentTypeChanged(type) },
                        label = { Text(type.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    }

    @Composable
    private fun ActionPanel(
        liveScanEnabled: Boolean,
        onLiveScanClick: () -> Unit,
        onGalleryClick: () -> Unit,
        onCameraClick: () -> Unit
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLiveScanClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (liveScanEnabled) "Stop Live Scan" else "Automatic Live Scan")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onGalleryClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gallery")
                    }
                    OutlinedButton(
                        onClick = onCameraClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Camera")
                    }
                }
            }
        }
    }

    @Composable
    private fun LiveCameraCard(
        documentType: DocumentType,
        onResult: (OcrDocumentResult) -> Unit,
        onError: (String) -> Unit
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Point the camera at the document",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                ) {
                    key(documentType) {
                        CameraPreview(
                            documentType = documentType,
                            onResult = onResult,
                            onError = onError,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.86f)
                            .aspectRatio(1.58f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                }
            }
        }
    }

    @Composable
    private fun CameraPreview(
        documentType: DocumentType,
        onResult: (OcrDocumentResult) -> Unit,
        onError: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val currentOnResult by rememberUpdatedState(onResult)
        val currentOnError by rememberUpdatedState(onError)
        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

        DisposableEffect(Unit) {
            onDispose { cameraExecutor.shutdown() }
        }

        AndroidCameraPreview(
            modifier = modifier,
            bindCamera = { previewView ->
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener(
                    {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    cameraExecutor,
                                    OcrImageAnalyzer(
                                        documentType = documentType,
                                        onDocumentDetected = { detected ->
                                            ContextCompat.getMainExecutor(context).execute {
                                                currentOnResult(detected)
                                            }
                                        },
                                        onError = { throwable ->
                                            ContextCompat.getMainExecutor(context).execute {
                                                currentOnError(throwable.message ?: "Live scan could not read the frame.")
                                            }
                                        }
                                    )
                                )
                            }

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    },
                    ContextCompat.getMainExecutor(context)
                )
            }
        )
    }

    @Composable
    private fun AndroidCameraPreview(
        modifier: Modifier,
        bindCamera: (PreviewView) -> Unit
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = modifier,
            factory = { context ->
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    bindCamera(this)
                }
            }
        )
    }

    @Composable
    private fun ImagePreview(bitmap: Bitmap) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Dokumen terpilih",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }

    @Composable
    private fun ResultSummary(result: OcrDocumentResult) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF4EF)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.documentType.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF143D2A)
                    )
                    Text(
                        text = "${result.fields.size} fields detected",
                        color = Color(0xFF426052)
                    )
                }
                Text(
                    text = "${(result.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF143D2A)
                )
            }
        }
    }

    @Composable
    private fun FieldRow(label: String, value: String) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label,
                    modifier = Modifier.width(132.dp),
                    color = Color(0xFF52606D),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF17212B),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    @Composable
    private fun RawTextCard(rawText: String) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Raw OCR", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rawText,
                    color = Color(0xFF52606D),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    @Composable
    private fun StatusCard(message: String, isError: Boolean) {
        val background = if (isError) Color(0xFFFFECEB) else Color.White
        val foreground = if (isError) Color(0xFF8A1F17) else Color(0xFF52606D)
        Card(
            colors = CardDefaults.cardColors(containerColor = background),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isError) Color(0xFFD93025) else Color(0xFF2E7D5B))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = message, color = foreground)
            }
        }
    }

    private fun scanUri(
        uri: Uri,
        documentType: DocumentType,
        onDone: (Bitmap?, OcrDocumentResult?, String?) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = loadBitmap(uri)
                val result = scanner.scanBitmap(bitmap, documentType)
                withContext(Dispatchers.Main) {
                    onDone(bitmap, result, null)
                }
            } catch (throwable: Throwable) {
                withContext(Dispatchers.Main) {
                    onDone(null, null, throwable.message ?: "Failed to read the image.")
                    Toast.makeText(this@MainActivity, throwable.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    private fun createImageUri(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ocr-document-${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return requireNotNull(contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}
