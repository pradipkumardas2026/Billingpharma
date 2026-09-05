package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PrintHelper {

    fun downloadPdf(context: Context, pdfFile: File, targetFileName: String): File? {
        return try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, targetFileName)
            pdfFile.copyTo(destFile, overwrite = true)
            Toast.makeText(context, "Saved to Downloads: " + destFile.name, Toast.LENGTH_LONG).show()
            destFile
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to download PDF: " + e.message, Toast.LENGTH_SHORT).show()
            null
        }
    }

    fun openPdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF with..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf(context: Context, file: File, subject: String = "PharmaBill Invoice") {
        try {
            val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Shared from PharmaBill Management Software.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share PDF via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share PDF: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun printPdf(context: Context, file: File, jobName: String = "PharmaBill_Print") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "Print service not available on device", Toast.LENGTH_SHORT).show()
                return
            }
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                    } else {
                        val info = PrintDocumentInfo.Builder(jobName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        val input = FileInputStream(file)
                        val output = FileOutputStream(destination?.fileDescriptor)
                        val buf = ByteArray(4096)
                        var bytesRead: Int
                        while (input.read(buf).also { bytesRead = it } >= 0) {
                            if (cancellationSignal?.isCanceled == true) {
                                callback?.onWriteCancelled()
                                input.close()
                                output.close()
                                return
                            }
                            output.write(buf, 0, bytesRead)
                        }
                        input.close()
                        output.close()
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
            printManager.print(jobName, printAdapter, printAttributes)
        } catch (e: Exception) {
            Toast.makeText(context, "Printing error: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }
}
