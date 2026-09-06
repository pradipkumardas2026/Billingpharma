package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface PharmaDao {

    // Settings
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SettingsEntity)

    // Medicines
    @Query("SELECT * FROM medicines ORDER BY productName ASC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE productName LIKE '%' || :query || '%' ORDER BY productName ASC")
    fun searchMedicines(query: String): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteMedicineById(id: Long)

    @Query("UPDATE medicines SET stockQuantity = stockQuantity - :qty, freeQuantity = freeQuantity - :freeQty WHERE id = :id")
    suspend fun deductStock(id: Long, qty: Int, freeQty: Int)

    @Query("UPDATE medicines SET stockQuantity = stockQuantity + :qty, freeQuantity = freeQuantity + :freeQty WHERE id = :id")
    suspend fun reverseStock(id: Long, qty: Int, freeQty: Int)

    // Parties
    @Query("SELECT * FROM parties ORDER BY partyName ASC")
    fun getAllParties(): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE partyName LIKE '%' || :query || '%' ORDER BY partyName ASC")
    fun searchParties(query: String): Flow<List<PartyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: PartyEntity): Long

    @Update
    suspend fun updateParty(party: PartyEntity)

    @Query("DELETE FROM parties WHERE id = :id")
    suspend fun deletePartyById(id: Long)

    // Doctors
    @Query("SELECT * FROM doctors ORDER BY doctorName ASC")
    fun getAllDoctors(): Flow<List<DoctorEntity>>

    @Query("SELECT * FROM doctors WHERE doctorName LIKE '%' || :query || '%' ORDER BY doctorName ASC")
    fun searchDoctors(query: String): Flow<List<DoctorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: DoctorEntity): Long

    @Update
    suspend fun updateDoctor(doctor: DoctorEntity)

    @Query("DELETE FROM doctors WHERE id = :id")
    suspend fun deleteDoctorById(id: Long)

    // Patients
    @Query("SELECT * FROM patients ORDER BY patientName ASC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE patientName LIKE '%' || :query || '%' ORDER BY patientName ASC")
    fun searchPatients(query: String): Flow<List<PatientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deletePatientById(id: Long)

    // Invoices
    @Query("SELECT * FROM invoices ORDER BY invoiceNumber DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(invoiceNumber: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE customerName = :customerName ORDER BY invoiceNumber DESC")
    fun getInvoicesForCustomer(customerName: String): Flow<List<InvoiceEntity>>

    @Query("SELECT COALESCE(MAX(invoiceNumber), 16122) FROM invoices")
    suspend fun getMaxInvoiceNumber(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices WHERE invoiceNumber = :invoiceNumber")
    suspend fun deleteInvoice(invoiceNumber: Long)

    // Invoice Items
    @Query("SELECT * FROM invoice_items WHERE invoiceNumber = :invoiceNumber ORDER BY slNo ASC")
    fun getItemsForInvoiceFlow(invoiceNumber: Long): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceNumber = :invoiceNumber ORDER BY slNo ASC")
    suspend fun getItemsForInvoice(invoiceNumber: Long): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items ORDER BY invoiceNumber DESC, slNo ASC")
    fun getAllInvoiceItems(): Flow<List<InvoiceItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Query("DELETE FROM invoice_items WHERE invoiceNumber = :invoiceNumber")
    suspend fun deleteInvoiceItems(invoiceNumber: Long)

    // Stock Transactions
    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    fun getAllStockTransactions(): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<StockTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockTransaction(transaction: StockTransactionEntity): Long

    @Query("DELETE FROM stock_transactions WHERE referenceInvoice = :invoiceNumber")
    suspend fun deleteStockTransactionsForInvoice(invoiceNumber: Long)

    @Query("DELETE FROM stock_transactions WHERE medicineId = :medicineId")
    suspend fun deleteStockTransactionsForMedicine(medicineId: Long)

    @Query("UPDATE invoices SET paidAmount = :paidAmount, dueAmount = :dueAmount WHERE invoiceNumber = :invoiceNumber")
    suspend fun updatePayment(invoiceNumber: Long, paidAmount: Double, dueAmount: Double)

    @Query("""
        UPDATE invoices 
        SET customerName = :newName, customerAddress = :newAddress, customerDl = :newDl, customerGstPan = :newGstPan, customerPhone = :newPhone 
        WHERE (customerType = 'PARTY' AND customerId = :partyId) OR (customerName = :oldName AND :oldName != '')
    """)
    suspend fun updateInvoiceCustomerDetailsForParty(
        partyId: Long,
        oldName: String,
        newName: String,
        newAddress: String,
        newDl: String,
        newGstPan: String,
        newPhone: String
    )

    @Query("""
        UPDATE invoices 
        SET customerName = :newName, customerAddress = :newAddress, customerPhone = :newPhone, doctorDetails = :qualification 
        WHERE (customerType = 'DOCTOR' AND customerId = :doctorId) OR (customerName = :oldName AND :oldName != '')
    """)
    suspend fun updateInvoiceCustomerDetailsForDoctor(
        doctorId: Long,
        oldName: String,
        newName: String,
        newAddress: String,
        newPhone: String,
        qualification: String
    )

    @Query("""
        UPDATE invoices 
        SET customerName = :newName, customerAddress = :newAddress, customerPhone = :newPhone, doctorDetails = :doctorName 
        WHERE (customerType = 'PATIENT' AND customerId = :patientId) OR (customerName = :oldName AND :oldName != '')
    """)
    suspend fun updateInvoiceCustomerDetailsForPatient(
        patientId: Long,
        oldName: String,
        newName: String,
        newAddress: String,
        newPhone: String,
        doctorName: String
    )

    // Admin Users Management (Max 10 sub-admins)
    @Query("SELECT * FROM admin_users ORDER BY id ASC")
    fun getAllAdminUsers(): Flow<List<AdminUserEntity>>

    @Query("SELECT COUNT(*) FROM admin_users")
    suspend fun getAdminCount(): Int

    @Query("SELECT * FROM admin_users WHERE mobileNumber = :mobile AND password = :pass LIMIT 1")
    suspend fun findAdminUser(mobile: String, pass: String): AdminUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminUser(admin: AdminUserEntity): Long

    @Update
    suspend fun updateAdminUser(admin: AdminUserEntity)

    @Query("DELETE FROM admin_users WHERE id = :id")
    suspend fun deleteAdminUser(id: Long)

    // Guest Logins Tracking
    @Query("SELECT * FROM guest_logins ORDER BY loginTimestamp DESC")
    fun getAllGuestLogins(): Flow<List<GuestLoginEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuestLogin(login: GuestLoginEntity): Long

    @Query("SELECT * FROM guest_logins WHERE mobileNumber = :mobile AND loginTimestamp = :timestamp LIMIT 1")
    suspend fun findGuestLogin(mobile: String, timestamp: Long): GuestLoginEntity?

    @Query("DELETE FROM guest_logins WHERE id = :id")
    suspend fun deleteGuestLogin(id: Long)

    @Query("DELETE FROM guest_logins")
    suspend fun clearGuestLogins()

    // Sync Operations Queue (Offline-First Multi-Device Synchronization)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncOperation(op: SyncOperationEntity): Long

    @Update
    suspend fun updateSyncOperation(op: SyncOperationEntity)

    @Query("SELECT * FROM sync_operations WHERE status IN ('PENDING', 'FAILED') ORDER BY timestamp ASC")
    suspend fun getPendingSyncOperations(): List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations ORDER BY timestamp DESC LIMIT 50")
    fun getAllSyncOperationsFlow(): Flow<List<SyncOperationEntity>>

    @Query("SELECT COUNT(*) FROM sync_operations WHERE status IN ('PENDING', 'FAILED')")
    fun getPendingSyncCountFlow(): Flow<Int>

    @Query("DELETE FROM sync_operations WHERE id = :id")
    suspend fun deleteSyncOperation(id: Long)

    @Query("UPDATE sync_operations SET status = :status, errorMessage = :error, retryCount = :retryCount WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, error: String?, retryCount: Int)

    @Query("DELETE FROM sync_operations WHERE status = 'SYNCED'")
    suspend fun clearSyncedOperations()

    // Deduplication & Conflict-Resolution Lookups
    @Query("SELECT * FROM stock_transactions WHERE medicineId = :medicineId AND timestamp = :timestamp AND type = :type AND qty = :qty LIMIT 1")
    suspend fun findStockTransaction(medicineId: Long, timestamp: Long, type: String, qty: Int): StockTransactionEntity?

    @Query("SELECT * FROM medicines WHERE productName = :name AND batchNumber = :batch LIMIT 1")
    suspend fun findMedicineByNameAndBatch(name: String, batch: String): MedicineEntity?

    @Query("SELECT * FROM parties WHERE partyName = :name AND contactNumber = :phone LIMIT 1")
    suspend fun findPartyByNameAndPhone(name: String, phone: String): PartyEntity?

    @Query("SELECT * FROM doctors WHERE doctorName = :name AND phoneNumber = :phone LIMIT 1")
    suspend fun findDoctorByNameAndPhone(name: String, phone: String): DoctorEntity?

    @Query("SELECT * FROM patients WHERE patientName = :name AND phoneNumber = :phone LIMIT 1")
    suspend fun findPatientByNameAndPhone(name: String, phone: String): PatientEntity?

    @Query("SELECT * FROM admin_users WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun findAdminByMobile(mobile: String): AdminUserEntity?

    // Sync Deletion Tombstones (Prevents Deleted Records from Reappearing)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstone(tombstone: TombstoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstones(tombstones: List<TombstoneEntity>)

    @Query("SELECT * FROM sync_tombstones WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun getTombstone(entityType: String, entityId: String): TombstoneEntity?

    @Query("SELECT COUNT(*) FROM sync_tombstones WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun isTombstoned(entityType: String, entityId: String): Int

    @Query("SELECT * FROM sync_tombstones")
    suspend fun getAllTombstones(): List<TombstoneEntity>

    @Query("DELETE FROM sync_tombstones WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteTombstone(entityType: String, entityId: String)
}
