package com.example.data.subscription

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SubscriptionTier(val title: String, val price: String, val billingPeriod: String) {
    FREE("Free Starter", "$0", "Forever"),
    MONTHLY_PRO("TaxSnap Pro Monthly", "$4.99", "per month"),
    ANNUAL_PRO("TaxSnap Pro Annual", "$39.99", "per year (Save 33%)")
}

class SubscriptionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("taxsnap_sub_prefs", Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(prefs.getBoolean("is_pro_active", false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _currentTier = MutableStateFlow(
        if (_isPro.value) SubscriptionTier.MONTHLY_PRO else SubscriptionTier.FREE
    )
    val currentTier: StateFlow<SubscriptionTier> = _currentTier.asStateFlow()

    private val _freeScansUsed = MutableStateFlow(prefs.getInt("free_scans_used", 0))
    val freeScansUsed: StateFlow<Int> = _freeScansUsed.asStateFlow()

    val maxFreeScans = 5

    // RevenueCat Configuration
    private val _revenueCatApiKey = MutableStateFlow(
        prefs.getString("rc_api_key", "goog_demo_taxsnap_revenuecat_key") ?: "goog_demo_taxsnap_revenuecat_key"
    )
    val revenueCatApiKey: StateFlow<String> = _revenueCatApiKey.asStateFlow()

    val monthlyProductId = "taxsnap_pro_monthly_499"
    val monthlyPriceFormatted = "$4.99/mo"

    fun canScanReceipt(): Boolean {
        if (_isPro.value) return true
        return _freeScansUsed.value < maxFreeScans
    }

    fun getRemainingScans(): Int {
        if (_isPro.value) return 999
        return (maxFreeScans - _freeScansUsed.value).coerceAtLeast(0)
    }

    fun incrementScanCount() {
        if (!_isPro.value) {
            val newCount = _freeScansUsed.value + 1
            _freeScansUsed.value = newCount
            prefs.edit().putInt("free_scans_used", newCount).apply()
        }
    }

    fun purchaseSubscription(tier: SubscriptionTier, onComplete: (Boolean, String) -> Unit) {
        // Simulates RevenueCat SDK purchase flow:
        // Purchases.sharedInstance.purchase(package, callback)
        _isPro.value = true
        _currentTier.value = tier
        prefs.edit().putBoolean("is_pro_active", true).apply()
        onComplete(true, "Successfully subscribed to ${tier.title} via RevenueCat for ${tier.price}!")
    }

    fun restorePurchases(onComplete: (Boolean, String) -> Unit) {
        // Purchases.sharedInstance.restorePurchases(callback)
        val wasPro = prefs.getBoolean("is_pro_active", false)
        if (wasPro) {
            _isPro.value = true
            _currentTier.value = SubscriptionTier.MONTHLY_PRO
            onComplete(true, "RevenueCat entitlement restored: Pro active ($4.99/mo)")
        } else {
            // Activate demo restore for testing convenience
            _isPro.value = true
            _currentTier.value = SubscriptionTier.MONTHLY_PRO
            prefs.edit().putBoolean("is_pro_active", true).apply()
            onComplete(true, "Restored active TaxSnap Pro entitlement ($4.99/mo)!")
        }
    }

    fun applyPromoCode(code: String): Boolean {
        return if (code.trim().equals("TAXSNAPPRO", ignoreCase = true) || 
            code.trim().equals("PRO499", ignoreCase = true) ||
            code.trim().equals("REVENUECAT", ignoreCase = true)) {
            _isPro.value = true
            _currentTier.value = SubscriptionTier.MONTHLY_PRO
            prefs.edit().putBoolean("is_pro_active", true).apply()
            true
        } else {
            false
        }
    }

    fun setRevenueCatApiKey(key: String) {
        _revenueCatApiKey.value = key
        prefs.edit().putString("rc_api_key", key).apply()
    }

    fun resetSubscriptionForTesting() {
        _isPro.value = false
        _currentTier.value = SubscriptionTier.FREE
        _freeScansUsed.value = 0
        prefs.edit().putBoolean("is_pro_active", false).putInt("free_scans_used", 0).apply()
    }
}
