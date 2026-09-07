package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.DateUtils
import com.example.util.GstPdfGenerator
import com.example.util.PrintHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GstScreen(
    viewModel: PharmaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val purchases by viewModel.purchaseInvoices.collectAsState()
    val medicines by viewModel.medicines.collectAsState()

    // Sub-Admin & Guest Protection: "sub admin er kachhe option show korbe na"
    if (!authState.isMasterAdmin) {
        AlertDialog(
            onDismissRequest = onBack,
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AlertRed) },
            title = { Text("Master Admin Only") },
            text = { Text("The GST Tax Register and Filing Portal is strictly restricted to the Master Admin.") },
            confirmButton = {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                ) {
                    Text("Go Back")
                }
            }
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Selling GST, 1: Purchase GST, 2: GST Summary
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf("ALL") } // "ALL", "MONTH", "TODAY"

    // Dialog states
    var viewingInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var invoiceToDelete by remember { mutableStateOf<InvoiceEntity?>(null) }
    var purchaseToEdit by remember { mutableStateOf<PurchaseInvoiceEntity?>(null) }
    var purchaseToDelete by remember { mutableStateOf<PurchaseInvoiceEntity?>(null) }
    var showAddPurchaseDialog by remember { mutableStateOf(false) }

    // PDF Action Sheet dialog state
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var pdfTitle by remember { mutableStateOf("") }

    val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    val thisMonthPrefix = remember { SimpleDateFormat("/MM/yyyy", Locale.getDefault()).format(Date()) }

    // Filter Selling Invoices
    val filteredInvoices = remember(invoices, searchQuery, selectedDateFilter) {
        invoices.filter { inv ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                inv.invoiceNumber.toString().contains(searchQuery, ignoreCase = true) ||
                inv.customerName.contains(searchQuery, ignoreCase = true) ||
                inv.customerDl.contains(searchQuery, ignoreCase = true) ||
                inv.customerGstPan.contains(searchQuery, ignoreCase = true) ||
                inv.dateFormatted.contains(searchQuery, ignoreCase = true)
            }
            val matchesDate = when (selectedDateFilter) {
                "TODAY" -> inv.dateFormatted.startsWith(todayStr.take(10)) || DateUtils.isToday(inv.date)
                "MONTH" -> inv.dateFormatted.contains(thisMonthPrefix)
                else -> true
            }
            matchesSearch && matchesDate
        }
    }

    // Filter Purchase Invoices
    val filteredPurchases = remember(purchases, searchQuery, selectedDateFilter) {
        purchases.filter { p ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                p.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                p.companyName.contains(searchQuery, ignoreCase = true) ||
                p.companyGst.contains(searchQuery, ignoreCase = true) ||
                p.itemsSummary.contains(searchQuery, ignoreCase = true) ||
                p.dateFormatted.contains(searchQuery, ignoreCase = true)
            }
            val matchesDate = when (selectedDateFilter) {
                "TODAY" -> p.dateFormatted.startsWith(todayStr.take(10)) || DateUtils.isToday(p.date)
                "MONTH" -> p.dateFormatted.contains(thisMonthPrefix)
                else -> true
            }
            matchesSearch && matchesDate
        }
    }

    // Selling GST Calculations
    val totalSellingTaxable = remember(filteredInvoices) { filteredInvoices.sumOf { it.totalAmount } }
    val totalSellingCgst = remember(filteredInvoices) { filteredInvoices.sumOf { it.cgstAmount } }
    val totalSellingSgst = remember(filteredInvoices) { filteredInvoices.sumOf { it.sgstAmount } }
    val totalSellingGst = totalSellingCgst + totalSellingSgst // sgst + cgst = gst
    val totalSellingNet = remember(filteredInvoices) { filteredInvoices.sumOf { it.netAmount } }

    // Purchase GST Calculations
    val totalPurchaseTaxable = remember(filteredPurchases) { filteredPurchases.sumOf { it.taxableAmount } }
    val totalPurchaseCgst = remember(filteredPurchases) { filteredPurchases.sumOf { it.cgstAmount } }
    val totalPurchaseSgst = remember(filteredPurchases) { filteredPurchases.sumOf { it.sgstAmount } }
    val totalPurchaseGst = totalPurchaseCgst + totalPurchaseSgst // sgst + cgst = gst
    val totalPurchaseNet = remember(filteredPurchases) { filteredPurchases.sumOf { it.totalAmount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("GST Register", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AlertRed,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "MASTER ADMIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Purchase & Selling Tax Portal (জিএসটি বিবরণী)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkyBluePrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(OffWhite)
        ) {
            // Tab Navigation Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = SkyBluePrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Selling GST", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            Text("বিক্রয় জিএসটি (${filteredInvoices.size})", fontSize = 10.sp, color = if (selectedTab == 0) SkyBluePrimary else Color.Gray)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Purchase GST", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            Text("ক্রয় জিএসটি (${filteredPurchases.size})", fontSize = 10.sp, color = if (selectedTab == 1) SuccessGreen else Color.Gray)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("GST Summary", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            Text("GSTR-3B Tax", fontSize = 10.sp, color = if (selectedTab == 2) Color(0xFF6A1B9A) else Color.Gray)
                        }
                    }
                )
            }

            // Search Bar & Date Filter (Tabs 0 & 1)
            if (selectedTab == 0 || selectedTab == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    if (selectedTab == 0) "Search Invoice #, Party, DL No..."
                                    else "Search Bill #, Company, Items...",
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("gst_search_field"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyBluePrimary,
                                unfocusedBorderColor = SkyBlueBorder
                            )
                        )

                        // Print / Export PDF Button
                        Button(
                            onClick = {
                                if (settings != null) {
                                    val period = when (selectedDateFilter) {
                                        "TODAY" -> "Today ($todayStr)"
                                        "MONTH" -> "This Month (${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())})"
                                        else -> "All Records"
                                    }
                                    if (selectedTab == 0) {
                                        val file = GstPdfGenerator.generateSellingGstPdf(context, settings!!, filteredInvoices, period)
                                        generatedPdfFile = file
                                        pdfTitle = "Selling GST Register (GSTR-1)"
                                    } else {
                                        val file = GstPdfGenerator.generatePurchaseGstPdf(context, settings!!, filteredPurchases, period)
                                        generatedPdfFile = file
                                        pdfTitle = "Purchase GST Register (GSTR-2)"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 0) SkyBluePrimary else SuccessGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(50.dp).testTag("print_gst_pdf_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Date Filters & Add Purchase action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedDateFilter == "ALL",
                                onClick = { selectedDateFilter = "ALL" },
                                label = { Text("All Time", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedDateFilter == "MONTH",
                                onClick = { selectedDateFilter = "MONTH" },
                                label = { Text("This Month", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedDateFilter == "TODAY",
                                onClick = { selectedDateFilter = "TODAY" },
                                label = { Text("Today", fontSize = 11.sp) }
                            )
                        }

                        if (selectedTab == 1) {
                            Button(
                                onClick = { showAddPurchaseDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("add_purchase_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("+ Add Purchase", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Main Content Body based on tab
            when (selectedTab) {
                0 -> {
                    // SELLING GST VIEW
                    SellingGstContent(
                        invoices = filteredInvoices,
                        totalTaxable = totalSellingTaxable,
                        totalCgst = totalSellingCgst,
                        totalSgst = totalSellingSgst,
                        totalGst = totalSellingGst,
                        totalNet = totalSellingNet,
                        onViewInvoice = { viewingInvoice = it },
                        onDeleteInvoice = { invoiceToDelete = it }
                    )
                }
                1 -> {
                    // PURCHASE GST VIEW
                    PurchaseGstContent(
                        purchases = filteredPurchases,
                        totalTaxable = totalPurchaseTaxable,
                        totalCgst = totalPurchaseCgst,
                        totalSgst = totalPurchaseSgst,
                        totalGst = totalPurchaseGst,
                        totalNet = totalPurchaseNet,
                        onEditPurchase = { purchaseToEdit = it },
                        onDeletePurchase = { purchaseToDelete = it }
                    )
                }
                2 -> {
                    // GST SUMMARY VIEW (GSTR-3B)
                    GstSummaryContent(
                        sellingInvoicesCount = filteredInvoices.size,
                        sellingTaxable = totalSellingTaxable,
                        sellingCgst = totalSellingCgst,
                        sellingSgst = totalSellingSgst,
                        sellingTotalGst = totalSellingGst,
                        purchaseCount = filteredPurchases.size,
                        purchaseTaxable = totalPurchaseTaxable,
                        purchaseCgst = totalPurchaseCgst,
                        purchaseSgst = totalPurchaseSgst,
                        purchaseTotalGst = totalPurchaseGst,
                        onExportPdf = {
                            if (settings != null) {
                                val file = GstPdfGenerator.generateSellingGstPdf(context, settings!!, filteredInvoices, "GST Summary Statement")
                                generatedPdfFile = file
                                pdfTitle = "GST Sales & Tax Statement"
                            }
                        }
                    )
                }
            }
        }
    }

    // View Invoice Dialog
    if (viewingInvoice != null) {
        InvoiceViewDialog(
            invoice = viewingInvoice!!,
            viewModel = viewModel,
            onDismiss = { viewingInvoice = null },
            onEdit = {
                viewingInvoice = null
                viewModel.startEditInvoice(it)
                viewModel.navigateTo(com.example.ui.viewmodel.NavigationTab.NEW_BILL)
            }
        )
    }

    // Delete Invoice Confirmation Dialog:
    // "master admin bill delet korle oi gst file thekeo delete hobe."
    if (invoiceToDelete != null) {
        val inv = invoiceToDelete!!
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AlertRed, modifier = Modifier.size(32.dp)) },
            title = { Text("Delete Bill #${inv.invoiceNumber}?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Are you sure you want to permanently delete this bill?")
                    Text("• Party: ${inv.customerName}", fontWeight = FontWeight.SemiBold)
                    Text("• Amount: ₹${String.format("%.2f", inv.netAmount)} (CGST: ₹${String.format("%.2f", inv.cgstAmount)}, SGST: ₹${String.format("%.2f", inv.sgstAmount)})")
                    Text("• All inventory stock will be automatically reversed.", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    Text("• This bill and its GST will be instantly removed from this GST Register.", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInvoice(inv.invoiceNumber)
                        invoiceToDelete = null
                        Toast.makeText(context, "Bill #${inv.invoiceNumber} deleted and removed from GST Register", Toast.LENGTH_SHORT).show()
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

    // Add / Edit Purchase Dialog
    if (showAddPurchaseDialog || purchaseToEdit != null) {
        val initialPurchase = purchaseToEdit
        PurchaseEntryDialog(
            purchase = initialPurchase,
            existingMedicines = medicines,
            onDismiss = {
                showAddPurchaseDialog = false
                purchaseToEdit = null
            },
            onSave = { purchase ->
                viewModel.addOrUpdatePurchaseInvoice(purchase)
                showAddPurchaseDialog = false
                purchaseToEdit = null
                Toast.makeText(context, "Purchase bill saved to Purchase GST!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Purchase Confirmation Dialog
    if (purchaseToDelete != null) {
        val p = purchaseToDelete!!
        AlertDialog(
            onDismissRequest = { purchaseToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed) },
            title = { Text("Delete Purchase Bill?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Delete purchase bill '${p.invoiceNumber}' from ${p.companyName}? Total: ₹${String.format("%.2f", p.totalAmount)}. It will be removed from Purchase GST.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePurchaseInvoice(p.id)
                        purchaseToDelete = null
                        Toast.makeText(context, "Purchase invoice deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { purchaseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PDF Actions Dialog (Print / Share / Download)
    if (generatedPdfFile != null) {
        val file = generatedPdfFile!!
        AlertDialog(
            onDismissRequest = { generatedPdfFile = null },
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AlertRed, modifier = Modifier.size(36.dp)) },
            title = { Text(pdfTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GST report generated successfully!", fontSize = 13.sp)
                    Text("File: ${file.name}", fontSize = 11.sp, color = Color.Gray)
                    Text("You can Print directly to a connected printer, Share via WhatsApp/Email, or Download to phone storage.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PrintHelper.printPdf(context, file, pdfTitle.replace(" ", "_"))
                        generatedPdfFile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print Now")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            PrintHelper.sharePdf(context, file, pdfTitle)
                            generatedPdfFile = null
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                    Button(
                        onClick = {
                            PrintHelper.downloadPdf(context, file, file.name)
                            generatedPdfFile = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SELLING GST CONTENT COMPONENT
// -------------------------------------------------------------
@Composable
private fun SellingGstContent(
    invoices: List<InvoiceEntity>,
    totalTaxable: Double,
    totalCgst: Double,
    totalSgst: Double,
    totalGst: Double,
    totalNet: Double,
    onViewInvoice: (InvoiceEntity) -> Unit,
    onDeleteInvoice: (InvoiceEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Selling GST Metrics Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SkyBlueContainer),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Selling GST Summary (বিক্রয় জিএসটি সারসংক্ষেপ)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SkyBlueText)
                        Text("${invoices.size} Invoices", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SkyBluePrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Taxable Sales", fontSize = 11.sp, color = Color.DarkGray)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", totalTaxable)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("CGST Collected", fontSize = 11.sp, color = Color.DarkGray)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", totalCgst)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyBluePrimary)
                        }
                        Column {
                            Text("SGST Collected", fontSize = 11.sp, color = Color.DarkGray)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", totalSgst)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyBluePrimary)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = SkyBlueBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Total GST (CGST + SGST): ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", totalGst)}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = AlertRed)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Net Bill Value: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", totalNet)}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                        }
                    }
                }
            }
        }

        if (invoices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Selling Invoices Found", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Sales created in New Bill will automatically appear here.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            itemsIndexed(invoices, key = { _, item -> "inv_${item.id}_${item.invoiceNumber}" }) { index, inv ->
                val totalGstAmt = inv.cgstAmount + inv.sgstAmount
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Header: Invoice No, Customer, Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = SkyBlueContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "#${inv.invoiceNumber}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SkyBluePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(inv.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(inv.dateFormatted, fontSize = 11.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Customer Drug License & GST Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("DL No: ", fontSize = 10.sp, color = Color.DarkGray)
                                    Text(
                                        if (inv.customerDl.isNotBlank()) inv.customerDl else "N/A",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (inv.customerDl.isNotBlank()) SkyBluePrimary else Color.Gray
                                    )
                                }
                            }

                            if (inv.customerGstPan.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("GSTIN: ", fontSize = 10.sp, color = Color.DarkGray)
                                        Text(inv.customerGstPan, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

                        // GST Breakdown: Taxable, CGST, SGST, Total GST
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Taxable Value", fontSize = 10.sp, color = Color.Gray)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", inv.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("CGST", fontSize = 10.sp, color = Color.Gray)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", inv.cgstAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("SGST", fontSize = 10.sp, color = Color.Gray)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", inv.sgstAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Total GST", fontSize = 10.sp, color = AlertRed, fontWeight = FontWeight.Bold)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", totalGstAmt)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Bill Net", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", inv.netAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Actions Row: View Bill & Delete Bill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onViewInvoice(inv) },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Bill", fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onDeleteInvoice(inv) },
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Bill", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PURCHASE GST CONTENT COMPONENT
// -------------------------------------------------------------
@Composable
private fun PurchaseGstContent(
    purchases: List<PurchaseInvoiceEntity>,
    totalTaxable: Double,
    totalCgst: Double,
    totalSgst: Double,
    totalGst: Double,
    totalNet: Double,
    onEditPurchase: (PurchaseInvoiceEntity) -> Unit,
    onDeletePurchase: (PurchaseInvoiceEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Purchase GST Metrics Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreenLight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Purchase GST (Input Tax Credit / ITC)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuccessGreen)
                        Text("${purchases.size} Purchases", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Taxable Purchase", fontSize = 11.sp, color = Color.DarkGray)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", totalTaxable)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Purchase CGST", fontSize = 11.sp, color = Color.DarkGray)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", totalCgst)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuccessGreen)
                        }
                        Column {
                            Text("Purchase SGST", fontSize = 11.sp, color = Color.DarkGray)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", totalSgst)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuccessGreen)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFA5D6A7))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Total Purchase GST (ITC): ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", totalGst)}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Total Purchase Value: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", totalNet)}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                        }
                    }
                }
            }
        }

        if (purchases.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Purchase Records Yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Tap '+ Add Purchase' above to record inward medicine purchases and supplier GST.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            itemsIndexed(purchases, key = { _, item -> "pur_${item.id}_${item.invoiceNumber}" }) { index, p ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Header: Supplier Invoice #, Company Name, Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = SuccessGreenLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        p.invoiceNumber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(p.companyName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(p.dateFormatted, fontSize = 11.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Company GSTIN & Items Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (p.companyGst.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Supplier GSTIN: ", fontSize = 10.sp, color = Color.DarkGray)
                                        Text(p.companyGst, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    }
                                }
                            }

                            if (p.itemsSummary.isNotBlank()) {
                                Text(
                                    "Items: ${p.itemsSummary}",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    maxLines = 1
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

                        // Purchase GST Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Taxable Amt", fontSize = 10.sp, color = Color.Gray)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", p.taxableAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("CGST", fontSize = 10.sp, color = Color.Gray)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", p.cgstAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("SGST", fontSize = 10.sp, color = Color.Gray)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", p.sgstAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Total GST (ITC)", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", p.totalGstAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Purchase", fontSize = 10.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", p.totalAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Actions Row: Edit & Delete Purchase
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onEditPurchase(p) },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onDeletePurchase(p) },
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// GST SUMMARY & TAX COMPARISON COMPONENT (GSTR-3B)
// -------------------------------------------------------------
@Composable
private fun GstSummaryContent(
    sellingInvoicesCount: Int,
    sellingTaxable: Double,
    sellingCgst: Double,
    sellingSgst: Double,
    sellingTotalGst: Double,
    purchaseCount: Int,
    purchaseTaxable: Double,
    purchaseCgst: Double,
    purchaseSgst: Double,
    purchaseTotalGst: Double,
    onExportPdf: () -> Unit
) {
    val netGstPayable = sellingTotalGst - purchaseTotalGst
    val netCgst = sellingCgst - purchaseCgst
    val netSgst = sellingSgst - purchaseSgst

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (netGstPayable >= 0) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (netGstPayable >= 0) "Net GST Payable to Govt" else "ITC Tax Credit Balance (Carry Forward)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netGstPayable >= 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%.2f", Math.abs(netGstPayable))}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netGstPayable >= 0) AlertRed else SuccessGreen
                            )
                        }
                        Icon(
                            if (netGstPayable >= 0) Icons.Default.AccountBalance else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = if (netGstPayable >= 0) AlertRed else SuccessGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Net CGST", fontSize = 11.sp, color = Color.DarkGray)
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%.2f", Math.abs(netCgst))}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("Net SGST", fontSize = 11.sp, color = Color.DarkGray)
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%.2f", Math.abs(netSgst))}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("Rule Check", fontSize = 11.sp, color = Color.DarkGray)
                            Text("CGST + SGST = GST", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SkyBluePrimary)
                        }
                    }
                }
            }
        }

        // Side-by-Side Comparison Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tax Breakdown Comparison (ট্যাক্স তুলনা)", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    // Output GST Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Output GST (From Selling)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("$sellingInvoicesCount Sales Bills | Taxable ₹${String.format(Locale.ENGLISH, "%.2f", sellingTaxable)}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Text("₹${String.format(Locale.ENGLISH, "%.2f", sellingTotalGst)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyBluePrimary)
                    }

                    Divider(color = Color(0xFFEEEEEE))

                    // Input GST Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Input Tax Credit / ITC (From Purchase)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("$purchaseCount Purchase Bills | Taxable ₹${String.format(Locale.ENGLISH, "%.2f", purchaseTaxable)}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Text("₹${String.format(Locale.ENGLISH, "%.2f", purchaseTotalGst)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuccessGreen)
                    }

                    Divider(color = Color(0xFFEEEEEE))

                    // Formula explanation
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("GST Calculation Rules:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
                            Text("1. Total GST = CGST (50%) + SGST (50%)", fontSize = 11.sp, color = Color.DarkGray)
                            Text("2. Net Tax Payable = Output GST (Selling) - Input GST (Purchases)", fontSize = 11.sp, color = Color.DarkGray)
                            Text("3. Deleted bills are permanently excluded from tax reports automatically.", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADD / EDIT PURCHASE INVOICE DIALOG
// -------------------------------------------------------------
@Composable
private fun PurchaseEntryDialog(
    purchase: PurchaseInvoiceEntity?,
    existingMedicines: List<com.example.data.local.entity.MedicineEntity>,
    onDismiss: () -> Unit,
    onSave: (PurchaseInvoiceEntity) -> Unit
) {
    val currentDateStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

    var invoiceNumber by remember { mutableStateOf(purchase?.invoiceNumber ?: "PUR-${SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())}-${(100..999).random()}") }
    var dateFormatted by remember { mutableStateOf(purchase?.dateFormatted?.ifEmpty { currentDateStr } ?: currentDateStr) }
    var companyName by remember { mutableStateOf(purchase?.companyName ?: "") }
    var companyGst by remember { mutableStateOf(purchase?.companyGst ?: "") }
    var companyPhone by remember { mutableStateOf(purchase?.companyPhone ?: "") }
    var itemsSummary by remember { mutableStateOf(purchase?.itemsSummary ?: "") }
    var taxableAmountStr by remember { mutableStateOf(if (purchase != null && purchase.taxableAmount > 0) purchase.taxableAmount.toString() else "") }
    var selectedGstPercent by remember { mutableDoubleStateOf(purchase?.gstRatePercent ?: 12.0) }
    var customCgstStr by remember { mutableStateOf(if (purchase != null) String.format(Locale.ENGLISH, "%.2f", purchase.cgstAmount) else "") }
    var customSgstStr by remember { mutableStateOf(if (purchase != null) String.format(Locale.ENGLISH, "%.2f", purchase.sgstAmount) else "") }
    var note by remember { mutableStateOf(purchase?.note ?: "") }

    // Auto calculate CGST & SGST when taxable or GST% changes
    LaunchedEffect(taxableAmountStr, selectedGstPercent) {
        val taxable = taxableAmountStr.toDoubleOrNull() ?: 0.0
        val halfRate = selectedGstPercent / 2.0
        val halfTax = taxable * (halfRate / 100.0)
        customCgstStr = String.format(Locale.ENGLISH, "%.2f", halfTax)
        customSgstStr = String.format(Locale.ENGLISH, "%.2f", halfTax)
    }

    val taxable = taxableAmountStr.toDoubleOrNull() ?: 0.0
    val cgst = customCgstStr.toDoubleOrNull() ?: 0.0
    val sgst = customSgstStr.toDoubleOrNull() ?: 0.0
    val totalGst = cgst + sgst
    val totalBill = taxable + totalGst

    val companySuggestions = remember(existingMedicines) {
        existingMedicines.mapNotNull { it.companyName.ifBlank { it.manufacturerName } }
            .filter { it.isNotBlank() }
            .distinct()
            .take(6)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (purchase == null) "New Purchase Bill (ক্রয় জিএসটি বিল)" else "Edit Purchase Bill",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SuccessGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Invoice / Bill #", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("purchase_invoice_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dateFormatted,
                        onValueChange = { dateFormatted = it },
                        label = { Text("Date (DD/MM/YYYY)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("purchase_date_input"),
                        singleLine = true
                    )
                }

                // Company Name
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company / Supplier Name *", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("purchase_company_input"),
                    singleLine = true
                )

                // Quick Company Chips
                if (companySuggestions.isNotEmpty() && companyName.isBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        companySuggestions.take(3).forEach { comp ->
                            AssistChip(
                                onClick = { companyName = comp },
                                label = { Text(comp, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Company GSTIN & Items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = companyGst,
                        onValueChange = { companyGst = it },
                        label = { Text("Supplier GSTIN", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = itemsSummary,
                        onValueChange = { itemsSummary = it },
                        label = { Text("Items / Medicines", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Dolo 650, Pan 40", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Taxable Amount
                OutlinedTextField(
                    value = taxableAmountStr,
                    onValueChange = { taxableAmountStr = it },
                    label = { Text("Taxable Amount (₹) *", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("purchase_taxable_input"),
                    singleLine = true
                )

                // GST Rate Selection Chips
                Text("GST Rate (জিএসটি হার):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                        FilterChip(
                            selected = selectedGstPercent == rate,
                            onClick = { selectedGstPercent = rate },
                            label = { Text("${rate.toInt()}%", fontSize = 11.sp) }
                        )
                    }
                }

                // CGST & SGST calculated fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customCgstStr,
                        onValueChange = { customCgstStr = it },
                        label = { Text("CGST (₹)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customSgstStr,
                        onValueChange = { customSgstStr = it },
                        label = { Text("SGST (₹)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Calculated Summary Banner
                Surface(
                    color = SuccessGreenLight,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total GST: ₹${String.format(Locale.ENGLISH, "%.2f", totalGst)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        Text("Total Bill: ₹${String.format(Locale.ENGLISH, "%.2f", totalBill)}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (companyName.isBlank()) {
                                return@Button
                            }
                            val updated = PurchaseInvoiceEntity(
                                id = purchase?.id ?: 0L,
                                invoiceNumber = invoiceNumber.trim().ifEmpty { "PUR-${System.currentTimeMillis()}" },
                                date = System.currentTimeMillis(),
                                dateFormatted = dateFormatted.trim(),
                                companyName = companyName.trim(),
                                companyGst = companyGst.trim(),
                                companyPhone = companyPhone.trim(),
                                itemsSummary = itemsSummary.trim().ifEmpty { "Medicines Purchase" },
                                totalQty = 1,
                                taxableAmount = taxable,
                                gstRatePercent = selectedGstPercent,
                                cgstAmount = cgst,
                                sgstAmount = sgst,
                                totalGstAmount = totalGst,
                                totalAmount = totalBill,
                                note = note.trim(),
                                createdAt = purchase?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        enabled = companyName.isNotBlank() && taxable > 0
                    ) {
                        Text("Save Purchase", color = Color.White)
                    }
                }
            }
        }
    }
}
