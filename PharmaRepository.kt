package com.example.data.repository

import com.example.data.local.dao.PharmaDao
import com.example.data.local.entity.AdminUserEntity
import com.example.data.local.entity.DoctorEntity
import com.example.data.local.entity.GuestLoginEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.MedicineEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PatientEntity
import com.example.data.local.entity.SettingsEntity
import com.example.data.local.entity.StockTransactionEntity
import com.example.data.local.entity.SyncOperationEntity
import com.example.data.local.entity.TombstoneEntity
import com.example.data.sync.FirebaseSyncEngine
import com.example.data.sync.SyncSerializer
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow

class PharmaRepository(
    private val dao: PharmaDao,
    private val syncEngine: FirebaseSyncEngine? = null
) {

    private suspend fun queueSync(entityType: String, entityId: String, operation: String, payloadJson: String) {
        try {
            dao.insertSyncOperation(
                SyncOperationEntity(
                    entityType = entityType,
                    entityId = entityId,
                    operation = operation,
                    payloadJson = payloadJson,
                    timestamp = System.currentTimeMillis(),
                    status = "PENDING",
                    deviceId = syncEngine?.getDeviceId() ?: ""
                )
            )
            syncEngine?.triggerAutomaticSync()
        } catch (_: Exception) {}
    }

    val settingsFlow: Flow<SettingsEntity?> = dao.getSettingsFlow()

    suspend fun getSettings(): SettingsEntity? = dao.getSettings()

    suspend fun updateSettings(settings: SettingsEntity) {
        dao.insertOrUpdateSettings(settings)
        queueSync("SETTINGS", "1", "UPDATE", SyncSerializer.settingsToJson(settings))
    }

    fun getAllMedicines(): Flow<List<MedicineEntity>> = dao.getAllMedicines()

    fun searchMedicines(query: String): Flow<List<MedicineEntity>> = dao.searchMedicines(query)

    suspend fun getMedicineById(id: Long): MedicineEntity? = dao.getMedicineById(id)

    suspend fun insertMedicine(medicine: MedicineEntity): Long {
        val id = dao.insertMedicine(medicine)
        if (medicine.stockQuantity > 0 || medicine.freeQuantity > 0) {
            val rate = if (medicine.purchaseRate > 0.0) medicine.purchaseRate else medicine.price
            val st = StockTransactionEntity(
                medicineId = id,
                productName = medicine.productName,
                companyName = if (medicine.companyName.isNotBlank()) medicine.companyName else medicine.manufacturerName,
                type = "RECEIPT",
                referenceInvoice = null,
                qty = medicine.stockQuantity,
                freeQty = medicine.freeQuantity,
                rate = rate,
                amount = medicine.stockQuantity * rate,
                timestamp = if (medicine.createdAt > 0) medicine.createdAt else System.currentTimeMillis(),
                dateFormatted = DateUtils.currentDateString()
            )
            val stId = dao.insertStockTransaction(st)
            queueSync("STOCK_TRANSACTION", "${id}_${st.timestamp}_RECEIPT_${st.qty}", "INSERT", SyncSerializer.stockTransactionToJson(st.copy(id = stId)))
        }
        val insertedMed = dao.getMedicineById(id) ?: medicine.copy(id = id)
        queueSync("MEDICINE", id.toString(), "INSERT", SyncSerializer.medicineToJson(insertedMed))
        return id
    }

    suspend fun updateMedicine(medicine: MedicineEntity) {
        val existing = dao.getMedicineById(medicine.id)
        if (existing != null && medicine.stockQuantity != existing.stockQuantity) {
            val diff = medicine.stockQuantity - existing.stockQuantity
            if (diff > 0) {
                val rate = if (medicine.purchaseRate > 0.0) medicine.purchaseRate else medicine.price
                val st = StockTransactionEntity(
                    medicineId = medicine.id,
                    productName = medicine.productName,
                    companyName = if (medicine.companyName.isNotBlank()) medicine.companyName else medicine.manufacturerName,
                    type = "RECEIPT",
                    referenceInvoice = null,
                    qty = diff,
                    freeQty = (medicine.freeQuantity - existing.freeQuantity).coerceAtLeast(0),
                    rate = rate,
                    amount = diff * rate,
                    timestamp = System.currentTimeMillis(),
                    dateFormatted = DateUtils.currentDateString()
                )
                val stId = dao.insertStockTransaction(st)
                queueSync("STOCK_TRANSACTION", "${medicine.id}_${st.timestamp}_RECEIPT_${st.qty}", "INSERT", SyncSerializer.stockTransactionToJson(st.copy(id = stId)))
            } else if (diff < 0) {
                val issueQty = -diff
                val rate = if (medicine.saleRate > 0.0) medicine.saleRate else medicine.price
                val st = StockTransactionEntity(
                    medicineId = medicine.id,
                    productName = medicine.productName,
                    companyName = if (medicine.companyName.isNotBlank()) medicine.companyName else medicine.manufacturerName,
                    type = "ISSUE",
                    referenceInvoice = null,
                    qty = issueQty,
                    freeQty = 0,
                    rate = rate,
                    amount = issueQty * rate,
                    timestamp = System.currentTimeMillis(),
                    dateFormatted = DateUtils.currentDateString()
                )
                val stId = dao.insertStockTransaction(st)
                queueSync("STOCK_TRANSACTION", "${medicine.id}_${st.timestamp}_ISSUE_${st.qty}", "INSERT", SyncSerializer.stockTransactionToJson(st.copy(id = stId)))
            }
        }
        dao.updateMedicine(medicine)
        queueSync("MEDICINE", medicine.id.toString(), "UPDATE", SyncSerializer.medicineToJson(medicine))
    }

    suspend fun deleteMedicine(id: Long) {
        val tomb = TombstoneEntity(
            entityType = "MEDICINE",
            entityId = id.toString(),
            deletedAt = System.currentTimeMillis(),
            deviceId = syncEngine?.getDeviceId() ?: "",
            userMobile = syncEngine?.getCurrentUserMobile() ?: ""
        )
        dao.insertTombstone(tomb)
        dao.deleteStockTransactionsForMedicine(id)
        dao.deleteMedicineById(id)
        queueSync("MEDICINE", id.toString(), "DELETE", SyncSerializer.tombstoneToJson(tomb))
    }

    suspend fun addStockToMedicine(
        medicineId: Long,
        addedQty: Int,
        addedFreeQty: Int,
        newBatch: String,
        newExpiry: String,
        newPurchaseRate: Double?,
        newMrp: Double?,
        newSaleRate: Double?
    ) {
        val med = dao.getMedicineById(medicineId) ?: return
        val updatedMed = med.copy(
            stockQuantity = med.stockQuantity + addedQty,
            freeQuantity = med.freeQuantity + addedFreeQty,
            batchNumber = if (newBatch.isNotBlank()) newBatch else med.batchNumber,
            expiryDate = if (newExpiry.isNotBlank()) newExpiry else med.expiryDate,
            purchaseRate = newPurchaseRate ?: med.purchaseRate,
            mrp = newMrp ?: med.mrp,
            saleRate = newSaleRate ?: med.saleRate,
            price = newSaleRate ?: med.price
        )
        dao.updateMedicine(updatedMed)
        queueSync("MEDICINE", med.id.toString(), "UPDATE", SyncSerializer.medicineToJson(updatedMed))
        val rate = newPurchaseRate ?: (if (med.purchaseRate > 0.0) med.purchaseRate else med.price)
        val st = StockTransactionEntity(
            medicineId = med.id,
            productName = med.productName,
            companyName = if (med.companyName.isNotBlank()) med.companyName else med.manufacturerName,
            type = "RECEIPT",
            referenceInvoice = null,
            qty = addedQty,
            freeQty = addedFreeQty,
            rate = rate,
            amount = addedQty * rate,
            timestamp = System.currentTimeMillis(),
            dateFormatted = DateUtils.currentDateString()
        )
        val stId = dao.insertStockTransaction(st)
        queueSync("STOCK_TRANSACTION", "${med.id}_${st.timestamp}_RECEIPT_${st.qty}", "INSERT", SyncSerializer.stockTransactionToJson(st.copy(id = stId)))
    }

    fun getAllParties(): Flow<List<PartyEntity>> = dao.getAllParties()

    fun searchParties(query: String): Flow<List<PartyEntity>> = dao.searchParties(query)

    suspend fun insertParty(party: PartyEntity): Long {
        val id = dao.insertParty(party)
        queueSync("PARTY", id.toString(), "INSERT", SyncSerializer.partyToJson(party.copy(id = id)))
        return id
    }

    suspend fun updateParty(party: PartyEntity) {
        dao.updateParty(party)
        queueSync("PARTY", party.id.toString(), "UPDATE", SyncSerializer.partyToJson(party))
    }

    suspend fun updatePartyAndSyncInvoices(party: PartyEntity, oldName: String) {
        dao.updateParty(party)
        queueSync("PARTY", party.id.toString(), "UPDATE", SyncSerializer.partyToJson(party))
        dao.updateInvoiceCustomerDetailsForParty(
            partyId = party.id,
            oldName = oldName,
            newName = party.partyName,
            newAddress = party.address,
            newDl = party.dlNumber,
            newGstPan = party.gstPanNumber,
            newPhone = party.contactNumber
        )
    }

    suspend fun deleteParty(id: Long) {
        val tomb = TombstoneEntity(
            entityType = "PARTY",
            entityId = id.toString(),
            deletedAt = System.currentTimeMillis(),
            deviceId = syncEngine?.getDeviceId() ?: "",
            userMobile = syncEngine?.getCurrentUserMobile() ?: ""
        )
        dao.insertTombstone(tomb)
        dao.deletePartyById(id)
        queueSync("PARTY", id.toString(), "DELETE", SyncSerializer.tombstoneToJson(tomb))
    }

    fun getAllDoctors(): Flow<List<DoctorEntity>> = dao.getAllDoctors()

    fun searchDoctors(query: String): Flow<List<DoctorEntity>> = dao.searchDoctors(query)

    suspend fun insertDoctor(doctor: DoctorEntity): Long {
        val id = dao.insertDoctor(doctor)
        queueSync("DOCTOR", id.toString(), "INSERT", SyncSerializer.doctorToJson(doctor.copy(id = id)))
        return id
    }

    suspend fun updateDoctor(doctor: DoctorEntity) {
        dao.updateDoctor(doctor)
        queueSync("DOCTOR", doctor.id.toString(), "UPDATE", SyncSerializer.doctorToJson(doctor))
    }

    suspend fun updateDoctorAndSyncInvoices(doctor: DoctorEntity, oldName: String) {
        dao.updateDoctor(doctor)
        queueSync("DOCTOR", doctor.id.toString(), "UPDATE", SyncSerializer.doctorToJson(doctor))
        dao.updateInvoiceCustomerDetailsForDoctor(
            doctorId = doctor.id,
            oldName = oldName,
            newName = doctor.doctorName,
            newAddress = doctor.address,
            newPhone = doctor.phoneNumber,
            qualification = doctor.qualification
        )
    }

    suspend fun deleteDoctor(id: Long) {
        val tomb = TombstoneEntity(
            entityType = "DOCTOR",
            entityId = id.toString(),
            deletedAt = System.currentTimeMillis(),
            deviceId = syncEngine?.getDeviceId() ?: "",
            userMobile = syncEngine?.getCurrentUserMobile() ?: ""
        )
        dao.insertTombstone(tomb)
        dao.deleteDoctorById(id)
        queueSync("DOCTOR", id.toString(), "DELETE", SyncSerializer.tombstoneToJson(tomb))
    }

    fun getAllPatients(): Flow<List<PatientEntity>> = dao.getAllPatients()

    fun searchPatients(query: String): Flow<List<PatientEntity>> = dao.searchPatients(query)

    suspend fun insertPatient(patient: PatientEntity): Long {
        val id = dao.insertPatient(patient)
        queueSync("PATIENT", id.toString(), "INSERT", SyncSerializer.patientToJson(patient.copy(id = id)))
        return id
    }

    suspend fun updatePatient(patient: PatientEntity) {
        dao.updatePatient(patient)
        queueSync("PATIENT", patient.id.toString(), "UPDATE", SyncSerializer.patientToJson(patient))
    }

    suspend fun updatePatientAndSyncInvoices(patient: PatientEntity, oldName: String) {
        dao.updatePatient(patient)
        queueSync("PATIENT", patient.id.toString(), "UPDATE", SyncSerializer.patientToJson(patient))
        dao.updateInvoiceCustomerDetailsForPatient(
            patientId = patient.id,
            oldName = oldName,
            newName = patient.patientName,
            newAddress = patient.address,
            newPhone = patient.phoneNumber,
            doctorName = patient.doctorName
        )
    }

    suspend fun deletePatient(id: Long) {
        val tomb = TombstoneEntity(
            entityType = "PATIENT",
            entityId = id.toString(),
            deletedAt = System.currentTimeMillis(),
            deviceId = syncEngine?.getDeviceId() ?: "",
            userMobile = syncEngine?.getCurrentUserMobile() ?: ""
        )
        dao.insertTombstone(tomb)
        dao.deletePatientById(id)
        queueSync("PATIENT", id.toString(), "DELETE", SyncSerializer.tombstoneToJson(tomb))
    }

    fun getAllInvoices(): Flow<List<InvoiceEntity>> = dao.getAllInvoices()

    fun getAllInvoiceItems(): Flow<List<InvoiceItemEntity>> = dao.getAllInvoiceItems()

    suspend fun getInvoice(invoiceNumber: Long): InvoiceEntity? = dao.getInvoiceByNumber(invoiceNumber)

    suspend fun getItemsForInvoice(invoiceNumber: Long): List<InvoiceItemEntity> = dao.getItemsForInvoice(invoiceNumber)

    fun getItemsForInvoiceFlow(invoiceNumber: Long): Flow<List<InvoiceItemEntity>> = dao.getItemsForInvoiceFlow(invoiceNumber)

    fun getInvoicesForCustomer(name: String): Flow<List<InvoiceEntity>> = dao.getInvoicesForCustomer(name)

    suspend fun getNextInvoiceNumber(): Long {
        val maxInv = dao.getMaxInvoiceNumber() ?: 16122L
        return if (maxInv < 16122L) 16123L else maxInv + 1L
    }

    suspend fun saveBill(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        dao.insertInvoice(invoice)
        dao.insertInvoiceItems(items)
        for (item in items) {
            dao.deductStock(item.medicineId, item.qty, item.freeQty)
            val st = StockTransactionEntity(
                medicineId = item.medicineId,
                productName = item.productName,
                companyName = item.manufacturer,
                type = "SALE",
                referenceInvoice = invoice.invoiceNumber,
                qty = item.qty,
                freeQty = item.freeQty,
                rate = item.price,
                amount = item.itemTotalAmount,
                timestamp = invoice.date,
                dateFormatted = invoice.dateFormatted
            )
            val stId = dao.insertStockTransaction(st)
            queueSync("STOCK_TRANSACTION", "${st.medicineId}_${st.timestamp}_SALE_${st.qty}", "INSERT", SyncSerializer.stockTransactionToJson(st.copy(id = stId)))
        }
        queueSync("INVOICE", invoice.invoiceNumber.toString(), "INSERT", SyncSerializer.invoiceToJson(invoice, items))
    }

    suspend fun editBill(updatedInvoice: InvoiceEntity, newItems: List<InvoiceItemEntity>) {
        val oldItems = dao.getItemsForInvoice(updatedInvoice.invoiceNumber)
        for (item in oldItems) {
            dao.reverseStock(item.medicineId, item.qty, item.freeQty)
        }
        dao.deleteStockTransactionsForInvoice(updatedInvoice.invoiceNumber)
        dao.deleteInvoiceItems(updatedInvoice.invoiceNumber)

        dao.updateInvoice(updatedInvoice)
        dao.insertInvoiceItems(newItems)
        for (item in newItems) {
            dao.deductStock(item.medicineId, item.qty, item.freeQty)
            val st = StockTransactionEntity(
                medicineId = item.medicineId,
                productName = item.productName,
                companyName = item.manufacturer,
                type = "SALE",
                referenceInvoice = updatedInvoice.invoiceNumber,
                qty = item.qty,
                freeQty = item.freeQty,
                rate = item.price,
                amount = item.itemTotalAmount,
                timestamp = updatedInvoice.date,
                dateFormatted = updatedInvoice.dateFormatted
            )
            val stId = dao.insertStockTransaction(st)
            queueSync("STOCK_TRANSACTION", "${st.medicineId}_${st.timestamp}_SALE_${st.qty}", "INSERT", SyncSerializer.stockTransactionToJson(st.copy(id = stId)))
        }
        queueSync("INVOICE", updatedInvoice.invoiceNumber.toString(), "UPDATE", SyncSerializer.invoiceToJson(updatedInvoice, newItems))
    }

    suspend fun deleteBill(invoiceNumber: Long) {
        val items = dao.getItemsForInvoice(invoiceNumber)
        for (item in items) {
            dao.reverseStock(item.medicineId, item.qty, item.freeQty)
        }
        dao.deleteStockTransactionsForInvoice(invoiceNumber)
        dao.deleteInvoiceItems(invoiceNumber)
        dao.deleteInvoice(invoiceNumber)
        val tomb = TombstoneEntity(
            entityType = "INVOICE",
            entityId = invoiceNumber.toString(),
            deletedAt = System.currentTimeMillis(),
            deviceId = syncEngine?.getDeviceId() ?: "",
            userMobile = syncEngine?.getCurrentUserMobile() ?: ""
        )
        dao.insertTombstone(tomb)
        queueSync("INVOICE", invoiceNumber.toString(), "DELETE", SyncSerializer.tombstoneToJson(tomb))
    }

    suspend fun recordPayment(invoiceNumber: Long, paymentAmount: Double) {
        val invoice = dao.getInvoiceByNumber(invoiceNumber) ?: return
        val newPaid = invoice.paidAmount + paymentAmount
        val newDue = (invoice.netAmount - newPaid).coerceAtLeast(0.0)
        dao.updatePayment(invoiceNumber, newPaid, newDue)
        val items = dao.getItemsForInvoice(invoiceNumber)
        val updatedInv = invoice.copy(paidAmount = newPaid, dueAmount = newDue)
        queueSync("INVOICE", invoiceNumber.toString(), "UPDATE", SyncSerializer.invoiceToJson(updatedInv, items))
    }

    fun getAllStockTransactions(): Flow<List<StockTransactionEntity>> = dao.getAllStockTransactions()

    suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<StockTransactionEntity> =
        dao.getTransactionsBetween(startTime, endTime)

    // Admin Users (Max 10 sub-admins)
    fun getAllAdminUsers(): Flow<List<AdminUserEntity>> = dao.getAllAdminUsers()

    suspend fun getAdminCount(): Int = dao.getAdminCount()

    suspend fun findAdminUser(mobile: String, pass: String): AdminUserEntity? =
        dao.findAdminUser(mobile.trim(), pass.trim())

    suspend fun createAdminUser(name: String, mobile: String, pass: String, creatorMobile: String): Boolean {
        val currentCount = dao.getAdminCount()
        if (currentCount >= 10) return false
        val entity = AdminUserEntity(
            name = name.trim(),
            mobileNumber = mobile.trim(),
            password = pass.trim(),
            createdByMobile = creatorMobile.trim(),
            createdAt = System.currentTimeMillis()
        )
        val id = dao.insertAdminUser(entity)
        queueSync("ADMIN_USER", id.toString(), "INSERT", SyncSerializer.adminUserToJson(entity.copy(id = id)))
        return true
    }

    suspend fun updateAdminUser(admin: AdminUserEntity) {
        dao.updateAdminUser(admin)
        queueSync("ADMIN_USER", admin.id.toString(), "UPDATE", SyncSerializer.adminUserToJson(admin))
    }

    suspend fun deleteAdminUser(id: Long) {
        val tomb = TombstoneEntity(
            entityType = "ADMIN_USER",
            entityId = id.toString(),
            deletedAt = System.currentTimeMillis(),
            deviceId = syncEngine?.getDeviceId() ?: "",
            userMobile = syncEngine?.getCurrentUserMobile() ?: ""
        )
        dao.insertTombstone(tomb)
        dao.deleteAdminUser(id)
        queueSync("ADMIN_USER", id.toString(), "DELETE", SyncSerializer.tombstoneToJson(tomb))
    }

    // Guest Logins Tracking & Multi-Device Sync
    fun getAllGuestLogins(): Flow<List<GuestLoginEntity>> = dao.getAllGuestLogins()

    suspend fun recordGuestLogin(mobile: String): Long {
        val timestamp = System.currentTimeMillis()
        val entry = GuestLoginEntity(
            mobileNumber = mobile.trim(),
            loginTimestamp = timestamp
        )
        val id = dao.insertGuestLogin(entry)
        val fullEntry = entry.copy(id = id)
        queueSync("GUEST_LOGIN", "${mobile.trim()}_$timestamp", "INSERT", SyncSerializer.guestLoginToJson(fullEntry))
        syncEngine?.triggerAutomaticSync()
        return id
    }

    suspend fun deleteGuestLogin(id: Long) {
        val tomb = TombstoneEntity(
            entityType = "GUEST_LOGIN",
            entityId = id.toString(),
            deletedAt = System.currentTimeMillis(),
            deviceId = syncEngine?.getDeviceId() ?: "",
            userMobile = syncEngine?.getCurrentUserMobile() ?: ""
        )
        dao.insertTombstone(tomb)
        dao.deleteGuestLogin(id)
        queueSync("GUEST_LOGIN", id.toString(), "DELETE", SyncSerializer.tombstoneToJson(tomb))
        syncEngine?.triggerAutomaticSync()
    }

    suspend fun clearGuestLogins() = dao.clearGuestLogins()
}
