package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GuestLoginEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.MedicineEntity
import com.example.data.sync.SyncState
import com.example.ui.screens.*
import com.example.ui.theme.AlertRed
import com.example.ui.theme.PharmaBillProTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBlueContainer
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.DateUtils
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    private val viewModel: PharmaViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PharmaBillProTheme {
                val authState by viewModel.authState.collectAsState()
                val currentTab by viewModel.currentTab.collectAsState()
                val settings by viewModel.settings.collectAsState()
                val medicines by viewModel.medicines.collectAsState()
                val invoices by viewModel.invoices.collectAsState()
                val guestLogins by viewModel.guestLogins.collectAsState()
                val syncInfo by viewModel.syncInfo.collectAsState()

                val lowStockCount = remember(medicines) { medicines.count { it.stockQuantity <= it.lowStockLevel } }
                val expCount = remember(medicines) { medicines.count { DateUtils.isNearExpiry(it.expiryDate) || DateUtils.isExpired(it.expiryDate) } }
                val dueCount = remember(invoices) { invoices.count { it.dueAmount > 0 } }
                val guestLoginAlertCount = if (authState.isAdmin) guestLogins.size else 0
                val alertsCount = lowStockCount + expCount + dueCount + guestLoginAlertCount

                var showStockStatementDialog by remember { mutableStateOf(false) }
                var showKhataDialog by remember { mutableStateOf(false) }
                var showSettingsDialog by remember { mutableStateOf(false) }
                var showSyncStatusDialog by remember { mutableStateOf(false) }
                var showCustomerManageDialog by remember { mutableStateOf(false) }
                var customerManageInitialTab by remember { mutableStateOf("PARTY") }

                var selectedInvoiceForView by remember { mutableStateOf<InvoiceEntity?>(null) }
                var medicineToEdit by remember { mutableStateOf<MedicineEntity?>(null) }
                var showAdminOnlyDialog by remember { mutableStateOf(false) }
                var adminDialogTitle by remember { mutableStateOf("Only for Admin user") }
                var adminDialogMessage by remember { mutableStateOf("Guest users have view-only access. Billing, editing, deleting, bill modifying, printing, and amount details are restricted to Admin users.") }

                var lastGuestLoginId by remember { mutableLongStateOf(guestLogins.firstOrNull()?.id ?: 0L) }
                var newGuestNotification by remember { mutableStateOf<GuestLoginEntity?>(null) }

                LaunchedEffect(guestLogins) {
                    if (authState.isAdmin && guestLogins.isNotEmpty()) {
                        val latest = guestLogins.first()
                        if (lastGuestLoginId != 0L && (latest.id > lastGuestLoginId || latest.loginTimestamp > (System.currentTimeMillis() - 180_000L))) {
                            if (newGuestNotification?.id != latest.id) {
                                newGuestNotification = latest
                            }
                        }
                        lastGuestLoginId = latest.id
                    }
                }

                if (!authState.isLoggedIn) {
                    LoginScreen(viewModel = viewModel)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = settings.businessName.ifEmpty { "PharmaBill Pro" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = PureWhite
                                        )
                                        Text(
                                            text = if (authState.isAdmin) "Admin Access" else "Guest (View Only)",
                                            fontSize = 11.sp,
                                            color = SkyBlueContainer
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = SkyBluePrimary,
                                    actionIconContentColor = PureWhite
                                ),
                                actions = {
                                    // Automatic Offline-First Cloud Sync Status Indicator
                                    IconButton(
                                        onClick = { showSyncStatusDialog = true },
                                        modifier = Modifier.testTag("sync_status_btn")
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (syncInfo.pendingCount > 0) {
                                                    Badge(containerColor = Color(0xFFFF9800)) {
                                                        Text("${syncInfo.pendingCount}", color = PureWhite, fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    syncInfo.state == SyncState.SYNCING -> Icons.Default.CloudSync
                                                    syncInfo.isOnline -> Icons.Default.CloudDone
                                                    else -> Icons.Default.CloudOff
                                                },
                                                contentDescription = "Cloud Sync Status",
                                                tint = when {
                                                    syncInfo.state == SyncState.SYNCING -> SkyBlueContainer
                                                    syncInfo.isOnline -> PureWhite
                                                    else -> PureWhite.copy(alpha = 0.65f)
                                                }
                                            )
                                        }
                                    }

                                    if (authState.isMasterAdmin) {
                                        IconButton(
                                            onClick = { viewModel.switchTab(NavigationTab.GST) },
                                            modifier = Modifier.testTag("gst_top_nav_btn")
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = AlertRed) {
                                                        Text("GST", color = PureWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.ReceiptLong, contentDescription = "GST Register", tint = PureWhite)
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { showStockStatementDialog = true },
                                        modifier = Modifier.testTag("stock_statement_btn")
                                    ) {
                                        Icon(Icons.Default.Assessment, contentDescription = "Stock Statement")
                                    }

                                    IconButton(
                                        onClick = { showKhataDialog = true },
                                        modifier = Modifier.testTag("khata_btn")
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = "Khata Register")
                                    }

                                    IconButton(
                                        onClick = { showSettingsDialog = true },
                                        modifier = Modifier.testTag("settings_btn")
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }

                                    IconButton(
                                        onClick = { viewModel.logout() },
                                        modifier = Modifier.testTag("logout_btn")
                                    ) {
                                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = PureWhite,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == NavigationTab.HOME,
                                    onClick = { viewModel.switchTab(NavigationTab.HOME) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_home")
                                )

                                NavigationBarItem(
                                    selected = currentTab == NavigationTab.NEW_BILL,
                                    onClick = {
                                        if (authState.isGuest) {
                                            showAdminOnlyDialog = true
                                        } else {
                                            viewModel.switchTab(NavigationTab.NEW_BILL)
                                        }
                                    },
                                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = "New Bill") },
                                    label = { Text("New Bill", fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_bill")
                                )

                                NavigationBarItem(
                                    selected = currentTab == NavigationTab.INVENTORY,
                                    onClick = { viewModel.switchTab(NavigationTab.INVENTORY) },
                                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Inventory") },
                                    label = { Text("Inventory", fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_inventory")
                                )

                                NavigationBarItem(
                                    selected = currentTab == NavigationTab.ALERTS,
                                    onClick = { viewModel.switchTab(NavigationTab.ALERTS) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (alertsCount > 0) {
                                                    Badge(containerColor = AlertRed) {
                                                        Text("$alertsCount", color = PureWhite, fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                                        }
                                    },
                                    label = { Text("Alerts", fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_alerts")
                                )

                                NavigationBarItem(
                                    selected = currentTab == NavigationTab.SALES || currentTab == NavigationTab.SALES_HISTORY,
                                    onClick = { viewModel.switchTab(NavigationTab.SALES) },
                                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Sales") },
                                    label = { Text("Sales", fontSize = 11.sp) },
                                    modifier = Modifier.testTag("nav_sales")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .imePadding()
                        ) {
                            when (currentTab) {
                                NavigationTab.HOME -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onNewBill = { viewModel.switchTab(NavigationTab.NEW_BILL) },
                                        onStockStatement = { showStockStatementDialog = true },
                                        onKhata = { showKhataDialog = true },
                                        onManageCustomers = {
                                            customerManageInitialTab = "PARTY"
                                            showCustomerManageDialog = true
                                        },
                                        onInvoiceClick = { selectedInvoiceForView = it }
                                    )
                                }
                                NavigationTab.NEW_BILL -> {
                                    NewBillScreen(
                                        viewModel = viewModel,
                                        onOpenAddCustomer = { type ->
                                            customerManageInitialTab = type
                                            showCustomerManageDialog = true
                                        },
                                        onViewInvoice = { selectedInvoiceForView = it }
                                    )
                                }
                                NavigationTab.INVENTORY -> {
                                    InventoryScreen(
                                        viewModel = viewModel,
                                        onEditMedicine = {
                                            if (!authState.isMasterAdmin) {
                                                adminDialogTitle = "Master Admin Access Required"
                                                adminDialogMessage = "Product edit and delete are restricted to Master Admin only. Sub-admin and Guest users cannot edit or delete products."
                                                showAdminOnlyDialog = true
                                            } else {
                                                medicineToEdit = it
                                            }
                                        }
                                    )
                                }
                                NavigationTab.ALERTS -> {
                                    AlertsScreen(
                                        viewModel = viewModel,
                                        onViewInvoice = { selectedInvoiceForView = it }
                                    )
                                }
                                NavigationTab.SALES -> {
                                    SalesScreen(
                                        viewModel = viewModel,
                                        onNewBill = { viewModel.switchTab(NavigationTab.NEW_BILL) },
                                        onViewInvoice = { selectedInvoiceForView = it },
                                        onEditInvoice = { inv ->
                                            viewModel.loadInvoiceForEdit(inv)
                                        },
                                        onOpenKhata = { showKhataDialog = true }
                                    )
                                }
                                NavigationTab.SALES_HISTORY -> {
                                    SalesHistoryScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.switchTab(NavigationTab.SALES) },
                                        onViewInvoice = { selectedInvoiceForView = it },
                                        onNewBill = { viewModel.switchTab(NavigationTab.NEW_BILL) }
                                    )
                                }
                                NavigationTab.SETTINGS -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onDismiss = { viewModel.switchTab(NavigationTab.HOME) }
                                    )
                                }
                                NavigationTab.GST -> {
                                    GstScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.switchTab(NavigationTab.HOME) }
                                    )
                                }
                            }

                            if (newGuestNotification != null && authState.isAdmin) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .align(Alignment.TopCenter),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "New Guest Login Alert",
                                                fontWeight = FontWeight.Bold,
                                                color = PureWhite,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                "Mobile: ${newGuestNotification?.mobileNumber} just entered the app",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.switchTab(NavigationTab.ALERTS)
                                                newGuestNotification = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("View Alert", fontSize = 11.sp, color = PureWhite)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { newGuestNotification = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showStockStatementDialog) {
                        StockStatementDialog(
                            viewModel = viewModel,
                            onDismiss = { showStockStatementDialog = false }
                        )
                    }

                    if (showKhataDialog) {
                        KhataScreen(
                            viewModel = viewModel,
                            onDismiss = { showKhataDialog = false }
                        )
                    }

                    if (showSettingsDialog) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showSettingsDialog = false },
                            properties = androidx.compose.ui.window.DialogProperties(
                                usePlatformDefaultWidth = false,
                                decorFitsSystemWindows = false
                            )
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .systemBarsPadding()
                                    .imePadding()
                                ) {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onDismiss = { showSettingsDialog = false }
                                )
                            }
                        }
                    }

                    if (showSyncStatusDialog) {
                        SyncStatusDialog(
                            viewModel = viewModel,
                            onDismiss = { showSyncStatusDialog = false }
                        )
                    }

                    if (showCustomerManageDialog) {
                        CustomerManageDialog(
                            initialTab = customerManageInitialTab,
                            viewModel = viewModel,
                            onDismiss = { showCustomerManageDialog = false }
                        )
                    }

                    selectedInvoiceForView?.let { inv ->
                        InvoiceViewDialog(
                            invoice = inv,
                            viewModel = viewModel,
                            onDismiss = { selectedInvoiceForView = null },
                            onEdit = { invoiceToEdit ->
                                if (authState.isGuest) {
                                    showAdminOnlyDialog = true
                                } else {
                                    selectedInvoiceForView = null
                                    viewModel.loadInvoiceForEdit(invoiceToEdit)
                                }
                            }
                        )
                    }

                    medicineToEdit?.let { med ->
                        AddEditMedicineDialog(
                            existingMedicine = med,
                            viewModel = viewModel,
                            onDismiss = { medicineToEdit = null }
                        )
                    }

                    if (showAdminOnlyDialog) {
                        AdminOnlyDialog(
                            onDismiss = { showAdminOnlyDialog = false },
                            title = adminDialogTitle,
                            message = adminDialogMessage
                        )
                    }
                }
            }
        }
    }
}
