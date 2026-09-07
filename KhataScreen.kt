package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.InvoiceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.PdfGenerator
import com.example.util.PrintHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataScreen(
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val invoiceItems by viewModel.invoiceItems.collectAsState()
    val parties by viewModel.parties.collectAsState()
    val khataRows by remember { derivedStateOf { viewModel.getKhataRows() } }

    var selectedPartyName by remember { mutableStateOf("ALL") }
    var partyDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf("PARTY_WISE") } // "PARTY_WISE" or "TABLE"

    var invoiceToDelete by remember { mutableStateOf<InvoiceEntity?>(null) }
    var viewingInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    // Collect all unique party names
    val allPartyNames = remember(parties, invoices) {
        val list = mutableListOf("ALL")
        parties.forEach { if (!list.contains(it.partyName)) list.add(it.partyName) }
        invoices.forEach { if (!list.contains(it.customerName)) list.add(it.customerName) }
        list
    }

    // Matching party entity if a specific party is selected
    val selectedPartyEntity = remember(selectedPartyName, parties) {
        if (selectedPartyName == "ALL") null
        else parties.firstOrNull { it.partyName.equals(selectedPartyName, ignoreCase = true) }
    }

    // Invoices for selected party
    val partyInvoices = remember(invoices, selectedPartyName, searchQuery) {
        invoices.filter { inv ->
            val matchesParty = selectedPartyName == "ALL" || inv.customerName.equals(selectedPartyName, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                inv.customerName.contains(searchQuery, ignoreCase = true) ||
                inv.invoiceNumber.toString().contains(searchQuery) ||
                inv.customerDl.contains(searchQuery, ignoreCase = true) ||
                inv.customerPhone.contains(searchQuery)
            matchesParty && matchesSearch
        }.sortedByDescending { it.invoiceNumber }
    }

    val totalBilled = remember(partyInvoices) { partyInvoices.sumOf { it.netAmount } }
    val totalPaid = remember(partyInvoices) { partyInvoices.sumOf { it.paidAmount } }
    val totalDue = remember(partyInvoices) { partyInvoices.sumOf { it.dueAmount } }

    // Filtered Khata rows for the classic table view
    val filteredRows = remember(khataRows, selectedPartyName, searchQuery) {
        khataRows.filter { row ->
            val matchesParty = selectedPartyName == "ALL" || row.partyName.equals(selectedPartyName, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                row.partyName.contains(searchQuery, ignoreCase = true) ||
                row.productName.contains(searchQuery, ignoreCase = true) ||
                row.invoiceNumber.toString().contains(searchQuery) ||
                row.note.contains(searchQuery, ignoreCase = true)
            matchesParty && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KHATA REGISTER / SALES BOOK (খাতা)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SkyBluePrimary
                        )
                        Text(
                            text = settings.businessName,
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Party Selector Dropdown (Party Wise Selection)
                ExposedDropdownMenuBox(
                    expanded = partyDropdownExpanded,
                    onExpandedChange = { partyDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedPartyName == "ALL") "All Parties / Customers (সকল পার্টি)" else "Selected Party: $selectedPartyName",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Party for Khata (পার্টি নির্বাচন করুন)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SkyBluePrimary) },
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
                        allPartyNames.forEach { pName ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (pName == "ALL") "ALL PARTIES (সকল পার্টি)" else pName,
                                        fontWeight = if (pName == selectedPartyName) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    selectedPartyName = pName
                                    partyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Selected Party Profile Banner (if specific party selected)
                if (selectedPartyName != "ALL") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SkyBlueLight),
                        border = BorderStroke(1.dp, SkyBlueBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedPartyName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                                Text(
                                    text = "${partyInvoices.size} Invoices",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SkyBluePrimary
                                )
                            }
                            selectedPartyEntity?.let { p ->
                                if (p.dlNumber.isNotBlank()) Text("D.L. No: ${p.dlNumber}", fontSize = 11.sp, color = DarkText)
                                if (p.contactNumber.isNotBlank()) Text("Phone: ${p.contactNumber}", fontSize = 11.sp, color = MutedText)
                                if (p.address.isNotBlank()) Text("Address: ${p.address}", fontSize = 11.sp, color = MutedText)
                                if (p.gstPanNumber.isNotBlank()) Text("GST/PAN: ${p.gstPanNumber}", fontSize = 11.sp, color = MutedText)
                            }
                        }
                    }
                }

                // Summary Numbers & View Switcher Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, LightBorder),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("Total Billed", fontSize = 9.sp, color = MutedText)
                            Text(if (authState.isGuest) "••••" else "₹${String.format(Locale.ENGLISH, "%.2f", totalBilled)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyBluePrimary)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, LightBorder),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("Total Paid", fontSize = 9.sp, color = MutedText)
                            Text(if (authState.isGuest) "••••" else "₹${String.format(Locale.ENGLISH, "%.2f", totalPaid)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, LightBorder),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("Balance Due", fontSize = 9.sp, color = MutedText)
                            Text(if (authState.isGuest) "••••" else "₹${String.format(Locale.ENGLISH, "%.2f", totalDue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (totalDue > 0) AlertRed else SuccessGreen)
                        }
                    }
                }

                // Actions & Search Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search medicine, bill #...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    // View Mode Switcher
                    FilterChip(
                        selected = viewMode == "PARTY_WISE",
                        onClick = { viewMode = "PARTY_WISE" },
                        label = { Text("Details", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = viewMode == "TABLE",
                        onClick = { viewMode = "TABLE" },
                        label = { Text("Table", fontSize = 11.sp) }
                    )

                    IconButton(
                        onClick = {
                            if (authState.isGuest) {
                                showAdminOnlyDialog = true
                            } else {
                                val file = PdfGenerator.INSTANCE.generateKhataPdf(context, settings, filteredRows)
                                PrintHelper.sharePdf(context, file, "Khata Register - $selectedPartyName")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = SkyBluePrimary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            if (authState.isGuest) {
                                showAdminOnlyDialog = true
                            } else {
                                val file = PdfGenerator.INSTANCE.generateKhataPdf(context, settings, filteredRows)
                                PrintHelper.printPdf(context, file, "Khata_Register")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print PDF", tint = SkyBluePrimary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            if (authState.isGuest) {
                                showAdminOnlyDialog = true
                            } else {
                                val file = PdfGenerator.INSTANCE.generateKhataPdf(context, settings, filteredRows)
                                val targetName = "Khata_${selectedPartyName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
                                PrintHelper.downloadPdf(context, file, targetName)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    }
                }

                // Main Content
                if (viewMode == "PARTY_WISE") {
                    // Party Wise Detailed Bills View with Products and Free Goods
                    if (partyInvoices.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No billing records found for this selection.", color = MutedText, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(partyInvoices, key = { it.id }) { inv ->
                                val billProducts = remember(inv.invoiceNumber, invoiceItems) {
                                    invoiceItems.filter { it.invoiceNumber == inv.invoiceNumber }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                                    border = BorderStroke(1.dp, LightBorder)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Invoice Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Bill #${inv.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyBluePrimary)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(inv.customerName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DarkText)
                                            }
                                            Text(inv.dateStr, fontSize = 11.sp, color = MutedText)
                                        }

                                        // Products and Free Goods Table (পণ্য ও ফ্রি মালের তালিকা)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                                .padding(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Product Name (পণ্য)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkText, modifier = Modifier.weight(2f))
                                                Text("Qty", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkText, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                                Text("Free Goods", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = SkyBluePrimary, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                                                Text("Rate (₹)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkText, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                                                Text("Total (₹)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkText, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                            }
                                            HorizontalDivider(thickness = 0.5.dp, color = SkyBlueBorder)

                                            if (billProducts.isEmpty()) {
                                                Text("Total items: ${inv.itemCount} (Qty: ${inv.totalQty}, Free: ${inv.totalFree})", fontSize = 10.sp, color = MutedText)
                                            } else {
                                                billProducts.forEach { item ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(2f)) {
                                                            Text(item.productName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                                                            Text("Batch: ${item.batchNo.ifBlank { "N/A" }} | Exp: ${item.expDate.ifBlank { "N/A" }}", fontSize = 9.sp, color = MutedText)
                                                        }
                                                        Text("${item.qty}", fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                                        Text(
                                                            "${item.freeQty}",
                                                            fontSize = 11.sp,
                                                            fontWeight = if (item.freeQty > 0) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (item.freeQty > 0) SkyBluePrimary else DarkText,
                                                            modifier = Modifier.weight(0.9f),
                                                            textAlign = TextAlign.End
                                                        )
                                                        Text("₹${String.format(Locale.ENGLISH, "%.2f", item.price)}", fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                                                        Text(
                                                            if (authState.isGuest) "••••" else "₹${String.format(Locale.ENGLISH, "%.2f", item.itemTotalAmount)}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.weight(1f),
                                                            textAlign = TextAlign.End
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Bill Totals & Actions Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = if (authState.isGuest) "Total: ••••••" else "Bill: ₹${String.format(Locale.ENGLISH, "%.2f", inv.netAmount)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkText
                                                )
                                                Text(
                                                    text = if (authState.isGuest) "Paid: ••••••" else "Paid: ₹${String.format(Locale.ENGLISH, "%.2f", inv.paidAmount)}",
                                                    fontSize = 11.sp,
                                                    color = SuccessGreen
                                                )
                                                Text(
                                                    text = if (authState.isGuest) "Due: ••••••" else "Due: ₹${String.format(Locale.ENGLISH, "%.2f", inv.dueAmount)}",
                                                    fontSize = 11.sp,
                                                    color = if (inv.dueAmount > 0) AlertRed else SuccessGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                // View Bill
                                                IconButton(
                                                    onClick = { viewingInvoice = inv },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Visibility, contentDescription = "View Bill", tint = SkyBluePrimary, modifier = Modifier.size(17.dp))
                                                }

                                                // Modify/Edit Bill
                                                IconButton(
                                                    onClick = {
                                                        onDismiss()
                                                        viewModel.loadInvoiceForEdit(inv)
                                                        viewModel.switchTab(com.example.ui.viewmodel.NavigationTab.NEW_BILL)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Bill", tint = SkyBlueSecondary, modifier = Modifier.size(17.dp))
                                                }

                                                // Delete Bill from Khata: reverses all products & free goods back into inventory stock statement
                                                if (authState.isAdmin) {
                                                    IconButton(
                                                        onClick = { invoiceToDelete = inv },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Bill", tint = AlertRed, modifier = Modifier.size(17.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Classic Table View
                    val horizontalScrollState = rememberScrollState()
                    val verticalScrollState = rememberScrollState()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScrollState)
                                .verticalScroll(verticalScrollState)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .background(SkyBlueContainer)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Bill #", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                                    Text("Date", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                                    Text("Party / Customer", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(160.dp))
                                    Text("Product Name", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(200.dp))
                                    Text("Qty", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                    Text("Free", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                    Text("Bill Amt (₹)", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                                    Text("Note / Description", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(150.dp))
                                    if (authState.isAdmin) {
                                        Text("Action", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                    }
                                }

                                HorizontalDivider(thickness = 1.dp, color = SkyBlueBorder)

                                filteredRows.forEachIndexed { index, row ->
                                    val rowBg = if (index % 2 == 0) PureWhite else OffWhite
                                    Row(
                                        modifier = Modifier
                                            .background(rowBg)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(if (row.isFirstItemOfInvoice) "#${row.invoiceNumber}" else "", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                                        Text(if (row.isFirstItemOfInvoice) row.date else "", fontSize = 11.sp, modifier = Modifier.width(90.dp))
                                        Text(if (row.isFirstItemOfInvoice) row.partyName else "", fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(160.dp))
                                        Text(row.productName, fontSize = 11.sp, modifier = Modifier.width(200.dp))
                                        Text("${row.quantity}", fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                        Text("${row.free}", fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End, color = if (row.free > 0) SkyBluePrimary else DarkText)
                                        Text(
                                            if (row.isFirstItemOfInvoice) {
                                                if (authState.isGuest) "••••••" else String.format(Locale.ENGLISH, "%.2f", row.billAmount)
                                            } else "",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(90.dp),
                                            textAlign = TextAlign.End
                                        )
                                        Text(if (row.isFirstItemOfInvoice && row.note.isNotBlank()) row.note else "", fontSize = 11.sp, color = MutedText, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(150.dp).padding(horizontal = 4.dp))
                                        if (authState.isAdmin) {
                                            Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.Center) {
                                                if (row.isFirstItemOfInvoice) {
                                                    IconButton(
                                                        onClick = {
                                                            val inv = invoices.firstOrNull { it.invoiceNumber == row.invoiceNumber }
                                                            if (inv != null) invoiceToDelete = inv
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Bill", tint = AlertRed, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = LightBorder)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // View Bill Dialog
    viewingInvoice?.let { inv ->
        InvoiceViewDialog(
            invoice = inv,
            viewModel = viewModel,
            onDismiss = { viewingInvoice = null },
            onEdit = {
                viewingInvoice = null
                onDismiss()
                viewModel.loadInvoiceForEdit(it)
                viewModel.switchTab(com.example.ui.viewmodel.NavigationTab.NEW_BILL)
            }
        )
    }

    // Delete Bill Confirmation Dialog:
    // "Party khata theke jakhn Kono bill jakhan delete hobe sei product sei free goods sab abar stock statement a ager moto bose jabe"
    invoiceToDelete?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AlertRed, modifier = Modifier.size(32.dp)) },
            title = { Text("Delete Bill #${inv.invoiceNumber}?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Deleting this bill from Khata will:")
                    Text("1. Completely restore all sold products and free goods back into inventory stock.", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    Text("2. The Stock Statement opening/closing figures will automatically reset to previous state.", color = SkyBluePrimary, fontWeight = FontWeight.Bold)
                    Text("3. This bill will also be removed from GST Registers and Customer Ledgers.", color = AlertRed, fontWeight = FontWeight.Bold)
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
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { invoiceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdminOnlyDialog) {
        AdminOnlyDialog(onDismiss = { showAdminOnlyDialog = false })
    }
}
