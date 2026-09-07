package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.NumberToWords
import com.example.util.PdfGenerator
import com.example.util.PrintHelper

@Composable
fun InvoiceViewDialog(
    invoice: InvoiceEntity,
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit,
    onEdit: (InvoiceEntity) -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val allInvoiceItems by viewModel.invoiceItems.collectAsState()
    val specificItemsFlow by viewModel.getItemsForInvoiceFlow(invoice.invoiceNumber).collectAsState(initial = emptyList())
    var directItemsFromDb by remember(invoice.invoiceNumber) { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }

    LaunchedEffect(invoice.invoiceNumber) {
        val loaded = viewModel.getItemsForInvoice(invoice.invoiceNumber)
        if (loaded.isNotEmpty()) {
            directItemsFromDb = loaded
        }
    }

    val items = remember(specificItemsFlow, directItemsFromDb, allInvoiceItems, invoice) {
        when {
            specificItemsFlow.isNotEmpty() -> specificItemsFlow
            directItemsFromDb.isNotEmpty() -> directItemsFromDb
            else -> allInvoiceItems.filter { it.invoiceNumber == invoice.invoiceNumber }
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(8.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Pinned Top Header Row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PureWhite,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TAX INVOICE / BILL",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyBluePrimary
                                )
                                Text(
                                    text = "Invoice #${invoice.invoiceNumber}  •  ${invoice.dateFormatted}",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }

                    HorizontalDivider(color = SkyBlueBorder)

                    // Smoothly Scrollable Full Page Content
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Business & Customer Details Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SkyBlueLight),
                            border = BorderStroke(1.dp, SkyBlueBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(settings.businessName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkText)
                                Text(settings.address, fontSize = 11.sp, color = MutedText)
                                Text("D.L. No: ${settings.dlNumber}  |  GSTIN: ${settings.gstNumber}", fontSize = 11.sp, color = MutedText)
                                Text("Phone: ${settings.contactNumber}", fontSize = 11.sp, color = MutedText)

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = SkyBlueBorder)

                                Text("Billed To:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SkyBlueSecondary)
                                Text("${invoice.customerName} (${invoice.customerType})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                if (invoice.customerPhone.isNotEmpty()) Text("Phone: ${invoice.customerPhone}", fontSize = 11.sp)
                                if (invoice.customerAddress.isNotEmpty()) Text("Address: ${invoice.customerAddress}", fontSize = 11.sp)
                                if (invoice.customerDl.isNotEmpty()) Text("D.L.: ${invoice.customerDl}", fontSize = 11.sp)
                                if (invoice.customerGstPan.isNotEmpty()) Text("GSTIN: ${invoice.customerGstPan}", fontSize = 11.sp)
                                if (invoice.doctorDetails.isNotEmpty()) Text("Doctor/Prescription: ${invoice.doctorDetails}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Items Table
                        val horizontalScrollState = rememberScrollState()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .background(SkyBlueContainer)
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Sl", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(30.dp))
                                        Text("Product Name", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(160.dp))
                                        Text("TYPE", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                        Text("Batch", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                                        Text("Exp", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                                        Text("Qty", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                        Text("Free", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                        Text("MRP", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                        Text("Rate", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                        Text("Disc%", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
                                        Text("Net(₹)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
                                    }

                                    HorizontalDivider(thickness = 1.dp, color = SkyBlueBorder)

                                    if (items.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No products / items recorded for this bill",
                                                fontSize = 12.sp,
                                                color = MutedText
                                            )
                                        }
                                    } else {
                                        items.forEachIndexed { index, item ->
                                            val rowBg = if (index % 2 == 0) PureWhite else OffWhite
                                            Row(
                                                modifier = Modifier
                                                    .background(rowBg)
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${index + 1}", fontSize = 11.sp, modifier = Modifier.width(30.dp))
                                                Text(item.productName, fontSize = 11.sp, modifier = Modifier.width(160.dp))
                                                Text(item.pack, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                                Text(item.batchNo, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                                                Text(item.expDate, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                                                Text("${item.qty}", fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                                Text("${item.freeQty}", fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                                Text(if (authState.isGuest) "••••••" else String.format("%.2f", item.mrp), fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                                Text(if (authState.isGuest) "••••••" else String.format("%.2f", item.price), fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                                Text("${item.discountPercent}%", fontSize = 11.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
                                                Text(if (authState.isGuest) "••••••" else String.format("%.2f", item.itemTotalAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
                                            }
                                            HorizontalDivider(thickness = 0.5.dp, color = LightBorder)
                                        }
                                    }
                                }
                            }
                        }

                        // Financial Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OffWhite),
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SummaryRow("Gross Amount:", if (authState.isGuest) "••••••" else "₹${String.format("%.2f", invoice.totalAmount)}")
                                SummaryRow("Discount:", if (authState.isGuest) "••••••" else "- ₹${String.format("%.2f", invoice.lessDiscount)}")
                                SummaryRow("GST (CGST + SGST):", if (authState.isGuest) "••••••" else "+ ₹${String.format("%.2f", invoice.cgstAmount + invoice.sgstAmount)}")
                                SummaryRow("ADJ CR/DR NOTE:", if (authState.isGuest) "••••••" else "₹${String.format("%.2f", invoice.adjustmentAmount)}")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                SummaryRow("Grand Total (Net):", if (authState.isGuest) "••••••" else "₹${String.format("%.2f", invoice.netAmount)}", isBold = true)
                                SummaryRow("Paid Amount:", if (authState.isGuest) "••••••" else "₹${String.format("%.2f", invoice.paidAmount)}", textColor = SuccessGreen)
                                SummaryRow("Due Amount:", if (authState.isGuest) "••••••" else "₹${String.format("%.2f", invoice.dueAmount)}", textColor = if (invoice.dueAmount > 0) AlertRed else SuccessGreen, isBold = true)

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (authState.isGuest) "Amount in words: ••••••" else "Amount in words: ${NumberToWords.convert(invoice.netAmount)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MutedText
                                )
                            }
                        }

                        if (invoice.note.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = OffWhite),
                                border = BorderStroke(1.dp, SkyBlueBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Notes,
                                        contentDescription = null,
                                        tint = SkyBluePrimary,
                                        modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            "Description / Note (Customer Record):",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MutedText
                                        )
                                        Text(
                                            invoice.note,
                                            fontSize = 12.sp,
                                            color = DarkText
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons inside scrollable area so user can scroll effortlessly
                        Text("Bill Actions", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (authState.isGuest) {
                                        showAdminOnlyDialog = true
                                    } else {
                                        val file = PdfGenerator.INSTANCE.generateInvoicePdf(context, settings, invoice, items)
                                        PrintHelper.sharePdf(context, file, "Invoice #${invoice.invoiceNumber}")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyBlueSecondary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (authState.isGuest) {
                                        showAdminOnlyDialog = true
                                    } else {
                                        val file = PdfGenerator.INSTANCE.generateInvoicePdf(context, settings, invoice, items)
                                        PrintHelper.printPdf(context, file, "Invoice_${invoice.invoiceNumber}")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Print", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (authState.isGuest) {
                                        showAdminOnlyDialog = true
                                    } else {
                                        val file = PdfGenerator.INSTANCE.generateInvoicePdf(context, settings, invoice, items)
                                        PrintHelper.downloadPdf(context, file, "Invoice_${invoice.invoiceNumber}.pdf")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download", fontSize = 11.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (authState.isGuest) {
                                        showAdminOnlyDialog = true
                                    } else {
                                        onEdit(invoice)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Modify / Edit Bill", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (authState.isGuest) {
                                        showAdminOnlyDialog = true
                                    } else {
                                        showDeleteConfirm = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Bill", fontSize = 11.sp)
                            }
                        }

                        // Generous bottom spacing so bottom elements clear system navigation easily
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Bill #${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, color = AlertRed) },
            text = {
                Text("This action will:\n• Reverse product quantities & free quantities back into inventory stock.\n• Reverse product sales entries in Stock Statement.\n• Remove this bill from Khata ledger & Sales History.\n• Permanently delete this bill from local & cloud databases.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(invoice.invoiceNumber)
                        showDeleteConfirm = false
                        onDismiss()
                        Toast.makeText(context, "Bill #${invoice.invoiceNumber} deleted. Stock reversed & Khata updated.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdminOnlyDialog) {
        AdminOnlyDialog(onDismiss = { showAdminOnlyDialog = false })
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    textColor: androidx.compose.ui.graphics.Color = DarkText
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) DarkText else MutedText
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}
