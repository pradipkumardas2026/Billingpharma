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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DoctorEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.MedicineEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PatientEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillDraftItem
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.PdfGenerator
import com.example.util.PrintHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBillScreen(
    viewModel: PharmaViewModel,
    onOpenAddCustomer: (String) -> Unit,
    onViewInvoice: (InvoiceEntity) -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val medicines by viewModel.medicines.collectAsState()
    val parties by viewModel.parties.collectAsState()
    val doctors by viewModel.doctors.collectAsState()
    val patients by viewModel.patients.collectAsState()

    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    val billCustomerType by viewModel.billCustomerType.collectAsState()
    val billCustomerName by viewModel.billCustomerName.collectAsState()
    val billCustomerPhone by viewModel.billCustomerPhone.collectAsState()
    val billCustomerAddress by viewModel.billCustomerAddress.collectAsState()
    val billCustomerDl by viewModel.billCustomerDl.collectAsState()
    val billCustomerGst by viewModel.billCustomerGst.collectAsState()
    val billDoctorName by viewModel.billDoctorName.collectAsState()

    val billItems by viewModel.billItems.collectAsState()
    val billGrossAmount by viewModel.billGrossAmount.collectAsState()
    val billDiscountAmount by viewModel.billDiscountAmount.collectAsState()
    val billTotalGst by viewModel.billTotalGst.collectAsState()
    val billRoundOff by viewModel.billRoundOff.collectAsState()
    val billAdjustmentAmount by viewModel.adjustmentAmount.collectAsState()
    val billNetAmount by viewModel.billNetAmount.collectAsState()
    val billPaidAmount by viewModel.billPaidAmount.collectAsState()
    val billDueAmount by viewModel.billDueAmount.collectAsState()
    val billNote by viewModel.billNote.collectAsState()
    val editingInvoiceNumber by viewModel.editingInvoiceNumber.collectAsState()

    var medSearchQuery by remember { mutableStateOf("") }
    var medDropdownExpanded by remember { mutableStateOf(false) }

    var showCustomerSearchDialog by remember { mutableStateOf(false) }
    var showCustomerCategoryChooser by remember { mutableStateOf(false) }
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var showAddDoctorDialog by remember { mutableStateOf(false) }
    var showAddPatientDialog by remember { mutableStateOf(false) }
    var editingSelectedParty by remember { mutableStateOf<PartyEntity?>(null) }
    var editingSelectedDoctor by remember { mutableStateOf<DoctorEntity?>(null) }
    var editingSelectedPatient by remember { mutableStateOf<PatientEntity?>(null) }
    var itemToEdit by remember { mutableStateOf<Pair<Int, BillDraftItem>?>(null) }
    var medicineToAdd by remember { mutableStateOf<MedicineEntity?>(null) }
    var generatedInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var generatedInvoiceItems by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }

    val customerTypes = listOf("PARTY", "DOCTOR", "PATIENT")

    if (authState.isGuest) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhite)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(AlertRed.copy(alpha = 0.12f), RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Billing Restricted",
                            tint = AlertRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Billing Restricted",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Guest users have view-only access. Generating new bills, modifying bills, or printing bills is only permitted for Admin accounts.",
                        fontSize = 13.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Edit Mode Banner (if editing existing invoice)
        if (editingInvoiceNumber != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SkyBlueContainer),
                border = BorderStroke(1.dp, SkyBluePrimary)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Modifying Invoice #$editingInvoiceNumber",
                        fontWeight = FontWeight.Bold,
                        color = SkyBlueSecondary,
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { viewModel.resetBillDraft() }) {
                        Text("Cancel Edit", color = AlertRed, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 1: Customer Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, LightBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Customer / Patient Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = { showCustomerSearchDialog = true },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Search", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (authState.isGuest) {
                                    showAdminOnlyDialog = true
                                } else {
                                    showCustomerCategoryChooser = true
                                }
                            },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("New Customer", fontSize = 11.sp)
                        }
                    }
                }

                // Type Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    customerTypes.forEach { type ->
                        FilterChip(
                            selected = billCustomerType == type,
                            onClick = { viewModel.setCustomerType(type) },
                            label = { Text(type, fontSize = 11.sp) }
                        )
                    }
                }

                val currentParty = remember(parties, billCustomerName, billCustomerPhone) {
                    parties.find {
                        (billCustomerName.isNotBlank() && it.partyName.equals(billCustomerName.trim(), ignoreCase = true)) ||
                        (billCustomerPhone.isNotBlank() && it.contactNumber.equals(billCustomerPhone.trim(), ignoreCase = true))
                    }
                }
                val currentDoctor = remember(doctors, billCustomerName, billCustomerPhone) {
                    doctors.find {
                        (billCustomerName.isNotBlank() && it.doctorName.equals(billCustomerName.trim(), ignoreCase = true)) ||
                        (billCustomerPhone.isNotBlank() && it.phoneNumber.equals(billCustomerPhone.trim(), ignoreCase = true))
                    }
                }
                val currentPatient = remember(patients, billCustomerName, billCustomerPhone) {
                    patients.find {
                        (billCustomerName.isNotBlank() && it.patientName.equals(billCustomerName.trim(), ignoreCase = true)) ||
                        (billCustomerPhone.isNotBlank() && it.phoneNumber.equals(billCustomerPhone.trim(), ignoreCase = true))
                    }
                }

                // Dynamic Selector & Edit / Update Details button
                when (billCustomerType) {
                    "PARTY" -> {
                        var partyDropdownExpanded by remember { mutableStateOf(false) }
                        var partyQuery by remember { mutableStateOf("") }
                        val filteredParties = remember(parties, partyQuery) {
                            if (partyQuery.isBlank()) parties
                            else parties.filter {
                                it.partyName.contains(partyQuery, ignoreCase = true) ||
                                it.contactNumber.contains(partyQuery) ||
                                it.dlNumber.contains(partyQuery, ignoreCase = true)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = partyDropdownExpanded,
                                onExpandedChange = { partyDropdownExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = if (partyDropdownExpanded) partyQuery else billCustomerName.ifBlank { partyQuery },
                                    onValueChange = {
                                        partyQuery = it
                                        partyDropdownExpanded = true
                                    },
                                    label = { Text("Select Party (${parties.size} registered)") },
                                    placeholder = { Text("Choose registered party...") },
                                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = SkyBluePrimary) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyDropdownExpanded) },
                                    singleLine = true,
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SkyBluePrimary,
                                        unfocusedBorderColor = SkyBlueBorder
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = partyDropdownExpanded,
                                    onDismissRequest = { partyDropdownExpanded = false }
                                ) {
                                    if (filteredParties.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No matching party found", fontSize = 12.sp, color = MutedText) },
                                            onClick = { partyDropdownExpanded = false }
                                        )
                                    } else {
                                        filteredParties.forEach { p ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(p.partyName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(
                                                            "Phone: ${p.contactNumber.ifBlank { "N/A" }} | DL: ${p.dlNumber.ifBlank { "N/A" }} | ${p.address.ifBlank { "Local" }}",
                                                            fontSize = 11.sp,
                                                            color = MutedText
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setCustomer(
                                                        name = p.partyName,
                                                        phone = p.contactNumber,
                                                        address = p.address,
                                                        dl = p.dlNumber,
                                                        gst = p.gstPanNumber
                                                    )
                                                    partyQuery = p.partyName
                                                    partyDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val partyToEdit = currentParty ?: PartyEntity(
                                        id = 0L,
                                        partyName = billCustomerName,
                                        dlNumber = billCustomerDl,
                                        gstPanNumber = billCustomerGst,
                                        address = billCustomerAddress,
                                        contactNumber = billCustomerPhone
                                    )
                                    editingSelectedParty = partyToEdit
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Edit & Update", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Details", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    "DOCTOR" -> {
                        var doctorDropdownExpanded by remember { mutableStateOf(false) }
                        var doctorQuery by remember { mutableStateOf("") }
                        val filteredDoctors = remember(doctors, doctorQuery) {
                            if (doctorQuery.isBlank()) doctors
                            else doctors.filter {
                                it.doctorName.contains(doctorQuery, ignoreCase = true) ||
                                it.phoneNumber.contains(doctorQuery) ||
                                it.qualification.contains(doctorQuery, ignoreCase = true)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = doctorDropdownExpanded,
                                onExpandedChange = { doctorDropdownExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = if (doctorDropdownExpanded) doctorQuery else billCustomerName.ifBlank { doctorQuery },
                                    onValueChange = {
                                        doctorQuery = it
                                        doctorDropdownExpanded = true
                                    },
                                    label = { Text("Select Doctor (${doctors.size} registered)") },
                                    placeholder = { Text("Choose registered doctor...") },
                                    leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null, tint = SkyBluePrimary) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorDropdownExpanded) },
                                    singleLine = true,
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SkyBluePrimary,
                                        unfocusedBorderColor = SkyBlueBorder
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = doctorDropdownExpanded,
                                    onDismissRequest = { doctorDropdownExpanded = false }
                                ) {
                                    if (filteredDoctors.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No matching doctor found", fontSize = 12.sp, color = MutedText) },
                                            onClick = { doctorDropdownExpanded = false }
                                        )
                                    } else {
                                        filteredDoctors.forEach { d ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(d.doctorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(
                                                            "${d.qualification.ifBlank { "Doctor" }} | Phone: ${d.phoneNumber.ifBlank { "N/A" }} | ${d.address.ifBlank { "Clinic" }}",
                                                            fontSize = 11.sp,
                                                            color = MutedText
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setCustomer(
                                                        name = d.doctorName,
                                                        phone = d.phoneNumber,
                                                        address = d.address,
                                                        doctor = d.qualification
                                                    )
                                                    doctorQuery = d.doctorName
                                                    doctorDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val docToEdit = currentDoctor ?: DoctorEntity(
                                        id = 0L,
                                        doctorName = billCustomerName,
                                        qualification = billDoctorName,
                                        phoneNumber = billCustomerPhone,
                                        address = billCustomerAddress
                                    )
                                    editingSelectedDoctor = docToEdit
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Edit & Update", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Details", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    "PATIENT" -> {
                        var patientDropdownExpanded by remember { mutableStateOf(false) }
                        var patientQuery by remember { mutableStateOf("") }
                        val filteredPatients = remember(patients, patientQuery) {
                            if (patientQuery.isBlank()) patients
                            else patients.filter {
                                it.patientName.contains(patientQuery, ignoreCase = true) ||
                                it.phoneNumber.contains(patientQuery) ||
                                it.doctorName.contains(patientQuery, ignoreCase = true)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = patientDropdownExpanded,
                                onExpandedChange = { patientDropdownExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = if (patientDropdownExpanded) patientQuery else billCustomerName.ifBlank { patientQuery },
                                    onValueChange = {
                                        patientQuery = it
                                        patientDropdownExpanded = true
                                    },
                                    label = { Text("Select Patient (${patients.size} registered)") },
                                    placeholder = { Text("Choose registered patient...") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SkyBluePrimary) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientDropdownExpanded) },
                                    singleLine = true,
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SkyBluePrimary,
                                        unfocusedBorderColor = SkyBlueBorder
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = patientDropdownExpanded,
                                    onDismissRequest = { patientDropdownExpanded = false }
                                ) {
                                    if (filteredPatients.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No matching patient found", fontSize = 12.sp, color = MutedText) },
                                            onClick = { patientDropdownExpanded = false }
                                        )
                                    } else {
                                        filteredPatients.forEach { pat ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(pat.patientName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(
                                                            "Phone: ${pat.phoneNumber.ifBlank { "N/A" }} | Dr: ${pat.doctorName.ifBlank { "N/A" }} | ${pat.address.ifBlank { "Local" }}",
                                                            fontSize = 11.sp,
                                                            color = MutedText
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setCustomer(
                                                        name = pat.patientName,
                                                        phone = pat.phoneNumber,
                                                        address = pat.address,
                                                        doctor = pat.doctorName
                                                    )
                                                    patientQuery = pat.patientName
                                                    patientDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val patToEdit = currentPatient ?: PatientEntity(
                                        id = 0L,
                                        patientName = billCustomerName,
                                        phoneNumber = billCustomerPhone,
                                        address = billCustomerAddress,
                                        doctorName = billDoctorName
                                    )
                                    editingSelectedPatient = patToEdit
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Edit & Update", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Details", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Customer Fields
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = billCustomerName,
                        onValueChange = { viewModel.updateCustomerDetails(name = it) },
                        label = { Text("Customer Name *") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                    OutlinedTextField(
                        value = billCustomerPhone,
                        onValueChange = { viewModel.updateCustomerDetails(phone = it) },
                        label = { Text("Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = billCustomerAddress,
                        onValueChange = { viewModel.updateCustomerDetails(address = it) },
                        label = { Text("Address / Location") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                    if (billCustomerType == "PARTY") {
                        OutlinedTextField(
                            value = billCustomerDl,
                            onValueChange = { viewModel.updateCustomerDetails(dl = it) },
                            label = { Text("D.L. Number") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyBluePrimary,
                                unfocusedBorderColor = SkyBlueBorder
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = billDoctorName,
                            onValueChange = { viewModel.updateCustomerDetails(doctor = it) },
                            label = { Text("Referred Doctor") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyBluePrimary,
                                unfocusedBorderColor = SkyBlueBorder
                            )
                        )
                    }
                }
            }
        }

        // Section 2: Medicine Search & Add
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, LightBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Product to Bill", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)

                val filteredMeds = remember(medicines, medSearchQuery) {
                    if (medSearchQuery.isBlank()) emptyList()
                    else medicines.filter {
                        it.productName.contains(medSearchQuery, ignoreCase = true) ||
                        it.composition.contains(medSearchQuery, ignoreCase = true) ||
                        it.companyName.contains(medSearchQuery, ignoreCase = true) ||
                        it.manufacturerName.contains(medSearchQuery, ignoreCase = true) ||
                        it.batchNumber.contains(medSearchQuery, ignoreCase = true)
                    }.take(12)
                }

                ExposedDropdownMenuBox(
                    expanded = medDropdownExpanded && filteredMeds.isNotEmpty(),
                    onExpandedChange = { medDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = medSearchQuery,
                        onValueChange = {
                            medSearchQuery = it
                            medDropdownExpanded = it.isNotBlank()
                        },
                        placeholder = { Text("Search by medicine name, composition, company, batch...") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth().testTag("bill_search_medicine"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = medDropdownExpanded && filteredMeds.isNotEmpty(),
                        onDismissRequest = { medDropdownExpanded = false }
                    ) {
                        filteredMeds.forEach { med ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(med.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        if (med.composition.isNotBlank()) {
                                            Text("Comp: ${med.composition}", fontSize = 11.sp, color = SkyBluePrimary)
                                        }
                                        Text(
                                            "TYPE: ${med.packagingType} • ${med.manufacturerName.ifBlank { med.companyName }} • Batch: ${med.batchNumber} • Exp: ${med.expiryDate} • Stock: ${med.stockQuantity}",
                                            fontSize = 11.sp,
                                            color = MutedText
                                        )
                                    }
                                },
                                onClick = {
                                    if (authState.isGuest) {
                                        showAdminOnlyDialog = true
                                        medDropdownExpanded = false
                                    } else {
                                        medicineToAdd = med
                                        medDropdownExpanded = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Draft Items Table
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
                    Text("Billed Items (${billItems.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)
                    if (billItems.isNotEmpty()) {
                        TextButton(onClick = {
                            if (authState.isGuest) {
                                showAdminOnlyDialog = true
                            } else {
                                viewModel.resetBillDraft()
                            }
                        }) {
                            Text("Clear All", color = AlertRed, fontSize = 11.sp)
                        }
                    }
                }

                if (billItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No items added yet. Search and select medicines above.", color = MutedText, fontSize = 13.sp)
                    }
                } else {
                    val horizontalScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .background(SkyBlueContainer)
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sl", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(30.dp))
                                Text("Product Name", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(150.dp))
                                Text("TYPE", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                Text("Qty", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                Text("Free", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                Text("Rate", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                                Text("Disc%", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(45.dp), textAlign = TextAlign.End)
                                Text("Net(₹)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                                Text("Action", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                            }

                            HorizontalDivider(thickness = 1.dp, color = SkyBlueBorder)

                            billItems.forEachIndexed { index, item ->
                                val rowBg = if (index % 2 == 0) PureWhite else OffWhite
                                Row(
                                    modifier = Modifier
                                        .background(rowBg)
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}", fontSize = 11.sp, modifier = Modifier.width(30.dp))
                                    Column(modifier = Modifier.width(150.dp)) {
                                        Text(item.productName, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("B:${item.batchNo} E:${item.expDate}", fontSize = 9.sp, color = MutedText)
                                    }
                                    Text(item.pack, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                    Text("${item.qty}", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                    Text("${item.freeQty}", fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                    Text(String.format(Locale.US, "%.2f", item.price), fontSize = 11.sp, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                                    Text("${item.discountPercent}%", fontSize = 11.sp, modifier = Modifier.width(45.dp), textAlign = TextAlign.End)
                                    Text(String.format(Locale.US, "%.2f", item.itemTotalAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)

                                    Row(modifier = Modifier.width(70.dp), horizontalArrangement = Arrangement.Center) {
                                        IconButton(
                                            onClick = {
                                                if (authState.isGuest) {
                                                    showAdminOnlyDialog = true
                                                } else {
                                                    itemToEdit = Pair(index, item)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Item", tint = SkyBluePrimary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                if (authState.isGuest) {
                                                    showAdminOnlyDialog = true
                                                } else {
                                                    viewModel.removeDraftItem(index)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Item", tint = AlertRed, modifier = Modifier.size(16.dp))
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

        // Section 4: Calculation & Payment
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, LightBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Bill Summary & Payment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)

                SummaryRow("Gross Amount:", "₹${String.format(Locale.US, "%.2f", billGrossAmount)}")
                if (billDiscountAmount > 0.0) {
                    SummaryRow("Less Discount:", "- ₹${String.format(Locale.US, "%.2f", billDiscountAmount)}")
                }
                if (billTotalGst > 0.0) {
                    SummaryRow("Add GST (SGST+CGST):", "+ ₹${String.format(Locale.US, "%.2f", billTotalGst)}")
                }
                val totalAmountBeforeAdj = billItems.sumOf { it.itemTotalAmount }
                SummaryRow("Total Amount:", "₹${String.format(Locale.US, "%.2f", totalAmountBeforeAdj)}", isBold = true)

                // Manual Adjust Amount Field (ADJ CR/DR NOTE)
                OutlinedTextField(
                    value = if (billAdjustmentAmount == 0.0) "" else billAdjustmentAmount.toString(),
                    onValueChange = {
                        val adj = it.toDoubleOrNull() ?: 0.0
                        viewModel.setAdjustmentAmount(adj)
                    },
                    label = { Text("ADJ CR/DR NOTE (- ₹)") },
                    placeholder = { Text("0.00") },
                    supportingText = {
                        Text("Subtracted from Total Amount (Printed as ADJ CR/DR NOTE in PDF bill)", fontSize = 11.sp, color = MutedText)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjust_amount_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                SummaryRow("Net Payable Amount:", "₹${String.format(Locale.US, "%.2f", billNetAmount)}", isBold = true, textColor = SkyBlueSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = if (billPaidAmount == 0.0) "" else billPaidAmount.toString(),
                        onValueChange = {
                            val paid = it.toDoubleOrNull() ?: 0.0
                            viewModel.setPaidAmount(paid)
                        },
                        label = { Text("Paid Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("paid_amount_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Due Balance:", fontSize = 11.sp, color = MutedText)
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", billDueAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (billDueAmount > 0) AlertRed else SuccessGreen
                        )
                    }
                }

                OutlinedTextField(
                    value = billNote,
                    onValueChange = { viewModel.setBillNote(it) },
                    label = { Text("Description / Note") },
                    placeholder = { Text("Enter description or note for this customer / bill...") },
                    supportingText = {
                        Text(
                            "Visible in Sales History & Khata for this customer. (Hidden from printed bill copy)",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SkyBlueBorder
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (authState.isGuest) {
                            showAdminOnlyDialog = true
                            return@Button
                        }
                        if (billCustomerName.isBlank()) {
                            Toast.makeText(context, "Please enter customer name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (billItems.isEmpty()) {
                            Toast.makeText(context, "Please add at least one item to bill", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.saveOrFinalizeCurrentBillWithItems { savedInv, savedItems ->
                            generatedInvoice = savedInv
                            generatedInvoiceItems = savedItems
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("finalize_bill_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (editingInvoiceNumber != null) "UPDATE INVOICE" else "SAVE & FINALIZE INVOICE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }

    // Customer Search Dialog
    if (showCustomerSearchDialog) {
        AlertDialog(
            onDismissRequest = { showCustomerSearchDialog = false },
            title = { Text("Select Customer", fontWeight = FontWeight.Bold) },
            text = {
                var searchCustQuery by remember { mutableStateOf("") }
                val allList: List<CustomerItem> = remember(parties, doctors, patients, billCustomerType) {
                    when (billCustomerType) {
                        "PARTY" -> parties.map { CustomerItem(it.partyName, "PARTY", it.contactNumber, it.address, it.dlNumber, it.gstPanNumber, partyEntity = it) }
                        "DOCTOR" -> doctors.map { CustomerItem(it.doctorName, "DOCTOR", it.phoneNumber, it.address, "", "", it.qualification, doctorEntity = it) }
                        else -> patients.map { CustomerItem(it.patientName, "PATIENT", it.phoneNumber, it.address, "", "", it.doctorName, patientEntity = it) }
                    }
                }
                val filteredCust = allList.filter {
                    searchCustQuery.isBlank() ||
                    it.name.contains(searchCustQuery, ignoreCase = true) ||
                    it.phone.contains(searchCustQuery)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchCustQuery,
                        onValueChange = { searchCustQuery = it },
                        placeholder = { Text("Search by name or phone...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(filteredCust) { c ->
                            Card(
                                onClick = {
                                    viewModel.setCustomer(
                                        name = c.name,
                                        phone = c.phone,
                                        address = c.address,
                                        dl = c.dl,
                                        gst = c.gst,
                                        doctor = c.doctor
                                    )
                                    showCustomerSearchDialog = false
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = SkyBlueLight)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Phone: ${c.phone.ifBlank { "N/A" }}  |  ${c.address.ifBlank { "Local" }}", fontSize = 11.sp, color = MutedText)
                                        if (c.dl.isNotBlank() || c.gst.isNotBlank()) {
                                            Text("DL: ${c.dl}  •  GST: ${c.gst}", fontSize = 10.sp, color = SkyBluePrimary)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            if (c.partyEntity != null) {
                                                editingSelectedParty = c.partyEntity
                                            } else if (c.doctorEntity != null) {
                                                editingSelectedDoctor = c.doctorEntity
                                            } else if (c.patientEntity != null) {
                                                editingSelectedPatient = c.patientEntity
                                            }
                                            showCustomerSearchDialog = false
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit details", tint = SkyBluePrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCustomerSearchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Customer Category Chooser Dialog (Party, Doctor, or Patient)
    if (showCustomerCategoryChooser) {
        AlertDialog(
            onDismissRequest = { showCustomerCategoryChooser = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SkyBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Customer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select customer category to add to directory:",
                        fontSize = 12.sp,
                        color = MutedText
                    )

                    // Option 1: Party (Pharmacy / Medical Store)
                    Card(
                        onClick = {
                            showCustomerCategoryChooser = false
                            showAddPartyDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = OffWhite),
                        border = BorderStroke(1.dp, SkyBlueBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = SkyBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Party / Pharmacy Store", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                                Text("Add into Party directory with DL & GST/PAN", fontSize = 11.sp, color = MutedText)
                            }
                        }
                    }

                    // Option 2: Doctor
                    Card(
                        onClick = {
                            showCustomerCategoryChooser = false
                            showAddDoctorDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = OffWhite),
                        border = BorderStroke(1.dp, SkyBlueBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = SkyBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Doctor", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                                Text("Add into Doctor directory with qualifications & clinic", fontSize = 11.sp, color = MutedText)
                            }
                        }
                    }

                    // Option 3: Patient
                    Card(
                        onClick = {
                            showCustomerCategoryChooser = false
                            showAddPatientDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = OffWhite),
                        border = BorderStroke(1.dp, SkyBlueBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = SkyBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Patient (Retail Walk-in)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                                Text("Add into Patient directory with doctor reference", fontSize = 11.sp, color = MutedText)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCustomerCategoryChooser = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Direct Add Party Dialog
    if (showAddPartyDialog) {
        AddEditPartyDialog(
            existingParty = null,
            viewModel = viewModel,
            onDismiss = { showAddPartyDialog = false },
            onSaved = { newParty ->
                viewModel.setCustomerType("PARTY")
                viewModel.setCustomer(
                    name = newParty.partyName,
                    phone = newParty.contactNumber,
                    address = newParty.address,
                    dl = newParty.dlNumber,
                    gst = newParty.gstPanNumber
                )
            }
        )
    }

    // Direct Add Doctor Dialog
    if (showAddDoctorDialog) {
        AddEditDoctorDialog(
            existingDoctor = null,
            viewModel = viewModel,
            onDismiss = { showAddDoctorDialog = false },
            onSaved = { newDoc ->
                viewModel.setCustomerType("DOCTOR")
                viewModel.setCustomer(
                    name = newDoc.doctorName,
                    phone = newDoc.phoneNumber,
                    address = newDoc.address,
                    doctor = newDoc.qualification
                )
            }
        )
    }

    // Direct Add Patient Dialog
    if (showAddPatientDialog) {
        AddEditPatientDialog(
            existingPatient = null,
            viewModel = viewModel,
            onDismiss = { showAddPatientDialog = false },
            onSaved = { newPat ->
                viewModel.setCustomerType("PATIENT")
                viewModel.setCustomer(
                    name = newPat.patientName,
                    phone = newPat.phoneNumber,
                    address = newPat.address,
                    doctor = newPat.doctorName
                )
            }
        )
    }

    // Direct Edit / Update Party Dialog
    editingSelectedParty?.let { pToEdit ->
        AddEditPartyDialog(
            existingParty = if (pToEdit.id != 0L) pToEdit else null,
            viewModel = viewModel,
            onDismiss = { editingSelectedParty = null },
            onSaved = { updatedParty ->
                viewModel.setCustomerType("PARTY")
                viewModel.setCustomer(
                    name = updatedParty.partyName,
                    phone = updatedParty.contactNumber,
                    address = updatedParty.address,
                    dl = updatedParty.dlNumber,
                    gst = updatedParty.gstPanNumber
                )
                editingSelectedParty = null
                Toast.makeText(context, "Party details updated across system & bill!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Direct Edit / Update Doctor Dialog
    editingSelectedDoctor?.let { dToEdit ->
        AddEditDoctorDialog(
            existingDoctor = if (dToEdit.id != 0L) dToEdit else null,
            viewModel = viewModel,
            onDismiss = { editingSelectedDoctor = null },
            onSaved = { updatedDoc ->
                viewModel.setCustomerType("DOCTOR")
                viewModel.setCustomer(
                    name = updatedDoc.doctorName,
                    phone = updatedDoc.phoneNumber,
                    address = updatedDoc.address,
                    doctor = updatedDoc.qualification
                )
                editingSelectedDoctor = null
                Toast.makeText(context, "Doctor details updated across system & bill!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Direct Edit / Update Patient Dialog
    editingSelectedPatient?.let { ptToEdit ->
        AddEditPatientDialog(
            existingPatient = if (ptToEdit.id != 0L) ptToEdit else null,
            viewModel = viewModel,
            onDismiss = { editingSelectedPatient = null },
            onSaved = { updatedPat ->
                viewModel.setCustomerType("PATIENT")
                viewModel.setCustomer(
                    name = updatedPat.patientName,
                    phone = updatedPat.phoneNumber,
                    address = updatedPat.address,
                    doctor = updatedPat.doctorName
                )
                editingSelectedPatient = null
                Toast.makeText(context, "Patient details updated across system & bill!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog to enter Qty, Free, Rate, Discount, GST when adding a medicine to Bill
    medicineToAdd?.let { med ->
        var addQtyStr by remember { mutableStateOf("1") }
        var addFreeStr by remember { mutableStateOf("0") }
        var addRateStr by remember { mutableStateOf(if (med.saleRate > 0) med.saleRate.toString() else med.price.toString()) }
        var addDiscStr by remember { mutableStateOf("0.0") }
        var addGstStr by remember { mutableStateOf(med.gstPercent.toString()) }

        val q = addQtyStr.toIntOrNull() ?: 1
        val f = addFreeStr.toIntOrNull() ?: 0
        val r = addRateStr.toDoubleOrNull() ?: med.price
        val d = addDiscStr.toDoubleOrNull() ?: 0.0
        val g = addGstStr.toDoubleOrNull() ?: med.gstPercent

        val baseAmt = q * r
        val discAmt = baseAmt * (d / 100.0)
        val taxableAmt = baseAmt - discAmt
        val gstAmt = taxableAmt * (g / 100.0)
        val totalAmt = taxableAmt + gstAmt

        AlertDialog(
            onDismissRequest = { medicineToAdd = null },
            title = {
                Column {
                    Text(med.productName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "TYPE: ${med.packagingType} • Batch: ${med.batchNumber} • Stock: ${med.stockQuantity}",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = addQtyStr,
                            onValueChange = { addQtyStr = it },
                            label = { Text("Quantity *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyBluePrimary,
                                unfocusedBorderColor = SkyBlueBorder
                            )
                        )
                        OutlinedTextField(
                            value = addFreeStr,
                            onValueChange = { addFreeStr = it },
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
                            value = addRateStr,
                            onValueChange = { addRateStr = it },
                            label = { Text("Sale Rate (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyBluePrimary,
                                unfocusedBorderColor = SkyBlueBorder
                            )
                        )
                        OutlinedTextField(
                            value = addDiscStr,
                            onValueChange = { addDiscStr = it },
                            label = { Text("Discount %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyBluePrimary,
                                unfocusedBorderColor = SkyBlueBorder
                            )
                        )
                    }

                    OutlinedTextField(
                        value = addGstStr,
                        onValueChange = { addGstStr = it },
                        label = { Text("GST % (SGST + CGST)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    // Calculation Preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SkyBlueLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Taxable Value:", fontSize = 11.sp, color = DarkText)
                                Text("₹${String.format(Locale.US, "%.2f", taxableAmt)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GST Amount ($g%):", fontSize = 11.sp, color = DarkText)
                                Text("+ ₹${String.format(Locale.US, "%.2f", gstAmt)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = SkyBlueBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net Total:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyBluePrimary)
                                Text("₹${String.format(Locale.US, "%.2f", totalAmt)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SkyBluePrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addMedicineToBillWithOptions(
                            medicine = med,
                            qty = q.coerceAtLeast(1),
                            freeQty = f.coerceAtLeast(0),
                            price = r,
                            discountPercent = d.coerceAtLeast(0.0),
                            gstPercent = g.coerceAtLeast(0.0)
                        )
                        medicineToAdd = null
                        medSearchQuery = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                ) {
                    Text("Add to Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { medicineToAdd = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Item Dialog
    itemToEdit?.let { (index, item) ->
        var editQtyStr by remember { mutableStateOf(item.qty.toString()) }
        var editFreeStr by remember { mutableStateOf(item.freeQty.toString()) }
        var editRateStr by remember { mutableStateOf(item.price.toString()) }
        var editDiscStr by remember { mutableStateOf(item.discountPercent.toString()) }
        var editGstStr by remember { mutableStateOf((item.sgstPercent + item.cgstPercent).toString()) }

        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("Edit ${item.productName}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editQtyStr,
                            onValueChange = { editQtyStr = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editFreeStr,
                            onValueChange = { editFreeStr = it },
                            label = { Text("Free Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editRateStr,
                            onValueChange = { editRateStr = it },
                            label = { Text("Sale Rate (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editDiscStr,
                            onValueChange = { editDiscStr = it },
                            label = { Text("Discount %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = editGstStr,
                        onValueChange = { editGstStr = it },
                        label = { Text("GST % (SGST + CGST)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = editQtyStr.toIntOrNull() ?: 1
                        val f = editFreeStr.toIntOrNull() ?: 0
                        val r = editRateStr.toDoubleOrNull() ?: item.price
                        val d = editDiscStr.toDoubleOrNull() ?: 0.0
                        val totalGst = editGstStr.toDoubleOrNull() ?: (item.sgstPercent + item.cgstPercent)
                        val halfGst = totalGst / 2.0

                        viewModel.updateDraftItem(
                            index = index,
                            newQty = q,
                            newFree = f,
                            newPrice = r,
                            newDiscount = d,
                            newSgst = halfGst,
                            newCgst = halfGst,
                            newPack = item.pack
                        )
                        itemToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // Bill Generated Success Dialog
    generatedInvoice?.let { inv ->
        val specificItemsFlow by viewModel.getItemsForInvoiceFlow(inv.invoiceNumber).collectAsState(initial = emptyList())
        val allInvoiceItems by viewModel.invoiceItems.collectAsState()
        val invItems = remember(generatedInvoiceItems, specificItemsFlow, allInvoiceItems, inv) {
            when {
                generatedInvoiceItems.isNotEmpty() -> generatedInvoiceItems
                specificItemsFlow.isNotEmpty() -> specificItemsFlow
                else -> allInvoiceItems.filter { it.invoiceNumber == inv.invoiceNumber }
            }
        }

        AlertDialog(
            onDismissRequest = { generatedInvoice = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bill Generated Successfully!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Invoice #${inv.invoiceNumber} • ${inv.dateFormatted}", fontWeight = FontWeight.SemiBold)
                    Text("Customer: ${inv.customerName}")
                    Text("Total Net: ₹${String.format(Locale.US, "%.2f", inv.netAmount)}", fontWeight = FontWeight.Bold, color = SkyBluePrimary)
                    Text("Paid: ₹${String.format(Locale.US, "%.2f", inv.paidAmount)} | Due: ₹${String.format(Locale.US, "%.2f", inv.dueAmount)}", color = if (inv.dueAmount > 0) AlertRed else SuccessGreen)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = inv
                        generatedInvoice = null
                        onViewInvoice(current)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                ) {
                    Text("View Full Bill")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {
                        if (authState.isGuest) {
                            showAdminOnlyDialog = true
                        } else {
                            val file = PdfGenerator.INSTANCE.generateInvoicePdf(context, settings, inv, invItems)
                            PrintHelper.sharePdf(context, file, "Invoice #${inv.invoiceNumber}")
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = SkyBluePrimary)
                    }

                    IconButton(onClick = {
                        if (authState.isGuest) {
                            showAdminOnlyDialog = true
                        } else {
                            val file = PdfGenerator.INSTANCE.generateInvoicePdf(context, settings, inv, invItems)
                            PrintHelper.printPdf(context, file, "Invoice_${inv.invoiceNumber}")
                        }
                    }) {
                        Icon(Icons.Default.Print, contentDescription = "Print", tint = SkyBluePrimary)
                    }

                    TextButton(onClick = { generatedInvoice = null }) {
                        Text("Done")
                    }
                }
            }
        )
    }

    if (showAdminOnlyDialog) {
        AdminOnlyDialog(onDismiss = { showAdminOnlyDialog = false })
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = if (isBold) 14.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) DarkText else MutedText
        )
        Text(
            text = value,
            fontSize = if (isBold) 14.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isBold) SkyBluePrimary else DarkText
        )
    }
}
