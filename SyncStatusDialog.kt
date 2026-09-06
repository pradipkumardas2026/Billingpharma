package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.sync.SyncInfo
import com.example.data.sync.SyncState
import com.example.ui.theme.AlertGreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.viewmodel.PharmaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusDialog(
    viewModel: PharmaViewModel,
    onDismiss: () -> Unit
) {
    val syncInfo by viewModel.syncInfo.collectAsState()
    var editConfigOpen by remember { mutableStateOf(false) }
    var projectIdInput by remember(syncInfo.projectId) { mutableStateOf(syncInfo.projectId) }
    var deviceNameInput by remember(syncInfo.deviceName) { mutableStateOf(syncInfo.deviceName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("sync_status_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (syncInfo.isOnline) SkyBluePrimary.copy(alpha = 0.15f)
                            else Color(0xFFFF9800).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            syncInfo.state == SyncState.SYNCING -> Icons.Default.CloudSync
                            syncInfo.isOnline -> Icons.Default.CloudDone
                            else -> Icons.Default.CloudOff
                        },
                        contentDescription = "Cloud Sync Status",
                        modifier = Modifier.size(36.dp),
                        tint = when {
                            syncInfo.state == SyncState.SYNCING -> SkyBluePrimary
                            syncInfo.isOnline -> AlertGreen
                            else -> Color(0xFFFF9800)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Cloud Sync & Multi-Device",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (syncInfo.isOnline) "Automatic Real-Time Sync Active" else "Offline-First Mode Active",
                    fontSize = 13.sp,
                    color = if (syncInfo.isOnline) AlertGreen else Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status Details Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        StatusRow(
                            label = "Internet Connection",
                            value = if (syncInfo.isOnline) "Connected (Online)" else "No Connection (Offline)",
                            valueColor = if (syncInfo.isOnline) AlertGreen else Color(0xFFFF9800)
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        StatusRow(
                            label = "Sync State",
                            value = when (syncInfo.state) {
                                SyncState.SYNCING -> "Synchronizing now..."
                                SyncState.SYNCED -> "All data synchronized"
                                SyncState.FAILED -> "Sync error (retrying)"
                                SyncState.OFFLINE -> "Offline (saved locally)"
                            },
                            valueColor = when (syncInfo.state) {
                                SyncState.SYNCED -> AlertGreen
                                SyncState.SYNCING -> SkyBluePrimary
                                SyncState.FAILED -> AlertRed
                                SyncState.OFFLINE -> Color(0xFFFF9800)
                            }
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        StatusRow(
                            label = "Pending Local Changes",
                            value = if (syncInfo.pendingCount == 0) "0 (Up to date)" else "${syncInfo.pendingCount} queued for cloud",
                            valueColor = if (syncInfo.pendingCount == 0) AlertGreen else Color(0xFFFF9800)
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        StatusRow(
                            label = "Last Cloud Sync",
                            value = syncInfo.lastSyncFormatted,
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        StatusRow(
                            label = "Device Name",
                            value = syncInfo.deviceName,
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        StatusRow(
                            label = "Firebase Project",
                            value = syncInfo.projectId,
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!syncInfo.errorMessage.isNullOrBlank()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            StatusRow(
                                label = "Error Detail",
                                value = syncInfo.errorMessage ?: "",
                                valueColor = AlertRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Information banner
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SkyBluePrimary.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = SkyBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "You can add, edit, and delete medicines, bills, and customers completely offline. As soon as you are back online, everything syncs automatically to all your devices without pressing any button.",
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Configuration Toggle
                AnimatedVisibility(visible = editConfigOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        OutlinedTextField(
                            value = deviceNameInput,
                            onValueChange = { deviceNameInput = it },
                            label = { Text("Device Name (e.g. Counter 1, Shop Tablet)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("device_name_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = projectIdInput,
                            onValueChange = { projectIdInput = it },
                            label = { Text("Firebase Cloud Project ID") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("firebase_project_id_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    projectIdInput = "pharma-billing-cloud"
                                    viewModel.updateFirebaseProjectId("pharma-billing-cloud")
                                }
                            ) {
                                Text("Reset Default")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    viewModel.updateDeviceName(deviceNameInput)
                                    viewModel.updateFirebaseProjectId(projectIdInput)
                                    editConfigOpen = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                            ) {
                                Text("Save Settings")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { editConfigOpen = !editConfigOpen },
                        modifier = Modifier.testTag("toggle_sync_config_btn")
                    ) {
                        Icon(
                            imageVector = if (editConfigOpen) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = "Configure",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editConfigOpen) "Hide Settings" else "Cloud Settings", fontSize = 12.sp)
                    }

                    Row {
                        if (syncInfo.isOnline) {
                            OutlinedButton(
                                onClick = { viewModel.triggerManualSync() },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("test_sync_now_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                            modifier = Modifier.testTag("close_sync_dialog_btn")
                        ) {
                            Text("Close", color = PureWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
