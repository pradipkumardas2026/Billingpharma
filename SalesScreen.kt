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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InvoiceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.PdfGenerator
import com.example.util.PrintHelper

@Composable
fun SalesScreen(
    viewModel: PharmaViewModel,
    onNewBill: () -> Unit,
    onViewInvoice: (InvoiceEntity) -> Unit,
    onEditInvoice: (InvoiceEntity) -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val invoiceItems by viewModel.invoiceItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }

    var invoiceToRecordPayment by remember { mutableStateOf<InvoiceEntity?>(null) }
    var paymentAmountStr by remember { mutableStateOf("") }
    var invoiceToDelete by remember { mutableStateOf<InvoiceEntity?>(null) }
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    val totalSales = remember(invoices) { invoices.sumOf { it.netAmount } }
    val totalPaid = remember(invoices) { invoices.sumOf { it.paidAmount } }
    val totalDue = remember(invoices) { invoices.sumOf { it.dueAmount } }

    val filteredInvoices = remember(invoices, searchQuery, selectedTypeFilter) {
        invoices.filter { inv ->
            val matchesType = selectedTypeFilter == "ALL" || inv.customerType.equals(selectedTypeFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                inv.invoiceNumber.toString().contains(searchQuery) ||
                inv.customerName.contains(searchQuery, ignoreCase = true) ||
                inv.dateStr.contains(searchQuery, ignoreCase = true)
            matchesType && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Financial KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryBox("Total Sales", if (authState.isGuest) "₹••••••" else "₹${String.format("%.2f", totalSales)}", SkyBluePrimary, modifier = Modifier.weight(1f))
            SummaryBox("Total Paid", if (authState.isGuest) "₹••••••" else "₹${String.format("%.2f", totalPaid)}", SuccessGreen, modifier = Modifier.weight(1f))
            SummaryBox("Total Due", if (authState.isGuest) "₹••••••" else "₹${String.format("%.2f", totalDue)}", AlertRed, modifier = Modifier.weight(1f))
        }

        // Quick Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.switchTab(NavigationTab.SALES_HISTORY) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlueSecondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sales History", fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.navigateToSalesHistory() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("KHATA", fontSize = 12.sp)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Invoice #, Party, Date...") },
            modifier = Modifier.fillMaxWidth().testTag("sales_search_field"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SkyBluePrimary,
                unfocusedBorderColor = SkyBlueBorder
            )
        )

        // Type Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "PARTY", "DOCTOR", "PATIENT").forEach { type ->
                FilterChip(
                    selected = selectedTypeFilter == type,
                    onClick = { selectedTypeFilter = type },
                    label = { Text(type, fontSize = 11.sp) }
                )
            }
        }

        // Invoices List
        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No invoices found.",
                    color = MutedText,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredInvoices, key = { it.id }) { inv ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bill #${inv.invoiceNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = inv.dateStr,
                                    fontSize = 12.sp,
                                    color = MutedText
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${inv.customerName} (${inv.customerType})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = SkyBlueSecondary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (authState.isGuest) "Total: ••••••" else "Total: ₹${String.format("%.2f", inv.netAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (authState.isGuest) "Paid: ••••••" else "Paid: ₹${String.format("%.2f", inv.paidAmount)}", fontSize = 12.sp, color = SuccessGreen)
                                Text(if (authState.isGuest) "Due: ••••••" else "Due: ₹${String.format("%.2f", inv.dueAmount)}", fontSize = 12.sp, color = if (inv.dueAmount > 0) AlertRed else SuccessGreen, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onViewInvoice(inv) },
                                    modifier = Modifier.height(32.dp),
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
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
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
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = "Print", tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
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
                                        modifier = Modifier.height(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Pay", fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        if (authState.isGuest) {
                                            showAdminOnlyDialog = true
                                        } else {
                                            onEditInvoice(inv)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Bill", tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (authState.isGuest) {
                                            showAdminOnlyDialog = true
                                        } else {
                                            invoiceToDelete = inv
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Bill", tint = AlertRed, modifier = Modifier.size(18.dp))
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
            title = { Text("Payment for Bill #${inv.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer: ${inv.customerName}")
                    Text("Outstanding Due: ₹${String.format("%.2f", inv.dueAmount)}", color = AlertRed, fontWeight = FontWeight.Bold)

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

    invoiceToDelete?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = { Text("Delete & Reverse Stock", fontWeight = FontWeight.Bold, color = AlertRed) },
            text = { Text("Are you sure you want to delete Bill #${inv.invoiceNumber}? All sold stock will be safely returned to inventory.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(inv.invoiceNumber)
                        invoiceToDelete = null
                        Toast.makeText(context, "Bill #${inv.invoiceNumber} deleted and stock reversed!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) {
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
fun SummaryBox(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 11.sp, color = MutedText)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
