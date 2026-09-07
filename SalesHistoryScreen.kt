package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.DateUtils
import com.example.util.PrintHelper
import java.text.SimpleDateFormat
import java.util.*

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

    // Date selection filter: TODAY, YESTERDAY, THIS_MONTH, ALL, CUSTOM
    var selectedDateMode by remember { mutableStateOf("TODAY") }
    var customSelectedDateStr by remember { mutableStateOf(DateUtils.currentDateString()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPartyFilter by remember { mutableStateOf("ALL") }
    var partyDropdownExpanded by remember { mutableStateOf(false) }

    var invoiceToDelete by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToRecordPayment by remember { mutableStateOf<InvoiceEntity?>(null) }
    var paymentAmountStr by remember { mutableStateOf("") }
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    // Calendar for Yesterday & Month calculations
    val calendar = Calendar.getInstance()
    val todayStr = remember { DateUtils.currentDateString() }
    val yesterdayStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(cal.time)
    }
    val currentMonthYear = remember {
        SimpleDateFormat("MM/yyyy", Locale.ENGLISH).format(Date())
    }

    // Filtered Invoices based on selected Date & Party
    val filteredInvoices = remember(invoices, selectedDateMode, customSelectedDateStr, selectedPartyFilter, searchQuery) {
        invoices.filter { inv ->
            // Date matching
            val matchesDate = when (selectedDateMode) {
                "TODAY" -> inv.dateFormatted == todayStr || DateUtils.isToday(inv.date)
                "YESTERDAY" -> inv.dateFormatted == yesterdayStr
                "THIS_MONTH" -> inv.dateFormatted.endsWith(currentMonthYear)
                "CUSTOM" -> inv.dateFormatted == customSelectedDateStr
                "ALL" -> true
                else -> true
            }

            // Party matching
            val matchesParty = selectedPartyFilter == "ALL" ||
                inv.customerName.equals(selectedPartyFilter, ignoreCase = true)

            // Search matching
            val matchesSearch = searchQuery.isBlank() ||
                inv.customerName.contains(searchQuery, ignoreCase = true) ||
                inv.invoiceNumber.toString().contains(searchQuery) ||
                inv.customerPhone.contains(searchQuery) ||
                inv.customerDl.contains(searchQuery, ignoreCase = true)

            matchesDate && matchesParty && matchesSearch
        }.sortedByDescending { it.invoiceNumber }
    }

    val totalSales = remember(filteredInvoices) { filteredInvoices.sumOf { it.netAmount } }
    val totalPaid = remember(filteredInvoices) { filteredInvoices.sumOf { it.paidAmount } }
    val totalDue = remember(filteredInvoices) { filteredInvoices.sumOf { it.dueAmount } }

    // All known party names for filter dropdown
    val partyNamesList = remember(parties, invoices) {
        val list = mutableListOf("ALL")
        parties.forEach { if (!list.contains(it.partyName)) list.add(it.partyName) }
        invoices.forEach { if (!list.contains(it.customerName)) list.add(it.customerName) }
        list
    }

    // Android DatePicker Dialog launcher
    fun showDatePicker() {
        val cal = Calendar.getInstance()
        val parts = customSelectedDateStr.split("/")
        if (parts.size == 3) {
            cal.set(Calendar.DAY_OF_MONTH, parts[0].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.MONTH, (parts[1].toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1)
            cal.set(Calendar.YEAR, parts[2].toIntOrNull() ?: cal.get(Calendar.YEAR))
        }
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.ENGLISH, "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                customSelectedDateStr = formatted
                selectedDateMode = "CUSTOM"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "Sales History (বিক্রয় ইতিহাস)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        text = when (selectedDateMode) {
                            "TODAY" -> "Today: $todayStr (${filteredInvoices.size} bills)"
                            "YESTERDAY" -> "Yesterday: $yesterdayStr (${filteredInvoices.size} bills)"
                            "THIS_MONTH" -> "This Month: $currentMonthYear (${filteredInvoices.size} bills)"
                            "CUSTOM" -> "Date: $customSelectedDateStr (${filteredInvoices.size} bills)"
                            else -> "All Dates (${filteredInvoices.size} bills)"
                        },
                        fontSize = 11.sp,
                        color = SkyBluePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = onNewBill,
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Bill", fontSize = 12.sp)
            }
        }

        // Date Selection Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedDateMode == "TODAY",
                onClick = { selectedDateMode = "TODAY" },
                label = { Text("Today (আজ)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedDateMode == "YESTERDAY",
                onClick = { selectedDateMode = "YESTERDAY" },
                label = { Text("Yesterday", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedDateMode == "THIS_MONTH",
                onClick = { selectedDateMode = "THIS_MONTH" },
                label = { Text("This Month", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedDateMode == "ALL",
                onClick = { selectedDateMode = "ALL" },
                label = { Text("All Dates", fontSize = 11.sp) }
            )
        }

        // Custom Date Picker & Party Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showDatePicker() },
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selectedDateMode == "CUSTOM") SkyBlueContainer else PureWhite
                ),
                border = BorderStroke(1.dp, if (selectedDateMode == "CUSTOM") SkyBluePrimary else SkyBlueBorder),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp), tint = SkyBluePrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedDateMode == "CUSTOM") customSelectedDateStr else "Pick Date (তারিখ)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Party Filter Dropdown
            ExposedDropdownMenuBox(
                expanded = partyDropdownExpanded,
                onExpandedChange = { partyDropdownExpanded = it },
                modifier = Modifier.weight(1.3f)
            ) {
                OutlinedTextField(
                    value = if (selectedPartyFilter == "ALL") "All Parties" else selectedPartyFilter,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyDropdownExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )
                ExposedDropdownMenu(
                    expanded = partyDropdownExpanded,
                    onDismissRequest = { partyDropdownExpanded = false }
                ) {
                    partyNamesList.forEach { pName ->
                        DropdownMenuItem(
                            text = { Text(if (pName == "ALL") "All Parties / Customers" else pName, fontSize = 12.sp) },
                            onClick = {
                                selectedPartyFilter = pName
                                partyDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Party name, Bill #, phone, D.L. #...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().testTag("sales_history_search"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SkyBluePrimary,
                unfocusedBorderColor = SkyBlueBorder
            )
        )

        // Summary Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, LightBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Billed", fontSize = 10.sp, color = MutedText)
                    Text(if (authState.isGuest) "₹••••••" else "₹${String.format(Locale.ENGLISH, "%.2f", totalSales)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SkyBluePrimary)
                }
                Column {
                    Text("Total Paid", fontSize = 10.sp, color = MutedText)
                    Text(if (authState.isGuest) "₹••••••" else "₹${String.format(Locale.ENGLISH, "%.2f", totalPaid)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
                Column {
                    Text("Balance Due", fontSize = 10.sp, color = MutedText)
                    Text(if (authState.isGuest) "₹••••••" else "₹${String.format(Locale.ENGLISH, "%.2f", totalDue)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (totalDue > 0) AlertRed else SuccessGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Bills Count", fontSize = 10.sp, color = MutedText)
                    Text("${filteredInvoices.size} Invoices", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
                }
            }
        }

        // Bill List
        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = SkyBlueBorder,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "No sales records found for this date & filter.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DarkText
                    )
                    Text(
                        text = "Try picking another date or click 'All Dates' to view all bills.",
                        fontSize = 12.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = {
                            selectedDateMode = "ALL"
                            selectedPartyFilter = "ALL"
                            searchQuery = ""
                        }
                    ) {
                        Text("Show All Sales History")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredInvoices, key = { it.id }) { inv ->
                    val billProducts = remember(inv.invoiceNumber, invoiceItems) {
                        invoiceItems.filter { it.invoiceNumber == inv.invoiceNumber }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Top Header: Bill Number, Date, Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Bill #${inv.invoiceNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SkyBluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (inv.dueAmount <= 0) SuccessGreen.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (inv.dueAmount <= 0) "PAID" else "DUE: ₹${String.format(Locale.ENGLISH, "%.2f", inv.dueAmount)}",
                                            color = if (inv.dueAmount <= 0) SuccessGreen else AlertRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = inv.dateStr,
                                    fontSize = 11.sp,
                                    color = MutedText,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Party details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = inv.customerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DarkText
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Type: ${inv.customerType}",
                                            fontSize = 11.sp,
                                            color = MutedText
                                        )
                                        if (inv.customerDl.isNotBlank()) {
                                            Text(
                                                text = "D.L: ${inv.customerDl}",
                                                fontSize = 11.sp,
                                                color = SkyBlueText,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (inv.customerPhone.isNotBlank()) {
                                            Text(
                                                text = "Ph: ${inv.customerPhone}",
                                                fontSize = 11.sp,
                                                color = MutedText
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(thickness = 0.5.dp, color = LightBorder)

                            // Full Bill Products Details (পণ্য ও ফ্রি মালের বিবরণ)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Product (পণ্য)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkText, modifier = Modifier.weight(2f))
                                    Text("Qty", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkText, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                    Text("Free", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SkyBluePrimary, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                    Text("Rate", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkText, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text("Total", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkText, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = SkyBlueBorder)

                                if (billProducts.isEmpty()) {
                                    Text("Total items: ${inv.itemCount} (Qty: ${inv.totalQty}, Free: ${inv.totalFree})", fontSize = 11.sp, color = MutedText)
                                } else {
                                    billProducts.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(2f)) {
                                                Text(item.productName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                                                Text(
                                                    "${item.pack.ifBlank { "Standard" }} | Batch: ${item.batchNo.ifBlank { "N/A" }} | Exp: ${item.expDate.ifBlank { "N/A" }}",
                                                    fontSize = 9.sp,
                                                    color = MutedText
                                                )
                                            }
                                            Text("${item.qty}", fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                            Text(
                                                "${item.freeQty}",
                                                fontSize = 11.sp,
                                                fontWeight = if (item.freeQty > 0) FontWeight.Bold else FontWeight.Normal,
                                                color = if (item.freeQty > 0) SkyBluePrimary else DarkText,
                                                modifier = Modifier.weight(0.7f),
                                                textAlign = TextAlign.End
                                            )
                                            Text("₹${String.format(Locale.ENGLISH, "%.2f", item.price)}", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                            Text(
                                                if (authState.isGuest) "••••" else "₹${String.format(Locale.ENGLISH, "%.2f", item.itemTotalAmount)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1.1f),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }
                            }

                            // Financials row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (authState.isGuest) "Total: ••••••" else "Bill Total: ₹${String.format(Locale.ENGLISH, "%.2f", inv.netAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                                Text(
                                    text = if (authState.isGuest) "Paid: ••••••" else "Paid: ₹${String.format(Locale.ENGLISH, "%.2f", inv.paidAmount)}",
                                    fontSize = 11.sp,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (authState.isGuest) "Due: ••••••" else "Due: ₹${String.format(Locale.ENGLISH, "%.2f", inv.dueAmount)}",
                                    fontSize = 11.sp,
                                    color = if (inv.dueAmount > 0) AlertRed else SuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Action Buttons: View Bill, Edit Bill, Delete Bill, Collect Payment
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Side-by-side View Bill button requested by user
                                Button(
                                    onClick = { onViewInvoice(inv) },
                                    modifier = Modifier.weight(1.4f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = "View Bill", modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View Bill (বিল দেখুন)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.loadInvoiceForEdit(inv)
                                        viewModel.switchTab(com.example.ui.viewmodel.NavigationTab.NEW_BILL)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Bill", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Edit", fontSize = 11.sp)
                                }

                                if (inv.dueAmount > 0 && !authState.isGuest) {
                                    Button(
                                        onClick = {
                                            invoiceToRecordPayment = inv
                                            paymentAmountStr = String.format(Locale.ENGLISH, "%.2f", inv.dueAmount)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("Pay", fontSize = 11.sp)
                                    }
                                }

                                if (authState.isAdmin) {
                                    IconButton(
                                        onClick = { invoiceToDelete = inv },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Bill",
                                            tint = AlertRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Invoice Confirmation Dialog
    // "Party khata theke jakhn Kono bill jakhan delete hobe sei product sei free goods sab abar stock statement a ager moto bose jabe"
    invoiceToDelete?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AlertRed, modifier = Modifier.size(32.dp)) },
            title = { Text("Delete Bill #${inv.invoiceNumber}?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Are you sure you want to permanently delete this bill?")
                    Text("• Party: ${inv.customerName}", fontWeight = FontWeight.SemiBold)
                    Text("• Amount: ₹${String.format(Locale.ENGLISH, "%.2f", inv.netAmount)}")
                    Text("• All products and free goods will be completely reversed into inventory stock statement.", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    Text("• This bill will be removed from Sales History, Khata, and GST Registers.", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(inv.invoiceNumber)
                        invoiceToDelete = null
                        Toast.makeText(context, "Bill #${inv.invoiceNumber} deleted and stock/free goods reversed!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Confirm Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { invoiceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Record Payment Dialog
    invoiceToRecordPayment?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToRecordPayment = null },
            title = { Text("Record Payment for Bill #${inv.invoiceNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Customer: ${inv.customerName}")
                    Text("Total: ₹${String.format(Locale.ENGLISH, "%.2f", inv.netAmount)}  |  Due: ₹${String.format(Locale.ENGLISH, "%.2f", inv.dueAmount)}", fontWeight = FontWeight.SemiBold, color = AlertRed)
                    OutlinedTextField(
                        value = paymentAmountStr,
                        onValueChange = { paymentAmountStr = it },
                        label = { Text("Payment Received (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = paymentAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.recordPayment(inv.invoiceNumber, amt)
                            invoiceToRecordPayment = null
                            Toast.makeText(context, "Payment of ₹$amt recorded!", Toast.LENGTH_SHORT).show()
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
