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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AdminUserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel
import com.example.ui.viewmodel.UserRole

@Composable
fun SettingsScreen(
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val adminUsers by viewModel.adminUsers.collectAsState()
    val syncInfo by viewModel.syncInfo.collectAsState()

    val isAdmin = authState.isAdmin
    val isMasterAdmin = authState.isMasterAdmin

    var showSyncDetailsDialog by remember { mutableStateOf(false) }

    var businessName by remember(settings) { mutableStateOf(settings.businessName) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var gstNumber by remember(settings) { mutableStateOf(settings.gstNumber) }
    var dlNumber by remember(settings) { mutableStateOf(settings.dlNumber) }
    var contactNumber by remember(settings) { mutableStateOf(settings.contactNumber) }
    var adminPassword by remember(settings) { mutableStateOf(settings.adminPassword) }
    var adminMobile by remember(settings) { mutableStateOf(settings.adminMobile.ifBlank { "9002625428" }) }
    var showMasterPasswordVisible by remember { mutableStateOf(false) }

    // Sub-Admin creation dialog
    var showCreateAdminDialog by remember { mutableStateOf(false) }
    var newAdminName by remember { mutableStateOf("") }
    var newAdminMobile by remember { mutableStateOf("") }
    var newAdminPassword by remember { mutableStateOf("") }

    // Sub-Admin edit dialog
    var adminToEdit by remember { mutableStateOf<AdminUserEntity?>(null) }
    var editAdminName by remember { mutableStateOf("") }
    var editAdminMobile by remember { mutableStateOf("") }
    var editAdminPassword by remember { mutableStateOf("") }

    // Sub-Admin delete dialog
    var adminToDelete by remember { mutableStateOf<AdminUserEntity?>(null) }
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isMasterAdmin) "Master Admin Settings" else "Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = DarkText
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Settings",
                    tint = MutedText
                )
            }
        }

        // If Guest User: Only show message "This is only for Master Admin Panel"
        if (authState.isGuest) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = AlertRedLight,
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "This is only for Master Admin Panel",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Guest users have view-only access across the app. To view or configure system settings, please log in as Master Admin.",
                        fontSize = 13.sp,
                        color = MutedText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("BACK TO DASHBOARD", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (!isMasterAdmin) {
            // Sub-Admin (Other Admin):
            // "others admin settings a gele shudhu enterprises er nam ar oi admin mobile number ar nam show hobe. Anyo kichhu show hobe na"
            // Cannot edit/update anything!
            val adminRole = authState.role as? UserRole.Admin

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = SkyBluePrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enterprise Information",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SkyBlueLight, RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "ENTERPRISE NAME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SkyBlueSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = settings.businessName.ifBlank { "Pharmacy Enterprise" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = SkyBluePrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Profile Info",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OffWhite, RoundedCornerShape(8.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "ADMIN NAME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = adminRole?.adminName ?: "Sub-Admin",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkText
                            )
                        }

                        Divider(color = LightBorder)

                        Column {
                            Text(
                                text = "REGISTERED MOBILE NUMBER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = adminRole?.mobile ?: "",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkText
                            )
                        }
                    }

                    Surface(
                        color = SkyBlueLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SkyBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Profile & enterprise settings can only be edited and updated by the Master Admin.",
                                fontSize = 12.sp,
                                color = SkyBlueSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Master Admin ONLY: Can edit profile, tax details, address, master credentials, and manage sub-admins
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = SkyBluePrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Business Profile & Invoice Header",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business / Enterprise Name") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_business_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Location") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_address"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = gstNumber,
                        onValueChange = { gstNumber = it },
                        label = { Text("GSTIN / PAN") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_gstin"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = dlNumber,
                        onValueChange = { dlNumber = it },
                        label = { Text("Drug License (D.L.) Number") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_dl_number"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = { contactNumber = it },
                        label = { Text("Contact Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("settings_phone"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SkyBluePrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Master Admin Security",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }

                    Text(
                        text = "Master Admin Credentials (Editable ONLY by Master Admin)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SkyBlueSecondary
                    )

                    OutlinedTextField(
                        value = adminMobile,
                        onValueChange = { adminMobile = it },
                        label = { Text("Master Admin Mobile Number") },
                        placeholder = { Text("Enter 10-digit mobile number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("settings_admin_mobile"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = { Text("Master Admin Password") },
                        placeholder = { Text("Enter master password") },
                        visualTransformation = if (showMasterPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showMasterPasswordVisible = !showMasterPasswordVisible }) {
                                Icon(
                                    imageVector = if (showMasterPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = MutedText
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_admin_password"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )
                }
            }

            // Sub-Admins Management (Only visible and manageable by Master Admin)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, SkyBlueBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SupervisorAccount,
                                contentDescription = null,
                                tint = SkyBluePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sub-Admins Management",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                        }

                        Surface(
                            color = if (adminUsers.size >= 10) AlertRed.copy(alpha = 0.15f) else SkyBlueLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${adminUsers.size} / 10 Admins",
                                color = if (adminUsers.size >= 10) AlertRed else SkyBluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Master Admin can create up to 10 Sub-Admins. Each Admin can login on up to 5 devices with synchronized database updates. Sub-admins have full billing & inventory access but cannot modify mobile or password credentials.",
                        fontSize = 12.sp,
                        color = MutedText,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = {
                            newAdminName = ""
                            newAdminMobile = ""
                            newAdminPassword = ""
                            showCreateAdminDialog = true
                        },
                        enabled = adminUsers.size < 10,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (adminUsers.size < 10) "+ CREATE NEW ADMIN" else "MAXIMUM 10 ADMINS REACHED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (adminUsers.isNotEmpty()) {
                        Divider(color = SkyBlueBorder.copy(alpha = 0.6f))
                        Text(
                            text = "Active Sub-Admins List",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = DarkText
                        )

                        adminUsers.forEach { admin ->
                            var showPass by remember { mutableStateOf(false) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = OffWhite),
                                border = BorderStroke(1.dp, SkyBlueBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = admin.name.ifBlank { "Admin" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = DarkText
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Mobile: ${admin.mobileNumber}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SkyBlueSecondary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Password: " + if (showPass) admin.password else "••••••",
                                                fontSize = 12.sp,
                                                color = MutedText
                                            )
                                            IconButton(
                                                onClick = { showPass = !showPass },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (showPass) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "Show password",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MutedText
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                adminToEdit = admin
                                                editAdminName = admin.name
                                                editAdminMobile = admin.mobileNumber
                                                editAdminPassword = admin.password
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Admin",
                                                tint = SkyBluePrimary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                adminToDelete = admin
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Admin",
                                                tint = AlertRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Cloud Synchronization Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, SkyBlueBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = SkyBluePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cloud Sync & Multi-Device",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (syncInfo.isOnline) AlertGreen.copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (syncInfo.isOnline) "ONLINE" else "OFFLINE",
                                color = if (syncInfo.isOnline) AlertGreen else Color(0xFFFF9800),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Real-time automatic cloud synchronization is active. All additions, edits, and deletions work completely offline, and automatically sync to all devices when internet is available.",
                        fontSize = 12.sp,
                        color = MutedText,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pending Local Changes: ${syncInfo.pendingCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (syncInfo.pendingCount == 0) AlertGreen else Color(0xFFFF9800)
                        )

                        OutlinedButton(
                            onClick = { showSyncDetailsDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Details", fontSize = 12.sp)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val m = adminMobile.trim()
                    val p = adminPassword.trim()
                    if (m.length < 10) {
                        Toast.makeText(context, "Please enter a valid 10-digit Master Admin mobile number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (p.length < 4) {
                        Toast.makeText(context, "Master Admin password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val updated = settings.copy(
                        businessName = businessName.trim(),
                        address = address.trim(),
                        gstNumber = gstNumber.trim(),
                        dlNumber = dlNumber.trim(),
                        contactNumber = contactNumber.trim(),
                        adminMobile = adminMobile.trim(),
                        adminPassword = adminPassword.trim()
                    )
                    viewModel.updateSettings(updated)
                    Toast.makeText(context, "Settings & Master Admin Credentials Saved Successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_save_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAVE PROFILE & SETTINGS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }

    if (showAdminOnlyDialog) {
        AdminOnlyDialog(onDismiss = { showAdminOnlyDialog = false })
    }

    // Dialog: Create New Sub-Admin
    if (showCreateAdminDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAdminDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SkyBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Sub-Admin", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Create an Admin login for a secondary staff or device. They can log in on up to 5 devices.",
                        fontSize = 12.sp,
                        color = MutedText
                    )

                    OutlinedTextField(
                        value = newAdminName,
                        onValueChange = { newAdminName = it },
                        label = { Text("Admin / Staff Name") },
                        placeholder = { Text("e.g. Rahul Sharma") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = newAdminMobile,
                        onValueChange = { newAdminMobile = it },
                        label = { Text("Admin Mobile Number (10 digits)") },
                        placeholder = { Text("Enter mobile number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = newAdminPassword,
                        onValueChange = { newAdminPassword = it },
                        label = { Text("Admin Password") },
                        placeholder = { Text("Enter login password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                        viewModel.createSubAdmin(
                            name = newAdminName,
                            mobile = newAdminMobile,
                            pass = newAdminPassword
                        ) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) {
                                showCreateAdminDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                ) {
                    Text("CREATE ADMIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAdminDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Edit Sub-Admin (Only Master Admin can update mobile / password)
    adminToEdit?.let { admin ->
        AlertDialog(
            onDismissRequest = { adminToEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = SkyBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Admin Credentials", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Update the login credentials for this Admin. After updating, they will log in with these new credentials.",
                        fontSize = 12.sp,
                        color = MutedText
                    )

                    OutlinedTextField(
                        value = editAdminName,
                        onValueChange = { editAdminName = it },
                        label = { Text("Admin Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = editAdminMobile,
                        onValueChange = { editAdminMobile = it },
                        label = { Text("Mobile Number (Login ID)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = editAdminPassword,
                        onValueChange = { editAdminPassword = it },
                        label = { Text("New Password") },
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
                        val updated = admin.copy(
                            name = editAdminName.trim(),
                            mobileNumber = editAdminMobile.trim(),
                            password = editAdminPassword.trim()
                        )
                        viewModel.updateSubAdmin(updated) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                adminToEdit = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                ) {
                    Text("UPDATE CREDENTIALS")
                }
            },
            dismissButton = {
                TextButton(onClick = { adminToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Delete Sub-Admin Confirmation
    adminToDelete?.let { admin ->
        AlertDialog(
            onDismissRequest = { adminToDelete = null },
            title = { Text("Delete Sub-Admin Account?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete ${admin.name.ifBlank { "Admin" }} (${admin.mobileNumber})? They will no longer be able to log in.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubAdmin(admin.id) {
                            Toast.makeText(context, "Admin deleted successfully", Toast.LENGTH_SHORT).show()
                            adminToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { adminToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSyncDetailsDialog) {
        SyncStatusDialog(
            viewModel = viewModel,
            onDismiss = { showSyncDetailsDialog = false }
        )
    }
}
