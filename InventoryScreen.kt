package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MedicineEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.DateUtils

@Composable
fun InventoryScreen(
    viewModel: PharmaViewModel,
    onEditMedicine: (MedicineEntity) -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val medicines by viewModel.medicines.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showActionDialog by remember { mutableStateOf(false) }
    var showAddNewDialog by remember { mutableStateOf(false) }
    var showAddStockDialog by remember { mutableStateOf(false) }
    var medicineToDelete by remember { mutableStateOf<MedicineEntity?>(null) }
    var showMasterAdminOnlyDialog by remember { mutableStateOf(false) }
    var masterAdminDialogMessage by remember { mutableStateOf("") }

    val filteredMedicines = remember(medicines, searchQuery) {
        if (searchQuery.isBlank()) medicines
        else medicines.filter {
            it.productName.contains(searchQuery, ignoreCase = true) ||
            it.manufacturerName.contains(searchQuery, ignoreCase = true) ||
            it.companyName.contains(searchQuery, ignoreCase = true) ||
            it.batchNumber.contains(searchQuery, ignoreCase = true) ||
            it.rackLocation.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(12.dp)
    ) {
        // Search & Add Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search medicine, company, batch...") },
                modifier = Modifier.weight(1f).testTag("inventory_search_field"),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SkyBluePrimary,
                    unfocusedBorderColor = SkyBlueBorder
                )
            )

            Button(
                onClick = {
                    if (authState.isGuest) {
                        masterAdminDialogMessage = "Guest users have view-only access. Product adding is restricted to Admin."
                        showMasterAdminOnlyDialog = true
                    } else {
                        showActionDialog = true
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                modifier = Modifier.height(54.dp).testTag("inventory_add_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Inventory List
        if (filteredMedicines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No medicines found in inventory." else "No medicines matching \"$searchQuery\"",
                    color = MutedText,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMedicines, key = { it.id }) { med ->
                    MedicineInventoryCard(
                        medicine = med,
                        isAdmin = authState.isAdmin,
                        onBill = {
                            if (authState.isGuest) {
                                masterAdminDialogMessage = "Guest users have view-only access. Billing is restricted to Admin."
                                showMasterAdminOnlyDialog = true
                            } else {
                                viewModel.addMedicineToBill(med)
                                viewModel.switchTab(NavigationTab.NEW_BILL)
                            }
                        },
                        onEdit = {
                            if (!authState.isMasterAdmin) {
                                masterAdminDialogMessage = "Product edit is restricted to Master Admin only. Sub-admin and Guest users cannot edit products."
                                showMasterAdminOnlyDialog = true
                            } else {
                                onEditMedicine(med)
                            }
                        },
                        onDelete = {
                            if (!authState.isMasterAdmin) {
                                masterAdminDialogMessage = "Product delete is restricted to Master Admin only. Sub-admin and Guest users cannot delete products."
                                showMasterAdminOnlyDialog = true
                            } else {
                                medicineToDelete = med
                            }
                        }
                    )
                }
            }
        }
    }

    if (showActionDialog) {
        ProductAddOptionDialog(
            onAddNew = {
                showActionDialog = false
                showAddNewDialog = true
            },
            onAddStock = {
                showActionDialog = false
                showAddStockDialog = true
            },
            onDismiss = { showActionDialog = false }
        )
    }

    if (showAddNewDialog) {
        AddEditMedicineDialog(
            existingMedicine = null,
            viewModel = viewModel,
            onDismiss = { showAddNewDialog = false }
        )
    }

    if (showAddStockDialog) {
        AddStockToExistingMedicineDialog(
            viewModel = viewModel,
            onDismiss = { showAddStockDialog = false }
        )
    }

    medicineToDelete?.let { med ->
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            title = { Text("Delete Medicine", fontWeight = FontWeight.Bold, color = AlertRed) },
            text = { Text("Are you sure you want to permanently delete \"${med.productName}\" (Batch: ${med.batchNumber}) from inventory?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedicine(med.id)
                        medicineToDelete = null
                        Toast.makeText(context, "Deleted ${med.productName}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { medicineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMasterAdminOnlyDialog) {
        AdminOnlyDialog(
            onDismiss = { showMasterAdminOnlyDialog = false },
            title = "Master Admin Only",
            message = masterAdminDialogMessage.ifEmpty { "Product edit and delete are restricted to Master Admin only." }
        )
    }
}

@Composable
fun MedicineInventoryCard(
    medicine: MedicineEntity,
    isAdmin: Boolean,
    onBill: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpired = DateUtils.isExpired(medicine.expiryDate)
    val isNearExpiry = DateUtils.isNearExpiry(medicine.expiryDate)
    val isLowStock = medicine.stockQuantity <= medicine.lowStockLevel

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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.productName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DarkText
                    )
                    if (medicine.composition.isNotBlank()) {
                        Text(
                            text = "Comp: ${medicine.composition}",
                            fontSize = 11.sp,
                            color = SkyBluePrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${medicine.manufacturerName.ifEmpty { medicine.companyName }} • TYPE: ${medicine.packagingType} • Rack: ${medicine.rackLocation.ifEmpty { "N/A" }}",
                        fontSize = 11.sp,
                        color = MutedText
                    )
                    Text(
                        text = "Batch: ${medicine.batchNumber} • Exp: ${medicine.expiryDate}",
                        fontSize = 11.sp,
                        color = if (isExpired || isNearExpiry) AlertRed else MutedText,
                        fontWeight = if (isExpired || isNearExpiry) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Stock: ${medicine.stockQuantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isLowStock) AlertRed else SuccessGreen
                    )
                    if (medicine.freeQuantity > 0) {
                        Text(
                            text = "+ Free: ${medicine.freeQuantity}",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAdmin) "MRP: ₹${String.format("%.2f", medicine.mrp)} | Rate: ₹${String.format("%.2f", medicine.saleRate)} | GST: ${medicine.gstPercent}%" else "MRP: •••••• | Rate: •••••• | GST: ${medicine.gstPercent}%",
                    fontSize = 11.sp,
                    color = DarkText
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onBill,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Bill", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Edit", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
