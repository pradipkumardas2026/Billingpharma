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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.ui.viewmodel.PharmaViewModel
import com.example.util.DateUtils
import com.example.util.PdfGenerator
import com.example.util.PrintHelper
import com.example.util.StockStatementRow
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockStatementDialog(
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val medicines by viewModel.medicines.collectAsState()

    val companies = remember(medicines) {
        listOf("ALL COMPANIES") + medicines.map { it.manufacturer.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    var selectedCompany by remember { mutableStateOf("ALL COMPANIES") }
    var companyDropdownExpanded by remember { mutableStateOf(false) }

    var fromDateStr by remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        mutableStateOf(DateUtils.formatDate(cal.timeInMillis))
    }
    var toDateStr by remember {
        mutableStateOf(DateUtils.getTodayDateString())
    }

    var statementRows by remember { mutableStateOf<List<StockStatementRow>?>(null) }
    var showAdminOnlyDialog by remember { mutableStateOf(false) }

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
                            text = "Stock & Inventory Statement",
                            fontSize = 18.sp,
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

                Spacer(modifier = Modifier.height(12.dp))

                // Filters
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SkyBlueLight),
                    border = BorderStroke(1.dp, SkyBlueBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = companyDropdownExpanded,
                            onExpandedChange = { companyDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCompany,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Company") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyDropdownExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SkyBluePrimary,
                                    unfocusedBorderColor = SkyBlueBorder
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = companyDropdownExpanded,
                                onDismissRequest = { companyDropdownExpanded = false }
                            ) {
                                companies.forEach { comp ->
                                    DropdownMenuItem(
                                        text = { Text(comp) },
                                        onClick = {
                                            selectedCompany = comp
                                            companyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = fromDateStr,
                                onValueChange = { fromDateStr = it },
                                label = { Text("From Date") },
                                placeholder = { Text("DD/MM/YYYY") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SkyBluePrimary,
                                    unfocusedBorderColor = SkyBlueBorder
                                )
                            )
                            OutlinedTextField(
                                value = toDateStr,
                                onValueChange = { toDateStr = it },
                                label = { Text("To Date") },
                                placeholder = { Text("DD/MM/YYYY") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SkyBluePrimary,
                                    unfocusedBorderColor = SkyBlueBorder
                                )
                            )
                        }

                        // Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    fromDateStr = DateUtils.getTodayDateString()
                                    toDateStr = DateUtils.getTodayDateString()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Today", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.DAY_OF_MONTH, 1)
                                    fromDateStr = DateUtils.formatDate(cal.timeInMillis)
                                    toDateStr = DateUtils.getTodayDateString()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("This Month", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    fromDateStr = "01/01/2026"
                                    toDateStr = "31/12/2026"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("All 2026", fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val fromMs = DateUtils.parseDateToMillis(fromDateStr) ?: 0L
                                val toMs = (DateUtils.parseDateToMillis(toDateStr) ?: System.currentTimeMillis()) + (24 * 60 * 60 * 1000 - 1)
                                val rows = viewModel.calculateStockStatement(selectedCompany, fromMs, toMs)
                                statementRows = rows
                                if (rows.isEmpty()) {
                                    Toast.makeText(context, "No stock data found for criteria", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("generate_stock_statement_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Statement")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar (PDF actions)
                statementRows?.let { rows ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (authState.isGuest) {
                                    showAdminOnlyDialog = true
                                } else {
                                    val file = PdfGenerator.INSTANCE.generateStockStatementPdf(context, settings, selectedCompany, fromDateStr, toDateStr, rows)
                                    PrintHelper.sharePdf(context, file, "Stock Statement $selectedCompany")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBlueSecondary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share PDF", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (authState.isGuest) {
                                    showAdminOnlyDialog = true
                                } else {
                                    val file = PdfGenerator.INSTANCE.generateStockStatementPdf(context, settings, selectedCompany, fromDateStr, toDateStr, rows)
                                    PrintHelper.printPdf(context, file, "Stock_Statement")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print PDF", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (authState.isGuest) {
                                    showAdminOnlyDialog = true
                                } else {
                                    val file = PdfGenerator.INSTANCE.generateStockStatementPdf(context, settings, selectedCompany, fromDateStr, toDateStr, rows)
                                    val targetName = "Stock_Statement_${System.currentTimeMillis()}.pdf"
                                    PrintHelper.downloadPdf(context, file, targetName)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary Stats Card
                    val totalOpening = remember(rows) { rows.sumOf { it.openingStock } }
                    val totalReceipt = remember(rows) { rows.sumOf { it.inwardQuantity } }
                    val totalIssue = remember(rows) { rows.sumOf { it.outwardQuantity } }
                    val totalClosing = remember(rows) { rows.sumOf { it.closingStock } }
                    val totalValuation = remember(rows) { rows.sumOf { it.closingValuation } }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SkyBlueContainer),
                        border = BorderStroke(1.dp, SkyBlueBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Opening", fontSize = 10.sp, color = MutedText)
                                Text("$totalOpening", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Receipt", fontSize = 10.sp, color = MutedText)
                                Text("+$totalReceipt", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Issue (Sale)", fontSize = 10.sp, color = MutedText)
                                Text("-$totalIssue", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Closing", fontSize = 10.sp, color = MutedText)
                                Text("$totalClosing", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SkyBluePrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                                    Text("Medicine / Product", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(180.dp))
                                    Text("TYPE", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                                    Text("Opening", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                                    Text("Receipt", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                                    Text("Issue", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                                    Text("Closing", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                                    Text("Value (₹)", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(85.dp), textAlign = TextAlign.End)
                                }

                                HorizontalDivider(thickness = 1.dp, color = SkyBlueBorder)

                                rows.forEachIndexed { index, row ->
                                    val rowBg = if (index % 2 == 0) PureWhite else OffWhite
                                    Row(
                                        modifier = Modifier
                                            .background(rowBg)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.width(180.dp)) {
                                            Text(row.medicineName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text(row.manufacturer, fontSize = 10.sp, color = MutedText)
                                        }
                                        Text(row.unitType, fontSize = 11.sp, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                                        Text("${row.openingStock}", fontSize = 11.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                                        Text("${row.inwardQuantity}", fontSize = 11.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End, color = SuccessGreen)
                                        Text("${row.outwardQuantity}", fontSize = 11.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End, color = AlertRed)
                                        Text("${row.closingStock}", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                                        Text(if (authState.isGuest) "••••••" else String.format("%.2f", row.closingValuation), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(85.dp), textAlign = TextAlign.End)
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = LightBorder)
                                }

                                // Total Footer Row
                                Row(
                                    modifier = Modifier
                                        .background(SkyBlueContainer)
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(250.dp))
                                    Text("$totalOpening", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                                    Text("$totalReceipt", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End, color = SuccessGreen)
                                    Text("$totalIssue", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End, color = AlertRed)
                                    Text("$totalClosing", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End, color = SkyBluePrimary)
                                    Text(if (authState.isGuest) "••••••" else String.format("%.2f", totalValuation), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(85.dp), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdminOnlyDialog) {
        AdminOnlyDialog(onDismiss = { showAdminOnlyDialog = false })
    }
}
