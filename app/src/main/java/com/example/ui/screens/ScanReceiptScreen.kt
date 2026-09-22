package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.ReceiptScanResult
import com.example.data.model.TaxCategory
import com.example.ui.components.ScanAnimationBox
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.TaxSnapViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    viewModel: TaxSnapViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val scanStepText by viewModel.scanStepText.collectAsState()
    val scanResult by viewModel.currentScanResult.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.processReceiptImage(bitmap, "Camera Receipt Capture")
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.processReceiptImage(bitmap, "Gallery Receipt")
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Receipt Scanner",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Powered by Gemini Vision (gemini-2.5-flash)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isScanning) {
                // Scanning Animation View
                ScanAnimationBox(statusText = scanStepText)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "TaxSnap AI is extracting line items, calculating sales tax & evaluating IRS deductions...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (scanResult != null) {
                // Extraction Review & Save Form
                ReceiptReviewForm(
                    scanResult = scanResult!!,
                    bitmap = capturedBitmap,
                    onSave = { merchant, date, total, tax, tip, category, payment, notes, deductPercent, items, rationale ->
                        viewModel.saveScannedReceipt(
                            merchant = merchant,
                            date = date,
                            totalAmount = total,
                            taxAmount = tax,
                            tipAmount = tip,
                            category = category,
                            paymentMethod = payment,
                            notes = notes,
                            deductiblePercent = deductPercent,
                            lineItems = items,
                            deductionRationale = rationale
                        )
                    },
                    onRescan = {
                        viewModel.currentScanResult.value = null
                        viewModel.capturedBitmap.value = null
                    }
                )
            } else {
                // Capture / Sample Selection View
                CapturePromptCard(
                    onCameraLaunch = { cameraLauncher.launch(null) },
                    onGalleryLaunch = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPresetSelect = { presetName ->
                        val sampleBitmap = createSyntheticReceiptBitmap(presetName)
                        viewModel.processReceiptImage(sampleBitmap, presetName)
                    }
                )

                if (scanError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = scanError ?: "Error processing receipt",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CapturePromptCard(
    onCameraLaunch: () -> Unit,
    onGalleryLaunch: () -> Unit,
    onPresetSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Capture or Pick Receipt",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Take a photo of any printed receipt, SaaS invoice, or restaurant bill.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Camera Button
            Button(
                onClick = onCameraLaunch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("camera_capture_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF003822))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Take Photo with Camera",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003822)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pick From Gallery Button
            OutlinedButton(
                onClick = onGalleryLaunch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("gallery_pick_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Collections, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select from Photos & Files")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Try Sample Receipts Presets
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AmberGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Instant 1-Tap Sample Presets",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Test Gemini Vision AI OCR immediately without physical receipts:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SamplePresetChip(
                        title = "☕ Client Bistro Lunch",
                        desc = "Meals & Dining • 50% IRS Deductible",
                        onClick = { onPresetSelect("Coffee & Bistro Meeting") }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SamplePresetChip(
                        title = "💻 Best Buy Hardware & Cables",
                        desc = "Office Equipment • 100% Tax Deductible",
                        onClick = { onPresetSelect("Tech Hardware & Cables") }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SamplePresetChip(
                        title = "🚖 Yellow Cab Airport Transit",
                        desc = "Travel & Transit • 100% Business Travel",
                        onClick = { onPresetSelect("Airport Cab Transit") }
                    )
                }
            }
        }
    }
}

@Composable
fun SamplePresetChip(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = CyanAccent.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Scan AI",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyanAccent,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewForm(
    scanResult: ReceiptScanResult,
    bitmap: Bitmap?,
    onSave: (
        merchant: String,
        date: String,
        total: Double,
        tax: Double,
        tip: Double,
        category: String,
        payment: String,
        notes: String,
        deductPercent: Int,
        items: String,
        rationale: String
    ) -> Unit,
    onRescan: () -> Unit
) {
    var merchant by remember { mutableStateOf(scanResult.merchant) }
    var date by remember { mutableStateOf(scanResult.date) }
    var totalText by remember { mutableStateOf(String.format(Locale.US, "%.2f", scanResult.totalAmount)) }
    var taxText by remember { mutableStateOf(String.format(Locale.US, "%.2f", scanResult.taxAmount)) }
    var tipText by remember { mutableStateOf(String.format(Locale.US, "%.2f", scanResult.tipAmount)) }
    var category by remember { mutableStateOf(scanResult.category) }
    var paymentMethod by remember { mutableStateOf(scanResult.paymentMethod) }
    var notes by remember { mutableStateOf(scanResult.notes) }
    var deductiblePercent by remember { mutableIntStateOf(scanResult.deductiblePercent) }
    var lineItems by remember { mutableStateOf(scanResult.lineItemsSummary) }
    var rationale by remember { mutableStateOf(scanResult.deductionRationale) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Success Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldLight.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Extracted (${scanResult.confidenceScore}% confidence)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldLight
                        )
                    }
                }

                TextButton(onClick = onRescan) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-scan")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scanned receipt preview thumbnail if present
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Receipt Image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Fields
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Vendor Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_merchant"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = it },
                    label = { Text("Total Amount ($)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_total"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_date"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = taxText,
                    onValueChange = { taxText = it },
                    label = { Text("Sales Tax ($)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = tipText,
                    onValueChange = { tipText = it },
                    label = { Text("Tip / Gratuity ($)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tax Category Selection
            Text(
                text = "Tax Category",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            val categories = listOf(
                "Meals & Dining", "Office Supplies", "Travel & Lodging",
                "Software & SaaS", "Utilities & Internet", "General Business"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category.equals(cat, ignoreCase = true),
                        onClick = {
                            category = cat
                            if (cat == "Meals & Dining") {
                                deductiblePercent = 50
                                rationale = "50% Tax Deductible under IRS business meal rules."
                            } else {
                                deductiblePercent = 100
                                rationale = "100% Tax Deductible business expense."
                            }
                        },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldLight,
                            selectedLabelColor = Color(0xFF003822)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // IRS Deductibility Slider & Info
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IRS Deductible %",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$deductiblePercent% Deductible",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = EmeraldLight
                        )
                    }

                    Slider(
                        value = deductiblePercent.toFloat(),
                        onValueChange = { deductiblePercent = it.toInt() },
                        valueRange = 0f..100f,
                        steps = 9
                    )

                    Text(
                        text = rationale,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = lineItems,
                onValueChange = { lineItems = it },
                label = { Text("Extracted Line Items") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Business Purpose / Notes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    val parsedTotal = totalText.toDoubleOrNull() ?: 0.0
                    val parsedTax = taxText.toDoubleOrNull() ?: 0.0
                    val parsedTip = tipText.toDoubleOrNull() ?: 0.0
                    onSave(
                        merchant,
                        date,
                        parsedTotal,
                        parsedTax,
                        parsedTip,
                        category,
                        paymentMethod,
                        notes,
                        deductiblePercent,
                        lineItems,
                        rationale
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_receipt_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF003822))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Receipt to TaxSnap",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003822)
                    )
                )
            }
        }
    }
}

/**
 * Creates a synthetic image bitmap with receipt text for visual testing
 */
fun createSyntheticReceiptBitmap(title: String): Bitmap {
    val width = 400
    val height = 600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background paper
    val bgPaint = Paint().apply { color = android.graphics.Color.WHITE }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Border / Paper texture
    val borderPaint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRect(8f, 8f, (width - 8).toFloat(), (height - 8).toFloat(), borderPaint)

    // Text Header
    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 24f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("TAXSNAP AI RECEIPT", (width / 2).toFloat(), 50f, textPaint)

    textPaint.textSize = 18f
    textPaint.isFakeBoldText = false
    canvas.drawText(title, (width / 2).toFloat(), 85f, textPaint)

    // Divider
    canvas.drawLine(30f, 110f, (width - 30).toFloat(), 110f, borderPaint)

    // Items
    textPaint.textAlign = Paint.Align.LEFT
    textPaint.textSize = 16f
    canvas.drawText("Date: 2026-09-21", 40f, 150f, textPaint)
    canvas.drawText("Item 1: Business Expense", 40f, 200f, textPaint)
    canvas.drawText("Item 2: Sales Tax 8.25%", 40f, 240f, textPaint)
    canvas.drawText("Payment: Corporate Card", 40f, 280f, textPaint)

    canvas.drawLine(30f, 320f, (width - 30).toFloat(), 320f, borderPaint)

    textPaint.textSize = 22f
    textPaint.isFakeBoldText = true
    canvas.drawText("TOTAL AMOUNT", 40f, 370f, textPaint)

    return bitmap
}
