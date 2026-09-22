package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a scanned receipt with tax deduction categorization.
 */
@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchant: String,
    val date: String, // e.g. "2026-09-21"
    val totalAmount: Double,
    val taxAmount: Double = 0.0,
    val tipAmount: Double = 0.0,
    val category: String = "General Business", // e.g. "Meals & Dining", "Office Supplies", "Travel & Lodging", "Software & SaaS", etc.
    val paymentMethod: String = "Credit Card",
    val notes: String = "",
    val deductiblePercent: Int = 100, // 0 - 100%
    val lineItemsSummary: String = "",
    val imageBase64OrUri: String? = null,
    val confidenceScore: Int = 95,
    val isDeductible: Boolean = true,
    val deductionRationale: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val estimatedTaxSavings: Double
        get() = totalAmount * (deductiblePercent / 100.0)
}

/**
 * Standard tax expense categories with IRS guidance
 */
enum class TaxCategory(val displayName: String, val defaultDeductiblePercent: Int, val iconName: String) {
    MEALS_DINING("Meals & Dining", 50, "Restaurant"),
    OFFICE_SUPPLIES("Office Supplies", 100, "Inventory"),
    TRAVEL_LODGING("Travel & Lodging", 100, "Flight"),
    SOFTWARE_SAAS("Software & SaaS", 100, "Computer"),
    UTILITIES_INTERNET("Utilities & Internet", 100, "Wifi"),
    PROFESSIONAL_SERVICES("Professional Services", 100, "Work"),
    MARKETING_ADS("Marketing & Ads", 100, "Campaign"),
    HEALTHCARE("Medical & Health", 100, "MedicalServices"),
    AUTO_GAS("Vehicle & Gas", 100, "DirectionsCar"),
    GENERAL_BUSINESS("General Business", 100, "ReceiptLong");

    companion object {
        fun fromString(value: String): TaxCategory {
            return entries.firstOrNull { 
                it.displayName.equals(value, ignoreCase = true) || 
                it.name.equals(value.replace(" ", "_").replace("&", "").replace("-", "_"), ignoreCase = true) 
            } ?: GENERAL_BUSINESS
        }
    }
}
