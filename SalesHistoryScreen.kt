package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InvoiceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.PdfGenerator
import com.example.util.PrintHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    viewModel: PharmaViewModel,
    onBack: () -> Unit,
    onViewInvoice: (InvoiceEntity) -> Unit,
    onNewBill: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val invoiceItems by viewModel.invoiceItems.collectAsState()
    val parties by viewModel.parties.collectAsState()
    val doctors by viewModel.doctors.collectAsState()
    val patients by viewModel.patients.collectAsState()

    val salesHistoryTargetCustomer by viewModel.salesHistoryTargetCustomer.collectAsState()
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    val allCustomers = remember(parties, doctors, patients, invoices) {
        val list = mutableListOf<CustomerItem>()
        parties.forEach { list.add(CustomerItem(it.partyName, "PARTY", it.contactNumber, it.address, it.dlNumber, it.gstPanNumber)) }
        doctors.forEach { list.add(CustomerItem(it.doctorName, "DOCTOR", it.phoneNumber, it.address, "", "", it.qualification)) }
        patients.forEach { list.add(CustomerItem(it.patientName, "PATIENT", it.phoneNumber, it.address, "", "", it.doctorName)) }
        invoices.forEach { inv ->
            if (list.none { it.name.equals(inv.customerName, ignoreCase = true) }) {
                list.add(CustomerItem(inv.customerName, inv.customerType, inv.customerPhone, inv.customerAddress, inv.customerDl, inv.customerGstPan))
            }
        }
        list
    }

    var selectedCustomer by remember { mutableStateOf<CustomerItem?>(null) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(salesHistoryTargetCustomer, allCustomers) {
        salesHistoryTargetCustomer?.let { target ->
            val match = allCustomers.find { it.name.equals(target, ignoreCase = true) }
                ?: CustomerItem(name = target, type = "PARTY")
            selectedCustomer = match
        }
    }

    val customerInvoices = remember(selectedCustomer, invoices) {
        val cust = selectedCustomer
        if (cust == null) emptyList()
        else invoices.filter { it.customerName.equals(cust.name, ignoreCase = true) }
    }

    val totalBilled = remember(customerInvoices) { customerInvoices.sumOf { it.netAmount } }
    val totalPaid = remember(customerInvoices) { customerInvoices.sumOf { it.paidAmount } }
    val totalDue = remember(customerInvoices) { customerInvoices.sumOf { it.dueAmount } }

    var invoiceToRecordPayment by remember { mutableStateOf<InvoiceEntity?>(null) }
    var paymentAmountStr by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Customer Sales Ledger", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkText)
            }
        }

        // Customer Selection
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedCustomer?.let { "${it.name} (${it.type})" } ?: customerSearchQuery,
                onValueChange = {
                    customerSearchQuery = it
                    dropdownExpanded = true
                },
                placeholder = { Text("Search customer name, phone, address...") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SkyBluePrimary,
                    unfocusedBorderColor = SkyBlueBorder
                )
            )

            val matching = allCustomers.filter {
                customerSearchQuery.isBlank() ||
                it.name.contains(customerSearchQuery, ignoreCase = true) ||
                it.phone.contains(customerSearchQuery)
            }

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                matching.forEach { cust ->
                    DropdownMenuItem(
                        text = { Text("${cust.name} • ${cust.type} (${cust.phone})") },
                        onClick = {
                            selectedCustomer = cust
                            customerSearchQuery = cust.name
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        selectedCustomer?.let { cust ->
            // Customer Header & Statement Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SkyBlueLight),
                border = BorderStroke(1.dp, SkyBlueBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkText)
                    Text("Type: ${cust.type}  |  Phone: ${cust.phone}", fontSize = 11.sp, color = MutedText)
                    if (cust.address.isNotEmpty()) Text("Address: ${cust.address}", fontSize = 11.sp, color = MutedText)
                    if (cust.dl.isNotEmpty()) Text("D.L. No: ${cust.dl}", fontSize = 11.sp, color = MutedText)
                    if (cust.gst.isNotEmpty()) Text("GSTIN: ${cust.gst}", fontSize = 11.sp, color = MutedText)

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryBox("Total Billed", "₹${String.format(java.util.Locale.US, "%.2f", totalBilled)}", SkyBluePrimary, modifier = Modifier.weight(1f))
                        SummaryBox("Total Paid", "₹${String.format(java.util.Locale.US, "%.2f", totalPaid)}", SuccessGreen, modifier = Modifier.weight(1f))
                        SummaryBox("Balance Due", "₹${String.format(java.util.Locale.US, "%.2f", totalDue)}", AlertRed, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Statement Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                if (authState.isGuest) {
                                    showAdminOnlyDialog = true
                                } else {
                                    val file = PdfGenerator.INSTANCE.generateCustomerSalesHistoryPdf(
                                        context, settings, cust.name, cust.type, cust.phone, cust.address, cust.dl, cust.gst, customerInvoices
                                    )
                                    PrintHelper.sharePdf(context, file, "Statement ${cust.name}")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBlueSecondary),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Share Statement", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (authState.isGuest) {
                                    showAdminOnlyDialog = true
                                } else {
                                    val file = PdfGenerator.INSTANCE.generateCustomerSalesHistoryPdf(
                                        context, settings, cust.name, cust.type, cust.phone, cust.address, cust.dl, cust.gst, customerInvoices
                                    )
                                    PrintHelper.printPdf(context, file, "Statement_${cust.name}")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Print Statement", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (authState.isGuest) {
                                    showAdminOnlyDialog = true
                                } else {
                                    val file = PdfGenerator.INSTANCE.generateCustomerSalesHistoryPdf(
                                        context, settings, cust.name, cust.type, cust.phone, cust.address, cust.dl, cust.gst, customerInvoices
                                    )
                                    val targetName = "Statement_${cust.name}_${System.currentTimeMillis()}.pdf"
                                    PrintHelper.downloadPdf(context, file, targetName)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Download", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Customer Invoices List
            if (customerInvoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No billing records found for this customer.", color = MutedText, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customerInvoices, key = { it.id }) { inv ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Bill #${inv.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(inv.dateStr, fontSize = 11.sp, color = MutedText)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (authState.isGuest) "Total: ••••••" else "Total: ₹${String.format("%.2f", inv.netAmount)}", fontSize = 12.sp)
                                    Text(if (authState.isGuest) "Paid: ••••••" else "Paid: ₹${String.format("%.2f", inv.paidAmount)}", fontSize = 12.sp, color = SuccessGreen)
                                    Text(if (authState.isGuest) "Due: ••••••" else "Due: ₹${String.format("%.2f", inv.dueAmount)}", fontSize = 12.sp, color = if (inv.dueAmount > 0) AlertRed else SuccessGreen, fontWeight = FontWeight.Bold)
                                }

                                if (inv.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Notes,
                                                contentDescription = "Note",
                                                tint = SkyBluePrimary,
                                                modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "Description / Note:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MutedText
                                                )
                                                Text(
                                                    text = inv.note,
                                                    fontSize = 11.sp,
                                                    color = DarkText
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onViewInvoice(inv) },
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("View", fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = {
                                            if (authState.isGuest) {
                                                showAdminOnlyDialog = true
                                            } else {
                                                val items = invoiceItems.filter { it.invoiceNumber == inv.invoiceNumber }
                                                val file = PdfGenerator.INSTANCE.generateInvoicePdf(context, settings, inv, items)
                                                PrintHelper.sharePdf(context, file, "Invoice #${inv.invoiceNumber}")
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = SkyBluePrimary, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            if (authState.isGuest) {
                                                showAdminOnlyDialog = true
                                            } else {
                                                val items = invoiceItems.filter { it.invoiceNumber == inv.invoiceNumber }
                                                val file = PdfGenerator.INSTANCE.generateInvoicePdf(context, settings, inv, items)
                                                PrintHelper.printPdf(context, file, "Invoice_${inv.invoiceNumber}")
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = "Print", tint = SkyBluePrimary, modifier = Modifier.size(16.dp))
                                    }

                                    if (inv.dueAmount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Button(
                                            onClick = {
                                                if (authState.isGuest) {
                                                    showAdminOnlyDialog = true
                                                } else {
                                                    invoiceToRecordPayment = inv
                                                    paymentAmountStr = inv.dueAmount.toString()
                                                }
                                            },
                                            modifier = Modifier.height(30.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Pay", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    invoiceToRecordPayment?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToRecordPayment = null },
            title = { Text("Record Payment for Bill #${inv.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer: ${inv.customerName}")
                    Text("Due Balance: ₹${String.format("%.2f", inv.dueAmount)}", color = AlertRed, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = paymentAmountStr,
                        onValueChange = { paymentAmountStr = it },
                        label = { Text("Payment Received (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountStr.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.recordPayment(inv.invoiceNumber, amount)
                            Toast.makeText(context, "Payment recorded!", Toast.LENGTH_SHORT).show()
                            invoiceToRecordPayment = null
                        } else {
                            Toast.makeText(context, "Please enter valid payment amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Record Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToRecordPayment = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdminOnlyDialog) {
        AdminOnlyDialog(onDismiss = { showAdminOnlyDialog = false })
    }
}
