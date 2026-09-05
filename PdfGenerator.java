package com.example.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import com.example.BuildConfig;
import com.example.data.local.entity.InvoiceEntity;
import com.example.data.local.entity.InvoiceItemEntity;
import com.example.data.local.entity.SettingsEntity;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.io.CloseableKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.jvm.internal.StringCompanionObject;
import kotlin.ranges.RangesKt;
import kotlin.text.Regex;
import kotlin.text.StringsKt;

/* compiled from: PdfGenerator.kt */
@Metadata(d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\bÇ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H\u0002J,\u0010\b\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eJ<\u0010\u0010\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u0014\u001a\u00020\u00122\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00160\u000eJ\u001e\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00120\u000e2\u0006\u0010\u0018\u001a\u00020\u00122\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J$\u0010\u001b\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\t\u001a\u00020\n2\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u001c0\u000eJ\\\u0010\u001d\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u001e\u001a\u00020\u00122\u0006\u0010\u001f\u001a\u00020\u00122\b\b\u0002\u0010 \u001a\u00020\u00122\b\b\u0002\u0010!\u001a\u00020\u00122\b\b\u0002\u0010\"\u001a\u00020\u00122\b\b\u0002\u0010#\u001a\u00020\u00122\f\u0010$\u001a\b\u0012\u0004\u0012\u00020\f0\u000e¨\u0006%"}, d2 = {"Lcom/example/util/PdfGenerator;", "", "<init>", "()V", "getPdfDirectory", "Ljava/io/File;", "context", "Landroid/content/Context;", "generateInvoicePdf", "settings", "Lcom/example/data/local/entity/SettingsEntity;", "invoice", "Lcom/example/data/local/entity/InvoiceEntity;", "items", "", "Lcom/example/data/local/entity/InvoiceItemEntity;", "generateStockStatementPdf", "companyName", "", "fromDate", "toDate", "rows", "Lcom/example/util/StockStatementRow;", "wrapTextToLines", "text", "maxCharsPerLine", "", "generateKhataPdf", "Lcom/example/util/KhataRow;", "generateCustomerSalesHistoryPdf", "customerName", "customerType", "customerPhone", "customerAddress", "customerDl", "customerGst", "invoices", "app"}, k = BuildConfig.VERSION_CODE, mv = {2, 2, 0}, xi = 48)
/* loaded from: /tmp/dex/classes7.dex */
public final class PdfGenerator {
    public static final int $stable = 0;
    public static final PdfGenerator INSTANCE = new PdfGenerator();

    private PdfGenerator() {
    }

    private final File getPdfDirectory(Context context) {
        File dir = new File(context.getCacheDir(), "pdfs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public final File generateInvoicePdf(Context context, SettingsEntity settings, InvoiceEntity invoice, List<InvoiceItemEntity> items) {
        float curX;
        float summaryY;
        int i;
        int i2;
        float headerBottomY;
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(settings, "settings");
        Intrinsics.checkNotNullParameter(invoice, "invoice");
        Intrinsics.checkNotNullParameter(items, "items");
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 420, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(1);
        Paint linePaint = new Paint();
        linePaint.setColor(-16777216);
        linePaint.setStrokeWidth(0.8f);
        linePaint.setStyle(Paint.Style.STROKE);
        Paint paint2 = new Paint();
        paint2.setColor(-12303292);
        paint2.setStrokeWidth(0.5f);
        paint2.setStyle(Paint.Style.STROKE);
        Paint dottedLinePaint = paint2;
        paint2.setPathEffect(new DashPathEffect(new float[]{2.0f, 2.0f}, 0.0f));
        float rightEdge = 595 - 10.0f;
        float bottomEdge = 420 - 10.0f;
        canvas.drawRect(10.0f, 10.0f, rightEdge, bottomEdge, linePaint);
        paint.setColor(-16777216);
        paint.setTextSize(10.0f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("TAX INVOICE / GST INVOICE", 595 / 2.0f, 10.0f + 12.0f, paint);
        canvas.drawLine(10.0f, 10.0f + 15.0f, rightEdge, 10.0f + 15.0f, linePaint);
        float margin = 595;
        float midX = margin / 2.0f;
        float headerBottomY2 = 10.0f + 85.0f;
        canvas.drawLine(midX, 10.0f + 15.0f, midX, headerBottomY2, linePaint);
        canvas.drawLine(10.0f, headerBottomY2, rightEdge, headerBottomY2, linePaint);
        float headerBottomY3 = headerBottomY2;
        paint.setTextAlign(Paint.Align.LEFT);
        float yPos = 26.0f + 10.0f;
        paint.setTextSize(8.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String upperCase = settings.getBusinessName().toUpperCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(upperCase, "toUpperCase(...)");
        canvas.drawText(upperCase, 10.0f + 5.0f, yPos, paint);
        paint.setTextSize(6.5f);
        paint.setTypeface(Typeface.DEFAULT);
        float yPos2 = yPos + 10.0f;
        canvas.drawText(settings.getAddress(), 10.0f + 5.0f, yPos2, paint);
        float yPos3 = yPos2 + 9.0f;
        canvas.drawText("GSTIN: " + settings.getGstNumber() + " | D.L. No: " + settings.getDlNumber(), 10.0f + 5.0f, yPos3, paint);
        float yPos4 = yPos3 + 9.0f;
        canvas.drawText("Phone: " + settings.getContactNumber(), 10.0f + 5.0f, yPos4, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(8.0f);
        canvas.drawText("INVOICE NO: " + invoice.getInvoiceNumber(), 10.0f + 5.0f, yPos4 + 11.0f, paint);
        float yPos5 = 10.0f + 26.0f;
        paint.setTextSize(8.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String upperCase2 = invoice.getCustomerName().toUpperCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(upperCase2, "toUpperCase(...)");
        canvas.drawText("BUYER: " + upperCase2, midX + 5.0f, yPos5, paint);
        paint.setTextSize(6.5f);
        paint.setTypeface(Typeface.DEFAULT);
        float yPos6 = yPos5 + 10.0f;
        canvas.drawText(!StringsKt.isBlank(invoice.getCustomerAddress()) ? invoice.getCustomerAddress() : "Local", midX + 5.0f, yPos6, paint);
        float yPos7 = yPos6 + 9.0f;
        canvas.drawText("D.L. No: " + (!StringsKt.isBlank(invoice.getCustomerDl()) ? invoice.getCustomerDl() : "N/A") + " | GST/PAN: " + (!StringsKt.isBlank(invoice.getCustomerGstPan()) ? invoice.getCustomerGstPan() : "N/A"), midX + 5.0f, yPos7, paint);
        float yPos8 = yPos7 + 9.0f;
        canvas.drawText("Phone: " + (!StringsKt.isBlank(invoice.getCustomerPhone()) ? invoice.getCustomerPhone() : "N/A") + (!StringsKt.isBlank(invoice.getDoctorDetails()) ? " | Ref: " + invoice.getDoctorDetails() : ""), midX + 5.0f, yPos8, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(8.0f);
        canvas.drawText("DATE: " + invoice.getDateFormatted(), midX + 5.0f, yPos8 + 11.0f, paint);
        String[] colHeaders = {"QTY", "FREE", "DESCRIPTION", "TYPE", "MRP", "MFG", "BATCH", "EXP", "RATE", "DIS%", "GST%", "AMOUNT"};
        float[] fArr = {24.0f, 22.0f, 138.0f, 32.0f, 38.0f, 44.0f, 40.0f, 34.0f, 38.0f, 28.0f, 32.0f, 55.0f};
        float tableWidth = rightEdge - 10.0f;
        float sumWidths = ArraysKt.sum(fArr);
        float scaleRatio = tableWidth / sumWidths;
        Collection arrayList = new ArrayList(fArr.length);
        int i3 = 0;
        for (int length = fArr.length; i3 < length; length = length) {
            arrayList.add(Float.valueOf(fArr[i3] * scaleRatio));
            i3++;
        }
        float[] scaledWidths = CollectionsKt.toFloatArray((List) arrayList);
        float tableHeaderY = headerBottomY3 + 12.0f;
        int i4 = 5;
        canvas.drawLine(10.0f, headerBottomY3 + 14.0f, rightEdge, headerBottomY3 + 14.0f, linePaint);
        float summaryY2 = bottomEdge - 95.0f;
        float curX2 = 10.0f;
        paint.setTextSize(5.8f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        int i5 = 0;
        int length2 = colHeaders.length;
        while (i5 < length2) {
            float colW = scaledWidths[i5];
            int i6 = i4;
            float[] scaledWidths2 = scaledWidths;
            canvas.drawText(colHeaders[i5], curX2 + (colW / 2.0f), tableHeaderY, paint);
            if (i5 <= 0) {
                curX = curX2;
                summaryY = summaryY2;
                i = i5;
                i2 = length2;
                headerBottomY = headerBottomY3;
            } else {
                i = i5;
                i2 = length2;
                float summaryY3 = summaryY2;
                float summaryY4 = headerBottomY3;
                canvas.drawLine(curX2, summaryY4, curX2, summaryY3, linePaint);
                curX = curX2;
                headerBottomY = summaryY4;
                summaryY = summaryY3;
            }
            curX2 = curX + colW;
            i5 = i + 1;
            length2 = i2;
            linePaint = linePaint;
            i4 = i6;
            headerBottomY3 = headerBottomY;
            scaledWidths = scaledWidths2;
            summaryY2 = summaryY;
        }
        int i7 = i4;
        float[] scaledWidths3 = scaledWidths;
        float summaryY5 = summaryY2;
        float headerBottomY4 = headerBottomY3;
        Paint linePaint2 = linePaint;
        float rowY = headerBottomY4 + 23.0f;
        float rowY2 = 1.4E-44f;
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(5.6f);
        Iterable<InvoiceItemEntity> take = CollectionsKt.take(items, 10);
        float margin2 = rowY;
        for (InvoiceItemEntity invoiceItemEntity : take) {
            Iterable iterable = take;
            paint.setTextAlign(Paint.Align.CENTER);
            float itemRowLimit = rowY2;
            canvas.drawText(String.valueOf(invoiceItemEntity.getQty()), 10.0f + (scaledWidths3[0] / 2.0f), margin2, paint);
            float f = 10.0f + scaledWidths3[0];
            canvas.drawText(String.valueOf(invoiceItemEntity.getFreeQty()), (scaledWidths3[1] / 2.0f) + f, margin2, paint);
            float f2 = f + scaledWidths3[1];
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(invoiceItemEntity.getProductName().length() > 26 ? StringsKt.take(invoiceItemEntity.getProductName(), 24) + ".." : invoiceItemEntity.getProductName(), f2 + 2.0f, margin2, paint);
            float f3 = f2 + scaledWidths3[2];
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(invoiceItemEntity.getPack(), f3 + (scaledWidths3[3] / 2.0f), margin2, paint);
            float f4 = f3 + scaledWidths3[3];
            paint.setTextAlign(Paint.Align.RIGHT);
            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
            String[] colHeaders2 = colHeaders;
            String format = String.format(Locale.ENGLISH, "%.1f", Arrays.copyOf(new Object[]{Double.valueOf(invoiceItemEntity.getMrp())}, 1));
            Intrinsics.checkNotNullExpressionValue(format, "format(...)");
            canvas.drawText(format, (scaledWidths3[4] + f4) - 2.0f, margin2, paint);
            float f5 = f4 + scaledWidths3[4];
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(invoiceItemEntity.getManufacturer().length() > 8 ? StringsKt.take(invoiceItemEntity.getManufacturer(), 7) + "." : invoiceItemEntity.getManufacturer(), f5 + 2.0f, margin2, paint);
            float f6 = f5 + scaledWidths3[i7];
            canvas.drawText(StringsKt.take(invoiceItemEntity.getBatchNo(), 7), f6 + 2.0f, margin2, paint);
            float f7 = f6 + scaledWidths3[6];
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(StringsKt.take(invoiceItemEntity.getExpDate(), i7), f7 + (scaledWidths3[7] / 2.0f), margin2, paint);
            float f8 = f7 + scaledWidths3[7];
            paint.setTextAlign(Paint.Align.RIGHT);
            StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
            String format2 = String.format(Locale.ENGLISH, "%.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoiceItemEntity.getNetRate())}, 1));
            Intrinsics.checkNotNullExpressionValue(format2, "format(...)");
            canvas.drawText(format2, (scaledWidths3[8] + f8) - 2.0f, margin2, paint);
            float f9 = f8 + scaledWidths3[8];
            StringCompanionObject stringCompanionObject3 = StringCompanionObject.INSTANCE;
            String format3 = String.format(Locale.ENGLISH, "%.0f%%", Arrays.copyOf(new Object[]{Double.valueOf(invoiceItemEntity.getDiscountPercent())}, 1));
            Intrinsics.checkNotNullExpressionValue(format3, "format(...)");
            canvas.drawText(format3, (scaledWidths3[9] + f9) - 2.0f, margin2, paint);
            float f10 = f9 + scaledWidths3[9];
            double sgstPercent = invoiceItemEntity.getSgstPercent() + invoiceItemEntity.getCgstPercent();
            StringCompanionObject stringCompanionObject4 = StringCompanionObject.INSTANCE;
            String format4 = String.format(Locale.ENGLISH, "%.0f%%", Arrays.copyOf(new Object[]{Double.valueOf(sgstPercent)}, 1));
            Intrinsics.checkNotNullExpressionValue(format4, "format(...)");
            canvas.drawText(format4, (scaledWidths3[10] + f10) - 2.0f, margin2, paint);
            float f11 = scaledWidths3[10] + f10;
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            StringCompanionObject stringCompanionObject5 = StringCompanionObject.INSTANCE;
            String format5 = String.format(Locale.ENGLISH, "%.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoiceItemEntity.getItemTotalAmount())}, 1));
            Intrinsics.checkNotNullExpressionValue(format5, "format(...)");
            canvas.drawText(format5, (scaledWidths3[11] + f11) - 2.0f, margin2, paint);
            paint.setTypeface(Typeface.DEFAULT);
            float rowY3 = margin2;
            Paint dottedLinePaint2 = dottedLinePaint;
            canvas.drawLine(10.0f, margin2 + 3.0f, rightEdge, margin2 + 3.0f, dottedLinePaint2);
            dottedLinePaint = dottedLinePaint2;
            take = iterable;
            colHeaders = colHeaders2;
            i7 = 5;
            margin2 = rowY3 + 12.0f;
            rowY2 = itemRowLimit;
        }
        float dividerX = midX + 45.0f;
        canvas.drawLine(10.0f, summaryY5, rightEdge, summaryY5, linePaint2);
        canvas.drawLine(dividerX, summaryY5, dividerX, bottomEdge, linePaint2);
        float leftX = 10.0f + 6.0f;
        paint.setTextAlign(Paint.Align.LEFT);
        float sumLeftY = summaryY5 + 11.0f;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(6.8f);
        StringCompanionObject stringCompanionObject6 = StringCompanionObject.INSTANCE;
        String format6 = String.format(Locale.ENGLISH, "%.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoice.getTotalMrpValue())}, 1));
        Intrinsics.checkNotNullExpressionValue(format6, "format(...)");
        canvas.drawText("TOTAL M.R.P. VALUE: ₹ " + format6, leftX, sumLeftY, paint);
        float sumLeftY2 = sumLeftY + 10.0f;
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(6.2f);
        canvas.drawText("NO. OF ITEMS: " + items.size() + "   |   TOTAL QTY: " + invoice.getTotalQty() + "   |   FREE: " + invoice.getTotalFree(), leftX, sumLeftY2, paint);
        float sumLeftY3 = sumLeftY2 + 10.0f;
        StringCompanionObject stringCompanionObject7 = StringCompanionObject.INSTANCE;
        String format7 = String.format(Locale.ENGLISH, "%.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoice.getAdjustmentAmount())}, 1));
        Intrinsics.checkNotNullExpressionValue(format7, "format(...)");
        canvas.drawText("ADJ CR/DR NOTE: ₹ " + format7, leftX, sumLeftY3, paint);
        float sumLeftY4 = sumLeftY3 + 11.0f;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(6.2f);
        canvas.drawText("RUPEES IN WORDS:", leftX, sumLeftY4, paint);
        float sumLeftY5 = sumLeftY4 + 9.0f;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        paint.setTextSize(5.8f);
        String upperCase3 = invoice.getAmountInWords().toUpperCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(upperCase3, "toUpperCase(...)");
        List<String> wrappedWords = wrapTextToLines(upperCase3, 46);
        for (int wIdx = 0; wIdx < Math.min(wrappedWords.size(), 2); wIdx++) {
            String wLine = wrappedWords.get(wIdx);
            canvas.drawText(wLine, leftX, sumLeftY5, paint);
            sumLeftY5 += 8.0f;
        }
        float termsY = RangesKt.coerceAtLeast(summaryY5 + 76.0f, sumLeftY5 + 2.0f);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(5.2f);
        paint.setColor(-12303292);
        canvas.drawText("Terms: Subject to West Bengal Jurisdiction. Goods once sold will not be taken back.", leftX, termsY, paint);
        paint.setColor(-16777216);
        canvas.drawText("Thank you for your business! Stay healthy.", leftX, termsY + 8.0f, paint);
        float colRightLabelX = dividerX + 8.0f;
        float colRightValX = rightEdge - 8.0f;
        Ref.FloatRef sumRightY = new Ref.FloatRef();
        sumRightY.element = summaryY5 + 11.0f;
        StringCompanionObject stringCompanionObject8 = StringCompanionObject.INSTANCE;
        String format8 = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoice.getTotalAmount())}, 1));
        Intrinsics.checkNotNullExpressionValue(format8, "format(...)");
        generateInvoicePdf$drawSummaryLine$default(paint, canvas, colRightLabelX, sumRightY, colRightValX, "TOTAL AMOUNT", format8, false, 0.0f, 384, null);
        StringCompanionObject stringCompanionObject9 = StringCompanionObject.INSTANCE;
        String format9 = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoice.getLessDiscount())}, 1));
        Intrinsics.checkNotNullExpressionValue(format9, "format(...)");
        generateInvoicePdf$drawSummaryLine$default(paint, canvas, colRightLabelX, sumRightY, colRightValX, "LESS DISCOUNT", format9, false, 0.0f, 384, null);
        double totalGstAmt = invoice.getCgstAmount() + invoice.getSgstAmount();
        StringCompanionObject stringCompanionObject10 = StringCompanionObject.INSTANCE;
        String format10 = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(totalGstAmt)}, 1));
        Intrinsics.checkNotNullExpressionValue(format10, "format(...)");
        generateInvoicePdf$drawSummaryLine$default(paint, canvas, colRightLabelX, sumRightY, colRightValX, "GST AMOUNT", format10, false, 0.0f, 384, null);
        if (invoice.getAdjustmentAmount() > 0.0d) {
            String formatAdj = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoice.getAdjustmentAmount())}, 1));
            Intrinsics.checkNotNullExpressionValue(formatAdj, "format(...)");
            generateInvoicePdf$drawSummaryLine$default(paint, canvas, colRightLabelX, sumRightY, colRightValX, "ADJ CR/DR NOTE", formatAdj, false, 0.0f, 384, null);
        }
        canvas.drawLine(dividerX, sumRightY.element - 1.0f, rightEdge, sumRightY.element - 1.0f, linePaint2);
        sumRightY.element += 4.0f;
        StringCompanionObject stringCompanionObject11 = StringCompanionObject.INSTANCE;
        String format11 = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoice.getNetAmount())}, 1));
        Intrinsics.checkNotNullExpressionValue(format11, "format(...)");
        generateInvoicePdf$drawSummaryLine(paint, canvas, colRightLabelX, sumRightY, colRightValX, "NET PAYABLE", format11, true, 8.2f);
        float signY = summaryY5 + 76.0f;
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(6.2f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String upperCase4 = settings.getBusinessName().toUpperCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(upperCase4, "toUpperCase(...)");
        canvas.drawText("For " + upperCase4, rightEdge - 8.0f, signY, paint);
        paint.setTextSize(5.4f);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("(Authorised Signatory)", rightEdge - 8.0f, signY + 8.0f, paint);
        pdfDocument.finishPage(page);
        File file = new File(getPdfDirectory(context), "Invoice_" + invoice.getInvoiceNumber() + ".pdf");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            pdfDocument.writeTo(fileOutputStream);
            fileOutputStream.close();
            pdfDocument.close();
            return file;
        } catch (Exception e) {
            pdfDocument.close();
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    static /* synthetic */ void generateInvoicePdf$drawSummaryLine$default(Paint paint, Canvas canvas, float f, Ref.FloatRef floatRef, float f2, String str, String str2, boolean z, float f3, int i, Object obj) {
        generateInvoicePdf$drawSummaryLine(paint, canvas, f, floatRef, f2, str, str2, (i & 128) != 0 ? false : z, (i & 256) != 0 ? 6.2f : f3);
    }

    private static final void generateInvoicePdf$drawSummaryLine(Paint paint, Canvas canvas, float colRightLabelX, Ref.FloatRef sumRightY, float colRightValX, String label, String value, boolean isBold, float textSize) {
        paint.setTextSize(textSize);
        Typeface typeface = Typeface.DEFAULT;
        if (isBold) {
            typeface = Typeface.create(typeface, Typeface.BOLD);
        }
        paint.setTypeface(typeface);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(label, colRightLabelX, sumRightY.element, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(value, colRightValX, sumRightY.element, paint);
        sumRightY.element += 10.0f;
    }

    public final File generateStockStatementPdf(Context context, SettingsEntity settings, String companyName, String fromDate, String toDate, List<StockStatementRow> rows) {
        String upperCase;
        float tableTopY;
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(settings, "settings");
        Intrinsics.checkNotNullParameter(companyName, "companyName");
        Intrinsics.checkNotNullParameter(fromDate, "fromDate");
        Intrinsics.checkNotNullParameter(toDate, "toDate");
        Intrinsics.checkNotNullParameter(rows, "rows");
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(1);
        Paint linePaint = new Paint();
        linePaint.setColor(-16777216);
        linePaint.setStrokeWidth(0.8f);
        linePaint.setStyle(Paint.Style.STROKE);
        float rightEdge = 595 - 20.0f;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(14.0f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("STOCK & STATEMENT", 595 / 2.0f, 20.0f + 20.0f, paint);
        paint.setTextSize(11.0f);
        String upperCase2 = settings.getBusinessName().toUpperCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(upperCase2, "toUpperCase(...)");
        canvas.drawText(upperCase2, 595 / 2.0f, 20.0f + 36.0f, paint);
        paint.setTextSize(9.0f);
        paint.setTypeface(Typeface.DEFAULT);
        if (StringsKt.isBlank(companyName)) {
            upperCase = "ALL COMPANIES";
        } else {
            upperCase = companyName.toUpperCase(Locale.ROOT);
            Intrinsics.checkNotNullExpressionValue(upperCase, "toUpperCase(...)");
        }
        canvas.drawText("Company: " + upperCase, 595 / 2.0f, 50.0f + 20.0f, paint);
        canvas.drawText("Date: From " + fromDate + " To " + toDate, 595 / 2.0f, 63.0f + 20.0f, paint);
        float tableTopY2 = 20.0f + 80.0f;
        canvas.drawRect(20.0f, tableTopY2, rightEdge, tableTopY2 + 28.0f, linePaint);
        paint.setTextSize(7.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Product Details", (125.0f / 2.0f) + 20.0f, tableTopY2 + 18.0f, paint);
        canvas.drawLine(20.0f + 125.0f, tableTopY2, 20.0f + 125.0f, (842 - 20.0f) - 40.0f, linePaint);
        canvas.drawText("TYPE", 20.0f + 125.0f + (46.0f / 2.0f), tableTopY2 + 18.0f, paint);
        canvas.drawLine(20.0f + 125.0f + 46.0f, tableTopY2, 20.0f + 125.0f + 46.0f, (842 - 20.0f) - 40.0f, linePaint);
        float tableTopY3 = tableTopY2;
        String[] groups = {"OPENING", "RECEIPT", "ISSUE", "CLOSING"};
        int g = 0;
        int length = groups.length;
        while (true) {
            tableTopY = tableTopY3;
            if (g >= length) {
                break;
            }
            float startX = 20.0f + 125.0f + 46.0f + (g * 48.0f * 2.0f);
            canvas.drawText(groups[g], startX + 48.0f, tableTopY + 11.0f, paint);
            canvas.drawLine(startX, tableTopY + 14.0f, startX + (48.0f * 2.0f), tableTopY + 14.0f, linePaint);
            paint.setTextSize(6.5f);
            canvas.drawText("Qty", (48.0f / 2.0f) + startX, tableTopY + 24.0f, paint);
            canvas.drawText("Amount", startX + 48.0f + (48.0f / 2.0f), tableTopY + 24.0f, paint);
            paint.setTextSize(7.5f);
            canvas.drawLine(startX, tableTopY, startX, (842 - 20.0f) - 40.0f, linePaint);
            canvas.drawLine(startX + 48.0f, tableTopY + 14.0f, startX + 48.0f, (842 - 20.0f) - 40.0f, linePaint);
            g++;
            groups = groups;
            tableTopY3 = tableTopY;
            length = length;
        }
        float rowY = tableTopY + 40.0f;
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(6.5f);
        int totRecQty = 0;
        int totCloseQty = 0;
        int totOpenQty = 0;
        Iterator it = CollectionsKt.take(rows, 45).iterator();
        int totIssQty = 0;
        float margin = rowY;
        double totCloseAmt = 0.0d;
        double totCloseAmt2 = 0.0d;
        double totIssAmt = 0.0d;
        double totRecAmt = 0.0d;
        while (true) {
            Iterator it2 = it;
            int totIssQty2 = totIssQty;
            if (it.hasNext()) {
                StockStatementRow row = (StockStatementRow) it2.next();
                paint.setTextAlign(Paint.Align.LEFT);
                int totRecQty2 = totRecQty;
                canvas.drawText(StringsKt.take(row.getProductName(), 20), 20.0f + 4.0f, margin, paint);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(row.getPackagingType(), 20.0f + 125.0f + (46.0f / 2.0f), margin, paint);
                paint.setTextAlign(Paint.Align.RIGHT);
                float x = 20.0f + 125.0f + 46.0f;
                canvas.drawText(String.valueOf(row.getOpeningQty()), (x + 48.0f) - 3.0f, margin, paint);
                StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
                String format = String.format(Locale.ENGLISH, "%.1f", Arrays.copyOf(new Object[]{Double.valueOf(row.getOpeningAmount())}, 1));
                Intrinsics.checkNotNullExpressionValue(format, "format(...)");
                canvas.drawText(format, (x + (48.0f * 2.0f)) - 3.0f, margin, paint);
                float x2 = x + (48.0f * 2.0f);
                canvas.drawText(String.valueOf(row.getReceiptQty()), (x2 + 48.0f) - 3.0f, margin, paint);
                StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
                String format2 = String.format(Locale.ENGLISH, "%.1f", Arrays.copyOf(new Object[]{Double.valueOf(row.getReceiptAmount())}, 1));
                Intrinsics.checkNotNullExpressionValue(format2, "format(...)");
                canvas.drawText(format2, (x2 + (48.0f * 2.0f)) - 3.0f, margin, paint);
                float x3 = x2 + (48.0f * 2.0f);
                canvas.drawText(String.valueOf(row.getIssueQty()), (x3 + 48.0f) - 3.0f, margin, paint);
                StringCompanionObject stringCompanionObject3 = StringCompanionObject.INSTANCE;
                String format3 = String.format(Locale.ENGLISH, "%.1f", Arrays.copyOf(new Object[]{Double.valueOf(row.getIssueAmount())}, 1));
                Intrinsics.checkNotNullExpressionValue(format3, "format(...)");
                canvas.drawText(format3, (x3 + (48.0f * 2.0f)) - 3.0f, margin, paint);
                float x4 = x3 + (48.0f * 2.0f);
                canvas.drawText(String.valueOf(row.getClosingQty()), (x4 + 48.0f) - 3.0f, margin, paint);
                StringCompanionObject stringCompanionObject4 = StringCompanionObject.INSTANCE;
                String format4 = String.format(Locale.ENGLISH, "%.1f", Arrays.copyOf(new Object[]{Double.valueOf(row.getClosingAmount())}, 1));
                Intrinsics.checkNotNullExpressionValue(format4, "format(...)");
                canvas.drawText(format4, (x4 + (48.0f * 2.0f)) - 3.0f, margin, paint);
                totOpenQty += row.getOpeningQty();
                totRecAmt += row.getOpeningAmount();
                totRecQty = totRecQty2 + row.getReceiptQty();
                totIssAmt += row.getReceiptAmount();
                totIssQty = totIssQty2 + row.getIssueQty();
                totCloseAmt2 += row.getIssueAmount();
                totCloseQty += row.getClosingQty();
                totCloseAmt += row.getClosingAmount();
                float rowY2 = margin;
                canvas.drawLine(20.0f, margin + 3.0f, rightEdge, margin + 3.0f, linePaint);
                margin = 13.0f + rowY2;
                it = it2;
            } else {
                int totRecQty3 = totRecQty;
                float totalY = (842 - 20.0f) - 35.0f;
                canvas.drawRect(20.0f, totalY, rightEdge, totalY + 18.0f, linePaint);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("TOTAL (" + rows.size() + ")", 20.0f + 4.0f, totalY + 12.0f, paint);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("-", 20.0f + 125.0f + (46.0f / 2.0f), totalY + 12.0f, paint);
                paint.setTextAlign(Paint.Align.RIGHT);
                float baseX = 20.0f + 125.0f + 46.0f;
                float rowY3 = totalY + 12.0f;
                canvas.drawText(String.valueOf(totOpenQty), (baseX + 48.0f) - 3.0f, rowY3, paint);
                StringCompanionObject stringCompanionObject5 = StringCompanionObject.INSTANCE;
                String format5 = String.format(Locale.ENGLISH, "%.1f", Arrays.copyOf(new Object[]{Double.valueOf(totRecAmt)}, 1));
                Intrinsics.checkNotNullExpressionValue(format5, "format(...)");
                canvas.drawText(format5, (baseX + (2.0f * 48.0f)) - 3.0f, totalY + 12.0f, paint);
                canvas.drawText(String.valueOf(totRecQty3), ((48.0f * 3.0f) + baseX) - 3.0f, totalY + 12.0f, paint);
                StringCompanionObject stringCompanionObject6 = StringCompanionObject.INSTANCE;
                String format6 = String.format(Locale.ENGLISH, "%.1f", Arrays.copyOf(new Object[]{Double.valueOf(totIssAmt)}, 1));
                Intrinsics.checkNotNullExpressionValue(format6, "format(...)");
                canvas.drawText(format6, (baseX + (4.0f * 48.0f)) - 3.0f, totalY + 12.0f, paint);
                canvas.drawText(String.valueOf(totIssQty2), ((5.0f * 48.0f) + baseX) - 3.0f, totalY + 12.0f, paint);
                StringCompanionObject stringCompanionObject7 = StringCompanionObject.INSTANCE;
                String format7 = String.format(Locale.ENGLISH, "%.1f", Arrays.copyOf(new Object[]{Double.valueOf(totCloseAmt2)}, 1));
                Intrinsics.checkNotNullExpressionValue(format7, "format(...)");
                canvas.drawText(format7, ((6.0f * 48.0f) + baseX) - 3.0f, totalY + 12.0f, paint);
                canvas.drawText(String.valueOf(totCloseQty), ((7.0f * 48.0f) + baseX) - 3.0f, totalY + 12.0f, paint);
                StringCompanionObject stringCompanionObject8 = StringCompanionObject.INSTANCE;
                String format8 = String.format(Locale.ENGLISH, "%.1f", Arrays.copyOf(new Object[]{Double.valueOf(totCloseAmt)}, 1));
                Intrinsics.checkNotNullExpressionValue(format8, "format(...)");
                canvas.drawText(format8, ((8.0f * 48.0f) + baseX) - 3.0f, totalY + 12.0f, paint);
                pdfDocument.finishPage(page);
                File file = new File(getPdfDirectory(context), "Stock_Statement_" + System.currentTimeMillis() + ".pdf");
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    pdfDocument.writeTo(fileOutputStream);
                    fileOutputStream.close();
                    pdfDocument.close();
                    return file;
                } catch (Exception e) {
                    pdfDocument.close();
                    throw new RuntimeException("Failed to generate PDF", e);
                }
            }
        }
    }

    private final List<String> wrapTextToLines(String text, int maxCharsPerLine) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] words = text.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxCharsPerLine) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    public final File generateKhataPdf(Context context, SettingsEntity settings, List<KhataRow> rows) {
        boolean z;
        String[] headers;
        float tableTopY;
        int i;
        float curX;
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(settings, "settings");
        Intrinsics.checkNotNullParameter(rows, "rows");
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(1);
        Paint linePaint = new Paint();
        linePaint.setColor(-16777216);
        linePaint.setStrokeWidth(0.8f);
        linePaint.setStyle(Paint.Style.STROKE);
        float rightEdge = 595 - 20.0f;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(14.0f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("SALES KHATA REGISTER (বিক্রয় খাতা)", 595 / 2.0f, 20.0f + 20.0f, paint);
        paint.setTextSize(10.0f);
        String upperCase = settings.getBusinessName().toUpperCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(upperCase, "toUpperCase(...)");
        canvas.drawText(upperCase, 595 / 2.0f, 35.0f + 20.0f, paint);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(8.0f);
        canvas.drawText("Generated on " + DateUtils.INSTANCE.currentDateString(), 595 / 2.0f, 48.0f + 20.0f, paint);
        float[] colWidths = {80.0f, 130.0f, 175.0f, 45.0f, 45.0f, 80.0f};
        float tableTopY2 = 65.0f + 20.0f;
        float tableTopY3 = tableTopY2;
        canvas.drawRect(20.0f, tableTopY3, rightEdge, tableTopY2 + 20.0f, linePaint);
        float rowY = 20.0f;
        String[] headers2 = {"Invoice & Date", "Party Name", "Product Details", "Qty", "Free", "Bill Amount"};
        paint.setTextSize(7.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        int length = headers2.length;
        float curX2 = 20.0f;
        int i2 = 0;
        while (i2 < length) {
            float w = colWidths[i2];
            paint.setTextAlign(i2 >= 3 ? Paint.Align.RIGHT : Paint.Align.LEFT);
            float textX = i2 >= 3 ? (curX2 + w) - 4.0f : curX2 + 4.0f;
            int i3 = i2;
            canvas.drawText(headers2[i2], textX, tableTopY3 + 14.0f, paint);
            if (i3 > 0) {
                float curX3 = (842 - 20.0f) - 30.0f;
                float curX4 = curX2;
                i = length;
                headers = headers2;
                canvas.drawLine(curX4, tableTopY3, curX2, curX3, linePaint);
                curX = curX4;
                tableTopY = tableTopY3;
            } else {
                headers = headers2;
                tableTopY = tableTopY3;
                i = length;
                curX = curX2;
            }
            curX2 = curX + w;
            length = i;
            headers2 = headers;
            i2 = i3 + 1;
            tableTopY3 = tableTopY;
        }
        float rowY2 = tableTopY3 + 34.0f;
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(6.8f);
        float margin = rowY2;
        for (KhataRow row : CollectionsKt.take(rows, 48)) {
            float x = rowY;
            if (row.isFirstItemOfInvoice()) {
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(String.valueOf(row.getInvoiceNumber()), x + 4.0f, margin - 2.0f, paint);
                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextSize(5.8f);
                canvas.drawText(row.getDate(), x + 4.0f, 7.0f + margin, paint);
                paint.setTextSize(6.8f);
            }
            float x2 = x + colWidths[0];
            if (row.isFirstItemOfInvoice()) {
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(StringsKt.take(row.getPartyName(), 22), x2 + 4.0f, margin, paint);
            }
            float x3 = x2 + colWidths[1];
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(StringsKt.take(row.getProductName(), 30), x3 + 4.0f, margin, paint);
            float x4 = x3 + colWidths[2];
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf(row.getQuantity()), (colWidths[3] + x4) - 4.0f, margin, paint);
            float x5 = x4 + colWidths[3];
            canvas.drawText(String.valueOf(row.getFree()), (colWidths[4] + x5) - 4.0f, margin, paint);
            float x6 = x5 + colWidths[4];
            if (!row.isFirstItemOfInvoice()) {
                z = true;
            } else {
                z = true;
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
                String format = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(row.getBillAmount())}, 1));
                Intrinsics.checkNotNullExpressionValue(format, "format(...)");
                canvas.drawText(format, (x6 + colWidths[5]) - 4.0f, margin, paint);
                paint.setTypeface(Typeface.DEFAULT);
            }
            float margin2 = rowY;
            canvas.drawLine(margin2, margin + 5.0f, rightEdge, margin + 5.0f, linePaint);
            rowY = margin2;
            margin += 13.0f;
        }
        pdfDocument.finishPage(page);
        File file = new File(getPdfDirectory(context), "Khata_Register_" + System.currentTimeMillis() + ".pdf");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            pdfDocument.writeTo(fileOutputStream);
            fileOutputStream.close();
            pdfDocument.close();
            return file;
        } catch (Exception e) {
            pdfDocument.close();
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    public static /* synthetic */ File generateCustomerSalesHistoryPdf$default(PdfGenerator pdfGenerator, Context context, SettingsEntity settingsEntity, String str, String str2, String str3, String str4, String str5, String str6, List list, int i, Object obj) {
        if ((i & 16) != 0) {
            str3 = "";
        }
        if ((i & 32) != 0) {
            str4 = "";
        }
        if ((i & 64) != 0) {
            str5 = "";
        }
        if ((i & 128) != 0) {
            str6 = "";
        }
        return pdfGenerator.generateCustomerSalesHistoryPdf(context, settingsEntity, str, str2, str3, str4, str5, str6, list);
    }

    public final File generateCustomerSalesHistoryPdf(Context context, SettingsEntity settings, String customerName, String customerType, String customerPhone, String customerAddress, String customerDl, String customerGst, List<InvoiceEntity> invoices) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(settings, "settings");
        Intrinsics.checkNotNullParameter(customerName, "customerName");
        Intrinsics.checkNotNullParameter(customerType, "customerType");
        Intrinsics.checkNotNullParameter(customerPhone, "customerPhone");
        Intrinsics.checkNotNullParameter(customerAddress, "customerAddress");
        Intrinsics.checkNotNullParameter(customerDl, "customerDl");
        Intrinsics.checkNotNullParameter(customerGst, "customerGst");
        Intrinsics.checkNotNullParameter(invoices, "invoices");
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(1);
        Paint linePaint = new Paint();
        linePaint.setColor(-16777216);
        linePaint.setStrokeWidth(0.8f);
        float rightEdge = 595 - 20.0f;
        canvas.drawRect(20.0f, 20.0f, rightEdge, 842 - 20.0f, linePaint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(14.0f);
        String upperCase = settings.getBusinessName().toUpperCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(upperCase, "toUpperCase(...)");
        canvas.drawText(upperCase, 595 / 2.0f, 20.0f + 20.0f, paint);
        paint.setTextSize(7.5f);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText(settings.getAddress(), 595 / 2.0f, 20.0f + 32.0f, paint);
        canvas.drawText("Phone: " + settings.getContactNumber() + "  |  D.L. No: " + settings.getDlNumber() + "  |  GSTIN: " + settings.getGstNumber(), 595 / 2.0f, 20.0f + 43.0f, paint);
        canvas.drawLine(20.0f, 20.0f + 50.0f, rightEdge, 20.0f + 50.0f, linePaint);
        float bannerY = 20.0f + 50.0f;
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#E6F4FE"));
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(20.0f, bannerY, rightEdge, bannerY + 22.0f, bgPaint);
        float tableTopY = bannerY;
        Paint bgPaint2 = bgPaint;
        canvas.drawLine(20.0f, tableTopY + 22.0f, rightEdge, tableTopY + 22.0f, linePaint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(10.0f);
        paint.setColor(Color.parseColor("#0288D1"));
        canvas.drawText("CUSTOMER SALES HISTORY & BILL REGISTER (কাস্টমার সেলস হিস্ট্রি)", 595 / 2.0f, tableTopY + 15.0f, paint);
        paint.setColor(-16777216);
        float custY = tableTopY + 22.0f;
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(8.5f);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Customer Name: " + customerName, 20.0f + 8.0f, custY + 14.0f, paint);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("Category: " + customerType + "  |  Phone: " + (!StringsKt.isBlank(customerPhone) ? customerPhone : "N/A"), 20.0f + 8.0f, custY + 26.0f, paint);
        canvas.drawText("Address: " + (!StringsKt.isBlank(customerAddress) ? customerAddress : "N/A"), 20.0f + 8.0f, 38.0f + custY, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Statement Date: " + DateUtils.INSTANCE.currentDateString(), rightEdge - 8.0f, custY + 14.0f, paint);
        if (!StringsKt.isBlank(customerDl)) {
            canvas.drawText("DL No: " + customerDl, rightEdge - 8.0f, custY + 26.0f, paint);
        }
        if (!StringsKt.isBlank(customerGst)) {
            canvas.drawText("GSTIN: " + customerGst, rightEdge - 8.0f, 38.0f + custY, paint);
        }
        float boxBottom = custY + 44.0f;
        canvas.drawLine(20.0f, boxBottom, rightEdge, boxBottom, linePaint);
        double totalSales = 0.0d;
        for (InvoiceEntity inv : invoices) {
            totalSales += inv.getNetAmount();
        }
        double totalPaid = 0.0d;
        for (InvoiceEntity inv : invoices) {
            totalPaid += inv.getPaidAmount();
        }
        double totalDue = 0.0d;
        for (InvoiceEntity inv : invoices) {
            totalDue += inv.getDueAmount();
        }
        Paint kpiPaint = new Paint();
        kpiPaint.setColor(Color.parseColor("#F1F5F9"));
        kpiPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(20.0f, boxBottom, rightEdge, boxBottom + 26.0f, kpiPaint);
        canvas.drawLine(20.0f, boxBottom + 26.0f, rightEdge, boxBottom + 26.0f, linePaint);
        Canvas canvas2 = canvas;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(8.0f);
        float kpiW = (rightEdge - 20.0f) / 4.0f;
        paint.setTypeface(Typeface.DEFAULT);
        canvas2.drawText("TOTAL BILLS", 20.0f + (0.5f * kpiW), boxBottom + 10.0f, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas2.drawText(invoices.size() + " Invoices", 20.0f + (0.5f * kpiW), boxBottom + 21.0f, paint);
        canvas.drawLine(20.0f + kpiW, boxBottom, 20.0f + kpiW, boxBottom + 26.0f, linePaint);
        paint.setTypeface(Typeface.DEFAULT);
        canvas2.drawText("TOTAL BUSINESS (সর্বমোট ব্যবসা)", 20.0f + (1.5f * kpiW), boxBottom + 10.0f, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
        String format = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(totalSales)}, 1));
        Intrinsics.checkNotNullExpressionValue(format, "format(...)");
        canvas2.drawText(format, 20.0f + (1.5f * kpiW), boxBottom + 21.0f, paint);
        canvas.drawLine(20.0f + (kpiW * 2.0f), boxBottom, 20.0f + (kpiW * 2.0f), boxBottom + 26.0f, linePaint);
        paint.setTypeface(Typeface.DEFAULT);
        canvas2.drawText("TOTAL PAID (জমা)", 20.0f + (2.5f * kpiW), boxBottom + 10.0f, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
        String format2 = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(totalPaid)}, 1));
        Intrinsics.checkNotNullExpressionValue(format2, "format(...)");
        canvas2.drawText(format2, 20.0f + (2.5f * kpiW), boxBottom + 21.0f, paint);
        canvas.drawLine(20.0f + (kpiW * 3.0f), boxBottom, 20.0f + (3.0f * kpiW), boxBottom + 26.0f, linePaint);
        paint.setTypeface(Typeface.DEFAULT);
        canvas2.drawText("MARKET DUE (বকেয়া)", 20.0f + (3.5f * kpiW), boxBottom + 10.0f, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        if (totalDue > 0.0d) {
            paint.setColor(Color.parseColor("#D32F2F"));
        }
        StringCompanionObject stringCompanionObject3 = StringCompanionObject.INSTANCE;
        String format3 = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(totalDue)}, 1));
        Intrinsics.checkNotNullExpressionValue(format3, "format(...)");
        canvas2.drawText(format3, 20.0f + (3.5f * kpiW), boxBottom + 21.0f, paint);
        paint.setColor(-16777216);
        float tableTopY2 = boxBottom + 26.0f;
        canvas2.drawRect(20.0f, tableTopY2, rightEdge, tableTopY2 + 20.0f, linePaint);
        float[] colWidths = {30.0f, 65.0f, 65.0f, 75.0f, 90.0f, 80.0f, 80.0f, 65.0f};
        String[] colTitles = {"SL", "INV NO", "DATE", "QTY/ITEMS", "BILL AMT (₹)", "PAID (₹)", "DUE (₹)", "STATUS"};
        paint.setTextSize(7.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        int length = colTitles.length;
        int i = 0;
        float curX = 20.0f;
        while (i < length) {
            float w = colWidths[i];
            int i2 = length;
            paint.setTextAlign((i < 4 || i > 6) ? Paint.Align.CENTER : Paint.Align.RIGHT);
            float textX = (i < 4 || i > 6) ? curX + (w / 2.0f) : (curX + w) - 4.0f;
            float bannerY2 = tableTopY;
            Paint bgPaint3 = bgPaint2;
            canvas2.drawText(colTitles[i], textX, tableTopY2 + 13.0f, paint);
            if (i < colTitles.length - 1) {
                canvas2.drawLine(curX + w, tableTopY2, curX + w, (842 - 20.0f) - 40.0f, linePaint);
            }
            curX += w;
            i++;
            tableTopY2 = tableTopY2;
            length = i2;
            tableTopY = bannerY2;
            bgPaint2 = bgPaint3;
        }
        float tableTopY3 = tableTopY2;
        float rowY = tableTopY3 + 20.0f + 12.0f;
        paint.setTextSize(7.0f);
        paint.setTypeface(Typeface.DEFAULT);
        Iterable take = CollectionsKt.take(invoices, 45);
        int i3 = 0;
        int i4 = 0;
        float rowY2 = rowY;
        for (Object obj : take) {
            int i5 = i4 + 1;
            if (i4 < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            InvoiceEntity invoiceEntity = (InvoiceEntity) obj;
            Iterable iterable = take;
            paint.setTextAlign(Paint.Align.CENTER);
            int i6 = i3;
            canvas2.drawText(String.valueOf(i4 + 1), 20.0f + (colWidths[0] / 2.0f), rowY2, paint);
            float f = 20.0f + colWidths[0];
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            float tableTopY4 = tableTopY3;
            canvas2.drawText("#" + invoiceEntity.getInvoiceNumber(), f + (colWidths[1] / 2.0f), rowY2, paint);
            paint.setTypeface(Typeface.DEFAULT);
            float f2 = f + colWidths[1];
            canvas2.drawText(invoiceEntity.getDateFormatted(), f2 + (colWidths[2] / 2.0f), rowY2, paint);
            float f3 = f2 + colWidths[2];
            canvas2.drawText(invoiceEntity.getTotalQty() + " pcs (" + invoiceEntity.getItemCount() + ")", f3 + (colWidths[3] / 2.0f), rowY2, paint);
            float f4 = f3 + colWidths[3];
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            StringCompanionObject stringCompanionObject4 = StringCompanionObject.INSTANCE;
            String format4 = String.format(Locale.ENGLISH, "%.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoiceEntity.getNetAmount())}, 1));
            Intrinsics.checkNotNullExpressionValue(format4, "format(...)");
            canvas2.drawText(format4, (f4 + colWidths[4]) - 4.0f, rowY2, paint);
            paint.setTypeface(Typeface.DEFAULT);
            float f5 = f4 + colWidths[4];
            StringCompanionObject stringCompanionObject5 = StringCompanionObject.INSTANCE;
            String format5 = String.format(Locale.ENGLISH, "%.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoiceEntity.getPaidAmount())}, 1));
            Intrinsics.checkNotNullExpressionValue(format5, "format(...)");
            canvas2.drawText(format5, (f5 + colWidths[5]) - 4.0f, rowY2, paint);
            float f6 = f5 + colWidths[5];
            if (invoiceEntity.getDueAmount() > 0.0d) {
                paint.setColor(Color.parseColor("#D32F2F"));
            }
            StringCompanionObject stringCompanionObject6 = StringCompanionObject.INSTANCE;
            String format6 = String.format(Locale.ENGLISH, "%.2f", Arrays.copyOf(new Object[]{Double.valueOf(invoiceEntity.getDueAmount())}, 1));
            Intrinsics.checkNotNullExpressionValue(format6, "format(...)");
            canvas2.drawText(format6, (f6 + colWidths[6]) - 4.0f, rowY2, paint);
            paint.setColor(-16777216);
            float f7 = f6 + colWidths[6];
            paint.setTextAlign(Paint.Align.CENTER);
            String str = invoiceEntity.getDueAmount() <= 0.0d ? "PAID" : "DUE";
            paint.setColor(Intrinsics.areEqual(str, "PAID") ? Color.parseColor("#0288D1") : Color.parseColor("#D32F2F"));
            canvas2.drawText(str, (colWidths[7] / 2.0f) + f7, rowY2, paint);
            paint.setColor(-16777216);
            Canvas canvas3 = canvas2;
            canvas3.drawLine(20.0f, rowY2 + 4.0f, rightEdge, rowY2 + 4.0f, linePaint);
            rowY2 += 13.0f;
            canvas2 = canvas3;
            i4 = i5;
            take = iterable;
            i3 = i6;
            tableTopY3 = tableTopY4;
        }
        Canvas canvas4 = canvas2;
        float summaryY = (842 - 20.0f) - 40.0f;
        canvas4.drawRect(20.0f, summaryY, rightEdge, summaryY + 16.0f, linePaint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas4.drawText("GRAND TOTAL (" + invoices.size() + " Bills)", 20.0f + 6.0f, summaryY + 11.0f, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        float totalRx = 20.0f + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3];
        StringCompanionObject stringCompanionObject7 = StringCompanionObject.INSTANCE;
        String format7 = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(totalSales)}, 1));
        Intrinsics.checkNotNullExpressionValue(format7, "format(...)");
        canvas4.drawText(format7, (colWidths[4] + totalRx) - 4.0f, summaryY + 11.0f, paint);
        float totalRx2 = totalRx + colWidths[4];
        StringCompanionObject stringCompanionObject8 = StringCompanionObject.INSTANCE;
        String format8 = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(totalPaid)}, 1));
        Intrinsics.checkNotNullExpressionValue(format8, "format(...)");
        canvas4.drawText(format8, (colWidths[5] + totalRx2) - 4.0f, summaryY + 11.0f, paint);
        float totalRx3 = colWidths[5] + totalRx2;
        if (totalDue > 0.0d) {
            paint.setColor(Color.parseColor("#D32F2F"));
        }
        StringCompanionObject stringCompanionObject9 = StringCompanionObject.INSTANCE;
        String format9 = String.format(Locale.ENGLISH, "₹ %.2f", Arrays.copyOf(new Object[]{Double.valueOf(totalDue)}, 1));
        Intrinsics.checkNotNullExpressionValue(format9, "format(...)");
        canvas4.drawText(format9, (colWidths[6] + totalRx3) - 4.0f, summaryY + 11.0f, paint);
        paint.setColor(-16777216);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(6.8f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String upperCase2 = settings.getBusinessName().toUpperCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(upperCase2, "toUpperCase(...)");
        canvas4.drawText("For " + upperCase2, rightEdge - 8.0f, (842 - 20.0f) - 15.0f, paint);
        paint.setTextSize(5.8f);
        paint.setTypeface(Typeface.DEFAULT);
        canvas4.drawText("(Authorised Signatory)", rightEdge - 8.0f, (842 - 20.0f) - 6.0f, paint);
        pdfDocument.finishPage(page);
        String cleanName = new Regex("[^a-zA-Z0-9]").replace(customerName, "_");
        File file = new File(getPdfDirectory(context), "Sales_History_" + cleanName + "_" + System.currentTimeMillis() + ".pdf");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            pdfDocument.writeTo(fileOutputStream);
            fileOutputStream.close();
            pdfDocument.close();
            return file;
        } catch (Exception e) {
            pdfDocument.close();
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}
