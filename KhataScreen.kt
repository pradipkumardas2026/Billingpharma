package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.DateUtils
import com.example.util.PdfGenerator
import com.example.util.PrintHelper

@Composable
fun KhataScreen(
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val khataRows by remember { derivedStateOf { viewModel.getKhataRows() } }

    var searchQuery by remember { mutableStateOf("") }
    var invoiceToDelete by remember { mutableStateOf<Long?>(null) }
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

    val filteredRows = remember(khataRows, searchQuery) {
        if (searchQuery.isBlank()) khataRows
        else khataRows.filter {
            it.partyName.contains(searchQuery, ignoreCase = true) ||
            it.productName.contains(searchQuery, ignoreCase = true) ||
            it.invoiceNumber.toString().contains(searchQuery) ||
            it.note.contains(searchQuery, ignoreCase = true)
        }
    }

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
                    Column {
                        Text(
                            text = "KHATA REGISTER / SALES BOOK (খাতা)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SkyBluePrimary
                        )
                        Text(
                            text = settings.businessName,
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions & Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Khata by party, medicine, bill #...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBluePrimary,
                            unfocusedBorderColor = SkyBlueBorder
                        )
                    )

                    IconButton(
                        onClick = {
                            if (authState.isGuest) {
                                showAdminOnlyDialog = true
                            } else {
                                val file = PdfGenerator.INSTANCE.generateKhataPdf(context, settings, filteredRows)
                                PrintHelper.sharePdf(context, file, "Khata Register")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = SkyBluePrimary)
                    }

                    IconButton(
                        onClick = {
                            if (authState.isGuest) {
                                showAdminOnlyDialog = true
                            } else {
                                val file = PdfGenerator.INSTANCE.generateKhataPdf(context, settings, filteredRows)
                                PrintHelper.printPdf(context, file, "Khata_Register")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print PDF", tint = SkyBluePrimary)
                    }

                    IconButton(
                        onClick = {
                            if (authState.isGuest) {
                                showAdminOnlyDialog = true
                            } else {
                                val file = PdfGenerator.INSTANCE.generateKhataPdf(context, settings, filteredRows)
                                val targetName = "Khata_${System.currentTimeMillis()}.pdf"
                                PrintHelper.downloadPdf(context, file, targetName)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = SuccessGreen)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Table
                val horizontalScrollState = rememberScrollState()
                val verticalScrollState = rememberScrollState()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                            .verticalScroll(verticalScrollState)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .background(SkyBlueContainer)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bill #", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                                Text("Date", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                                Text("Party / Customer", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(160.dp))
                                Text("Product Name", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(220.dp))
                                Text("Qty", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                Text("Free", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                Text("Bill Amt (₹)", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                                Text("Note / Description", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(160.dp))
                                if (authState.isAdmin) {
                                    Text("Action", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                }
                            }

                            HorizontalDivider(thickness = 1.dp, color = SkyBlueBorder)

                            filteredRows.forEachIndexed { index, row ->
                                val rowBg = if (index % 2 == 0) PureWhite else OffWhite
                                Row(
                                    modifier = Modifier
                                        .background(rowBg)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (row.isFirstItemOfInvoice) "#${row.invoiceNumber}" else "", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                                    Text(if (row.isFirstItemOfInvoice) row.date else "", fontSize = 11.sp, modifier = Modifier.width(90.dp))
                                    Text(if (row.isFirstItemOfInvoice) row.partyName else "", fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(160.dp))
                                    Text(row.productName, fontSize = 11.sp, modifier = Modifier.width(220.dp))
                                    Text("${row.quantity}", fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                    Text("${row.free}", fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                    Text(
                                        if (row.isFirstItemOfInvoice) {
                                            if (authState.isGuest) "••••••" else String.format(java.util.Locale.US, "%.2f", row.billAmount)
                                        } else "",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(90.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(if (row.isFirstItemOfInvoice && row.note.isNotBlank()) row.note else "", fontSize = 11.sp, color = MutedText, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(160.dp).padding(horizontal = 4.dp))
                                    if (authState.isAdmin) {
                                        Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.Center) {
                                            if (row.isFirstItemOfInvoice) {
                                                IconButton(
                                                    onClick = {
                                                        invoiceToDelete = row.invoiceNumber
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Bill", tint = AlertRed, modifier = Modifier.size(18.dp))
                                                }
                                            }
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
    }

    invoiceToDelete?.let { invNum ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = { Text("Delete Bill #$invNum", fontWeight = FontWeight.Bold, color = AlertRed) },
            text = {
                Text("Deleting this invoice from Khata will:\n1. Completely reverse all sold quantities back into inventory stock.\n2. Reset the invoice sequence so next bill takes this vacated number.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(invNum)
                        invoiceToDelete = null
                        Toast.makeText(context, "Invoice #$invNum deleted and stock reversed!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdminOnlyDialog) {
        AdminOnlyDialog(onDismiss = { showAdminOnlyDialog = false })
    }
}
