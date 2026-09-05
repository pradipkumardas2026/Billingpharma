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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.DoctorEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PatientEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel

@Composable
fun CustomerManageDialog(
    initialTab: String = "PARTY",
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val parties by viewModel.parties.collectAsState()
    val doctors by viewModel.doctors.collectAsState()
    val patients by viewModel.patients.collectAsState()

    var selectedTab by remember {
        mutableIntStateOf(when (initialTab) {
            "DOCTOR" -> 1
            "PATIENT" -> 2
            else -> 0
        })
    }

    var showAddPartyDialog by remember { mutableStateOf(false) }
    var partyToEdit by remember { mutableStateOf<PartyEntity?>(null) }

    var showAddDoctorDialog by remember { mutableStateOf(false) }
    var doctorToEdit by remember { mutableStateOf<DoctorEntity?>(null) }

    var showAddPatientDialog by remember { mutableStateOf(false) }
    var patientToEdit by remember { mutableStateOf<PatientEntity?>(null) }
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    val tabs = listOf("PARTIES", "DOCTORS", "PATIENTS")

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
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Customer & Directory Management", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PureWhite,
                    contentColor = SkyBluePrimary
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    0 -> {
                        // Parties
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${parties.size} Registered Pharmacies / Parties", fontSize = 13.sp, color = MutedText)
                            Button(
                                onClick = {
                                    if (authState.isGuest) {
                                        showAdminOnlyDialog = true
                                    } else {
                                        showAddPartyDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Party", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(parties, key = { it.id }) { party ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = OffWhite),
                                    border = BorderStroke(1.dp, LightBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(party.partyName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (party.contactNumber.isNotEmpty()) Text("Phone: ${party.contactNumber}", fontSize = 11.sp, color = MutedText)
                                            if (party.address.isNotEmpty()) Text("Address: ${party.address}", fontSize = 11.sp, color = MutedText)
                                            if (party.dlNumber.isNotEmpty()) Text("D.L.: ${party.dlNumber} | GST: ${party.gstPanNumber}", fontSize = 11.sp, color = MutedText)
                                        }

                                        Row {
                                            IconButton(onClick = {
                                                if (authState.isGuest) {
                                                    showAdminOnlyDialog = true
                                                } else {
                                                    partyToEdit = party
                                                }
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = {
                                                if (authState.isGuest) {
                                                    showAdminOnlyDialog = true
                                                } else {
                                                    viewModel.deleteParty(party.id)
                                                    Toast.makeText(context, "Deleted ${party.partyName}", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Doctors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${doctors.size} Registered Doctors", fontSize = 13.sp, color = MutedText)
                            Button(
                                onClick = {
                                    if (authState.isGuest) {
                                        showAdminOnlyDialog = true
                                    } else {
                                        showAddDoctorDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Doctor", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(doctors, key = { it.id }) { doc ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = OffWhite),
                                    border = BorderStroke(1.dp, LightBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.doctorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (doc.qualification.isNotEmpty()) Text("Degree: ${doc.qualification}", fontSize = 11.sp, color = MutedText)
                                            if (doc.phoneNumber.isNotEmpty()) Text("Phone: ${doc.phoneNumber}", fontSize = 11.sp, color = MutedText)
                                            if (doc.address.isNotEmpty()) Text("Clinic: ${doc.address}", fontSize = 11.sp, color = MutedText)
                                        }

                                        Row {
                                            IconButton(onClick = {
                                                if (authState.isGuest) {
                                                    showAdminOnlyDialog = true
                                                } else {
                                                    doctorToEdit = doc
                                                }
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = {
                                                if (authState.isGuest) {
                                                    showAdminOnlyDialog = true
                                                } else {
                                                    viewModel.deleteDoctor(doc.id)
                                                    Toast.makeText(context, "Deleted ${doc.doctorName}", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Patients
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${patients.size} Registered Patients", fontSize = 13.sp, color = MutedText)
                            Button(
                                onClick = {
                                    if (authState.isGuest) {
                                        showAdminOnlyDialog = true
                                    } else {
                                        showAddPatientDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Patient", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(patients, key = { it.id }) { pat ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = OffWhite),
                                    border = BorderStroke(1.dp, LightBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(pat.patientName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (pat.phoneNumber.isNotEmpty()) Text("Phone: ${pat.phoneNumber}", fontSize = 11.sp, color = MutedText)
                                            if (pat.address.isNotEmpty()) Text("Address: ${pat.address}", fontSize = 11.sp, color = MutedText)
                                            if (pat.doctorName.isNotEmpty()) Text("Doctor: ${pat.doctorName}", fontSize = 11.sp, color = MutedText)
                                        }

                                        Row {
                                            IconButton(onClick = {
                                                if (authState.isGuest) {
                                                    showAdminOnlyDialog = true
                                                } else {
                                                    patientToEdit = pat
                                                }
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = {
                                                if (authState.isGuest) {
                                                    showAdminOnlyDialog = true
                                                } else {
                                                    viewModel.deletePatient(pat.id)
                                                    Toast.makeText(context, "Deleted ${pat.patientName}", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPartyDialog || partyToEdit != null) {
        AddEditPartyDialog(
            existingParty = partyToEdit,
            viewModel = viewModel,
            onDismiss = {
                showAddPartyDialog = false
                partyToEdit = null
            }
        )
    }

    if (showAddDoctorDialog || doctorToEdit != null) {
        AddEditDoctorDialog(
            existingDoctor = doctorToEdit,
            viewModel = viewModel,
            onDismiss = {
                showAddDoctorDialog = false
                doctorToEdit = null
            }
        )
    }

    if (showAddPatientDialog || patientToEdit != null) {
        AddEditPatientDialog(
            existingPatient = patientToEdit,
            viewModel = viewModel,
            onDismiss = {
                showAddPatientDialog = false
                patientToEdit = null
            }
        )
    }

    if (showAdminOnlyDialog) {
        AdminOnlyDialog(onDismiss = { showAdminOnlyDialog = false })
    }
}
