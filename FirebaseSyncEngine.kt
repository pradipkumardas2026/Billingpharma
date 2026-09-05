package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.dao.PharmaDao
import com.example.data.local.entity.*
import com.example.util.NetworkMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

enum class SyncState {
    OFFLINE,
    SYNCING,
    SYNCED,
    FAILED
}

data class SyncInfo(
    val state: SyncState = SyncState.OFFLINE,
    val isOnline: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncFormatted: String = "Never",
    val errorMessage: String? = null,
    val deviceId: String = "",
    val deviceName: String = "This Device",
    val projectId: String = "pharmabill-cloud-sync"
)

class FirebaseSyncEngine(
    private val context: Context,
    private val dao: PharmaDao,
    private val networkMonitor: NetworkMonitor
) {
    private val prefs = context.getSharedPreferences("pharma_sync_prefs", Context.MODE_PRIVATE)

    private val _syncInfo = MutableStateFlow(loadInitialSyncInfo())
    val syncInfo: StateFlow<SyncInfo> = _syncInfo.asStateFlow()

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncMutex = Mutex()

    init {
        // Observe network changes
        syncScope.launch {
            networkMonitor.isOnline.collect { online ->
                _syncInfo.value = _syncInfo.value.copy(
                    isOnline = online,
                    state = if (!online) SyncState.OFFLINE else if (_syncInfo.value.pendingCount > 0) SyncState.SYNCING else SyncState.SYNCED
                )
                if (online) {
                    triggerAutomaticSync()
                }
            }
        }

        // Periodically poll for changes from other devices when online (every 25 seconds)
        syncScope.launch {
            while (isActive) {
                delay(25000L)
                if (networkMonitor.isOnline.value) {
                    triggerAutomaticSync()
                }
            }
        }
    }

    private fun loadInitialSyncInfo(): SyncInfo {
        var devId = prefs.getString("device_id", null)
        if (devId.isNullOrBlank()) {
            devId = "device_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString("device_id", devId).apply()
        }
        val devName = prefs.getString("device_name", "Device " + devId.takeLast(4)) ?: "This Device"
        val projId = prefs.getString("firebase_project_id", "pharmabill-cloud-sync") ?: "pharmabill-cloud-sync"
        val lastSync = prefs.getLong("last_sync_time", 0L)
        val formatted = if (lastSync > 0) formatTimestamp(lastSync) else "Never"

        return SyncInfo(
            state = if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE,
            isOnline = networkMonitor.isOnline.value,
            pendingCount = 0,
            lastSyncTimestamp = lastSync,
            lastSyncFormatted = formatted,
            deviceId = devId,
            deviceName = devName,
            projectId = projId
        )
    }

    fun getDeviceId(): String = _syncInfo.value.deviceId

    fun setFirebaseProjectId(newProjectId: String) = updateFirebaseProjectId(newProjectId)

    fun updateFirebaseProjectId(newProjectId: String) {
        val trimmed = newProjectId.trim().ifEmpty { "pharmabill-cloud-sync" }
        prefs.edit().putString("firebase_project_id", trimmed).apply()
        _syncInfo.value = _syncInfo.value.copy(projectId = trimmed)
        triggerAutomaticSync()
    }

    fun setDeviceName(deviceName: String) = updateDeviceName(deviceName)

    fun updateDeviceName(deviceName: String) {
        val trimmed = deviceName.trim().ifEmpty { "Device_" + UUID.randomUUID().toString().take(6) }
        prefs.edit().putString("device_name", trimmed).apply()
        _syncInfo.value = _syncInfo.value.copy(deviceName = trimmed)
    }

    fun cleanup() {
        try {
            networkMonitor.stopMonitoring()
            syncScope.cancel()
        } catch (_: Exception) {}
    }

    fun triggerAutomaticSync() {
        if (!networkMonitor.isOnline.value) {
            _syncInfo.value = _syncInfo.value.copy(state = SyncState.OFFLINE, isOnline = false)
            return
        }

        syncScope.launch {
            performFullSync()
        }
    }

    private suspend fun performFullSync() {
        if (!syncMutex.tryLock()) return

        try {
            val pendingOps = dao.getPendingSyncOperations()
            _syncInfo.value = _syncInfo.value.copy(
                state = SyncState.SYNCING,
                pendingCount = pendingOps.size,
                errorMessage = null
            )

            // Step 1: Upload pending local operations to Cloud
            var anyUploadFailed = false
            for (op in pendingOps) {
                val success = uploadOperationToFirebase(op)
                if (success) {
                    dao.deleteSyncOperation(op.id)
                } else {
                    anyUploadFailed = true
                    dao.updateSyncStatus(op.id, "FAILED", "Sync failed at ${Date()}", op.retryCount + 1)
                }
            }

            // Step 2: Download remote changes from other devices (Multi-Device Auto Sync)
            pullRemoteChanges()

            val remainingPending = dao.getPendingSyncOperations().size
            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_time", now).apply()

            _syncInfo.value = _syncInfo.value.copy(
                state = if (anyUploadFailed) SyncState.FAILED else SyncState.SYNCED,
                pendingCount = remainingPending,
                lastSyncTimestamp = now,
                lastSyncFormatted = formatTimestamp(now),
                errorMessage = if (anyUploadFailed) "Some changes could not sync. Automatic retry scheduled." else null
            )
        } catch (e: Exception) {
            Log.e("FirebaseSyncEngine", "Sync error", e)
            val pendingCount = dao.getPendingSyncOperations().size
            _syncInfo.value = _syncInfo.value.copy(
                state = SyncState.FAILED,
                pendingCount = pendingCount,
                errorMessage = e.localizedMessage ?: "Sync error occurred"
            )
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun uploadOperationToFirebase(op: SyncOperationEntity): Boolean = withContext(Dispatchers.IO) {
        val projectId = _syncInfo.value.projectId
        val collectionName = getCollectionForEntityType(op.entityType)
        val documentId = "${op.entityType.lowercase()}_${op.entityId}"
        val urlString = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collectionName/$documentId"

        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true

            val fieldsObject = JSONObject()
            fieldsObject.put("entityType", makeStringField(op.entityType))
            fieldsObject.put("entityId", makeStringField(op.entityId))
            fieldsObject.put("operation", makeStringField(op.operation))
            fieldsObject.put("payloadJson", makeStringField(op.payloadJson))
            fieldsObject.put("timestamp", makeIntegerField(op.timestamp))
            fieldsObject.put("deviceId", makeStringField(op.deviceId.ifEmpty { getDeviceId() }))
            fieldsObject.put("isDeleted", makeBooleanField(op.operation == "DELETE"))

            val body = JSONObject()
            body.put("fields", fieldsObject)

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            // 200 OK or 201 Created
            if (responseCode in 200..299) {
                conn.disconnect()
                return@withContext true
            } else {
                Log.w("FirebaseSyncEngine", "Firebase HTTP $responseCode for $documentId")
                conn.disconnect()
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("FirebaseSyncEngine", "Network failure uploading $documentId: ${e.message}")
            return@withContext false
        }
    }

    private suspend fun pullRemoteChanges() = withContext(Dispatchers.IO) {
        val projectId = _syncInfo.value.projectId
        val collections = listOf(
            "pharma_medicines",
            "pharma_parties",
            "pharma_doctors",
            "pharma_patients",
            "pharma_invoices",
            "pharma_settings",
            "pharma_guest_logins",
            "pharma_admin_users"
        )
        val currentDeviceId = getDeviceId()

        for (coll in collections) {
            val urlString = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$coll"
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                if (conn.responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseStr = reader.readText()
                    reader.close()
                    conn.disconnect()

                    val json = JSONObject(responseStr)
                    val documents = json.optJSONArray("documents") ?: JSONArray()
                    for (i in 0 until documents.length()) {
                        val doc = documents.getJSONObject(i)
                        val fields = doc.optJSONObject("fields") ?: continue
                        val docDeviceId = getStringField(fields, "deviceId")

                        // Only apply changes if they originated from OTHER devices (Multi-device sync)
                        if (docDeviceId != currentDeviceId && docDeviceId.isNotEmpty()) {
                            val entityType = getStringField(fields, "entityType")
                            val payloadJson = getStringField(fields, "payloadJson")
                            val isDeleted = getBooleanField(fields, "isDeleted")
                            val remoteTimestamp = getIntegerField(fields, "timestamp")

                            applyRemoteChange(entityType, payloadJson, isDeleted, remoteTimestamp)
                        }
                    }
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.w("FirebaseSyncEngine", "Error pulling collection $coll: ${e.message}")
            }
        }
    }

    private suspend fun applyRemoteChange(
        entityType: String,
        payloadJson: String,
        isDeleted: Boolean,
        remoteTimestamp: Long
    ) {
        if (payloadJson.isBlank()) return
        try {
            val json = JSONObject(payloadJson)
            when (entityType) {
                "MEDICINE" -> {
                    val id = json.optLong("id", 0L)
                    if (id > 0) {
                        val local = dao.getMedicineById(id)
                        if (isDeleted) {
                            dao.deleteMedicineById(id)
                        } else {
                            if (local == null || remoteTimestamp >= local.createdAt) {
                                val med = MedicineEntity(
                                    id = id,
                                    productName = json.optString("productName", ""),
                                    productType = json.optString("productType", "Tablet"),
                                    packagingType = json.optString("packagingType", "1×10T"),
                                    composition = json.optString("composition", ""),
                                    manufacturerName = json.optString("manufacturerName", ""),
                                    companyName = json.optString("companyName", ""),
                                    batchNumber = json.optString("batchNumber", ""),
                                    mfgDate = json.optString("mfgDate", ""),
                                    expiryDate = json.optString("expiryDate", ""),
                                    mrp = json.optDouble("mrp", 0.0),
                                    price = json.optDouble("price", 0.0),
                                    purchaseRate = json.optDouble("purchaseRate", 0.0),
                                    saleRate = json.optDouble("saleRate", 0.0),
                                    gstPercent = json.optDouble("gstPercent", 12.0),
                                    stockQuantity = json.optInt("stockQuantity", 0),
                                    rackLocation = json.optString("rackLocation", ""),
                                    freeQuantity = json.optInt("freeQuantity", 0),
                                    lowStockLevel = json.optInt("lowStockLevel", 10),
                                    createdAt = remoteTimestamp
                                )
                                dao.insertMedicine(med)
                            }
                        }
                    }
                }
                "PARTY" -> {
                    val id = json.optLong("id", 0L)
                    if (id > 0) {
                        if (isDeleted) {
                            dao.deletePartyById(id)
                        } else {
                            val party = PartyEntity(
                                id = id,
                                partyName = json.optString("partyName", ""),
                                dlNumber = json.optString("dlNumber", ""),
                                gstPanNumber = json.optString("gstPanNumber", ""),
                                contactNumber = json.optString("contactNumber", ""),
                                address = json.optString("address", "")
                            )
                            dao.insertParty(party)
                        }
                    }
                }
                "DOCTOR" -> {
                    val id = json.optLong("id", 0L)
                    if (id > 0) {
                        if (isDeleted) {
                            dao.deleteDoctorById(id)
                        } else {
                            val doc = DoctorEntity(
                                id = id,
                                doctorName = json.optString("doctorName", ""),
                                qualification = json.optString("qualification", ""),
                                doctorType = json.optString("doctorType", "Allopathic"),
                                phoneNumber = json.optString("phoneNumber", ""),
                                address = json.optString("address", "")
                            )
                            dao.insertDoctor(doc)
                        }
                    }
                }
                "PATIENT" -> {
                    val id = json.optLong("id", 0L)
                    if (id > 0) {
                        if (isDeleted) {
                            dao.deletePatientById(id)
                        } else {
                            val pat = PatientEntity(
                                id = id,
                                patientName = json.optString("patientName", ""),
                                phoneNumber = json.optString("phoneNumber", ""),
                                doctorName = json.optString("doctorName", ""),
                                address = json.optString("address", "")
                            )
                            dao.insertPatient(pat)
                        }
                    }
                }
                "INVOICE" -> {
                    val invNumber = json.optLong("invoiceNumber", 0L)
                    if (invNumber > 0) {
                        if (isDeleted) {
                            dao.deleteInvoiceItems(invNumber)
                            dao.deleteInvoice(invNumber)
                        } else {
                            val inv = InvoiceEntity(
                                invoiceNumber = invNumber,
                                date = json.optLong("date", remoteTimestamp),
                                dateFormatted = json.optString("dateFormatted", ""),
                                customerType = json.optString("customerType", "PATIENT"),
                                customerId = json.optLong("customerId", 0L),
                                customerName = json.optString("customerName", ""),
                                customerAddress = json.optString("customerAddress", ""),
                                customerDl = json.optString("customerDl", ""),
                                customerGstPan = json.optString("customerGstPan", ""),
                                customerPhone = json.optString("customerPhone", ""),
                                doctorDetails = json.optString("doctorDetails", ""),
                                totalMrpValue = json.optDouble("totalMrpValue", 0.0),
                                itemCount = json.optInt("itemCount", 0),
                                totalQty = json.optInt("totalQty", 0),
                                totalFree = json.optInt("totalFree", 0),
                                totalAmount = json.optDouble("totalAmount", 0.0),
                                lessDiscount = json.optDouble("lessDiscount", 0.0),
                                cgstAmount = json.optDouble("cgstAmount", 0.0),
                                sgstAmount = json.optDouble("sgstAmount", 0.0),
                                adjustmentAmount = json.optDouble("adjustmentAmount", 0.0),
                                netAmount = json.optDouble("netAmount", 0.0),
                                paidAmount = json.optDouble("paidAmount", 0.0),
                                dueAmount = json.optDouble("dueAmount", 0.0),
                                amountInWords = json.optString("amountInWords", ""),
                                note = json.optString("note", "")
                            )
                            val itemsList = mutableListOf<InvoiceItemEntity>()
                            val itemsJson = json.optJSONArray("items")
                            if (itemsJson != null) {
                                for (j in 0 until itemsJson.length()) {
                                    val it = itemsJson.getJSONObject(j)
                                    itemsList.add(
                                        InvoiceItemEntity(
                                            id = it.optLong("id", 0L),
                                            invoiceNumber = invNumber,
                                            slNo = it.optInt("slNo", j + 1),
                                            medicineId = it.optLong("medicineId", 0L),
                                            productName = it.optString("productName", ""),
                                            manufacturer = it.optString("manufacturer", ""),
                                            pack = it.optString("pack", ""),
                                            batchNo = it.optString("batchNo", ""),
                                            expDate = it.optString("expDate", ""),
                                            qty = it.optInt("qty", 0),
                                            freeQty = it.optInt("freeQty", 0),
                                            mrp = it.optDouble("mrp", 0.0),
                                            price = it.optDouble("price", 0.0),
                                            discountPercent = it.optDouble("discountPercent", 0.0),
                                            bonusPercent = it.optDouble("bonusPercent", 0.0),
                                            sgstPercent = it.optDouble("sgstPercent", 6.0),
                                            cgstPercent = it.optDouble("cgstPercent", 6.0),
                                            netRate = it.optDouble("netRate", 0.0),
                                            itemTotalAmount = it.optDouble("itemTotalAmount", 0.0)
                                        )
                                    )
                                }
                            }
                            dao.insertInvoice(inv)
                            if (itemsList.isNotEmpty()) {
                                dao.insertInvoiceItems(itemsList)
                            }
                        }
                    }
                }
                "SETTINGS" -> {
                    val settings = SettingsEntity(
                        id = 1,
                        businessName = json.optString("businessName", ""),
                        address = json.optString("address", ""),
                        gstNumber = json.optString("gstNumber", ""),
                        dlNumber = json.optString("dlNumber", ""),
                        contactNumber = json.optString("contactNumber", ""),
                        adminPassword = json.optString("adminPassword", "654321"),
                        adminMobile = json.optString("adminMobile", "9002625428")
                    )
                    dao.insertOrUpdateSettings(settings)
                }
                "GUEST_LOGIN" -> {
                    val id = json.optLong("id", 0L)
                    val mobile = json.optString("mobileNumber", "")
                    val timestamp = json.optLong("loginTimestamp", remoteTimestamp)
                    if (isDeleted) {
                        if (id > 0) {
                            dao.deleteGuestLogin(id)
                        } else if (mobile.isNotBlank()) {
                            val existing = dao.findGuestLogin(mobile, timestamp)
                            if (existing != null) dao.deleteGuestLogin(existing.id)
                        }
                    } else if (mobile.isNotBlank()) {
                        val existing = dao.findGuestLogin(mobile, timestamp)
                        if (existing == null) {
                            dao.insertGuestLogin(
                                GuestLoginEntity(
                                    mobileNumber = mobile,
                                    loginTimestamp = timestamp
                                )
                            )
                        }
                    }
                }
                "ADMIN_USER" -> {
                    val id = json.optLong("id", 0L)
                    val mobile = json.optString("mobileNumber", "")
                    if (mobile.isNotBlank()) {
                        if (isDeleted) {
                            if (id > 0) dao.deleteAdminUser(id)
                        } else {
                            val admin = AdminUserEntity(
                                id = id,
                                name = json.optString("name", ""),
                                mobileNumber = mobile,
                                password = json.optString("password", ""),
                                createdByMobile = json.optString("createdByMobile", ""),
                                createdAt = json.optLong("createdAt", remoteTimestamp)
                            )
                            dao.insertAdminUser(admin)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSyncEngine", "Error applying remote change for $entityType", e)
        }
    }

    private fun getCollectionForEntityType(entityType: String): String {
        return when (entityType.uppercase()) {
            "MEDICINE" -> "pharma_medicines"
            "INVOICE" -> "pharma_invoices"
            "INVOICE_ITEM" -> "pharma_invoice_items"
            "PARTY" -> "pharma_parties"
            "DOCTOR" -> "pharma_doctors"
            "PATIENT" -> "pharma_patients"
            "SETTINGS" -> "pharma_settings"
            "ADMIN_USER" -> "pharma_admin_users"
            "GUEST_LOGIN" -> "pharma_guest_logins"
            else -> "pharma_records"
        }
    }

    private fun makeStringField(value: String): JSONObject {
        val o = JSONObject()
        o.put("stringValue", value)
        return o
    }

    private fun makeIntegerField(value: Long): JSONObject {
        val o = JSONObject()
        o.put("integerValue", value.toString())
        return o
    }

    private fun makeBooleanField(value: Boolean): JSONObject {
        val o = JSONObject()
        o.put("booleanValue", value)
        return o
    }

    private fun getStringField(fields: JSONObject, key: String): String {
        val fieldObj = fields.optJSONObject(key) ?: return ""
        return fieldObj.optString("stringValue", "")
    }

    private fun getIntegerField(fields: JSONObject, key: String): Long {
        val fieldObj = fields.optJSONObject(key) ?: return 0L
        val str = fieldObj.optString("integerValue", "")
        return str.toLongOrNull() ?: fieldObj.optLong("integerValue", 0L)
    }

    private fun getBooleanField(fields: JSONObject, key: String): Boolean {
        val fieldObj = fields.optJSONObject(key) ?: return false
        return fieldObj.optBoolean("booleanValue", false)
    }

    private fun formatTimestamp(timeMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timeMs))
    }
}
