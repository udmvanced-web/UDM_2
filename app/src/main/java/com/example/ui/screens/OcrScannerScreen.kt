package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PaymentPartial
import com.example.ui.viewmodel.RepairViewModel
import com.example.util.ScannerAudioHelper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun OcrScannerScreen(
    viewModel: RepairViewModel,
    onNavigateBack: () -> Unit,
    onRepairFound: (Long) -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var detectedJobNumber by remember { mutableStateOf("") }
    var notFoundDialogVisible by remember { mutableStateOf(false) }
    var isScanningActive by remember { mutableStateOf(true) }
    val isCheckingDatabase = remember { AtomicBoolean(false) }

    val audioHelper = remember { ScannerAudioHelper(context) }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            textRecognizer.close()
            audioHelper.release()
        }
    }

    fun extractAndNormalizeJobNumber(rawText: String): String? {
        // Looks for sequences of digits (1 to 4 digits), or "JOB NO: 0014"
        val regex = Regex("""(?i)(?:job\s*(?:no|num)?[:.\s#]*)?(\d{1,4})\b""")
        val match = regex.find(rawText)
        if (match != null) {
            val digits = match.groupValues[1]
            val num = digits.toIntOrNull()
            if (num != null && num > 0) {
                return String.format(Locale.US, "%04d", num)
            }
        }
        return null
    }

    fun executeSearch(jobNumberInput: String) {
        val normalized = viewModel.repository.normalizeJobNumber(jobNumberInput)
        if (normalized.isNotBlank()) {
            coroutineScope.launch {
                val repair = viewModel.findRepairByJobNumber(normalized)
                if (repair != null) {
                    audioHelper.playConfirmationBeep()
                    onRepairFound(repair.id)
                } else {
                    notFoundDialogVisible = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090E1A))
    ) {
        // CAMERA PREVIEW OR FALLBACK
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        @OptIn(ExperimentalGetImage::class)
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && isScanningActive && !isCheckingDatabase.get()) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        textRecognizer.process(image)
                                            .addOnSuccessListener { visionText ->
                                                val found = extractAndNormalizeJobNumber(visionText.text)
                                                if (found != null && isScanningActive && isCheckingDatabase.compareAndSet(false, true)) {
                                                    coroutineScope.launch {
                                                        try {
                                                            val repair = viewModel.findRepairByJobNumber(found)
                                                            if (repair != null) {
                                                                // Valid confirmed existing job:
                                                                // 1. Immediately lock scanning so no subsequent frames or duplicate beeps run
                                                                isScanningActive = false
                                                                detectedJobNumber = found
                                                                // 2. Play ONE short confirmation beep/tick
                                                                audioHelper.playConfirmationBeep()
                                                                // 3. Open matching Job Details
                                                                onRepairFound(repair.id)
                                                            } else {
                                                                // Non-existing / invalid Job:
                                                                // Do NOT play beep!
                                                                isScanningActive = false
                                                                detectedJobNumber = found
                                                                notFoundDialogVisible = true
                                                            }
                                                        } finally {
                                                            isCheckingDatabase.set(false)
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("OcrScanner", "Camera bind error", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090E1A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                    Icon(
                        Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Camera permission is required to scan handwritten job stickers.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Text("Grant Camera Permission", color = Color(0xFF090E1A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // TOP CONTROLS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("ocr_back_btn")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "JOB TAG OCR SCANNER",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(onClick = {
                    detectedJobNumber = ""
                    isScanningActive = true
                    isCheckingDatabase.set(false)
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Scan Again", tint = Color.White)
                }
            }
        }

        // SCANNING FRAME RETICLE
        Box(
            modifier = Modifier
                .size(260.dp, 160.dp)
                .align(Alignment.Center)
                .border(2.dp, CyanAccent, RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            Text(
                text = "Point camera at 4-digit sticker\n(e.g. 0014)",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }

        // BOTTOM DETECTION & MANUAL CORRECTION PANEL
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "DETECTED JOB NUMBER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyanAccent,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Verify or manually correct handwritten digits below:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = detectedJobNumber,
                        onValueChange = { detectedJobNumber = it },
                        placeholder = { Text("0000", fontFamily = FontFamily.Monospace) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            letterSpacing = 4.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ocr_detected_number_input")
                    )

                    Button(
                        onClick = { executeSearch(detectedJobNumber) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("ocr_search_btn")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF090E1A))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search", color = Color(0xFF090E1A), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        detectedJobNumber = ""
                        isScanningActive = true
                        isCheckingDatabase.set(false)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan Again", color = CyanAccent, fontSize = 13.sp)
                    }

                    Text(
                        text = "Auto-normalizes to 4 digits",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // DIALOG: JOB NUMBER NOT FOUND
    if (notFoundDialogVisible) {
        val normalized = viewModel.repository.normalizeJobNumber(detectedJobNumber)
        AlertDialog(
            onDismissRequest = {
                notFoundDialogVisible = false
                isCheckingDatabase.set(false)
            },
            title = { Text("Job Number Not Found") },
            text = {
                Text("No repair job found with Job Number #$normalized. Please check the sticker number or enter it manually.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        notFoundDialogVisible = false
                        isScanningActive = true
                        isCheckingDatabase.set(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Scan Again", color = Color(0xFF090E1A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    notFoundDialogVisible = false
                    isCheckingDatabase.set(false)
                }) {
                    Text("Enter Manually")
                }
            }
        )
    }
}
