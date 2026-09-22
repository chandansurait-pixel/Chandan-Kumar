package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiVisionService
import com.example.data.api.ReceiptScanResult
import com.example.data.export.ExportManager
import com.example.data.local.AppDatabase
import com.example.data.model.Receipt
import com.example.data.repository.ReceiptRepository
import com.example.data.subscription.SubscriptionManager
import com.example.data.subscription.SubscriptionTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaxSnapViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ReceiptRepository(database.receiptDao())
    val geminiService = GeminiVisionService()
    val subscriptionManager = SubscriptionManager(application)

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
    }

    // Receipt data
    val allReceipts: StateFlow<List<Receipt>> = repository.allReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String?>(null)

    // Filtered receipts
    val filteredReceipts: StateFlow<List<Receipt>> = combine(
        allReceipts,
        searchQuery,
        selectedCategoryFilter
    ) { receipts, query, category ->
        receipts.filter { receipt ->
            val matchesQuery = query.isBlank() ||
                    receipt.merchant.contains(query, ignoreCase = true) ||
                    receipt.category.contains(query, ignoreCase = true) ||
                    receipt.notes.contains(query, ignoreCase = true)

            val matchesCategory = category == null || receipt.category.equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Metrics
    val totalExpense: StateFlow<Double> = allReceipts.combine(filteredReceipts) { all, filtered ->
        all.sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDeduction: StateFlow<Double> = allReceipts.combine(filteredReceipts) { all, _ ->
        all.sumOf { it.totalAmount * (it.deductiblePercent / 100.0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Subscription & Paywall
    val isPro: StateFlow<Boolean> = subscriptionManager.isPro
    val freeScansUsed: StateFlow<Int> = subscriptionManager.freeScansUsed
    val showPaywallModal = MutableStateFlow(false)

    // Scanning state
    val isScanning = MutableStateFlow(false)
    val scanStepText = MutableStateFlow("Ready to scan")
    val currentScanResult = MutableStateFlow<ReceiptScanResult?>(null)
    val capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val scanError = MutableStateFlow<String?>(null)

    // Dialogs & Navigation
    val showApiKeyDialog = MutableStateFlow(false)
    val selectedReceiptForDetail = MutableStateFlow<Receipt?>(null)
    val activeNavTab = MutableStateFlow("home") // "home", "scan", "export", "subscription"
    val toastMessage = MutableStateFlow<String?>(null)

    fun onScanReceiptClicked() {
        if (!subscriptionManager.canScanReceipt()) {
            showPaywallModal.value = true
        } else {
            activeNavTab.value = "scan"
        }
    }

    fun processReceiptImage(bitmap: Bitmap, presetName: String? = null) {
        if (!subscriptionManager.canScanReceipt()) {
            showPaywallModal.value = true
            return
        }

        viewModelScope.launch {
            isScanning.value = true
            scanError.value = null
            capturedBitmap.value = bitmap
            currentScanResult.value = null

            scanStepText.value = "Connecting to Gemini Vision AI..."
            delay(400)
            scanStepText.value = "Analyzing receipt OCR & itemizing amounts..."
            delay(500)
            scanStepText.value = "Categorizing tax deductions & IRS compliance..."

            val result = geminiService.analyzeReceipt(bitmap, presetName)
            isScanning.value = false

            result.onSuccess { scanResult ->
                currentScanResult.value = scanResult
                subscriptionManager.incrementScanCount()
                scanStepText.value = "Analysis complete!"
            }.onFailure { err ->
                scanError.value = err.message ?: "Failed to scan receipt"
                scanStepText.value = "Scan failed"
            }
        }
    }

    fun saveScannedReceipt(
        merchant: String,
        date: String,
        totalAmount: Double,
        taxAmount: Double,
        tipAmount: Double,
        category: String,
        paymentMethod: String,
        notes: String,
        deductiblePercent: Int,
        lineItems: String,
        deductionRationale: String
    ) {
        viewModelScope.launch {
            val receipt = Receipt(
                merchant = merchant.trim(),
                date = date.trim(),
                totalAmount = totalAmount,
                taxAmount = taxAmount,
                tipAmount = tipAmount,
                category = category.trim(),
                paymentMethod = paymentMethod.trim(),
                notes = notes.trim(),
                deductiblePercent = deductiblePercent,
                lineItemsSummary = lineItems.trim(),
                deductionRationale = deductionRationale.trim(),
                confidenceScore = 98,
                createdAt = System.currentTimeMillis()
            )
            repository.insertReceipt(receipt)
            currentScanResult.value = null
            capturedBitmap.value = null
            activeNavTab.value = "home"
            toastMessage.value = "Receipt from $merchant saved successfully!"
        }
    }

    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch {
            repository.deleteReceipt(receipt)
            if (selectedReceiptForDetail.value?.id == receipt.id) {
                selectedReceiptForDetail.value = null
            }
            toastMessage.value = "Deleted receipt from ${receipt.merchant}"
        }
    }

    fun updateReceipt(receipt: Receipt) {
        viewModelScope.launch {
            repository.updateReceipt(receipt)
            selectedReceiptForDetail.value = receipt
            toastMessage.value = "Receipt updated"
        }
    }

    fun clearToast() {
        toastMessage.value = null
    }

    fun purchaseMonthlyPro(onResult: (String) -> Unit) {
        subscriptionManager.purchaseSubscription(SubscriptionTier.MONTHLY_PRO) { success, msg ->
            if (success) {
                showPaywallModal.value = false
                onResult(msg)
            }
        }
    }

    fun restoreSubscription(onResult: (String) -> Unit) {
        subscriptionManager.restorePurchases { success, msg ->
            if (success) {
                showPaywallModal.value = false
                onResult(msg)
            }
        }
    }
}
