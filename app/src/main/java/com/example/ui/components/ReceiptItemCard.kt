package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Receipt
import com.example.data.model.TaxCategory
import com.example.ui.theme.CatMeals
import com.example.ui.theme.CatMedical
import com.example.ui.theme.CatOffice
import com.example.ui.theme.CatSoftware
import com.example.ui.theme.CatTravel
import com.example.ui.theme.CatUtilities
import com.example.ui.theme.EmeraldLight
import java.util.Locale

@Composable
fun ReceiptItemCard(
    receipt: Receipt,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryEnum = TaxCategory.fromString(receipt.category)
    val (icon, badgeColor) = getCategoryIconAndColor(categoryEnum)
    val taxDeductionAmount = receipt.totalAmount * (receipt.deductiblePercent / 100.0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("receipt_item_${receipt.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = receipt.category,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Merchant & Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = receipt.merchant,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = receipt.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = receipt.category,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = badgeColor,
                        maxLines = 1
                    )
                }

                if (receipt.lineItemsSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = receipt.lineItemsSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount & Tax Deduction Pill
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format(Locale.US, "$%.2f", receipt.totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Deductible Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldLight.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${receipt.deductiblePercent}% Save",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = EmeraldLight
                        )
                    }
                }

                Text(
                    text = String.format(Locale.US, "+$%.2f tax ded.", taxDeductionAmount),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }
        }
    }
}

fun getCategoryIconAndColor(category: TaxCategory): Pair<ImageVector, Color> {
    return when (category) {
        TaxCategory.MEALS_DINING -> Pair(Icons.Default.Restaurant, CatMeals)
        TaxCategory.OFFICE_SUPPLIES -> Pair(Icons.Default.Inventory, CatOffice)
        TaxCategory.TRAVEL_LODGING -> Pair(Icons.Default.Flight, CatTravel)
        TaxCategory.SOFTWARE_SAAS -> Pair(Icons.Default.Computer, CatSoftware)
        TaxCategory.UTILITIES_INTERNET -> Pair(Icons.Default.Wifi, CatUtilities)
        TaxCategory.PROFESSIONAL_SERVICES -> Pair(Icons.Default.Work, Color(0xFF6366F1))
        TaxCategory.MARKETING_ADS -> Pair(Icons.Default.Campaign, Color(0xFFEC4899))
        TaxCategory.HEALTHCARE -> Pair(Icons.Default.MedicalServices, CatMedical)
        TaxCategory.AUTO_GAS -> Pair(Icons.Default.DirectionsCar, Color(0xFF14B8A6))
        TaxCategory.GENERAL_BUSINESS -> Pair(Icons.Default.ReceiptLong, Color(0xFF94A3B8))
    }
}
