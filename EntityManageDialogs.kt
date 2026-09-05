package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.data.local.entity.DoctorEntity
import com.example.data.local.entity.MedicineEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PatientEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel

@Composable
fun AddEditMedicineDialog(
    existingMedicine: MedicineEntity?,
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val isEdit = existingMedicine != null

    var productName by remember { mutableStateOf(existingMedicine?.productName ?: "") }
    var productType by remember { mutableStateOf(existingMedicine?.productType ?: "Tablet") }
    var packagingType by remember { mutableStateOf(existingMedicine?.packagingType ?: "1×10T") }
    var composition by remember { mutableStateOf(existingMedicine?.composition ?: "") }
    var companyName by remember { mutableStateOf(existingMedicine?.companyName ?: "") }
    var manufacturerName by remember { mutableStateOf(existingMedicine?.manufacturerName ?: "") }
    var batchNumber by remember { mutableStateOf(existingMedicine?.batchNumber ?: "") }
    var rackLocation by remember { mutableStateOf(existingMedicine?.rackLocation ?: "") }
    var expiryDate by remember { mutableStateOf(existingMedicine?.expiryDate ?: "") }
    var purchaseRateStr by remember { mutableStateOf(existingMedicine?.purchaseRate?.toString() ?: "") }
    var mrpStr by remember { mutableStateOf(existingMedicine?.mrp?.toString() ?: "") }
    var saleRateStr by remember { mutableStateOf(existingMedicine?.saleRate?.toString() ?: "") }
    var gstPercentStr by remember { mutableStateOf(existingMedicine?.gstPercent?.toString() ?: "12.0") }
    var stockQuantityStr by remember { mutableStateOf(existingMedicine?.stockQuantity?.toString() ?: "0") }
    var freeQuantityStr by remember { mutableStateOf(existingMedicine?.freeQuantity?.toString() ?: "0") }
    var lowStockLevelStr by remember { mutableStateOf(existingMedicine?.lowStockLevel?.toString() ?: "10") }

    var showTypePickerPopup by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMasterAdminAlert by remember { mutableStateOf(false) }
    var masterAdminAlertMessage by remember { mutableStateOf("") }

    val tabletOptions = listOf("1×3T", "1×4T", "1×5T", "1×10T", "1×15T", "1×20T")
    val capsuleOptions = listOf("1×3C", "1×4C", "1×5C", "1×10C", "1×15C", "1×20C")
    val syrupOptions = listOf("15 ml", "30 ml", "50 ml", "60 ml", "100 ml", "200 ml")
    val sachetOptions = listOf("5 gm", "10 gm", "15 gm", "20 gm")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isEdit) "Edit Product" else "Add New Product",
                    fontWeight = FontWeight.Bold
                )
                if (isEdit) {
                    IconButton(
                        onClick = {
                            if (!authState.isMasterAdmin) {
                                masterAdminAlertMessage = "Product delete is restricted to Master Admin only. Sub-admin and Guest users cannot delete products."
                                showMasterAdminAlert = true
                            } else {
                                showDeleteConfirm = true
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Product", tint = AlertRed)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g. CALPOL 650 MG TABLET") },
                    modifier = Modifier.fillMaxWidth().testTag("medicine_name_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = composition,
                    onValueChange = { composition = it },
                    label = { Text("Composition / Generic Formula") },
                    placeholder = { Text("e.g. Paracetamol 650mg") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                // Product Category / Dosage Form & TYPE selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SkyBlueLight.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dosage Category & TYPE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SkyBluePrimary
                        )
                        TextButton(
                            onClick = { showTypePickerPopup = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Quick TYPE Popup", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Form selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Tablet", "Capsule", "Syrup", "Sachet").forEach { type ->
                            FilterChip(
                                selected = productType == type,
                                onClick = {
                                    productType = type
                                    packagingType = when (type) {
                                        "Tablet" -> "1×10T"
                                        "Capsule" -> "1×10C"
                                        "Syrup" -> "100 ml"
                                        "Sachet" -> "5 gm"
                                        else -> packagingType
                                    }
                                    showTypePickerPopup = true
                                },
                                label = { Text(type, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Quick Strip / Unit size buttons for current dosage form
                    val currentOptions = when (productType) {
                        "Tablet" -> tabletOptions
                        "Capsule" -> capsuleOptions
                        "Syrup" -> syrupOptions
                        "Sachet" -> sachetOptions
                        else -> emptyList()
                    }
                    if (currentOptions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            currentOptions.take(4).forEach { opt ->
                                val isSel = packagingType == opt
                                Surface(
                                    onClick = { packagingType = opt },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) SkyBluePrimary else PureWhite,
                                    border = BorderStroke(1.dp, if (isSel) SkyBluePrimary else SkyBlueBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) PureWhite else DarkText,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = packagingType,
                        onValueChange = { packagingType = it },
                        label = { Text("TYPE (Pack) *") },
                        placeholder = { Text("e.g. 1×10T, 100 ml, 5 gm") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                    OutlinedTextField(
                        value = manufacturerName,
                        onValueChange = { manufacturerName = it },
                        label = { Text("Manufacturer / Brand") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Batch Number *") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                    OutlinedTextField(
                        value = rackLocation,
                        onValueChange = { rackLocation = it },
                        label = { Text("Rack / Shelf") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Group") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry Date * (MM/YY)") },
                        placeholder = { Text("12/28") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchaseRateStr,
                        onValueChange = { purchaseRateStr = it },
                        label = { Text("Purchase / Cost (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                    OutlinedTextField(
                        value = mrpStr,
                        onValueChange = { mrpStr = it },
                        label = { Text("MRP (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = saleRateStr,
                        onValueChange = { saleRateStr = it },
                        label = { Text("Sale Rate (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                    OutlinedTextField(
                        value = gstPercentStr,
                        onValueChange = { gstPercentStr = it },
                        label = { Text("GST % (e.g. 12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQuantityStr,
                        onValueChange = { stockQuantityStr = it },
                        label = { Text("Stock Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                    OutlinedTextField(
                        value = freeQuantityStr,
                        onValueChange = { freeQuantityStr = it },
                        label = { Text("Free Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                    OutlinedTextField(
                        value = lowStockLevelStr,
                        onValueChange = { lowStockLevelStr = it },
                        label = { Text("Low Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isEdit && !authState.isMasterAdmin) {
                        masterAdminAlertMessage = "Product edit is restricted to Master Admin only. Sub-admin and Guest users cannot edit products."
                        showMasterAdminAlert = true
                        return@Button
                    }
                    if (productName.isBlank() || batchNumber.isBlank() || expiryDate.isBlank()) {
                        Toast.makeText(context, "Please fill required fields (Name, Batch, Expiry)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val mrp = mrpStr.toDoubleOrNull() ?: 0.0
                    val purchaseRate = purchaseRateStr.toDoubleOrNull() ?: 0.0
                    val saleRate = saleRateStr.toDoubleOrNull() ?: (if (mrp > 0) mrp else purchaseRate)
                    val gst = gstPercentStr.toDoubleOrNull() ?: 12.0
                    val stockQty = stockQuantityStr.toIntOrNull() ?: 0
                    val freeQty = freeQuantityStr.toIntOrNull() ?: 0
                    val lowAlert = lowStockLevelStr.toIntOrNull() ?: 10

                    val med = (existingMedicine ?: MedicineEntity(id = 0L, productName = "")).copy(
                        productName = productName.trim(),
                        productType = productType.trim(),
                        packagingType = packagingType.trim(),
                        composition = composition.trim(),
                        companyName = companyName.trim(),
                        manufacturerName = manufacturerName.trim(),
                        batchNumber = batchNumber.trim(),
                        rackLocation = rackLocation.trim(),
                        expiryDate = expiryDate.trim(),
                        purchaseRate = purchaseRate,
                        mrp = mrp,
                        price = saleRate,
                        saleRate = saleRate,
                        gstPercent = gst,
                        stockQuantity = stockQty,
                        freeQuantity = freeQty,
                        lowStockLevel = lowAlert
                    )
                    viewModel.addOrUpdateMedicine(med)
                    Toast.makeText(context, if (isEdit) "Product updated!" else "Product added to inventory!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
            ) {
                Text("Save Product")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isEdit) {
                    OutlinedButton(
                        onClick = {
                            if (!authState.isMasterAdmin) {
                                masterAdminAlertMessage = "Product delete is restricted to Master Admin only. Sub-admin and Guest users cannot delete products."
                                showMasterAdminAlert = true
                            } else {
                                showDeleteConfirm = true
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                        border = BorderStroke(1.dp, AlertRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = AlertRed, fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showDeleteConfirm && existingMedicine != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Product", fontWeight = FontWeight.Bold, color = AlertRed) },
            text = {
                Text("Are you sure you want to permanently delete \"${existingMedicine.productName}\" (Batch: ${existingMedicine.batchNumber}) from inventory?\n\nThis action can only be performed by Master Admin.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedicine(existingMedicine.id)
                        showDeleteConfirm = false
                        onDismiss()
                        Toast.makeText(context, "Deleted ${existingMedicine.productName}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMasterAdminAlert) {
        AlertDialog(
            onDismissRequest = { showMasterAdminAlert = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AlertRed, modifier = Modifier.size(32.dp)) },
            title = { Text("Master Admin Access Required", fontWeight = FontWeight.Bold, color = AlertRed) },
            text = { Text(masterAdminAlertMessage) },
            confirmButton = {
                Button(
                    onClick = { showMasterAdminAlert = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                ) {
                    Text("Understood")
                }
            }
        )
    }

    // Dedicated Quick TYPE Selection Popup
    if (showTypePickerPopup) {
        AlertDialog(
            onDismissRequest = { showTypePickerPopup = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = SkyBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Packaging TYPE", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose category and unit packaging size:",
                        fontSize = 12.sp,
                        color = MutedText
                    )

                    // Dosage tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Tablet", "Capsule", "Syrup", "Sachet").forEach { cat ->
                            val isSel = productType == cat
                            Surface(
                                onClick = { productType = cat },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) SkyBluePrimary else SkyBlueLight,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) PureWhite else DarkText,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    when (productType) {
                        "Tablet" -> {
                            Text("Strip Packaging for Tablets:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            val options = listOf(
                                "1×3T" to "3 Tabs Strip (1×3T)",
                                "1×4T" to "4 Tabs Strip (1×4T)",
                                "1×5T" to "5 Tabs Strip (1×5T)",
                                "1×10T" to "10 Tabs Strip (1×10T)",
                                "1×15T" to "15 Tabs Strip (1×15T)",
                                "1×20T" to "20 Tabs Strip (1×20T)"
                            )
                            options.forEach { (valKey, label) ->
                                OutlinedButton(
                                    onClick = {
                                        packagingType = valKey
                                        showTypePickerPopup = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = if (packagingType == valKey) ButtonDefaults.outlinedButtonColors(containerColor = SkyBlueLight) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text(label, fontWeight = if (packagingType == valKey) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        "Capsule" -> {
                            Text("Strip Packaging for Capsules:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            val options = listOf(
                                "1×3C" to "3 Capsules Strip (1×3C)",
                                "1×4C" to "4 Capsules Strip (1×4C)",
                                "1×5C" to "5 Capsules Strip (1×5C)",
                                "1×10C" to "10 Capsules Strip (1×10C)",
                                "1×15C" to "15 Capsules Strip (1×15C)",
                                "1×20C" to "20 Capsules Strip (1×20C)"
                            )
                            options.forEach { (valKey, label) ->
                                OutlinedButton(
                                    onClick = {
                                        packagingType = valKey
                                        showTypePickerPopup = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = if (packagingType == valKey) ButtonDefaults.outlinedButtonColors(containerColor = SkyBlueLight) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text(label, fontWeight = if (packagingType == valKey) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        "Syrup" -> {
                            Text("Volume / Bottle Size for Syrup:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            val options = listOf(
                                "15 ml" to "15 ml Bottle",
                                "30 ml" to "30 ml Bottle",
                                "50 ml" to "50 ml Bottle",
                                "60 ml" to "60 ml Bottle",
                                "100 ml" to "100 ml Bottle",
                                "200 ml" to "200 ml Bottle"
                            )
                            options.forEach { (valKey, label) ->
                                OutlinedButton(
                                    onClick = {
                                        packagingType = valKey
                                        showTypePickerPopup = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = if (packagingType == valKey) ButtonDefaults.outlinedButtonColors(containerColor = SkyBlueLight) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text(label, fontWeight = if (packagingType == valKey) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        "Sachet" -> {
                            Text("Grams Weight for Sachet:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            val options = listOf(
                                "5 gm" to "5 gm Sachet",
                                "10 gm" to "10 gm Sachet",
                                "15 gm" to "15 gm Sachet",
                                "20 gm" to "20 gm Sachet"
                            )
                            options.forEach { (valKey, label) ->
                                OutlinedButton(
                                    onClick = {
                                        packagingType = valKey
                                        showTypePickerPopup = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = if (packagingType == valKey) ButtonDefaults.outlinedButtonColors(containerColor = SkyBlueLight) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text(label, fontWeight = if (packagingType == valKey) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTypePickerPopup = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AddEditPartyDialog(
    existingParty: PartyEntity?,
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit,
    onSaved: ((PartyEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val isEdit = existingParty != null

    var partyName by remember { mutableStateOf(existingParty?.partyName ?: "") }
    var dlNumber by remember { mutableStateOf(existingParty?.dlNumber ?: "") }
    var gstPanNumber by remember { mutableStateOf(existingParty?.gstPanNumber ?: "") }
    var address by remember { mutableStateOf(existingParty?.address ?: "") }
    var contactNumber by remember { mutableStateOf(existingParty?.contactNumber ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) "Edit Party / Pharmacy" else "Add New Party / Pharmacy",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text("Party / Pharmacy Name *") },
                    placeholder = { Text("e.g. M/S BHIMESWARI MEDICAL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = dlNumber,
                    onValueChange = { dlNumber = it },
                    label = { Text("Drug License (D.L.) Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = gstPanNumber,
                    onValueChange = { gstPanNumber = it },
                    label = { Text("GSTIN / PAN Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                    if (partyName.isBlank()) {
                        Toast.makeText(context, "Please enter Party Name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val oldName = existingParty?.partyName ?: partyName.trim()
                    val party = (existingParty ?: PartyEntity(id = 0L, partyName = "")).copy(
                        partyName = partyName.trim(),
                        dlNumber = dlNumber.trim(),
                        gstPanNumber = gstPanNumber.trim(),
                        address = address.trim(),
                        contactNumber = contactNumber.trim()
                    )
                    viewModel.updatePartyAndKhata(party, oldName)
                    Toast.makeText(context, if (isEdit) "Party updated!" else "Party saved!", Toast.LENGTH_SHORT).show()
                    onSaved?.invoke(party)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
            ) {
                Text("Save Party")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddEditDoctorDialog(
    existingDoctor: DoctorEntity?,
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit,
    onSaved: ((DoctorEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val isEdit = existingDoctor != null

    var doctorName by remember { mutableStateOf(existingDoctor?.doctorName ?: "") }
    var qualification by remember { mutableStateOf(existingDoctor?.qualification ?: "") }
    var phoneNumber by remember { mutableStateOf(existingDoctor?.phoneNumber ?: "") }
    var address by remember { mutableStateOf(existingDoctor?.address ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) "Edit Doctor" else "Add New Doctor",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor Name *") },
                    placeholder = { Text("e.g. Dr. R. K. Sen, MBBS") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Specialization / Degree") },
                    placeholder = { Text("e.g. MD, MBBS, Child Specialist") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Clinic / Hospital Address") },
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
                    if (doctorName.isBlank()) {
                        Toast.makeText(context, "Please enter Doctor Name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val oldName = existingDoctor?.doctorName ?: doctorName.trim()
                    val doc = (existingDoctor ?: DoctorEntity(id = 0L, doctorName = "")).copy(
                        doctorName = doctorName.trim(),
                        qualification = qualification.trim(),
                        phoneNumber = phoneNumber.trim(),
                        address = address.trim()
                    )
                    viewModel.updateDoctorAndKhata(doc, oldName)
                    Toast.makeText(context, if (isEdit) "Doctor updated!" else "Doctor saved!", Toast.LENGTH_SHORT).show()
                    onSaved?.invoke(doc)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
            ) {
                Text("Save Doctor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddEditPatientDialog(
    existingPatient: PatientEntity?,
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit,
    onSaved: ((PatientEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val isEdit = existingPatient != null

    var patientName by remember { mutableStateOf(existingPatient?.patientName ?: "") }
    var phoneNumber by remember { mutableStateOf(existingPatient?.phoneNumber ?: "") }
    var address by remember { mutableStateOf(existingPatient?.address ?: "") }
    var doctorName by remember { mutableStateOf(existingPatient?.doctorName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) "Edit Patient" else "Add New Patient",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Prescribed / Referred by Doctor") },
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
                    if (patientName.isBlank()) {
                        Toast.makeText(context, "Please enter Patient Name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val oldName = existingPatient?.patientName ?: patientName.trim()
                    val pat = (existingPatient ?: PatientEntity(id = 0L, patientName = "")).copy(
                        patientName = patientName.trim(),
                        phoneNumber = phoneNumber.trim(),
                        address = address.trim(),
                        doctorName = doctorName.trim()
                    )
                    viewModel.updatePatientAndKhata(pat, oldName)
                    Toast.makeText(context, if (isEdit) "Patient updated!" else "Patient saved!", Toast.LENGTH_SHORT).show()
                    onSaved?.invoke(pat)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
            ) {
                Text("Save Patient")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ProductAddOptionDialog(
    onAddNew: () -> Unit,
    onAddStock: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Inventory Action", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddNew,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add New Product / Medicine")
                }

                Button(
                    onClick = onAddStock,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlueSecondary)
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Stock to Existing Product")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockToExistingMedicineDialog(
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val medicines by viewModel.medicines.collectAsState()

    var selectedMedicine by remember { mutableStateOf<MedicineEntity?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var additionalStockStr by remember { mutableStateOf("") }
    var additionalFreeStr by remember { mutableStateOf("0") }
    var newBatch by remember { mutableStateOf("") }
    var newExpiry by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Inward Stock", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMedicine?.productName ?: "Select Medicine...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Product / Medicine") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        medicines.forEach { med ->
                            DropdownMenuItem(
                                text = { Text("${med.productName} (${med.batchNumber}) - Stock: ${med.stockQuantity}") },
                                onClick = {
                                    selectedMedicine = med
                                    newBatch = med.batchNumber
                                    newExpiry = med.expiryDate
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                selectedMedicine?.let { med ->
                    Text(
                        text = "Current Stock: ${med.stockQuantity} | Free: ${med.freeQuantity} | Batch: ${med.batchNumber}",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = additionalStockStr,
                        onValueChange = { additionalStockStr = it },
                        label = { Text("Inward Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = additionalFreeStr,
                        onValueChange = { additionalFreeStr = it },
                        label = { Text("Free Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newBatch,
                        onValueChange = { newBatch = it },
                        label = { Text("Batch No") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = newExpiry,
                        onValueChange = { newExpiry = it },
                        label = { Text("Expiry (MM/YY)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val med = selectedMedicine
                    val addStock = additionalStockStr.toIntOrNull() ?: 0
                    val addFree = additionalFreeStr.toIntOrNull() ?: 0

                    if (med == null) {
                        Toast.makeText(context, "Please select a product", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (addStock <= 0) {
                        Toast.makeText(context, "Please enter valid stock quantity", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    viewModel.addStockToExistingMedicine(
                        medicineId = med.id,
                        addedQty = addStock,
                        addedFreeQty = addFree,
                        newBatch = newBatch.ifBlank { med.batchNumber },
                        newExpiry = newExpiry.ifBlank { med.expiryDate },
                        newPurchaseRate = med.purchaseRate,
                        newMrp = med.mrp,
                        newSaleRate = med.saleRate
                    )
                    Toast.makeText(context, "Added $addStock inward stock to ${med.productName}", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
            ) {
                Text("Add Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
