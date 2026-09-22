package com.example.data.repository

import com.example.data.local.ReceiptDao
import com.example.data.model.Receipt
import kotlinx.coroutines.flow.Flow

class ReceiptRepository(private val receiptDao: ReceiptDao) {
    val allReceipts: Flow<List<Receipt>> = receiptDao.getAllReceipts()

    fun getReceiptById(id: Long): Flow<Receipt?> = receiptDao.getReceiptById(id)

    fun searchReceipts(query: String): Flow<List<Receipt>> = receiptDao.searchReceipts(query)

    fun getReceiptsByCategory(category: String): Flow<List<Receipt>> = receiptDao.getReceiptsByCategory(category)

    suspend fun insertReceipt(receipt: Receipt): Long = receiptDao.insertReceipt(receipt)

    suspend fun updateReceipt(receipt: Receipt) = receiptDao.updateReceipt(receipt)

    suspend fun deleteReceipt(receipt: Receipt) = receiptDao.deleteReceipt(receipt)

    suspend fun deleteReceiptById(id: Long) = receiptDao.deleteReceiptById(id)

    suspend fun populateInitialDataIfEmpty() {
        if (receiptDao.getReceiptCount() == 0) {
            val seedReceipts = listOf(
                Receipt(
                    merchant = "AWS Cloud Services",
                    date = "2026-09-18",
                    totalAmount = 148.50,
                    taxAmount = 12.25,
                    tipAmount = 0.0,
                    category = "Software & SaaS",
                    paymentMethod = "Corporate Visa ••4821",
                    notes = "Monthly server hosting & Gemini Vision API infrastructure",
                    deductiblePercent = 100,
                    lineItemsSummary = "EC2 Cloud Instances ($110.00), S3 Storage ($38.50)",
                    confidenceScore = 99,
                    isDeductible = true,
                    deductionRationale = "100% Tax Deductible business software operating cost (IRS Section 162)."
                ),
                Receipt(
                    merchant = "Bistro & Co Client Lunch",
                    date = "2026-09-15",
                    totalAmount = 86.40,
                    taxAmount = 7.15,
                    tipAmount = 15.00,
                    category = "Meals & Dining",
                    paymentMethod = "Amex Platinum ••9012",
                    notes = "Quarterly client account review and contract renewal",
                    deductiblePercent = 50,
                    lineItemsSummary = "2x Executive Lunch ($56.00), Sparkling Water ($8.25), Dessert ($15.00)",
                    confidenceScore = 96,
                    isDeductible = true,
                    deductionRationale = "50% Tax Deductible under standard IRS rules for ordinary and necessary business meals."
                ),
                Receipt(
                    merchant = "Office Depot Store #412",
                    date = "2026-09-10",
                    totalAmount = 64.99,
                    taxAmount = 5.36,
                    tipAmount = 0.0,
                    category = "Office Supplies",
                    paymentMethod = "Mastercard ••3391",
                    notes = "High-speed document scanner paper & printer ink cartridges",
                    deductiblePercent = 100,
                    lineItemsSummary = "Multipurpose Copy Paper 5-ream ($34.99), Black Ink Cartridge ($30.00)",
                    confidenceScore = 98,
                    isDeductible = true,
                    deductionRationale = "100% Tax Deductible as direct administrative and office expense."
                ),
                Receipt(
                    merchant = "Uber Technologies",
                    date = "2026-09-08",
                    totalAmount = 38.75,
                    taxAmount = 2.45,
                    tipAmount = 5.00,
                    category = "Travel & Lodging",
                    paymentMethod = "Apple Pay ••1109",
                    notes = "Ride from SFO Airport to downtown client tech conference",
                    deductiblePercent = 100,
                    lineItemsSummary = "UberX Airport Trip ($31.30), Toll Surcharge ($5.00), Tip ($5.00)",
                    confidenceScore = 97,
                    isDeductible = true,
                    deductionRationale = "100% Tax Deductible local business transit travel."
                )
            )
            for (receipt in seedReceipts) {
                receiptDao.insertReceipt(receipt)
            }
        }
    }
}
