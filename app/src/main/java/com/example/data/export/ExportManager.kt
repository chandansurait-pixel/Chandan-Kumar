package com.example.data.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Receipt
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    /**
     * Generates RFC-4180 compliant CSV content
     */
    fun generateCsv(receipts: List<Receipt>): String {
        val sb = StringBuilder()
        // Header
        sb.append("Receipt ID,Date,Merchant,Category,Total Amount,Tax Amount,Tip Amount,Deductible %,Tax Deduction Amount,Payment Method,Notes,Line Items\n")

        var totalSpent = 0.0
        var totalDeductions = 0.0

        for (r in receipts) {
            totalSpent += r.totalAmount
            val deduction = r.totalAmount * (r.deductiblePercent / 100.0)
            totalDeductions += deduction

            val row = listOf(
                r.id.toString(),
                escapeCsv(r.date),
                escapeCsv(r.merchant),
                escapeCsv(r.category),
                String.format(Locale.US, "%.2f", r.totalAmount),
                String.format(Locale.US, "%.2f", r.taxAmount),
                String.format(Locale.US, "%.2f", r.tipAmount),
                "${r.deductiblePercent}%",
                String.format(Locale.US, "%.2f", deduction),
                escapeCsv(r.paymentMethod),
                escapeCsv(r.notes),
                escapeCsv(r.lineItemsSummary)
            )
            sb.append(row.joinToString(",")).append("\n")
        }

        // Summary row
        sb.append("\n")
        sb.append("TOTALS,,,\"ALL CATEGORIES\",")
        sb.append(String.format(Locale.US, "%.2f", totalSpent)).append(",,,,")
        sb.append(String.format(Locale.US, "%.2f", totalDeductions)).append(",,\n")

        return sb.toString()
    }

    /**
     * Generates Microsoft Excel XML Spreadsheet format (.xml/.xls)
     * Fully compatible with MS Excel, Google Sheets, and LibreOffice Calc.
     */
    fun generateExcelXml(receipts: List<Receipt>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\"?>\n")
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n")
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
        sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
        sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
        sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
        sb.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")

        // Styles
        sb.append(" <Styles>\n")
        sb.append("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n")
        sb.append("   <Alignment ss:Vertical=\"Bottom\"/>\n")
        sb.append("   <Font ss:FontName=\"Calibri\" x:Family=\"Swiss\" ss:Size=\"11\" ss:Color=\"#000000\"/>\n")
        sb.append("  </Style>\n")
        sb.append("  <Style ss:ID=\"Header\">\n")
        sb.append("   <Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Color=\"#FFFFFF\" ss:Bold=\"1\"/>\n")
        sb.append("   <Interior ss:Color=\"#0F172A\" ss:Pattern=\"Solid\"/>\n")
        sb.append("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
        sb.append("  </Style>\n")
        sb.append("  <Style ss:ID=\"Currency\">\n")
        sb.append("   <NumberFormat ss:Format=\"&quot;$&quot;#,##0.00\"/>\n")
        sb.append("   <Alignment ss:Horizontal=\"Right\"/>\n")
        sb.append("  </Style>\n")
        sb.append("  <Style ss:ID=\"TotalRow\">\n")
        sb.append("   <Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Bold=\"1\"/>\n")
        sb.append("   <Interior ss:Color=\"#E2E8F0\" ss:Pattern=\"Solid\"/>\n")
        sb.append("   <NumberFormat ss:Format=\"&quot;$&quot;#,##0.00\"/>\n")
        sb.append("  </Style>\n")
        sb.append(" </Styles>\n")

        sb.append(" <Worksheet ss:Name=\"TaxSnap_Receipts\">\n")
        sb.append("  <Table>\n")
        sb.append("   <Column ss:Width=\"50\"/>\n")  // ID
        sb.append("   <Column ss:Width=\"85\"/>\n")  // Date
        sb.append("   <Column ss:Width=\"140\"/>\n") // Merchant
        sb.append("   <Column ss:Width=\"120\"/>\n") // Category
        sb.append("   <Column ss:Width=\"85\"/>\n")  // Total
        sb.append("   <Column ss:Width=\"75\"/>\n")  // Tax
        sb.append("   <Column ss:Width=\"75\"/>\n")  // Tip
        sb.append("   <Column ss:Width=\"80\"/>\n")  // Deduct %
        sb.append("   <Column ss:Width=\"95\"/>\n")  // Tax Deduction
        sb.append("   <Column ss:Width=\"110\"/>\n") // Payment
        sb.append("   <Column ss:Width=\"160\"/>\n") // Notes
        sb.append("   <Column ss:Width=\"200\"/>\n") // Items

        // Header Row
        sb.append("   <Row ss:StyleID=\"Header\">\n")
        val headers = listOf("ID", "Date", "Merchant", "Category", "Total ($)", "Tax ($)", "Tip ($)", "Deductible %", "Tax Deduction ($)", "Payment Method", "Notes", "Line Items")
        for (h in headers) {
            sb.append("    <Cell><Data ss:Type=\"String\">$h</Data></Cell>\n")
        }
        sb.append("   </Row>\n")

        var grandTotal = 0.0
        var grandDeduction = 0.0

        // Data Rows
        for (r in receipts) {
            grandTotal += r.totalAmount
            val ded = r.totalAmount * (r.deductiblePercent / 100.0)
            grandDeduction += ded

            sb.append("   <Row>\n")
            sb.append("    <Cell><Data ss:Type=\"Number\">${r.id}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.date)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.merchant)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.category)}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Currency\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", r.totalAmount)}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Currency\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", r.taxAmount)}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Currency\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", r.tipAmount)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${r.deductiblePercent}%</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Currency\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", ded)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.paymentMethod)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.notes)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.lineItemsSummary)}</Data></Cell>\n")
            sb.append("   </Row>\n")
        }

        // Totals Row
        sb.append("   <Row ss:StyleID=\"TotalRow\">\n")
        sb.append("    <Cell><Data ss:Type=\"String\">TOTALS</Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\"></Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\"></Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\">All Categories</Data></Cell>\n")
        sb.append("    <Cell ss:StyleID=\"TotalRow\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", grandTotal)}</Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\"></Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\"></Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\"></Data></Cell>\n")
        sb.append("    <Cell ss:StyleID=\"TotalRow\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", grandDeduction)}</Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\"></Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\"></Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\"></Data></Cell>\n")
        sb.append("   </Row>\n")

        sb.append("  </Table>\n")
        sb.append(" </Worksheet>\n")
        sb.append("</Workbook>\n")

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        var clean = value.replace("\r", "").replace("\n", " ")
        if (clean.contains(",") || clean.contains("\"") || clean.contains(";")) {
            clean = clean.replace("\"", "\"\"")
            return "\"$clean\""
        }
        return clean
    }

    private fun escapeXml(value: String): String {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Copies CSV to system clipboard
     */
    fun copyToClipboard(context: Context, text: String, label: String = "TaxSnap Receipts CSV") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Writes to a cache file and shares via Android Share Sheet
     */
    fun shareExportFile(context: Context, content: String, isExcel: Boolean) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = if (isExcel) "xls" else "csv"
        val mimeType = if (isExcel) "application/vnd.ms-excel" else "text/csv"
        val fileName = "taxsnap_receipts_$timeStamp.$extension"

        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, fileName)
        FileWriter(file).use { writer ->
            writer.write(content)
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "TaxSnap AI Receipts Export ($fileName)")
            putExtra(Intent.EXTRA_TEXT, "Exported ${if (isExcel) "Excel spreadsheet" else "CSV"} of business receipts from TaxSnap AI.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Receipts Export"))
    }
}
