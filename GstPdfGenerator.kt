package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.local.entity.SettingsEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GstPdfGenerator {

    private fun getPdfDirectory(context: Context): File {
        val dir = File(context.cacheDir, "pdfs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Generates a comprehensive GST Sales Register (GSTR-1) PDF.
     * Contains: Invoice No, Date, Party Name, DL Number, GSTIN, Amount, CGST, SGST, and Total GST.
     */
    fun generateSellingGstPdf(
        context: Context,
        settings: SettingsEntity,
        invoices: List<InvoiceEntity>,
        periodText: String = "All Records"
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 842  // A4 Landscape for rich tabular display
        val pageHeight = 595
        val margin = 30f

        val titlePaint = Paint().apply {
            color = Color.rgb(2, 136, 209) // Primary Blue
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val headerTablePaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(30, 136, 229) // Header Blue
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val rowPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val rowBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            isAntiAlias = true
        }
        val zebraBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val totalBgPaint = Paint().apply {
            color = Color.rgb(236, 244, 255)
            style = Paint.Style.FILL
        }

        val rowsPerPage = 14
        val totalInvoices = invoices.size
        val totalPages = if (totalInvoices == 0) 1 else ((totalInvoices - 1) / rowsPerPage) + 1

        var totalTaxableSum = 0.0
        var totalCgstSum = 0.0
        var totalSgstSum = 0.0
        var totalGstSum = 0.0
        var totalNetSum = 0.0

        for (inv in invoices) {
            val taxable = inv.totalAmount
            val cgst = inv.cgstAmount
            val sgst = inv.sgstAmount
            val gst = cgst + sgst
            val net = inv.netAmount

            totalTaxableSum += taxable
            totalCgstSum += cgst
            totalSgstSum += sgst
            totalGstSum += gst
            totalNetSum += net
        }

        // Table Column X-positions (Landscape 842 width)
        val colSl = margin
        val colInv = margin + 28f
        val colDate = margin + 75f
        val colParty = margin + 145f
        val colDl = margin + 285f
        val colGstPan = margin + 375f
        val colTaxable = margin + 465f
        val colCgst = margin + 535f
        val colSgst = margin + 595f
        val colTotalGst = margin + 655f
        val colBillAmt = margin + 725f
        val tableRight = pageWidth - margin

        val generatedDate = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // 1. Business Header
            var curY = 32f
            canvas.drawText(settings.businessName.ifEmpty { "PHARMABILL PHARMACY" }, margin, curY, titlePaint)
            curY += 12f
            val businessDetails = "Address: ${settings.address}  |  D.L. No: ${settings.dlNumber}  |  GSTIN: ${settings.gstNumber}  |  Contact: ${settings.contactNumber}"
            canvas.drawText(businessDetails, margin, curY, subTitlePaint)
            curY += 14f

            // Document Title Banner
            val bannerPaint = Paint().apply {
                color = Color.rgb(240, 246, 255)
                style = Paint.Style.FILL
            }
            canvas.drawRect(margin, curY, tableRight, curY + 24f, bannerPaint)
            canvas.drawRect(margin, curY, tableRight, curY + 24f, borderPaint)

            val reportTitlePaint = Paint().apply {
                color = Color.rgb(13, 71, 161)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("SELLING GST REGISTER (বিক্রয় জিএসটি বিবরণী / GSTR-1)", margin + 10f, curY + 16f, reportTitlePaint)

            val rightText = "Period: $periodText  |  Generated: $generatedDate  |  Page ${pageIndex + 1} of $totalPages"
            val rightTextWidth = subTitlePaint.measureText(rightText)
            canvas.drawText(rightText, tableRight - rightTextWidth - 10f, curY + 16f, subTitlePaint)
            curY += 32f

            // Table Header Bar
            val headerH = 20f
            canvas.drawRect(margin, curY, tableRight, curY + headerH, headerBgPaint)
            canvas.drawRect(margin, curY, tableRight, curY + headerH, borderPaint)

            val textY = curY + 13.5f
            canvas.drawText("Sl", colSl + 4f, textY, headerTablePaint)
            canvas.drawText("Inv #", colInv + 2f, textY, headerTablePaint)
            canvas.drawText("Date", colDate + 2f, textY, headerTablePaint)
            canvas.drawText("Party / Customer Name", colParty + 2f, textY, headerTablePaint)
            canvas.drawText("DL Number", colDl + 2f, textY, headerTablePaint)
            canvas.drawText("Party GSTIN", colGstPan + 2f, textY, headerTablePaint)
            canvas.drawText("Taxable (₹)", colTaxable + 2f, textY, headerTablePaint)
            canvas.drawText("CGST (₹)", colCgst + 2f, textY, headerTablePaint)
            canvas.drawText("SGST (₹)", colSgst + 2f, textY, headerTablePaint)
            canvas.drawText("Total GST (₹)", colTotalGst + 2f, textY, headerTablePaint)
            canvas.drawText("Bill Net (₹)", colBillAmt + 2f, textY, headerTablePaint)

            curY += headerH

            // Table Rows
            val startIdx = pageIndex * rowsPerPage
            val endIdx = minOf(startIdx + rowsPerPage, totalInvoices)

            for (i in startIdx until endIdx) {
                val inv = invoices[i]
                val rowH = 19f
                val rowBaseline = curY + 13f

                if (i % 2 == 1) {
                    canvas.drawRect(margin, curY, tableRight, curY + rowH, zebraBgPaint)
                }
                canvas.drawRect(margin, curY, tableRight, curY + rowH, borderPaint)

                val cgst = inv.cgstAmount
                val sgst = inv.sgstAmount
                val totalGst = cgst + sgst

                canvas.drawText("${i + 1}", colSl + 4f, rowBaseline, rowPaint)
                canvas.drawText("#${inv.invoiceNumber}", colInv + 2f, rowBaseline, rowBoldPaint)
                canvas.drawText(inv.dateFormatted.ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(inv.date)) }, colDate + 2f, rowBaseline, rowPaint)

                // Party Name truncated if too long
                val pName = if (inv.customerName.length > 24) inv.customerName.take(22) + ".." else inv.customerName
                canvas.drawText(pName, colParty + 2f, rowBaseline, rowBoldPaint)

                // Customer DL number
                val dlText = inv.customerDl.ifEmpty { "N/A" }
                val truncatedDl = if (dlText.length > 15) dlText.take(13) + ".." else dlText
                canvas.drawText(truncatedDl, colDl + 2f, rowBaseline, rowPaint)

                // Customer GSTIN
                val gstText = inv.customerGstPan.ifEmpty { "-" }
                canvas.drawText(if (gstText.length > 15) gstText.take(13) + ".." else gstText, colGstPan + 2f, rowBaseline, rowPaint)

                // Amounts
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", inv.totalAmount), colTaxable + 2f, rowBaseline, rowPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", cgst), colCgst + 2f, rowBaseline, rowPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", sgst), colSgst + 2f, rowBaseline, rowPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", totalGst), colTotalGst + 2f, rowBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", inv.netAmount), colBillAmt + 2f, rowBaseline, rowBoldPaint)

                curY += rowH
            }

            // Summary Totals on the Last Page
            if (pageIndex == totalPages - 1) {
                val totalH = 22f
                val totalBaseline = curY + 15f
                canvas.drawRect(margin, curY, tableRight, curY + totalH, totalBgPaint)
                canvas.drawRect(margin, curY, tableRight, curY + totalH, borderPaint)

                canvas.drawText("GRAND TOTAL (${invoices.size} Invoices):", colParty + 2f, totalBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "₹%.2f", totalTaxableSum), colTaxable + 2f, totalBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "₹%.2f", totalCgstSum), colCgst + 2f, totalBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "₹%.2f", totalSgstSum), colSgst + 2f, totalBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "₹%.2f", totalGstSum), colTotalGst + 2f, totalBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "₹%.2f", totalNetSum), colBillAmt + 2f, totalBaseline, rowBoldPaint)

                curY += totalH + 16f

                // GST Statement Note and Signatory
                val notePaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    isAntiAlias = true
                }
                canvas.drawText("* Formula: Total GST = CGST + SGST. Master Admin verified tax statement.", margin, curY, notePaint)
                curY += 12f
                canvas.drawText("* This is a computer generated GSTR-1 Sales Tax statement for accounting and filing.", margin, curY, notePaint)

                val signPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val authSign = "For ${settings.businessName.ifEmpty { "LifeCare Pharmacy" }}\nAuthorised Signatory"
                canvas.drawText("For ${settings.businessName.ifEmpty { "LifeCare Pharmacy" }}", tableRight - 160f, curY - 6f, signPaint)
                canvas.drawText("Authorised Signatory", tableRight - 160f, curY + 8f, subTitlePaint)
            }

            pdfDocument.finishPage(page)
        }

        val file = File(getPdfDirectory(context), "Selling_GST_Register_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    /**
     * Generates a comprehensive GST Purchase Register (GSTR-2) PDF.
     * Contains: Purchase Invoice No, Date, Company/Supplier Name, Company GSTIN, Items, Taxable Amt, CGST, SGST, Total GST.
     */
    fun generatePurchaseGstPdf(
        context: Context,
        settings: SettingsEntity,
        purchases: List<PurchaseInvoiceEntity>,
        periodText: String = "All Records"
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 842  // A4 Landscape
        val pageHeight = 595
        val margin = 30f

        val titlePaint = Paint().apply {
            color = Color.rgb(46, 125, 50) // Forest Green
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val headerTablePaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(46, 125, 50) // Green Header
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val rowPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val rowBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            isAntiAlias = true
        }
        val zebraBgPaint = Paint().apply {
            color = Color.rgb(246, 252, 246)
            style = Paint.Style.FILL
        }
        val totalBgPaint = Paint().apply {
            color = Color.rgb(238, 247, 238)
            style = Paint.Style.FILL
        }

        val rowsPerPage = 14
        val totalPurchases = purchases.size
        val totalPages = if (totalPurchases == 0) 1 else ((totalPurchases - 1) / rowsPerPage) + 1

        var totalTaxableSum = 0.0
        var totalCgstSum = 0.0
        var totalSgstSum = 0.0
        var totalGstSum = 0.0
        var totalBillSum = 0.0

        for (p in purchases) {
            totalTaxableSum += p.taxableAmount
            totalCgstSum += p.cgstAmount
            totalSgstSum += p.sgstAmount
            totalGstSum += p.totalGstAmount
            totalBillSum += p.totalAmount
        }

        // Table Column Positions
        val colSl = margin
        val colInv = margin + 28f
        val colDate = margin + 85f
        val colCompany = margin + 155f
        val colCompanyGst = margin + 295f
        val colItems = margin + 385f
        val colTaxable = margin + 505f
        val colCgst = margin + 575f
        val colSgst = margin + 635f
        val colTotalGst = margin + 695f
        val colBillAmt = margin + 755f
        val tableRight = pageWidth - margin

        val generatedDate = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // 1. Business Header
            var curY = 32f
            canvas.drawText(settings.businessName.ifEmpty { "PHARMABILL PHARMACY" }, margin, curY, titlePaint)
            curY += 12f
            val businessDetails = "Address: ${settings.address}  |  D.L. No: ${settings.dlNumber}  |  GSTIN: ${settings.gstNumber}  |  Contact: ${settings.contactNumber}"
            canvas.drawText(businessDetails, margin, curY, subTitlePaint)
            curY += 14f

            // Document Title Banner
            val bannerPaint = Paint().apply {
                color = Color.rgb(238, 247, 238)
                style = Paint.Style.FILL
            }
            canvas.drawRect(margin, curY, tableRight, curY + 24f, bannerPaint)
            canvas.drawRect(margin, curY, tableRight, curY + 24f, borderPaint)

            val reportTitlePaint = Paint().apply {
                color = Color.rgb(27, 94, 32)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("PURCHASE GST REGISTER (ক্রয় জিএসটি বিবরণী / GSTR-2)", margin + 10f, curY + 16f, reportTitlePaint)

            val rightText = "Period: $periodText  |  Generated: $generatedDate  |  Page ${pageIndex + 1} of $totalPages"
            val rightTextWidth = subTitlePaint.measureText(rightText)
            canvas.drawText(rightText, tableRight - rightTextWidth - 10f, curY + 16f, subTitlePaint)
            curY += 32f

            // Table Header Bar
            val headerH = 20f
            canvas.drawRect(margin, curY, tableRight, curY + headerH, headerBgPaint)
            canvas.drawRect(margin, curY, tableRight, curY + headerH, borderPaint)

            val textY = curY + 13.5f
            canvas.drawText("Sl", colSl + 4f, textY, headerTablePaint)
            canvas.drawText("Bill #", colInv + 2f, textY, headerTablePaint)
            canvas.drawText("Date", colDate + 2f, textY, headerTablePaint)
            canvas.drawText("Company / Supplier Name", colCompany + 2f, textY, headerTablePaint)
            canvas.drawText("Supplier GSTIN", colCompanyGst + 2f, textY, headerTablePaint)
            canvas.drawText("Items Purchased", colItems + 2f, textY, headerTablePaint)
            canvas.drawText("Taxable (₹)", colTaxable + 2f, textY, headerTablePaint)
            canvas.drawText("CGST (₹)", colCgst + 2f, textY, headerTablePaint)
            canvas.drawText("SGST (₹)", colSgst + 2f, textY, headerTablePaint)
            canvas.drawText("Total GST", colTotalGst + 2f, textY, headerTablePaint)
            canvas.drawText("Total Bill (₹)", colBillAmt + 2f, textY, headerTablePaint)

            curY += headerH

            // Table Rows
            val startIdx = pageIndex * rowsPerPage
            val endIdx = minOf(startIdx + rowsPerPage, totalPurchases)

            for (i in startIdx until endIdx) {
                val p = purchases[i]
                val rowH = 19f
                val rowBaseline = curY + 13f

                if (i % 2 == 1) {
                    canvas.drawRect(margin, curY, tableRight, curY + rowH, zebraBgPaint)
                }
                canvas.drawRect(margin, curY, tableRight, curY + rowH, borderPaint)

                canvas.drawText("${i + 1}", colSl + 4f, rowBaseline, rowPaint)
                canvas.drawText(p.invoiceNumber, colInv + 2f, rowBaseline, rowBoldPaint)
                canvas.drawText(p.dateFormatted.ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(p.date)) }, colDate + 2f, rowBaseline, rowPaint)

                val comp = if (p.companyName.length > 22) p.companyName.take(20) + ".." else p.companyName
                canvas.drawText(comp, colCompany + 2f, rowBaseline, rowBoldPaint)

                val gstText = p.companyGst.ifEmpty { "-" }
                canvas.drawText(if (gstText.length > 15) gstText.take(13) + ".." else gstText, colCompanyGst + 2f, rowBaseline, rowPaint)

                val itemsText = if (p.itemsSummary.length > 22) p.itemsSummary.take(20) + ".." else p.itemsSummary.ifEmpty { "Medicines" }
                canvas.drawText(itemsText, colItems + 2f, rowBaseline, rowPaint)

                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", p.taxableAmount), colTaxable + 2f, rowBaseline, rowPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", p.cgstAmount), colCgst + 2f, rowBaseline, rowPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", p.sgstAmount), colSgst + 2f, rowBaseline, rowPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", p.totalGstAmount), colTotalGst + 2f, rowBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "%.2f", p.totalAmount), colBillAmt + 2f, rowBaseline, rowBoldPaint)

                curY += rowH
            }

            // Summary Totals on the Last Page
            if (pageIndex == totalPages - 1) {
                val totalH = 22f
                val totalBaseline = curY + 15f
                canvas.drawRect(margin, curY, tableRight, curY + totalH, totalBgPaint)
                canvas.drawRect(margin, curY, tableRight, curY + totalH, borderPaint)

                canvas.drawText("GRAND TOTAL (${purchases.size} Purchases):", colCompany + 2f, totalBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "₹%.2f", totalTaxableSum), colTaxable + 2f, totalBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "₹%.2f", totalCgstSum), colCgst + 2f, totalBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "₹%.2f", totalSgstSum), colSgst + 2f, totalBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "₹%.2f", totalGstSum), colTotalGst + 2f, totalBaseline, rowBoldPaint)
                canvas.drawText(String.format(Locale.ENGLISH, "₹%.2f", totalBillSum), colBillAmt + 2f, totalBaseline, rowBoldPaint)

                curY += totalH + 16f

                // GST Statement Note and Signatory
                val notePaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    isAntiAlias = true
                }
                canvas.drawText("* Formula: Total GST = CGST + SGST (Input Tax Credit / ITC). Master Admin verified.", margin, curY, notePaint)
                curY += 12f
                canvas.drawText("* This is a computer generated GSTR-2 Purchase Tax statement for accounting and claiming ITC.", margin, curY, notePaint)

                val signPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("For ${settings.businessName.ifEmpty { "LifeCare Pharmacy" }}", tableRight - 160f, curY - 6f, signPaint)
                canvas.drawText("Authorised Signatory", tableRight - 160f, curY + 8f, subTitlePaint)
            }

            pdfDocument.finishPage(page)
        }

        val file = File(getPdfDirectory(context), "Purchase_GST_Register_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }
}
