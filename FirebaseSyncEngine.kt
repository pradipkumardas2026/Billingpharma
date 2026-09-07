package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.dao.PharmaDao
import com.example.data.local.entity.*
import com.example.util.NetworkMonitor
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

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
    val projectId: String = "pharma-billing-cloud"
)

data class AppUpdateState(
    val hasUpdate: Boolean = false,
    val latestVersionName: String = "1.0",
    val latestVersionCode: Int = 1,
    val releaseNotes: String = "",
    val updateUrl: String = "",
    val publishedAt: Long = 0L
)

class FirebaseSyncEngine(
    private val context: Context,
    private val dao: PharmaDao,
    private val networkMonitor: NetworkMonitor
) {
    private val prefs = context.getSharedPreferences("pharma_sync_prefs", Context.MODE_PRIVATE)

    private val _syncInfo = MutableStateFlow(loadInitialSyncInfo())
    val syncInfo: StateFlow<SyncInfo> = _syncInfo.asStateFlow()

    private val _appUpdateState = MutableStateFlow(AppUpdateState())
    val appUpdateState: StateFlow<AppUpdateState> = _appUpdateState.asStateFlow()

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncMutex = Mutex()

    private val listenerRegistrations = mutableListOf<ListenerRegistration>()
    @Volatile
    private var lastDetailedError: String? = null

    private val firestore: FirebaseFirestore by lazy {
        val db = FirebaseFirestore.getInstance()
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
        } catch (e: Exception) {
            Log.d("FirebaseSyncEngine", "Firestore settings note: ${e.message}")
        }
        db
    }

    private val collectionsToSync = listOf(
        "pharma_medicines",
        "pharma_parties",
        "pharma_doctors",
        "pharma_patients",
        "pharma_invoices",
        "pharma_purchase_invoices",
        "pharma_settings",
        "pharma_admin_users",
        "pharma_guest_logins",
        "pharma_stock_transactions",
        "pharma_tombstones"
    )

    private var currentUserMobile: String = "9002625428"
    private var currentUserRole: String = "Master Admin"

    fun setCurrentUser(mobile: String, role: String) {
        currentUserMobile = mobile.trim().ifEmpty { "9002625428" }
        currentUserRole = role.trim().ifEmpty { "Master Admin" }
    }

    fun getCurrentUserMobile(): String = currentUserMobile
    fun getCurrentUserRole(): String = currentUserRole

    init {
        // Start real-time Firestore listeners for immediate multi-device push/pull
        setupRealtimeListeners()

        // Observe network state transitions
        syncScope.launch {
            networkMonitor.isOnline.collect { online ->
                val currentPending = dao.getPendingSyncOperations().size
                _syncInfo.value = _syncInfo.value.copy(
                    isOnline = online,
                    pendingCount = currentPending,
                    state = if (!online) SyncState.OFFLINE else if (currentPending > 0) SyncState.SYNCING else SyncState.SYNCED
                )
                if (online) {
                    triggerAutomaticSync()
                }
            }
        }

        // Periodic sync trigger when online
        syncScope.launch {
            while (isActive) {
                delay(20000L)
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
        val projId = prefs.getString("firebase_project_id", "pharma-billing-cloud") ?: "pharma-billing-cloud"
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
        val trimmed = newProjectId.trim().ifEmpty { "pharma-billing-cloud" }
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
            listenerRegistrations.forEach { it.remove() }
            listenerRegistrations.clear()
            networkMonitor.stopMonitoring()
            syncScope.cancel()
        } catch (_: Exception) {}
    }

    /**
     * Attaches Firestore Snapshot Listeners for real-time instantaneous sync across all devices.
     * When any device inserts/updates/deletes a record, all other devices receive the update immediately.
     */
    private fun setupRealtimeListeners() {
        val currentDeviceId = getDeviceId()

        for (collName in collectionsToSync) {
            try {
                val registration = firestore.collection(collName)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Log.w("FirebaseSyncEngine", "Snapshot listener error for $collName: ${error.message}")
                            return@addSnapshotListener
                        }

                        if (snapshots != null && !snapshots.isEmpty) {
                            syncScope.launch {
                                for (docChange in snapshots.documentChanges) {
                                    val doc = docChange.document
                                    val docDeviceId = doc.getString("deviceId") ?: doc.getString("deletedByDevice") ?: ""
                                    // Multi-device sync: only process changes originated from OTHER devices
                                    if (docDeviceId != currentDeviceId && docDeviceId.isNotEmpty()) {
                                        val entityType = doc.getString("entityType") ?: getEntityTypeFromCollection(collName)
                                        val entityId = doc.getString("entityId") ?: doc.id
                                        val payloadJson = doc.getString("payloadJson") ?: ""
                                        val isDeleted = doc.getBoolean("isDeleted") ?: (docChange.type == DocumentChange.Type.REMOVED) || (collName == "pharma_tombstones")
                                        val remoteTimestamp = doc.getLong("deletedAt") ?: doc.getLong("timestamp") ?: System.currentTimeMillis()
                                        val userMob = doc.getString("userMobile") ?: doc.getString("deletedByUser") ?: ""

                                        if (isDeleted || collName == "pharma_tombstones") {
                                            val tomb = TombstoneEntity(
                                                entityType = entityType,
                                                entityId = entityId,
                                                deletedAt = remoteTimestamp,
                                                deviceId = docDeviceId,
                                                userMobile = userMob
                                            )
                                            dao.insertTombstone(tomb)
                                            applyDeletionToRoom(entityType, entityId)
                                            Log.d("FirebaseSyncEngine", "Real-time deletion applied for $entityType $entityId")
                                        } else if (payloadJson.isNotBlank()) {
                                            // Check if tombstoned on this device
                                            if (dao.isTombstoned(entityType, entityId) > 0) {
                                                applyDeletionToRoom(entityType, entityId)
                                                Log.d("FirebaseSyncEngine", "Ignored resurrection of tombstoned $entityType $entityId")
                                            } else {
                                                applyRemoteChange(entityType, payloadJson, false, remoteTimestamp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                listenerRegistrations.add(registration)
            } catch (e: Exception) {
                Log.e("FirebaseSyncEngine", "Failed to register listener for $collName: ${e.message}")
            }
        }

        try {
            val updateReg = firestore.collection("pharma_app_meta").document("update_info")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val verCode = snapshot.getLong("versionCode")?.toInt() ?: 1
                    val verName = snapshot.getString("versionName") ?: "1.0"
                    val notes = snapshot.getString("releaseNotes") ?: ""
                    val url = snapshot.getString("updateUrl") ?: ""
                    val pubAt = snapshot.getLong("publishedAt") ?: 0L
                    val isAvail = snapshot.getBoolean("updateAvailable") ?: (verCode > 1)
                    val forcePrompt = snapshot.getBoolean("forcePrompt") ?: false
                    _appUpdateState.value = AppUpdateState(
                        hasUpdate = isAvail && (verCode > 1 || forcePrompt),
                        latestVersionName = verName,
                        latestVersionCode = verCode,
                        releaseNotes = notes,
                        updateUrl = url,
                        publishedAt = pubAt
                    )
                }
            listenerRegistrations.add(updateReg)
        } catch (e: Exception) {
            Log.w("FirebaseSyncEngine", "Failed to listen for app update: ${e.message}")
        }
    }

    suspend fun checkAppUpdateManually(): AppUpdateState = withContext(Dispatchers.IO) {
        try {
            val task = firestore.collection("pharma_app_meta").document("update_info").get()
            val snapshot = Tasks.await(task, 8, TimeUnit.SECONDS)
            if (snapshot != null && snapshot.exists()) {
                val verCode = snapshot.getLong("versionCode")?.toInt() ?: 1
                val verName = snapshot.getString("versionName") ?: "1.0"
                val notes = snapshot.getString("releaseNotes") ?: ""
                val url = snapshot.getString("updateUrl") ?: ""
                val pubAt = snapshot.getLong("publishedAt") ?: 0L
                val isAvail = snapshot.getBoolean("updateAvailable") ?: (verCode > 1)
                val forcePrompt = snapshot.getBoolean("forcePrompt") ?: false
                val state = AppUpdateState(
                    hasUpdate = isAvail && (verCode > 1 || forcePrompt),
                    latestVersionName = verName,
                    latestVersionCode = verCode,
                    releaseNotes = notes,
                    updateUrl = url,
                    publishedAt = pubAt
                )
                _appUpdateState.value = state
                return@withContext state
            }
        } catch (e: Exception) {
            Log.w("FirebaseSyncEngine", "Manual update check error: ${e.message}")
        }
        return@withContext _appUpdateState.value
    }

    suspend fun publishAppUpdate(versionName: String, versionCode: Int, releaseNotes: String, updateUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "versionName" to versionName,
                "versionCode" to versionCode,
                "releaseNotes" to releaseNotes,
                "updateUrl" to updateUrl,
                "updateAvailable" to true,
                "forcePrompt" to true,
                "publishedAt" to System.currentTimeMillis()
            )
            val task = firestore.collection("pharma_app_meta").document("update_info").set(data, SetOptions.merge())
            Tasks.await(task, 10, TimeUnit.SECONDS)
            _appUpdateState.value = AppUpdateState(
                hasUpdate = true,
                latestVersionName = versionName,
                latestVersionCode = versionCode,
                releaseNotes = releaseNotes,
                updateUrl = updateUrl,
                publishedAt = System.currentTimeMillis()
            )
            return@withContext true
        } catch (e: Exception) {
            Log.e("FirebaseSyncEngine", "Failed to publish app update: ${e.message}", e)
            return@withContext false
        }
    }

    private var autoSyncJob: Job? = null

    fun triggerAutomaticSync() {
        autoSyncJob?.cancel()
        autoSyncJob = syncScope.launch {
            delay(500) // Debounce rapid keystrokes/transactions to keep app blazing fast
            if (!networkMonitor.isOnline.value) {
                val pending = dao.getPendingSyncOperations().size
                _syncInfo.value = _syncInfo.value.copy(
                    state = SyncState.OFFLINE,
                    isOnline = false,
                    pendingCount = pending
                )
                return@launch
            }
            performFullSync(pullFromRemote = false)
        }
    }

    suspend fun performFullSync(pullFromRemote: Boolean = true) {
        if (!syncMutex.tryLock()) return

        try {
            val pendingOps = dao.getPendingSyncOperations()
            _syncInfo.value = _syncInfo.value.copy(
                state = SyncState.SYNCING,
                pendingCount = pendingOps.size,
                errorMessage = null
            )

            // Step 1: Upload pending local operations to Firebase Firestore
            var anyUploadFailed = false
            lastDetailedError = null
            for (op in pendingOps) {
                val success = uploadOperationToFirestore(op)
                if (success) {
                    dao.deleteSyncOperation(op.id)
                } else {
                    anyUploadFailed = true
                    val errText = lastDetailedError ?: "Sync failed at ${Date()}"
                    dao.updateSyncStatus(op.id, "FAILED", errText, op.retryCount + 1)
                }
            }

            // Step 2: Download remote changes from Firestore collections (parallel & throttled)
            if (pullFromRemote) {
                pullRemoteChanges(force = true)
            } else {
                pullRemoteChanges(force = false)
            }

            val remainingPending = dao.getPendingSyncOperations().size
            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_time", now).apply()

            val finalErrorMsg = if (anyUploadFailed) {
                lastDetailedError ?: "Some changes failed to upload. Auto-retry scheduled."
            } else null

            _syncInfo.value = _syncInfo.value.copy(
                state = if (anyUploadFailed) SyncState.FAILED else SyncState.SYNCED,
                pendingCount = remainingPending,
                lastSyncTimestamp = now,
                lastSyncFormatted = formatTimestamp(now),
                errorMessage = finalErrorMsg
            )
        } catch (e: Exception) {
            Log.e("FirebaseSyncEngine", "Sync error", e)
            val pendingCount = dao.getPendingSyncOperations().size
            val detailed = e.localizedMessage ?: e.message ?: e.toString()
            lastDetailedError = detailed
            _syncInfo.value = _syncInfo.value.copy(
                state = SyncState.FAILED,
                pendingCount = pendingCount,
                errorMessage = detailed
            )
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun uploadOperationToFirestore(op: SyncOperationEntity): Boolean = withContext(Dispatchers.IO) {
        val collectionName = getCollectionForEntityType(op.entityType)
        val documentId = getDocumentId(op.entityType, op.entityId)

        try {
            val devId = op.deviceId.ifEmpty { getDeviceId() }
            val now = System.currentTimeMillis()

            if (op.operation == "DELETE") {
                // 1. Upload to centralized pharma_tombstones
                val tombDocId = "tomb_${op.entityType}_${op.entityId}"
                val tombData = hashMapOf(
                    "entityType" to op.entityType,
                    "entityId" to op.entityId,
                    "deletedAt" to op.timestamp,
                    "deletedByDevice" to devId,
                    "deletedByUser" to currentUserMobile,
                    "isDeleted" to true,
                    "timestamp" to op.timestamp
                )
                val tombTask = firestore.collection("pharma_tombstones").document(tombDocId).set(tombData, SetOptions.merge())
                Tasks.await(tombTask, 12, TimeUnit.SECONDS)

                // 2. Mark entity document in its collection as deleted
                val entityData = hashMapOf(
                    "entityType" to op.entityType,
                    "entityId" to op.entityId,
                    "operation" to "DELETE",
                    "isDeleted" to true,
                    "deletedAt" to op.timestamp,
                    "timestamp" to op.timestamp,
                    "updatedAt" to now,
                    "deviceId" to devId,
                    "userMobile" to currentUserMobile
                )
                val docRef = firestore.collection(collectionName).document(documentId)
                val task = docRef.set(entityData, SetOptions.merge())
                Tasks.await(task, 12, TimeUnit.SECONDS)
                Log.d("FirebaseSyncEngine", "Uploaded deletion tombstone for ${op.entityType} ${op.entityId}")
                return@withContext true
            }

            // For INSERT / UPDATE: Check if record is tombstoned (prevent resurrection of deleted records)
            if (dao.isTombstoned(op.entityType, op.entityId) > 0) {
                Log.d("FirebaseSyncEngine", "Skipping upload of ${op.entityType} ${op.entityId} because it is tombstoned")
                return@withContext true
            }

            val data = hashMapOf(
                "entityType" to op.entityType,
                "entityId" to op.entityId,
                "operation" to op.operation,
                "payloadJson" to op.payloadJson,
                "timestamp" to op.timestamp,
                "deviceId" to devId,
                "userMobile" to currentUserMobile,
                "isDeleted" to false,
                "updatedAt" to now
            )

            val docRef = firestore.collection(collectionName).document(documentId)
            val task = docRef.set(data, SetOptions.merge())
            Tasks.await(task, 12, TimeUnit.SECONDS)
            Log.d("FirebaseSyncEngine", "Uploaded $documentId to $collectionName successfully")
            true
        } catch (e: Exception) {
            val errorDetails = "Failed to upload $collectionName/$documentId: ${e.javaClass.simpleName}: ${e.message}"
            Log.e("FirebaseSyncEngine", errorDetails, e)
            lastDetailedError = e.message ?: e.toString()
            false
        }
    }

    private var lastPullTime = 0L

    private suspend fun pullRemoteChanges(force: Boolean = false) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // If listeners are already actively receiving real-time Firestore pushes, avoid heavy sequential full pulls
        if (!force && (now - lastPullTime) < 300_000L) {
            return@withContext
        }
        lastPullTime = now
        val currentDeviceId = getDeviceId()

        // 1. Pull tombstones first to ensure any deleted records are immediately removed
        try {
            val tombTask = firestore.collection("pharma_tombstones").get()
            val tombSnapshot = Tasks.await(tombTask, 4, TimeUnit.SECONDS)
            for (doc in tombSnapshot.documents) {
                val entityType = doc.getString("entityType") ?: continue
                val entityId = doc.getString("entityId") ?: continue
                val deletedAt = doc.getLong("deletedAt") ?: doc.getLong("timestamp") ?: System.currentTimeMillis()
                val devId = doc.getString("deletedByDevice") ?: ""
                val user = doc.getString("deletedByUser") ?: ""

                val tomb = TombstoneEntity(
                    entityType = entityType,
                    entityId = entityId,
                    deletedAt = deletedAt,
                    deviceId = devId,
                    userMobile = user
                )
                dao.insertTombstone(tomb)
                applyDeletionToRoom(entityType, entityId)
            }
        } catch (e: Exception) {
            Log.w("FirebaseSyncEngine", "Note: tombstones check: ${e.message}")
        }

        // 2. Parallel pull of regular business collections using coroutines (drastically faster)
        coroutineScope {
            val nonTombstoneCollections = collectionsToSync.filter { it != "pharma_tombstones" }
            nonTombstoneCollections.map { collName ->
                async(Dispatchers.IO) {
                    try {
                        val queryTask = firestore.collection(collName).get()
                        val snapshot = Tasks.await(queryTask, 4, TimeUnit.SECONDS)
                        for (doc in snapshot.documents) {
                            val docDeviceId = doc.getString("deviceId") ?: ""
                            if (docDeviceId != currentDeviceId && docDeviceId.isNotEmpty()) {
                                val entityType = doc.getString("entityType") ?: getEntityTypeFromCollection(collName)
                                val entityId = doc.getString("entityId") ?: doc.id
                                val payloadJson = doc.getString("payloadJson") ?: ""
                                val isDeleted = doc.getBoolean("isDeleted") ?: false
                                val remoteTimestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                if (isDeleted) {
                                    val tomb = TombstoneEntity(
                                        entityType = entityType,
                                        entityId = entityId,
                                        deletedAt = remoteTimestamp,
                                        deviceId = docDeviceId,
                                        userMobile = doc.getString("userMobile") ?: ""
                                    )
                                    dao.insertTombstone(tomb)
                                    applyDeletionToRoom(entityType, entityId)
                                } else if (payloadJson.isNotBlank()) {
                                    if (dao.isTombstoned(entityType, entityId) > 0) {
                                        applyDeletionToRoom(entityType, entityId)
                                    } else {
                                        applyRemoteChange(entityType, payloadJson, false, remoteTimestamp)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseSyncEngine", "Non-fatal pull note for $collName: ${e.message}")
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * Direct cloud check for Sub-Admin login on new devices where local sync might not have finished yet.
     */
    suspend fun lookupSubAdminFromCloud(mobile: String, pass: String): AdminUserEntity? = withContext(Dispatchers.IO) {
        try {
            val queryTask = firestore.collection("pharma_admin_users")
                .whereEqualTo("isDeleted", false)
                .get()
            val snapshot = Tasks.await(queryTask, 8, TimeUnit.SECONDS)
            for (doc in snapshot.documents) {
                val payload = doc.getString("payloadJson") ?: continue
                val json = JSONObject(payload)
                val docMobile = json.optString("mobileNumber", "")
                val docPass = json.optString("password", "")
                if (docMobile == mobile.trim() && docPass == pass.trim()) {
                    val admin = AdminUserEntity(
                        id = json.optLong("id", System.currentTimeMillis()),
                        name = json.optString("name", "Admin"),
                        mobileNumber = docMobile,
                        password = docPass,
                        createdByMobile = json.optString("createdByMobile", "9002625428"),
                        createdAt = json.optLong("createdAt", System.currentTimeMillis())
                    )
                    dao.insertAdminUser(admin)
                    return@withContext admin
                }
            }
            null
        } catch (e: Exception) {
            Log.w("FirebaseSyncEngine", "Error querying sub-admin from cloud: ${e.message}")
            null
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
            when (entityType.uppercase()) {
                "MEDICINE" -> {
                    val id = json.optLong("id", 0L)
                    val prodName = json.optString("productName", "")
                    val batchNo = json.optString("batchNumber", "")
                    if (isDeleted) {
                        if (id > 0) dao.deleteMedicineById(id)
                        val match = dao.findMedicineByNameAndBatch(prodName, batchNo)
                        if (match != null) dao.deleteMedicineById(match.id)
                    } else if (prodName.isNotBlank()) {
                        val local = if (id > 0) dao.getMedicineById(id) else null
                        val existingByNameBatch = dao.findMedicineByNameAndBatch(prodName, batchNo)
                        val targetId = local?.id ?: existingByNameBatch?.id ?: if (id > 0) id else 0L

                        val med = MedicineEntity(
                            id = targetId,
                            productName = prodName,
                            productType = json.optString("productType", "Tablet"),
                            packagingType = json.optString("packagingType", "1×10T"),
                            composition = json.optString("composition", ""),
                            manufacturerName = json.optString("manufacturerName", ""),
                            companyName = json.optString("companyName", ""),
                            batchNumber = batchNo,
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
                "PARTY" -> {
                    val id = json.optLong("id", 0L)
                    val partyName = json.optString("partyName", "")
                    val contactNumber = json.optString("contactNumber", "")
                    if (isDeleted) {
                        if (id > 0) dao.deletePartyById(id)
                        val existing = dao.findPartyByNameAndPhone(partyName, contactNumber)
                        if (existing != null) dao.deletePartyById(existing.id)
                    } else if (partyName.isNotBlank()) {
                        val existing = dao.findPartyByNameAndPhone(partyName, contactNumber)
                        val targetId = if (id > 0) id else (existing?.id ?: 0L)
                        val party = PartyEntity(
                            id = targetId,
                            partyName = partyName,
                            dlNumber = json.optString("dlNumber", ""),
                            gstPanNumber = json.optString("gstPanNumber", ""),
                            contactNumber = contactNumber,
                            address = json.optString("address", "")
                        )
                        dao.insertParty(party)
                    }
                }
                "DOCTOR" -> {
                    val id = json.optLong("id", 0L)
                    val doctorName = json.optString("doctorName", "")
                    val phone = json.optString("phoneNumber", "")
                    if (isDeleted) {
                        if (id > 0) dao.deleteDoctorById(id)
                        val existing = dao.findDoctorByNameAndPhone(doctorName, phone)
                        if (existing != null) dao.deleteDoctorById(existing.id)
                    } else if (doctorName.isNotBlank()) {
                        val existing = dao.findDoctorByNameAndPhone(doctorName, phone)
                        val targetId = if (id > 0) id else (existing?.id ?: 0L)
                        val doc = DoctorEntity(
                            id = targetId,
                            doctorName = doctorName,
                            qualification = json.optString("qualification", ""),
                            doctorType = json.optString("doctorType", "Allopathic"),
                            phoneNumber = phone,
                            address = json.optString("address", "")
                        )
                        dao.insertDoctor(doc)
                    }
                }
                "PATIENT" -> {
                    val id = json.optLong("id", 0L)
                    val patientName = json.optString("patientName", "")
                    val phone = json.optString("phoneNumber", "")
                    if (isDeleted) {
                        if (id > 0) dao.deletePatientById(id)
                        val existing = dao.findPatientByNameAndPhone(patientName, phone)
                        if (existing != null) dao.deletePatientById(existing.id)
                    } else if (patientName.isNotBlank()) {
                        val existing = dao.findPatientByNameAndPhone(patientName, phone)
                        val targetId = if (id > 0) id else (existing?.id ?: 0L)
                        val pat = PatientEntity(
                            id = targetId,
                            patientName = patientName,
                            phoneNumber = phone,
                            doctorName = json.optString("doctorName", ""),
                            address = json.optString("address", "")
                        )
                        dao.insertPatient(pat)
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
                            Log.d("FirebaseSyncEngine", "Received real-time guest login: $mobile")
                        }
                    }
                }
                "ADMIN_USER" -> {
                    val id = json.optLong("id", 0L)
                    val mobile = json.optString("mobileNumber", "")
                    if (mobile.isNotBlank()) {
                        if (isDeleted) {
                            if (id > 0) dao.deleteAdminUser(id)
                            val existing = dao.findAdminByMobile(mobile)
                            if (existing != null) dao.deleteAdminUser(existing.id)
                        } else {
                            val existing = dao.findAdminByMobile(mobile)
                            val targetId = if (id > 0) id else (existing?.id ?: 0L)
                            val admin = AdminUserEntity(
                                id = targetId,
                                name = json.optString("name", ""),
                                mobileNumber = mobile,
                                password = json.optString("password", ""),
                                createdByMobile = json.optString("createdByMobile", ""),
                                createdAt = json.optLong("createdAt", remoteTimestamp)
                            )
                            dao.insertAdminUser(admin)
                            Log.d("FirebaseSyncEngine", "Received real-time admin user sync: $mobile")
                        }
                    }
                }
                "STOCK_TRANSACTION" -> {
                    val medId = json.optLong("medicineId", 0L)
                    val timestamp = json.optLong("timestamp", remoteTimestamp)
                    val type = json.optString("type", "RECEIPT")
                    val qty = json.optInt("qty", 0)
                    if (medId > 0 && !isDeleted) {
                        val existing = dao.findStockTransaction(medId, timestamp, type, qty)
                        if (existing == null) {
                            dao.insertStockTransaction(
                                StockTransactionEntity(
                                    medicineId = medId,
                                    productName = json.optString("productName", ""),
                                    companyName = json.optString("companyName", ""),
                                    type = type,
                                    referenceInvoice = if (json.has("referenceInvoice") && !json.isNull("referenceInvoice")) json.optLong("referenceInvoice") else null,
                                    qty = qty,
                                    freeQty = json.optInt("freeQty", 0),
                                    rate = json.optDouble("rate", 0.0),
                                    amount = json.optDouble("amount", 0.0),
                                    timestamp = timestamp,
                                    dateFormatted = json.optString("dateFormatted", "")
                                )
                            )
                        }
                    }
                }
                "PURCHASE_INVOICE" -> {
                    val id = json.optLong("id", 0L)
                    val invNo = json.optString("invoiceNumber", "")
                    if (isDeleted) {
                        if (id > 0) dao.deletePurchaseInvoiceById(id)
                        else if (invNo.isNotBlank()) dao.deletePurchaseInvoiceByNumber(invNo)
                    } else if (invNo.isNotBlank()) {
                        val existing = dao.getPurchaseInvoiceByNumber(invNo)
                        val purchase = PurchaseInvoiceEntity(
                            id = existing?.id ?: if (id > 0) id else 0L,
                            invoiceNumber = invNo,
                            date = json.optLong("date", remoteTimestamp),
                            dateFormatted = json.optString("dateFormatted", ""),
                            companyName = json.optString("companyName", ""),
                            companyGst = json.optString("companyGst", ""),
                            companyPhone = json.optString("companyPhone", ""),
                            itemsSummary = json.optString("itemsSummary", ""),
                            totalQty = json.optInt("totalQty", 1),
                            taxableAmount = json.optDouble("taxableAmount", 0.0),
                            gstRatePercent = json.optDouble("gstRatePercent", 12.0),
                            cgstAmount = json.optDouble("cgstAmount", 0.0),
                            sgstAmount = json.optDouble("sgstAmount", 0.0),
                            totalGstAmount = json.optDouble("totalGstAmount", 0.0),
                            totalAmount = json.optDouble("totalAmount", 0.0),
                            note = json.optString("note", ""),
                            createdAt = json.optLong("createdAt", remoteTimestamp)
                        )
                        dao.insertPurchaseInvoice(purchase)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSyncEngine", "Error applying remote change for $entityType", e)
        }
    }

    private suspend fun applyDeletionToRoom(entityType: String, entityId: String) {
        try {
            when (entityType.uppercase()) {
                "MEDICINE" -> {
                    val id = entityId.toLongOrNull() ?: 0L
                    if (id > 0) dao.deleteMedicineById(id)
                }
                "PARTY" -> {
                    val id = entityId.toLongOrNull() ?: 0L
                    if (id > 0) dao.deletePartyById(id)
                }
                "DOCTOR" -> {
                    val id = entityId.toLongOrNull() ?: 0L
                    if (id > 0) dao.deleteDoctorById(id)
                }
                "PATIENT" -> {
                    val id = entityId.toLongOrNull() ?: 0L
                    if (id > 0) dao.deletePatientById(id)
                }
                "INVOICE" -> {
                    val inv = entityId.toLongOrNull() ?: 0L
                    if (inv > 0) {
                        dao.deleteInvoiceItems(inv)
                        dao.deleteInvoice(inv)
                    }
                }
                "PURCHASE_INVOICE" -> {
                    val id = entityId.toLongOrNull() ?: 0L
                    if (id > 0) dao.deletePurchaseInvoiceById(id)
                    else dao.deletePurchaseInvoiceByNumber(entityId)
                }
                "ADMIN_USER" -> {
                    val id = entityId.toLongOrNull() ?: 0L
                    if (id > 0) dao.deleteAdminUser(id)
                }
                "GUEST_LOGIN" -> {
                    val id = entityId.toLongOrNull() ?: 0L
                    if (id > 0) dao.deleteGuestLogin(id)
                    if (entityId.contains("_")) {
                        val parts = entityId.split("_")
                        val mobile = parts.getOrNull(0) ?: ""
                        val timestamp = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                        if (mobile.isNotBlank() && timestamp > 0) {
                            val existing = dao.findGuestLogin(mobile, timestamp)
                            if (existing != null) dao.deleteGuestLogin(existing.id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSyncEngine", "Error applying local deletion for $entityType $entityId", e)
        }
    }

    private fun getCollectionForEntityType(entityType: String): String {
        return when (entityType.uppercase()) {
            "MEDICINE" -> "pharma_medicines"
            "INVOICE" -> "pharma_invoices"
            "PURCHASE_INVOICE" -> "pharma_purchase_invoices"
            "PARTY" -> "pharma_parties"
            "DOCTOR" -> "pharma_doctors"
            "PATIENT" -> "pharma_patients"
            "SETTINGS" -> "pharma_settings"
            "ADMIN_USER" -> "pharma_admin_users"
            "GUEST_LOGIN" -> "pharma_guest_logins"
            "STOCK_TRANSACTION" -> "pharma_stock_transactions"
            "TOMBSTONE" -> "pharma_tombstones"
            else -> "pharma_records"
        }
    }

    private fun getEntityTypeFromCollection(collectionName: String): String {
        return when (collectionName) {
            "pharma_medicines" -> "MEDICINE"
            "pharma_invoices" -> "INVOICE"
            "pharma_purchase_invoices" -> "PURCHASE_INVOICE"
            "pharma_parties" -> "PARTY"
            "pharma_doctors" -> "DOCTOR"
            "pharma_patients" -> "PATIENT"
            "pharma_settings" -> "SETTINGS"
            "pharma_admin_users" -> "ADMIN_USER"
            "pharma_guest_logins" -> "GUEST_LOGIN"
            "pharma_stock_transactions" -> "STOCK_TRANSACTION"
            "pharma_tombstones" -> "TOMBSTONE"
            else -> "UNKNOWN"
        }
    }

    private fun getDocumentId(entityType: String, entityId: String): String {
        val cleanId = entityId.replace("/", "_").replace(" ", "_")
        return when (entityType.uppercase()) {
            "MEDICINE" -> "med_$cleanId"
            "INVOICE" -> "inv_$cleanId"
            "PARTY" -> "party_$cleanId"
            "DOCTOR" -> "doc_$cleanId"
            "PATIENT" -> "pat_$cleanId"
            "SETTINGS" -> "settings_1"
            "ADMIN_USER" -> "admin_$cleanId"
            "GUEST_LOGIN" -> "guest_$cleanId"
            "STOCK_TRANSACTION" -> "st_$cleanId"
            "TOMBSTONE" -> "tomb_$cleanId"
            else -> "doc_$cleanId"
        }
    }

    private fun formatTimestamp(timeMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timeMs))
    }
}
