package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GuestLoginEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.MedicineEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel
import com.example.ui.viewmodel.UserRole
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertsScreen(
    viewModel: PharmaViewModel,
    onViewInvoice: (InvoiceEntity) -> Unit
) {
    val context = LocalContext.current
    val invoices by viewModel.invoices.collectAsState()
    val medicines by viewModel.medicines.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val guestLogins by viewModel.guestLogins.collectAsState()

    // Per-user alert dismissal key: Sub-admin and Master-admin can delete/dismiss alerts from their own page
    val userKey = remember(authState.role) {
        when (val role = authState.role) {
            is UserRole.Admin -> if (role.isMasterAdmin) "master_admin" else "subadmin_${role.mobile}"
            is UserRole.Guest -> "guest_${role.mobile}"
            else -> "default_user"
        }
    }

    val prefs = remember(context, userKey) {
        context.getSharedPreferences("PharmaAlertsDismissed_$userKey", Context.MODE_PRIVATE)
    }

    var dismissedDueInvoiceNumbers by remember(userKey) {
        mutableStateOf<Set<String>>(prefs.getStringSet("dismissed_due_invoices", emptySet())?.filterNotNull()?.toSet() ?: emptySet())
    }
    var dismissedLowStockIds by remember(userKey) {
        mutableStateOf<Set<String>>(prefs.getStringSet("dismissed_low_stock", emptySet())?.filterNotNull()?.toSet() ?: emptySet())
    }
    var dismissedNearExpiryIds by remember(userKey) {
        mutableStateOf<Set<String>>(prefs.getStringSet("dismissed_near_expiry", emptySet())?.filterNotNull()?.toSet() ?: emptySet())
    }
    var dismissedExpiredIds by remember(userKey) {
        mutableStateOf<Set<String>>(prefs.getStringSet("dismissed_expired", emptySet())?.filterNotNull()?.toSet() ?: emptySet())
    }

    fun dismissDueInvoice(invNum: Long) {
        val updated: Set<String> = dismissedDueInvoiceNumbers + invNum.toString()
        dismissedDueInvoiceNumbers = updated
        prefs.edit().putStringSet("dismissed_due_invoices", updated).apply()
        Toast.makeText(context, "Due alert #$invNum deleted from your page", Toast.LENGTH_SHORT).show()
    }

    fun restoreDueInvoices() {
        dismissedDueInvoiceNumbers = emptySet()
        prefs.edit().remove("dismissed_due_invoices").apply()
        Toast.makeText(context, "Due alerts restored to your page", Toast.LENGTH_SHORT).show()
    }

    fun dismissAllDueInvoices(items: List<InvoiceEntity>) {
        val updated: Set<String> = dismissedDueInvoiceNumbers + items.map { it.invoiceNumber.toString() }
        dismissedDueInvoiceNumbers = updated
        prefs.edit().putStringSet("dismissed_due_invoices", updated).apply()
        Toast.makeText(context, "All active due alerts deleted from your page", Toast.LENGTH_SHORT).show()
    }

    fun dismissLowStock(medId: Long, name: String) {
        val updated: Set<String> = dismissedLowStockIds + medId.toString()
        dismissedLowStockIds = updated
        prefs.edit().putStringSet("dismissed_low_stock", updated).apply()
        Toast.makeText(context, "Low stock alert for \"$name\" deleted from your page", Toast.LENGTH_SHORT).show()
    }

    fun restoreLowStock() {
        dismissedLowStockIds = emptySet()
        prefs.edit().remove("dismissed_low_stock").apply()
        Toast.makeText(context, "Low stock alerts restored", Toast.LENGTH_SHORT).show()
    }

    fun dismissAllLowStock(items: List<MedicineEntity>) {
        val updated: Set<String> = dismissedLowStockIds + items.map { it.id.toString() }
        dismissedLowStockIds = updated
        prefs.edit().putStringSet("dismissed_low_stock", updated).apply()
        Toast.makeText(context, "All low stock alerts deleted from your page", Toast.LENGTH_SHORT).show()
    }

    fun dismissNearExpiry(medId: Long, name: String) {
        val updated: Set<String> = dismissedNearExpiryIds + medId.toString()
        dismissedNearExpiryIds = updated
        prefs.edit().putStringSet("dismissed_near_expiry", updated).apply()
        Toast.makeText(context, "Near expiry alert for \"$name\" deleted from your page", Toast.LENGTH_SHORT).show()
    }

    fun restoreNearExpiry() {
        dismissedNearExpiryIds = emptySet()
        prefs.edit().remove("dismissed_near_expiry").apply()
        Toast.makeText(context, "Near expiry alerts restored", Toast.LENGTH_SHORT).show()
    }

    fun dismissAllNearExpiry(items: List<MedicineEntity>) {
        val updated: Set<String> = dismissedNearExpiryIds + items.map { it.id.toString() }
        dismissedNearExpiryIds = updated
        prefs.edit().putStringSet("dismissed_near_expiry", updated).apply()
        Toast.makeText(context, "All near expiry alerts deleted from your page", Toast.LENGTH_SHORT).show()
    }

    fun dismissExpired(medId: Long, name: String) {
        val updated: Set<String> = dismissedExpiredIds + medId.toString()
        dismissedExpiredIds = updated
        prefs.edit().putStringSet("dismissed_expired", updated).apply()
        Toast.makeText(context, "Expired alert for \"$name\" deleted from your page", Toast.LENGTH_SHORT).show()
    }

    fun restoreExpired() {
        dismissedExpiredIds = emptySet()
        prefs.edit().remove("dismissed_expired").apply()
        Toast.makeText(context, "Expired alerts restored", Toast.LENGTH_SHORT).show()
    }

    fun dismissAllExpired(items: List<MedicineEntity>) {
        val updated: Set<String> = dismissedExpiredIds + items.map { it.id.toString() }
        dismissedExpiredIds = updated
        prefs.edit().putStringSet("dismissed_expired", updated).apply()
        Toast.makeText(context, "All expired alerts deleted from your page", Toast.LENGTH_SHORT).show()
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = remember(authState.isAdmin) {
        if (authState.isAdmin) {
            listOf("DUE ALERTS", "LOW STOCK", "NEAR EXPIRY", "EXPIRED", "GUEST LOGINS")
        } else {
            listOf("DUE ALERTS", "LOW STOCK", "NEAR EXPIRY", "EXPIRED")
        }
    }

    val allDueInvoices = remember(invoices) { invoices.filter { it.dueAmount > 0.0 } }
    val dueInvoices = remember(allDueInvoices, dismissedDueInvoiceNumbers) {
        allDueInvoices.filter { it.invoiceNumber.toString() !in dismissedDueInvoiceNumbers }
    }

    val allLowStock = remember(medicines) { medicines.filter { it.stockQuantity <= it.lowStockLevel && it.stockQuantity > 0 } }
    val lowStockMedicines = remember(allLowStock, dismissedLowStockIds) {
        allLowStock.filter { it.id.toString() !in dismissedLowStockIds }
    }

    val allNearExpiry = remember(medicines) { medicines.filter { DateUtils.isNearExpiry(it.expiryDate) && !DateUtils.isExpired(it.expiryDate) } }
    val nearExpiryMedicines = remember(allNearExpiry, dismissedNearExpiryIds) {
        allNearExpiry.filter { it.id.toString() !in dismissedNearExpiryIds }
    }

    val allExpired = remember(medicines) { medicines.filter { DateUtils.isExpired(it.expiryDate) } }
    val expiredMedicines = remember(allExpired, dismissedExpiredIds) {
        allExpired.filter { it.id.toString() !in dismissedExpiredIds }
    }

    var guestSearchQuery by remember { mutableStateOf("") }
    val filteredGuestLogins = remember(guestLogins, guestSearchQuery) {
        if (guestSearchQuery.isBlank()) {
            guestLogins
        } else {
            guestLogins.filter { it.mobileNumber.contains(guestSearchQuery.trim(), ignoreCase = true) }
        }
    }

    var invoiceToRecordPayment by remember { mutableStateOf<InvoiceEntity?>(null) }
    var paymentAmountStr by remember { mutableStateOf("") }
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    // Deletion confirmation states
    var invoiceToDeleteAlert by remember { mutableStateOf<InvoiceEntity?>(null) }
    var lowStockToDeleteAlert by remember { mutableStateOf<MedicineEntity?>(null) }
    var nearExpiryToDeleteAlert by remember { mutableStateOf<MedicineEntity?>(null) }
    var expiredToDeleteAlert by remember { mutableStateOf<MedicineEntity?>(null) }
    var guestToDeleteRecord by remember { mutableStateOf<GuestLoginEntity?>(null) }
    var clearAllTabConfirmIndex by remember { mutableStateOf<Int?>(null) }
    var isTopGuestBannerDismissed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        // Real-Time Guest Notification Banner
        if (authState.isAdmin && guestLogins.isNotEmpty() && !isTopGuestBannerDismissed) {
            val latestGuest = guestLogins.first()
            val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SkyBlueLight.copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, SkyBluePrimary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AlertRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guest User Entry Alert",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = DarkText
                        )
                        Text(
                            text = "Mobile: ${latestGuest.mobileNumber} • ${timeFormat.format(Date(latestGuest.loginTimestamp))}",
                            fontSize = 11.sp,
                            color = SkyBlueSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = { selectedTab = 4 },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("View (${guestLogins.size})", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { isTopGuestBannerDismissed = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss Banner", tint = DarkText, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab.coerceIn(0, tabs.lastIndex),
            containerColor = PureWhite,
            contentColor = SkyBluePrimary,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, title ->
                val count = when (index) {
                    0 -> dueInvoices.size
                    1 -> lowStockMedicines.size
                    2 -> nearExpiryMedicines.size
                    3 -> expiredMedicines.size
                    4 -> guestLogins.size
                    else -> 0
                }
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(
                                    containerColor = if (index == 0 || index == 3) AlertRed else SkyBluePrimary
                                ) {
                                    Text("$count", color = PureWhite, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Due Alerts Tab
                val dismissedCount = allDueInvoices.size - dueInvoices.size
                Column(modifier = Modifier.fillMaxSize()) {
                    TabAlertHeader(
                        activeCount = dueInvoices.size,
                        dismissedCount = dismissedCount,
                        tabName = "Due",
                        onDismissAll = {
                            if (authState.isGuest) showAdminOnlyDialog = true
                            else clearAllTabConfirmIndex = 0
                        },
                        onRestoreAll = { restoreDueInvoices() }
                    )

                    if (dueInvoices.isEmpty()) {
                        EmptyAlertPlaceholder(
                            if (dismissedCount > 0) "All due alerts have been cleared from your page. Click 'Restore' to view again."
                            else "No outstanding customer dues found. All bills cleared!"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(dueInvoices, key = { it.invoiceNumber }) { invoice ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                                    border = BorderStroke(1.dp, AlertRedLight)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Bill #${invoice.invoiceNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = DarkText
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = invoice.dateFormatted,
                                                    fontSize = 12.sp,
                                                    color = MutedText
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(
                                                    onClick = {
                                                        if (authState.isGuest) showAdminOnlyDialog = true
                                                        else invoiceToDeleteAlert = invoice
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete Alert",
                                                        tint = AlertRed,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "${invoice.customerName} (${invoice.customerType})",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = SkyBlueSecondary
                                        )

                                        if (invoice.customerPhone.isNotEmpty()) {
                                            Text(
                                                text = "Phone: ${invoice.customerPhone}",
                                                fontSize = 12.sp,
                                                color = MutedText
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(if (authState.isGuest) "Total: ••••••" else "Total: ₹${String.format("%.2f", invoice.netAmount)}", fontSize = 12.sp)
                                            Text(if (authState.isGuest) "Paid: ••••••" else "Paid: ₹${String.format("%.2f", invoice.paidAmount)}", fontSize = 12.sp, color = SuccessGreen)
                                            Text(
                                                if (authState.isGuest) "Due: ••••••" else "Due: ₹${String.format("%.2f", invoice.dueAmount)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AlertRed
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    if (authState.isGuest) showAdminOnlyDialog = true
                                                    else invoiceToDeleteAlert = invoice
                                                },
                                                modifier = Modifier.height(36.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                                                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f))
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Delete Alert", fontSize = 11.sp, color = AlertRed)
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            OutlinedButton(
                                                onClick = { onViewInvoice(invoice) },
                                                modifier = Modifier.height(36.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Bill", fontSize = 12.sp)
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Button(
                                                onClick = {
                                                    if (authState.isGuest) {
                                                        showAdminOnlyDialog = true
                                                    } else {
                                                        invoiceToRecordPayment = invoice
                                                        paymentAmountStr = invoice.dueAmount.toString()
                                                    }
                                                },
                                                modifier = Modifier.height(36.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Record Payment", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Low Stock Tab
                val dismissedCount = allLowStock.size - lowStockMedicines.size
                Column(modifier = Modifier.fillMaxSize()) {
                    TabAlertHeader(
                        activeCount = lowStockMedicines.size,
                        dismissedCount = dismissedCount,
                        tabName = "Low Stock",
                        onDismissAll = {
                            if (authState.isGuest) showAdminOnlyDialog = true
                            else clearAllTabConfirmIndex = 1
                        },
                        onRestoreAll = { restoreLowStock() }
                    )

                    if (lowStockMedicines.isEmpty()) {
                        EmptyAlertPlaceholder(
                            if (dismissedCount > 0) "All low stock alerts have been cleared from your page. Click 'Restore' to view again."
                            else "All medicines are sufficiently stocked."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(lowStockMedicines, key = { it.id }) { med ->
                                MedicineAlertCard(
                                    med = med,
                                    alertType = "LOW STOCK",
                                    alertColor = AlertRed,
                                    onDeleteClick = {
                                        if (authState.isGuest) showAdminOnlyDialog = true
                                        else lowStockToDeleteAlert = med
                                    }
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                // Near Expiry Tab
                val dismissedCount = allNearExpiry.size - nearExpiryMedicines.size
                Column(modifier = Modifier.fillMaxSize()) {
                    TabAlertHeader(
                        activeCount = nearExpiryMedicines.size,
                        dismissedCount = dismissedCount,
                        tabName = "Near Expiry",
                        onDismissAll = {
                            if (authState.isGuest) showAdminOnlyDialog = true
                            else clearAllTabConfirmIndex = 2
                        },
                        onRestoreAll = { restoreNearExpiry() }
                    )

                    if (nearExpiryMedicines.isEmpty()) {
                        EmptyAlertPlaceholder(
                            if (dismissedCount > 0) "All near expiry alerts cleared from your page. Click 'Restore' to view again."
                            else "No products near expiry date."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(nearExpiryMedicines, key = { it.id }) { med ->
                                MedicineAlertCard(
                                    med = med,
                                    alertType = "NEAR EXPIRY",
                                    alertColor = SkyBluePrimary,
                                    onDeleteClick = {
                                        if (authState.isGuest) showAdminOnlyDialog = true
                                        else nearExpiryToDeleteAlert = med
                                    }
                                )
                            }
                        }
                    }
                }
            }
            3 -> {
                // Expired Tab
                val dismissedCount = allExpired.size - expiredMedicines.size
                Column(modifier = Modifier.fillMaxSize()) {
                    TabAlertHeader(
                        activeCount = expiredMedicines.size,
                        dismissedCount = dismissedCount,
                        tabName = "Expired",
                        onDismissAll = {
                            if (authState.isGuest) showAdminOnlyDialog = true
                            else clearAllTabConfirmIndex = 3
                        },
                        onRestoreAll = { restoreExpired() }
                    )

                    if (expiredMedicines.isEmpty()) {
                        EmptyAlertPlaceholder(
                            if (dismissedCount > 0) "All expired alerts cleared from your page. Click 'Restore' to view again."
                            else "No expired medicines found in inventory."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(expiredMedicines, key = { it.id }) { med ->
                                MedicineAlertCard(
                                    med = med,
                                    alertType = "EXPIRED",
                                    alertColor = AlertRed,
                                    onDeleteClick = {
                                        if (authState.isGuest) showAdminOnlyDialog = true
                                        else expiredToDeleteAlert = med
                                    }
                                )
                            }
                        }
                    }
                }
            }
            4 -> {
                // Guest Logins Tracking Tab
                val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
                var showClearAllGuestDialog by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = guestSearchQuery,
                            onValueChange = { guestSearchQuery = it },
                            placeholder = { Text("Search by guest mobile number...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SkyBluePrimary) },
                            trailingIcon = {
                                if (guestSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { guestSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyBluePrimary,
                                unfocusedBorderColor = SkyBlueBorder,
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = PureWhite
                            )
                        )

                        if (guestLogins.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    if (authState.isGuest) showAdminOnlyDialog = true
                                    else showClearAllGuestDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                                border = BorderStroke(1.dp, AlertRed)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredGuestLogins.isEmpty()) {
                        EmptyAlertPlaceholder(
                            if (guestSearchQuery.isBlank())
                                "No guest user logins recorded yet. When a guest logs in with their mobile number, it will appear here."
                            else
                                "No guest login found matching \"$guestSearchQuery\""
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredGuestLogins, key = { it.id }) { guest ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                                    border = BorderStroke(1.dp, SkyBlueBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .background(SkyBlueLight, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhoneAndroid,
                                                    contentDescription = "Guest Phone",
                                                    tint = SkyBluePrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = guest.mobileNumber,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = DarkText
                                                )
                                                Text(
                                                    text = "Login: ${dateFormat.format(Date(guest.loginTimestamp))}",
                                                    fontSize = 11.sp,
                                                    color = MutedText
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = SkyBluePrimary.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "GUEST USER",
                                                    color = SkyBluePrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = {
                                                    if (authState.isGuest) showAdminOnlyDialog = true
                                                    else guestToDeleteRecord = guest
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Guest Login",
                                                    tint = AlertRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showClearAllGuestDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearAllGuestDialog = false },
                        title = { Text("Clear All Guest Logins?", fontWeight = FontWeight.Bold) },
                        text = { Text("Are you sure you want to delete all recorded guest login entries from the database?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.clearGuestLogins()
                                    showClearAllGuestDialog = false
                                    Toast.makeText(context, "Guest login history cleared", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                            ) {
                                Text("Clear All")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearAllGuestDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }

    // --- DIALOGS FOR ALERT DELETION ---

    // 1. Due Alert Delete Confirmation
    invoiceToDeleteAlert?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToDeleteAlert = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(28.dp)) },
            title = { Text("Delete Due Alert from Page?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Do you want to delete the due alert for Bill #${inv.invoiceNumber} (${inv.customerName}, Due: ₹${String.format("%.2f", inv.dueAmount)}) from your alerts page?\n\n(This will hide the alert from your page without altering the invoice).")
            },
            confirmButton = {
                Button(
                    onClick = {
                        dismissDueInvoice(inv.invoiceNumber)
                        invoiceToDeleteAlert = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDeleteAlert = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Low Stock Alert Delete Confirmation
    lowStockToDeleteAlert?.let { med ->
        AlertDialog(
            onDismissRequest = { lowStockToDeleteAlert = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(28.dp)) },
            title = { Text("Delete Low Stock Alert?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Do you want to delete the low stock alert for \"${med.productName}\" (Batch: ${med.batchNumber}, Stock: ${med.stockQuantity}) from your alerts page?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        dismissLowStock(med.id, med.productName)
                        lowStockToDeleteAlert = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { lowStockToDeleteAlert = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Near Expiry Alert Delete Confirmation
    nearExpiryToDeleteAlert?.let { med ->
        AlertDialog(
            onDismissRequest = { nearExpiryToDeleteAlert = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(28.dp)) },
            title = { Text("Delete Near Expiry Alert?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Do you want to delete the near expiry alert for \"${med.productName}\" (Expiry: ${med.expiryDate}) from your alerts page?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        dismissNearExpiry(med.id, med.productName)
                        nearExpiryToDeleteAlert = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { nearExpiryToDeleteAlert = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Expired Alert Delete Confirmation (Sub Admin can dismiss alert; Master Admin can dismiss or delete product from inventory)
    expiredToDeleteAlert?.let { med ->
        AlertDialog(
            onDismissRequest = { expiredToDeleteAlert = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(28.dp)) },
            title = { Text("Delete Expired Product Alert", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Product: ${med.productName}")
                    Text("Batch: ${med.batchNumber} • Expiry: ${med.expiryDate}", color = MutedText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (authState.isMasterAdmin) {
                        Text(
                            "As Master Admin, you can choose to delete this alert from your page only, or permanently delete the expired product from inventory across all devices:",
                            fontSize = 12.sp
                        )
                    } else {
                        Text("Delete this expired medicine alert from your alerts page?", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (authState.isMasterAdmin) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteMedicine(med.id)
                                expiredToDeleteAlert = null
                                Toast.makeText(context, "Permanently deleted ${med.productName} from inventory", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            border = BorderStroke(1.dp, AlertRed)
                        ) {
                            Text("Delete from Inventory", fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = {
                            dismissExpired(med.id, med.productName)
                            expiredToDeleteAlert = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                    ) {
                        Text("Delete Alert", fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { expiredToDeleteAlert = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Individual Guest Login Record Deletion
    guestToDeleteRecord?.let { guest ->
        AlertDialog(
            onDismissRequest = { guestToDeleteRecord = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(28.dp)) },
            title = { Text("Delete Guest Login Record?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Delete the login history record for guest mobile: ${guest.mobileNumber}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGuestLogin(guest.id)
                        guestToDeleteRecord = null
                        Toast.makeText(context, "Guest login entry deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { guestToDeleteRecord = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Clear All Alerts on Tab Confirmation
    clearAllTabConfirmIndex?.let { tabIndex ->
        val tabTitle = tabs.getOrNull(tabIndex) ?: "Alerts"
        AlertDialog(
            onDismissRequest = { clearAllTabConfirmIndex = null },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = AlertRed, modifier = Modifier.size(30.dp)) },
            title = { Text("Clear All $tabTitle?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete/clear all active $tabTitle from your alerts page?\n\nYou can restore them at any time by tapping 'Restore'.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (tabIndex) {
                            0 -> dismissAllDueInvoices(dueInvoices)
                            1 -> dismissAllLowStock(lowStockMedicines)
                            2 -> dismissAllNearExpiry(nearExpiryMedicines)
                            3 -> dismissAllExpired(expiredMedicines)
                        }
                        clearAllTabConfirmIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Clear All on Page")
                }
            },
            dismissButton = {
                TextButton(onClick = { clearAllTabConfirmIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Record Payment Dialog
    invoiceToRecordPayment?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToRecordPayment = null },
            title = { Text("Receive Payment: Bill #${inv.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer: ${inv.customerName}", fontWeight = FontWeight.Medium)
                    Text("Total Due: ₹${String.format("%.2f", inv.dueAmount)}", color = AlertRed, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = paymentAmountStr,
                        onValueChange = { paymentAmountStr = it },
                        label = { Text("Amount Received (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
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
                        val amount = paymentAmountStr.toDoubleOrNull() ?: 0.0
                        if (amount <= 0) {
                            Toast.makeText(context, "Enter a valid positive amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amount > inv.dueAmount) {
                            Toast.makeText(context, "Amount cannot exceed due amount of ₹${inv.dueAmount}", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.recordPayment(inv.invoiceNumber, amount)
                        invoiceToRecordPayment = null
                        Toast.makeText(context, "Payment of ₹$amount recorded for Bill #${inv.invoiceNumber}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Save")
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

@Composable
fun TabAlertHeader(
    activeCount: Int,
    dismissedCount: Int,
    tabName: String,
    onDismissAll: () -> Unit,
    onRestoreAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Active: $activeCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            if (dismissedCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "($dismissedCount deleted)",
                    fontSize = 11.sp,
                    color = AlertRed,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = onRestoreAll,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("Restore", fontSize = 11.sp, color = SkyBluePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (activeCount > 0) {
            TextButton(
                onClick = onDismissAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear All on Page", fontSize = 11.sp, color = AlertRed, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MedicineAlertCard(
    med: MedicineEntity,
    alertType: String,
    alertColor: Color,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, alertColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(med.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${med.manufacturerName.ifEmpty { med.companyName }} • Pack: ${med.packagingType}", fontSize = 11.sp, color = MutedText)
                Text("Batch: ${med.batchNumber} • Exp: ${med.expiryDate}", fontSize = 11.sp, color = MutedText)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = alertColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = alertType,
                            color = alertColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stock: ${med.stockQuantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkText
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Alert",
                        tint = AlertRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAlertPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MutedText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
