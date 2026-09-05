package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel

@Composable
fun LoginScreen(
    viewModel: PharmaViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Admin, 1: Guest
    var adminMobile by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var guestMobile by remember { mutableStateOf("") }

    var showForgotDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBlueLight)
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(SkyBlueContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalPharmacy,
                        contentDescription = "Logo",
                        modifier = Modifier.size(36.dp),
                        tint = SkyBluePrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PharmaBill",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyBluePrimary
                    )
                    Text(
                        text = settings.businessName.ifEmpty { "Pharmacy Billing & Inventory System" },
                        fontSize = 13.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SkyBlueLight,
                    contentColor = SkyBluePrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ADMIN USER", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GUEST USER", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    )
                }

                if (selectedTab == 0) {
                    // Admin Section
                    OutlinedTextField(
                        value = adminMobile,
                        onValueChange = { adminMobile = it },
                        label = { Text("Admin Mobile Number") },
                        placeholder = { Text("Enter mobile number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("admin_mobile_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = { Text("Admin Password") },
                        placeholder = { Text("Enter password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                showForgotDialog = true
                            }
                        ) {
                            Text("Forgot Password?", color = SkyBluePrimary, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val success = viewModel.loginAdmin(adminMobile, adminPassword)
                            if (!success) {
                                Toast.makeText(context, "Invalid admin mobile or password!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("login_admin_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                    ) {
                        Text("LOGIN AS ADMIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    // Guest Section
                    OutlinedTextField(
                        value = guestMobile,
                        onValueChange = {
                            val digitsOnly = it.filter { ch -> ch.isDigit() }
                            if (digitsOnly.length <= 10) {
                                guestMobile = digitsOnly
                            }
                        },
                        label = { Text("Your Mobile Number (10 digits)") },
                        placeholder = { Text("Enter 10-digit mobile number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("guest_mobile_field"),
                        supportingText = {
                            if (guestMobile.isNotEmpty() && guestMobile.length != 10) {
                                Text("${guestMobile.length}/10 digits entered", color = AlertRed)
                            } else {
                                Text("Mandatory 10-digit mobile number to enter", color = MutedText)
                            }
                        },
                        isError = guestMobile.isNotEmpty() && guestMobile.length != 10,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    Button(
                        onClick = {
                            val cleanDigits = guestMobile.trim().filter { it.isDigit() }
                            if (cleanDigits.length != 10) {
                                Toast.makeText(
                                    context,
                                    "Guest login requires a valid 10-digit mobile number!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val success = viewModel.loginGuest(cleanDigits)
                                if (!success) {
                                    Toast.makeText(
                                        context,
                                        "Please enter a valid 10-digit mobile number!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("login_guest_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBlueSecondary)
                    ) {
                        Text("ENTER AS GUEST", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotDialog = false
            },
            icon = {
                Surface(
                    color = SkyBlueLight,
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = SkyBluePrimary,
                        modifier = Modifier.padding(12.dp).size(36.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Password Recovery",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Contact your Master Admin",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "For security reasons, OTP generation is disabled. Please reach out to your Master Admin to retrieve or reset your login credentials.",
                        fontSize = 13.sp,
                        color = MutedText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showForgotDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
