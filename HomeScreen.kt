package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InvoiceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.DateUtils

@Composable
fun HomeScreen(
    viewModel: PharmaViewModel,
    onNewBill: () -> Unit,
    onStockStatement: () -> Unit,
    onKhata: () -> Unit,
    onManageCustomers: () -> Unit,
    onInvoiceClick: (InvoiceEntity) -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val medicines by viewModel.medicines.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    val todaySalesAmount = remember(invoices) {
        invoices.filter { DateUtils.isToday(it.date) }.sumOf { it.netAmount }
    }
    val todaySalesCount = remember(invoices) {
        invoices.count { DateUtils.isToday(it.date) }
    }
    val totalDueAmount = remember(invoices) {
        invoices.sumOf { it.dueAmount }
    }
    val totalDueCount = remember(invoices) {
        invoices.count { it.dueAmount > 0 }
    }
    val totalStockValuation = remember(medicines) {
        medicines.sumOf { it.stockQuantity * it.price }
    }
    val lowStockCount = remember(medicines) {
        medicines.count { it.stockQuantity <= it.lowStockLevel }
    }
    val nearExpiryCount = remember(medicines) {
        medicines.count { DateUtils.isNearExpiry(it.expiryDate) }
    }
    val expiredCount = remember(medicines) {
        medicines.count { DateUtils.isExpired(it.expiryDate) }
    }
    val totalAlerts = lowStockCount + nearExpiryCount + expiredCount

    val filteredInvoices = remember(invoices, searchQuery) {
        if (searchQuery.isBlank()) invoices.take(20)
        else invoices.filter {
            it.invoiceNumber.toString().contains(searchQuery) ||
            it.customerName.contains(searchQuery, ignoreCase = true) ||
            it.doctorDetails.contains(searchQuery, ignoreCase = true) ||
            it.dateFormatted.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Metric Cards Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Today's Sales",
                value = if (authState.isGuest) "•••••" else "₹${String.format("%.2f", todaySalesAmount)}",
                subtitle = if (authState.isGuest) "Admin Only" else "$todaySalesCount Invoices",
                icon = Icons.Default.TrendingUp,
                color = SkyBluePrimary,
                containerColor = SkyBlueContainer,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Total Due",
                value = if (authState.isGuest) "•••••" else "₹${String.format("%.2f", totalDueAmount)}",
                subtitle = if (authState.isGuest) "Admin Only" else "$totalDueCount Pending Bills",
                icon = Icons.Default.Warning,
                color = AlertRed,
                containerColor = AlertRedLight,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Stock Value",
                value = if (authState.isGuest) "•••••" else "₹${String.format("%.2f", totalStockValuation)}",
                subtitle = if (authState.isGuest) "Admin Only" else "${medicines.size} Products",
                icon = Icons.Default.Inventory,
                color = SuccessGreen,
                containerColor = SuccessGreenLight,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Alerts",
                value = "$totalAlerts Items",
                subtitle = "Low: $lowStockCount | Exp: ${nearExpiryCount + expiredCount}",
                icon = Icons.Default.NotificationsActive,
                color = AlertRed,
                containerColor = AlertRedLight,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    if (authState.isGuest) showAdminOnlyDialog = true
                    else onNewBill()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Bill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onStockStatement,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlueSecondary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stock & Statement", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onKhata,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Khata (খাতা)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Secondary Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.switchTab(NavigationTab.SALES_HISTORY) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Sales History", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    if (authState.isGuest) showAdminOnlyDialog = true
                    else viewModel.switchTab(NavigationTab.INVENTORY)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Add Product", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    if (authState.isGuest) showAdminOnlyDialog = true
                    else onManageCustomers()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Add Customer", fontSize = 11.sp)
            }
        }

        // Master Admin GST Register Quick Banner: "sub admin er kachhe option show korbe na"
        if (authState.isMasterAdmin) {
            Card(
                onClick = { viewModel.switchTab(NavigationTab.GST) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FB)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SkyBlueBorder),
                modifier = Modifier.fillMaxWidth().testTag("home_gst_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = SkyBluePrimary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("GST Portal & Tax Register", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SkyBlueText)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AlertRed,
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        "MASTER ADMIN",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text("Selling GST & Purchase GST (PDF Register Print)", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SkyBluePrimary)
                }
            }
        }

        // Search Bar for Recent Invoices
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Invoice #, Party, Doctor, Patient, Date...") },
            modifier = Modifier.fillMaxWidth().testTag("home_invoice_search"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SkyBluePrimary,
                unfocusedBorderColor = SkyBlueBorder
            )
        )

        // Invoices List
        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No invoices found. Generate your first bill from 'New Bill' tab!" else "No matching invoices found.",
                    color = MutedText,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredInvoices, key = { it.invoiceNumber }) { inv ->
                    HomeInvoiceCard(
                        invoice = inv,
                        isGuest = authState.isGuest,
                        onView = { onInvoiceClick(inv) },
                        onHistory = { viewModel.navigateToSalesHistory(inv.customerName, inv.customerType) }
                    )
                }
            }
        }
    }

    if (showAdminOnlyDialog) {
        AdminOnlyDialog(onDismiss = { showAdminOnlyDialog = false })
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Medium)
                Surface(
                    color = containerColor,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkText)
            Text(subtitle, fontSize = 10.sp, color = MutedText)
        }
    }
}

@Composable
fun HomeInvoiceCard(
    invoice: InvoiceEntity,
    isGuest: Boolean,
    onView: () -> Unit,
    onHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bill #${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(invoice.dateFormatted, fontSize = 11.sp, color = MutedText)
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${invoice.customerName} (${invoice.customerType})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = SkyBlueSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isGuest) "••••••" else "₹${String.format("%.2f", invoice.netAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (invoice.dueAmount > 0) {
                        Text(if (isGuest) "Due: ••••••" else "Due: ₹${String.format("%.2f", invoice.dueAmount)}", color = AlertRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onView,
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("VIEW", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onHistory,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("History", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
