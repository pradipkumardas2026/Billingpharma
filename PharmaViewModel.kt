package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.PharmaRepository
import com.example.data.sync.AppUpdateState
import com.example.data.sync.FirebaseSyncEngine
import com.example.data.sync.SyncInfo
import com.example.data.sync.SyncState
import com.example.util.DateUtils
import com.example.util.KhataRow
import com.example.util.NetworkMonitor
import com.example.util.NumberToWords
import com.example.util.StockStatementRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

class PharmaViewModel(application: Application) : AndroidViewModel(application) {
    val networkMonitor: NetworkMonitor
    val syncEngine: FirebaseSyncEngine
    val syncInfo: StateFlow<SyncInfo>
    val repository: PharmaRepository

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _selectedCustomerNameForHistory = MutableStateFlow<String?>(null)
    val selectedCustomerNameForHistory: StateFlow<String?> = _selectedCustomerNameForHistory.asStateFlow()

    private val _selectedCustomerTypeForHistory = MutableStateFlow<String?>(null)
    val selectedCustomerTypeForHistory: StateFlow<String?> = _selectedCustomerTypeForHistory.asStateFlow()

    val salesHistoryTargetCustomer: StateFlow<String?> = _selectedCustomerNameForHistory.asStateFlow()

    val settings: StateFlow<SettingsEntity>
    val medicines: StateFlow<List<MedicineEntity>>
    val parties: StateFlow<List<PartyEntity>>
    val doctors: StateFlow<List<DoctorEntity>>
    val patients: StateFlow<List<PatientEntity>>
    val invoices: StateFlow<List<InvoiceEntity>>
    val invoiceItems: StateFlow<List<InvoiceItemEntity>>
    val stockTransactions: StateFlow<List<StockTransactionEntity>>
    val adminUsers: StateFlow<List<AdminUserEntity>>
    val guestLogins: StateFlow<List<GuestLoginEntity>>
    val purchaseInvoices: StateFlow<List<PurchaseInvoiceEntity>>
    val appUpdateState: StateFlow<AppUpdateState>

    val editingInvoiceNumber = MutableStateFlow<Long?>(null)
    val billCustomerType = MutableStateFlow("PARTY")
    val selectedParty = MutableStateFlow<PartyEntity?>(null)
    val selectedDoctor = MutableStateFlow<DoctorEntity?>(null)
    val selectedPatient = MutableStateFlow<PatientEntity?>(null)

    // Manual override fields for current bill customer
    val customCustomerName = MutableStateFlow("")
    val customCustomerPhone = MutableStateFlow("")
    val customCustomerAddress = MutableStateFlow("")
    val customCustomerDl = MutableStateFlow("")
    val customCustomerGst = MutableStateFlow("")
    val customDoctorName = MutableStateFlow("")

    val billItems = MutableStateFlow<List<BillDraftItem>>(emptyList())
    val adjustmentAmount = MutableStateFlow(0.0)
    val billPaidAmount = MutableStateFlow(0.0)
    val billNote = MutableStateFlow("")

    // Derived states for billing UI
    val billGrossAmount: StateFlow<Double> = billItems.map { items ->
        items.sumOf { it.price * it.qty }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val billDiscountAmount: StateFlow<Double> = billItems.map { items ->
        items.sumOf { it.price * (it.discountPercent / 100.0) * it.qty }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val billTotalGst: StateFlow<Double> = billItems.map { items ->
        items.sumOf { item ->
            val totalGstPct = item.sgstPercent + item.cgstPercent
            val discounted = item.price * (1.0 - (item.discountPercent / 100.0))
            (discounted * (totalGstPct / 100.0)) * item.qty
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val billRoundOff: StateFlow<Double> = adjustmentAmount.asStateFlow()

    val billNetAmount: StateFlow<Double> = combine(billItems, adjustmentAmount) { items, adj ->
        val total = items.sumOf { it.itemTotalAmount }
        (total - adj).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val billDueAmount: StateFlow<Double> = combine(billNetAmount, billPaidAmount) { net, paid ->
        (net - paid).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val billCustomerName: StateFlow<String> = combine(
        billCustomerType, selectedParty, selectedDoctor, selectedPatient, customCustomerName
    ) { type, p, d, pat, custom ->
        if (custom.isNotBlank()) custom
        else when (type) {
            "DOCTOR" -> d?.doctorName ?: ""
            "PATIENT" -> pat?.patientName ?: ""
            else -> p?.partyName ?: ""
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val billCustomerPhone: StateFlow<String> = combine(
        billCustomerType, selectedParty, selectedDoctor, selectedPatient, customCustomerPhone
    ) { type, p, d, pat, custom ->
        if (custom.isNotBlank()) custom
        else when (type) {
            "DOCTOR" -> d?.phoneNumber ?: ""
            "PATIENT" -> pat?.phoneNumber ?: ""
            else -> p?.contactNumber ?: ""
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val billCustomerAddress: StateFlow<String> = combine(
        billCustomerType, selectedParty, selectedDoctor, selectedPatient, customCustomerAddress
    ) { type, p, d, pat, custom ->
        if (custom.isNotBlank()) custom
        else when (type) {
            "DOCTOR" -> d?.address ?: ""
            "PATIENT" -> pat?.address ?: ""
            else -> p?.address ?: ""
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val billCustomerDl: StateFlow<String> = combine(
        billCustomerType, selectedParty, customCustomerDl
    ) { type, p, custom ->
        if (custom.isNotBlank()) custom
        else if (type == "PARTY") p?.dlNumber ?: "" else ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val billCustomerGst: StateFlow<String> = combine(
        billCustomerType, selectedParty, customCustomerGst
    ) { type, p, custom ->
        if (custom.isNotBlank()) custom
        else if (type == "PARTY") p?.gstPanNumber ?: "" else ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val billDoctorName: StateFlow<String> = combine(
        billCustomerType, selectedDoctor, selectedPatient, customDoctorName
    ) { type, d, pat, custom ->
        if (custom.isNotBlank()) custom
        else when (type) {
            "DOCTOR" -> d?.doctorName ?: ""
            "PATIENT" -> pat?.doctorName ?: ""
            else -> ""
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        val db = AppDatabase.getInstance(application)
        networkMonitor = NetworkMonitor(application)
        syncEngine = FirebaseSyncEngine(application, db.pharmaDao(), networkMonitor)
        syncInfo = syncEngine.syncInfo
        repository = PharmaRepository(db.pharmaDao(), syncEngine)

        settings = repository.settingsFlow
            .filterNotNull()
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                SettingsEntity(1)
            )

        medicines = repository.getAllMedicines()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        parties = repository.getAllParties()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        doctors = repository.getAllDoctors()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        patients = repository.getAllPatients()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        invoices = repository.getAllInvoices()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        invoiceItems = repository.getAllInvoiceItems()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        stockTransactions = repository.getAllStockTransactions()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        adminUsers = repository.getAllAdminUsers()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        guestLogins = repository.getAllGuestLogins()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        purchaseInvoices = repository.getAllPurchaseInvoices()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        appUpdateState = repository.appUpdateState

        // Ensure default Master Admin credentials 9002625428 / 654321 if old template defaults are present
        viewModelScope.launch {
            repository.settingsFlow.filterNotNull().first().let { current ->
                if (current.adminMobile == "9876543210" || current.adminPassword == "admin" || current.adminMobile.isBlank()) {
                    val updated = current.copy(
                        adminMobile = "9002625428",
                        adminPassword = "654321"
                    )
                    repository.updateSettings(updated)
                }
            }
        }
    }

    fun loginAdmin(mobile: String, pass: String): Boolean {
        val m = mobile.trim()
        val p = pass.trim()
        val currentSettings = settings.value
        val validMobile = currentSettings.adminMobile.ifBlank { "9002625428" }
        val validPass = currentSettings.adminPassword.ifBlank { "654321" }
        
        // 1. Check Master Admin
        if ((m == "9002625428" && p == "654321") || (m == validMobile && p == validPass)) {
            _authState.value = AuthState(
                role = UserRole.Admin(
                    mobile = m,
                    isMasterAdmin = true,
                    adminName = "Master Admin"
                )
            )
            _currentTab.value = NavigationTab.HOME
            return true
        }

        // 2. Check Sub-Admins locally
        val subAdmin = adminUsers.value.find { it.mobileNumber == m && it.password == p }
        if (subAdmin != null) {
            _authState.value = AuthState(
                role = UserRole.Admin(
                    mobile = m,
                    isMasterAdmin = false,
                    adminName = subAdmin.name.ifBlank { "Admin" }
                )
            )
            _currentTab.value = NavigationTab.HOME
            return true
        }

        // 3. Fallback: Check Cloud Firestore for newly created sub-admin from another device
        if (networkMonitor.isOnline.value) {
            try {
                val cloudAdmin = runBlocking(Dispatchers.IO) {
                    syncEngine.lookupSubAdminFromCloud(m, p)
                }
                if (cloudAdmin != null) {
                    _authState.value = AuthState(
                        role = UserRole.Admin(
                            mobile = m,
                            isMasterAdmin = false,
                            adminName = cloudAdmin.name.ifBlank { "Admin" }
                        )
                    )
                    _currentTab.value = NavigationTab.HOME
                    return true
                }
            } catch (e: Exception) {
                android.util.Log.w("PharmaViewModel", "Cloud admin lookup error: ${e.message}")
            }
        }

        return false
    }

    fun loginGuest(mobile: String): Boolean {
        val cleanDigits = mobile.trim().filter { it.isDigit() }
        if (cleanDigits.length != 10) {
            return false
        }
        _authState.value = AuthState(role = UserRole.Guest(cleanDigits))
        _currentTab.value = NavigationTab.HOME
        viewModelScope.launch {
            repository.recordGuestLogin(cleanDigits)
        }
        return true
    }

    fun createSubAdmin(name: String, mobile: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val count = adminUsers.value.size
            if (count >= 10) {
                onResult(false, "Maximum limit of 10 sub-admins reached!")
                return@launch
            }
            val m = mobile.trim()
            val p = pass.trim()
            if (m.length < 10) {
                onResult(false, "Please enter a valid 10-digit mobile number.")
                return@launch
            }
            if (p.length < 4) {
                onResult(false, "Password must be at least 4 characters.")
                return@launch
            }
            if (m == "9002625428" || m == settings.value.adminMobile || adminUsers.value.any { it.mobileNumber == m }) {
                onResult(false, "This mobile number is already registered as an admin!")
                return@launch
            }
            val creatorMobile = (_authState.value.role as? UserRole.Admin)?.mobile ?: "9002625428"
            val success = repository.createAdminUser(
                name = name.trim().ifBlank { "Admin ${count + 1}" },
                mobile = m,
                pass = p,
                creatorMobile = creatorMobile
            )
            if (success) {
                onResult(true, "New Admin created successfully! (Mobile: $m)")
            } else {
                onResult(false, "Failed to create Admin. Limit reached.")
            }
        }
    }

    fun updateSubAdmin(admin: AdminUserEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val m = admin.mobileNumber.trim()
            val p = admin.password.trim()
            if (m.length < 10) {
                onResult(false, "Please enter a valid 10-digit mobile number.")
                return@launch
            }
            if (p.length < 4) {
                onResult(false, "Password must be at least 4 characters.")
                return@launch
            }
            if (m == "9002625428" || m == settings.value.adminMobile) {
                onResult(false, "Cannot use Master Admin's mobile number for a sub-admin!")
                return@launch
            }
            if (adminUsers.value.any { it.id != admin.id && it.mobileNumber == m }) {
                onResult(false, "This mobile number is already used by another admin!")
                return@launch
            }
            repository.updateAdminUser(admin.copy(name = admin.name.trim(), mobileNumber = m, password = p))
            onResult(true, "Admin credentials updated! New login is active.")
        }
    }

    fun deleteSubAdmin(id: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.deleteAdminUser(id)
            onResult(true)
        }
    }

    fun deleteGuestLogin(id: Long) {
        viewModelScope.launch {
            repository.deleteGuestLogin(id)
        }
    }

    fun clearGuestLogins() {
        viewModelScope.launch {
            repository.clearGuestLogins()
        }
    }

    fun logout() {
        _authState.value = AuthState()
    }

    fun checkForgotPasswordUser(mobile: String): ForgotPasswordResult {
        val cleanMobile = mobile.trim()
        val currentSettings = settings.value
        val masterMobile = currentSettings.adminMobile.ifBlank { "9002625428" }
        if (cleanMobile == "9002625428" || cleanMobile == masterMobile) {
            val otp = String.format("%04d", Random.nextInt(1000, 9999))
            _authState.value = _authState.value.copy(
                isOtpSent = true,
                currentGeneratedOtp = otp,
                otpTargetMobile = cleanMobile
            )
            return ForgotPasswordResult.MasterAdminOtp(otp, cleanMobile)
        }
        val subAdmin = adminUsers.value.firstOrNull { it.mobileNumber.trim() == cleanMobile }
        if (subAdmin != null) {
            return ForgotPasswordResult.SubAdminNotice(subAdmin.name, cleanMobile)
        }
        return ForgotPasswordResult.NotFound
    }

    fun requestForgotPasswordOtp(mobile: String): String? {
        val res = checkForgotPasswordUser(mobile)
        return if (res is ForgotPasswordResult.MasterAdminOtp) res.otp else null
    }

    fun verifyOtpAndResetPassword(mobile: String, enteredOtp: String, newPassword: String): Boolean {
        if (_authState.value.isOtpSent &&
            _authState.value.otpTargetMobile == mobile.trim() &&
            _authState.value.currentGeneratedOtp == enteredOtp.trim()
        ) {
            viewModelScope.launch {
                val updated = settings.value.copy(adminPassword = newPassword.trim())
                repository.updateSettings(updated)
            }
            _authState.value = _authState.value.copy(isOtpSent = false, currentGeneratedOtp = "")
            return true
        }
        return false
    }

    fun switchTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun selectCustomerForHistory(customerName: String?, customerType: String?) {
        _selectedCustomerNameForHistory.value = customerName
        _selectedCustomerTypeForHistory.value = customerType
    }

    fun navigateToSalesHistory(customerName: String? = null, customerType: String? = null) {
        _selectedCustomerNameForHistory.value = customerName
        _selectedCustomerTypeForHistory.value = customerType
        _currentTab.value = NavigationTab.SALES_HISTORY
    }

    fun addOrUpdateMedicine(medicine: MedicineEntity) {
        viewModelScope.launch {
            if (medicine.id == 0L) {
                repository.insertMedicine(medicine)
            } else {
                repository.updateMedicine(medicine)
            }
        }
    }

    fun addStockToExistingMedicine(
        medicineId: Long,
        addedQty: Int,
        addedFreeQty: Int,
        newBatch: String,
        newExpiry: String,
        newPurchaseRate: Double?,
        newMrp: Double?,
        newSaleRate: Double?
    ) {
        viewModelScope.launch {
            repository.addStockToMedicine(
                medicineId = medicineId,
                addedQty = addedQty,
                addedFreeQty = addedFreeQty,
                newBatch = newBatch,
                newExpiry = newExpiry,
                newPurchaseRate = newPurchaseRate,
                newMrp = newMrp,
                newSaleRate = newSaleRate
            )
        }
    }

    fun deleteMedicine(id: Long) {
        viewModelScope.launch {
            repository.deleteMedicine(id)
        }
    }

    fun addOrUpdateParty(party: PartyEntity) {
        viewModelScope.launch {
            if (party.id == 0L) {
                repository.insertParty(party)
            } else {
                repository.updateParty(party)
            }
        }
    }

    fun updatePartyAndKhata(party: PartyEntity, oldName: String = party.partyName) {
        viewModelScope.launch {
            if (party.id == 0L) {
                val newId = repository.insertParty(party)
                selectedParty.value = party.copy(id = newId)
            } else {
                repository.updatePartyAndSyncInvoices(party, oldName)
                selectedParty.value = party
            }
        }
    }

    fun deleteParty(id: Long) {
        viewModelScope.launch {
            repository.deleteParty(id)
            if (selectedParty.value?.id == id) {
                selectedParty.value = null
            }
        }
    }

    fun addOrUpdateDoctor(doctor: DoctorEntity) {
        viewModelScope.launch {
            if (doctor.id == 0L) {
                repository.insertDoctor(doctor)
            } else {
                repository.updateDoctor(doctor)
            }
        }
    }

    fun updateDoctorAndKhata(doctor: DoctorEntity, oldName: String = doctor.doctorName) {
        viewModelScope.launch {
            if (doctor.id == 0L) {
                val newId = repository.insertDoctor(doctor)
                selectedDoctor.value = doctor.copy(id = newId)
            } else {
                repository.updateDoctorAndSyncInvoices(doctor, oldName)
                selectedDoctor.value = doctor
            }
        }
    }

    fun deleteDoctor(id: Long) {
        viewModelScope.launch {
            repository.deleteDoctor(id)
            if (selectedDoctor.value?.id == id) {
                selectedDoctor.value = null
            }
        }
    }

    fun addOrUpdatePatient(patient: PatientEntity) {
        viewModelScope.launch {
            if (patient.id == 0L) {
                repository.insertPatient(patient)
            } else {
                repository.updatePatient(patient)
            }
        }
    }

    fun updatePatientAndKhata(patient: PatientEntity, oldName: String = patient.patientName) {
        viewModelScope.launch {
            if (patient.id == 0L) {
                val newId = repository.insertPatient(patient)
                selectedPatient.value = patient.copy(id = newId)
            } else {
                repository.updatePatientAndSyncInvoices(patient, oldName)
                selectedPatient.value = patient
            }
        }
    }

    fun deletePatient(id: Long) {
        viewModelScope.launch {
            repository.deletePatient(id)
            if (selectedPatient.value?.id == id) {
                selectedPatient.value = null
            }
        }
    }

    // Purchase Invoice & Purchase GST operations
    fun addOrUpdatePurchaseInvoice(purchase: PurchaseInvoiceEntity) {
        viewModelScope.launch {
            if (purchase.id == 0L) {
                repository.insertPurchaseInvoice(purchase)
            } else {
                repository.updatePurchaseInvoice(purchase)
            }
        }
    }

    fun deletePurchaseInvoice(id: Long) {
        viewModelScope.launch {
            repository.deletePurchaseInvoice(id)
        }
    }

    fun updateSettings(newSettings: SettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }

    fun resetBillDraft() {
        editingInvoiceNumber.value = null
        billCustomerType.value = "PARTY"
        selectedParty.value = null
        selectedDoctor.value = null
        selectedPatient.value = null
        customCustomerName.value = ""
        customCustomerPhone.value = ""
        customCustomerAddress.value = ""
        customCustomerDl.value = ""
        customCustomerGst.value = ""
        customDoctorName.value = ""
        billItems.value = emptyList()
        adjustmentAmount.value = 0.0
        billPaidAmount.value = 0.0
        billNote.value = ""
    }

    fun setCustomerType(type: String) {
        billCustomerType.value = type
    }

    fun setPaidAmount(amount: Double) {
        billPaidAmount.value = amount
    }

    fun setAdjustmentAmount(amount: Double) {
        adjustmentAmount.value = amount
    }

    fun setBillNote(note: String) {
        billNote.value = note
    }

    fun setCustomer(
        name: String = "",
        phone: String = "",
        address: String = "",
        dl: String = "",
        gst: String = "",
        doctor: String = ""
    ) {
        customCustomerName.value = name
        customCustomerPhone.value = phone
        customCustomerAddress.value = address
        customCustomerDl.value = dl
        customCustomerGst.value = gst
        customDoctorName.value = doctor
    }

    fun updateCustomerDetails(
        name: String? = null,
        phone: String? = null,
        address: String? = null,
        dl: String? = null,
        gst: String? = null,
        doctor: String? = null
    ) {
        name?.let { customCustomerName.value = it }
        phone?.let { customCustomerPhone.value = it }
        address?.let { customCustomerAddress.value = it }
        dl?.let { customCustomerDl.value = it }
        gst?.let { customCustomerGst.value = it }
        doctor?.let { customDoctorName.value = it }
    }

    fun getItemsForInvoiceFlow(invoiceNumber: Long): Flow<List<InvoiceItemEntity>> = repository.getItemsForInvoiceFlow(invoiceNumber)
    suspend fun getItemsForInvoice(invoiceNumber: Long): List<InvoiceItemEntity> = repository.getItemsForInvoice(invoiceNumber)

    fun saveOrFinalizeCurrentBill(onSaved: (InvoiceEntity?) -> Unit) {
        viewModelScope.launch {
            val num = saveOrFinalizeCurrentBill()
            val inv = num?.let { repository.getInvoice(it) }
            onSaved(inv)
        }
    }

    fun saveOrFinalizeCurrentBillWithItems(onSaved: (InvoiceEntity?, List<InvoiceItemEntity>) -> Unit) {
        viewModelScope.launch {
            val itemsSnapshot = billItems.value
            val num = saveOrFinalizeCurrentBill()
            val inv = num?.let { repository.getInvoice(it) }
            val savedItems = if (num != null) repository.getItemsForInvoice(num) else emptyList()
            val finalItems = if (savedItems.isNotEmpty()) savedItems else {
                itemsSnapshot.mapIndexed { index, item ->
                    InvoiceItemEntity(
                        id = 0L,
                        invoiceNumber = num ?: 0L,
                        slNo = index + 1,
                        medicineId = item.medicineId,
                        productName = item.productName,
                        manufacturer = item.manufacturer,
                        pack = item.pack,
                        batchNo = item.batchNo,
                        expDate = item.expDate,
                        qty = item.qty,
                        freeQty = item.freeQty,
                        mrp = item.mrp,
                        price = item.price,
                        discountPercent = item.discountPercent,
                        bonusPercent = item.bonusPercent,
                        sgstPercent = item.sgstPercent,
                        cgstPercent = item.cgstPercent,
                        netRate = item.netRate,
                        itemTotalAmount = item.itemTotalAmount
                    )
                }
            }
            onSaved(inv, finalItems)
        }
    }

    fun addMedicineToBill(medicine: MedicineEntity) {
        val current = billItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.medicineId == medicine.id }
        if (existingIndex >= 0) {
            val item = current[existingIndex]
            updateBillItem(
                index = existingIndex,
                newQty = item.qty + 1,
                newFree = item.freeQty,
                newPrice = item.price,
                newDiscount = item.discountPercent,
                newSgst = item.sgstPercent,
                newCgst = item.cgstPercent
            )
            return
        }
        val sgst = medicine.gstPercent / 2.0
        val cgst = medicine.gstPercent / 2.0
        val disc = 0.0
        val discounted = medicine.price * (1.0 - (disc / 100.0))
        val netRate = discounted * (1.0 + (medicine.gstPercent / 100.0))
        val total = 1.0 * netRate
        val bonus = if (medicine.stockQuantity > 0) {
            (medicine.freeQuantity.toDouble() / medicine.stockQuantity) * 10.0
        } else 0.0
        val pack = if (medicine.packagingType.isBlank()) {
            if (medicine.productType == "Syrup") "100 ml" else "1×10T"
        } else medicine.packagingType

        val newItem = BillDraftItem(
            tempId = System.nanoTime(),
            medicineId = medicine.id,
            productName = medicine.productName,
            manufacturer = medicine.manufacturerName,
            pack = pack,
            batchNo = medicine.batchNumber,
            expDate = medicine.expiryDate,
            qty = 1,
            freeQty = 0,
            mrp = medicine.mrp,
            price = medicine.price,
            discountPercent = disc,
            bonusPercent = bonus,
            sgstPercent = sgst,
            cgstPercent = cgst,
            netRate = netRate,
            itemTotalAmount = total
        )
        billItems.value = billItems.value + newItem
    }

    fun addMedicineToBillWithOptions(
        medicine: MedicineEntity,
        qty: Int,
        freeQty: Int = 0,
        price: Double = if (medicine.saleRate > 0) medicine.saleRate else medicine.price,
        discountPercent: Double = 0.0,
        gstPercent: Double = medicine.gstPercent
    ) {
        val current = billItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.medicineId == medicine.id }
        val sgst = gstPercent / 2.0
        val cgst = gstPercent / 2.0
        val discounted = price * (1.0 - (discountPercent / 100.0))
        val netRate = discounted * (1.0 + (gstPercent / 100.0))
        val total = qty * netRate
        val bonus = if (medicine.stockQuantity > 0) {
            (medicine.freeQuantity.toDouble() / medicine.stockQuantity) * 10.0
        } else 0.0
        val pack = if (medicine.packagingType.isBlank()) {
            if (medicine.productType == "Syrup") "100 ml" else "1×10T"
        } else medicine.packagingType

        if (existingIndex >= 0) {
            val item = current[existingIndex]
            val newQty = item.qty + qty
            val newFree = item.freeQty + freeQty
            val newTotal = newQty * netRate
            current[existingIndex] = item.copy(
                qty = newQty,
                freeQty = newFree,
                price = price,
                discountPercent = discountPercent,
                sgstPercent = sgst,
                cgstPercent = cgst,
                netRate = netRate,
                itemTotalAmount = newTotal,
                pack = pack
            )
            billItems.value = current
        } else {
            val newItem = BillDraftItem(
                tempId = System.nanoTime(),
                medicineId = medicine.id,
                productName = medicine.productName,
                manufacturer = medicine.manufacturerName.ifBlank { medicine.companyName },
                pack = pack,
                batchNo = medicine.batchNumber,
                expDate = medicine.expiryDate,
                qty = qty,
                freeQty = freeQty,
                mrp = medicine.mrp,
                price = price,
                discountPercent = discountPercent,
                bonusPercent = bonus,
                sgstPercent = sgst,
                cgstPercent = cgst,
                netRate = netRate,
                itemTotalAmount = total
            )
            billItems.value = billItems.value + newItem
        }
    }

    fun updateBillItem(
        index: Int,
        newQty: Int,
        newFree: Int,
        newPrice: Double,
        newDiscount: Double,
        newSgst: Double,
        newCgst: Double,
        newPack: String? = null
    ) {
        val current = billItems.value.toMutableList()
        if (index in 0 until current.size) {
            val item = current[index]
            val totalGst = newSgst + newCgst
            val discountedPrice = newPrice * (1.0 - (newDiscount / 100.0))
            val calculatedNetRate = discountedPrice * (1.0 + (totalGst / 100.0))
            val calculatedTotal = newQty * calculatedNetRate
            current[index] = item.copy(
                qty = newQty,
                freeQty = newFree,
                price = newPrice,
                discountPercent = newDiscount,
                sgstPercent = newSgst,
                cgstPercent = newCgst,
                netRate = calculatedNetRate,
                itemTotalAmount = calculatedTotal,
                pack = newPack ?: item.pack
            )
            billItems.value = current
        }
    }

    fun updateDraftItem(
        index: Int,
        newQty: Int,
        newFree: Int,
        newPrice: Double,
        newDiscount: Double,
        newSgst: Double,
        newCgst: Double,
        newPack: String? = null
    ) {
        updateBillItem(index, newQty, newFree, newPrice, newDiscount, newSgst, newCgst, newPack)
    }

    fun removeBillItem(index: Int) {
        val current = billItems.value.toMutableList()
        if (index in 0 until current.size) {
            current.removeAt(index)
            billItems.value = current
        }
    }

    fun removeDraftItem(index: Int) {
        removeBillItem(index)
    }

    fun startEditingBill(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        editingInvoiceNumber.value = invoice.invoiceNumber
        billCustomerType.value = invoice.customerType
        when (invoice.customerType) {
            "PATIENT" -> {
                val patient = patients.value.firstOrNull { it.patientName == invoice.customerName }
                    ?: PatientEntity(
                        id = 0L,
                        patientName = invoice.customerName,
                        phoneNumber = invoice.customerPhone,
                        doctorName = invoice.doctorDetails,
                        address = invoice.customerAddress
                    )
                selectedPatient.value = patient
            }
            "PARTY" -> {
                val party = parties.value.firstOrNull { it.partyName == invoice.customerName }
                    ?: PartyEntity(
                        id = 0L,
                        partyName = invoice.customerName,
                        dlNumber = invoice.customerDl,
                        gstPanNumber = invoice.customerGstPan,
                        contactNumber = invoice.customerPhone,
                        address = invoice.customerAddress
                    )
                selectedParty.value = party
            }
            "DOCTOR" -> {
                val doctor = doctors.value.firstOrNull { it.doctorName == invoice.customerName }
                    ?: DoctorEntity(
                        id = 0L,
                        doctorName = invoice.customerName,
                        phoneNumber = invoice.customerPhone,
                        address = invoice.customerAddress
                    )
                selectedDoctor.value = doctor
            }
        }
        customCustomerName.value = invoice.customerName
        customCustomerPhone.value = invoice.customerPhone
        customCustomerAddress.value = invoice.customerAddress
        customCustomerDl.value = invoice.customerDl
        customCustomerGst.value = invoice.customerGstPan
        customDoctorName.value = invoice.doctorDetails

        billItems.value = items.map { item ->
            BillDraftItem(
                tempId = System.nanoTime(),
                medicineId = item.medicineId,
                productName = item.productName,
                manufacturer = item.manufacturer,
                pack = item.pack,
                batchNo = item.batchNo,
                expDate = item.expDate,
                qty = item.qty,
                freeQty = item.freeQty,
                mrp = item.mrp,
                price = item.price,
                discountPercent = item.discountPercent,
                bonusPercent = item.bonusPercent,
                sgstPercent = item.sgstPercent,
                cgstPercent = item.cgstPercent,
                netRate = item.netRate,
                itemTotalAmount = item.itemTotalAmount
            )
        }
        adjustmentAmount.value = invoice.adjustmentAmount
        billPaidAmount.value = invoice.paidAmount
        billNote.value = invoice.note
        _currentTab.value = NavigationTab.NEW_BILL
    }

    fun loadInvoiceForEdit(invoice: InvoiceEntity) {
        val items = invoiceItems.value.filter { it.invoiceNumber == invoice.invoiceNumber }
        startEditingBill(invoice, items)
    }

    fun loadInvoiceForEditing(invoiceNumber: Long) {
        val inv = invoices.value.firstOrNull { it.invoiceNumber == invoiceNumber } ?: return
        val items = invoiceItems.value.filter { it.invoiceNumber == invoiceNumber }
        startEditingBill(inv, items)
    }

    suspend fun saveOrFinalizeCurrentBill(): Long? {
        val items = billItems.value
        if (items.isEmpty()) return null

        val isEditing = editingInvoiceNumber.value != null
        val invoiceNum = editingInvoiceNumber.value ?: repository.getNextInvoiceNumber()

        val (custName, custAddr, custDl, custGst, custPhone, docDetails) = when (billCustomerType.value) {
            "DOCTOR" -> {
                val d = selectedDoctor.value
                Tuple6(
                    customCustomerName.value.ifBlank { d?.doctorName ?: "Doctor" },
                    customCustomerAddress.value.ifBlank { d?.address ?: "" },
                    "",
                    "",
                    customCustomerPhone.value.ifBlank { d?.phoneNumber ?: "" },
                    customDoctorName.value.ifBlank { d?.qualification ?: "" }
                )
            }
            "PATIENT" -> {
                val p = selectedPatient.value
                Tuple6(
                    customCustomerName.value.ifBlank { p?.patientName ?: "Patient" },
                    customCustomerAddress.value.ifBlank { p?.address ?: "" },
                    "",
                    "",
                    customCustomerPhone.value.ifBlank { p?.phoneNumber ?: "" },
                    customDoctorName.value.ifBlank { p?.doctorName ?: "" }
                )
            }
            else -> {
                val p = selectedParty.value
                Tuple6(
                    customCustomerName.value.ifBlank { p?.partyName ?: "Party" },
                    customCustomerAddress.value.ifBlank { p?.address ?: "" },
                    customCustomerDl.value.ifBlank { p?.dlNumber ?: "" },
                    customCustomerGst.value.ifBlank { p?.gstPanNumber ?: "" },
                    customCustomerPhone.value.ifBlank { p?.contactNumber ?: "" },
                    ""
                )
            }
        }

        val totalQty = items.sumOf { it.qty }
        val totalFree = items.sumOf { it.freeQty }
        val totalMrp = items.sumOf { it.mrp * it.qty }
        val grossSum = items.sumOf { it.price * it.qty }
        val lessDiscount = items.sumOf { it.price * (it.discountPercent / 100.0) * it.qty }
        val cgstSum = items.sumOf { ((it.netRate - (it.price * (1.0 - (it.discountPercent / 100.0)))) / 2.0) * it.qty }
        val sgstSum = cgstSum
        val grandTotal = items.sumOf { it.itemTotalAmount }
        val netPayable = (grandTotal - adjustmentAmount.value).coerceAtLeast(0.0)
        val paid = billPaidAmount.value.coerceAtMost(netPayable)
        val due = (netPayable - paid).coerceAtLeast(0.0)
        val words = NumberToWords.convert(netPayable)

        val invoice = InvoiceEntity(
            invoiceNumber = invoiceNum,
            date = System.currentTimeMillis(),
            dateFormatted = DateUtils.currentDateString(),
            customerType = billCustomerType.value,
            customerId = 0L,
            customerName = custName,
            customerAddress = custAddr,
            customerDl = custDl,
            customerGstPan = custGst,
            customerPhone = custPhone,
            doctorDetails = docDetails,
            totalMrpValue = totalMrp,
            itemCount = items.size,
            totalQty = totalQty,
            totalFree = totalFree,
            totalAmount = grossSum,
            lessDiscount = lessDiscount,
            cgstAmount = cgstSum,
            sgstAmount = sgstSum,
            adjustmentAmount = adjustmentAmount.value,
            netAmount = netPayable,
            paidAmount = paid,
            dueAmount = due,
            amountInWords = words,
            note = billNote.value.trim()
        )

        val invoiceItemEntities = items.mapIndexed { index, item ->
            InvoiceItemEntity(
                id = 0L,
                invoiceNumber = invoiceNum,
                slNo = index + 1,
                medicineId = item.medicineId,
                productName = item.productName,
                manufacturer = item.manufacturer,
                pack = item.pack,
                batchNo = item.batchNo,
                expDate = item.expDate,
                qty = item.qty,
                freeQty = item.freeQty,
                mrp = item.mrp,
                price = item.price,
                discountPercent = item.discountPercent,
                bonusPercent = item.bonusPercent,
                sgstPercent = item.sgstPercent,
                cgstPercent = item.cgstPercent,
                netRate = item.netRate,
                itemTotalAmount = item.itemTotalAmount
            )
        }

        if (isEditing) {
            repository.editBill(invoice, invoiceItemEntities)
        } else {
            repository.saveBill(invoice, invoiceItemEntities)
        }

        resetBillDraft()
        return invoiceNum
    }

    fun saveCurrentBill(onSaved: (Long?) -> Unit = {}) {
        viewModelScope.launch {
            val num = saveOrFinalizeCurrentBill()
            onSaved(num)
        }
    }

    fun deleteBill(invoiceNumber: Long) {
        viewModelScope.launch {
            repository.deleteBill(invoiceNumber)
        }
    }

    fun deleteInvoice(invoiceNumber: Long) {
        deleteBill(invoiceNumber)
    }

    fun startEditInvoice(invoice: InvoiceEntity) {
        loadInvoiceForEdit(invoice)
    }

    fun navigateTo(tab: NavigationTab) {
        switchTab(tab)
    }

    fun recordPayment(invoiceNumber: Long, amount: Double) {
        viewModelScope.launch {
            repository.recordPayment(invoiceNumber, amount)
        }
    }

    fun calculateStockStatement(companyName: String, startTime: Long, endTime: Long): List<StockStatementRow> {
        val allMeds = medicines.value.filter { med ->
            if (companyName.isNotBlank() && !companyName.equals("ALL COMPANIES", ignoreCase = true)) {
                companyName.equals(med.companyName, ignoreCase = true) || companyName.equals(med.manufacturerName, ignoreCase = true)
            } else true
        }

        val allTx = stockTransactions.value

        return allMeds.map { med ->
            val medTx = allTx.filter { it.medicineId == med.id }
            val hasInwardTx = medTx.any { it.type == "PURCHASE" || it.type == "RECEIPT" }

            // If no receipt/purchase transaction exists in DB for this medicine,
            // treat initial stock as a RECEIPT at medicine creation (or fallback timestamp)
            val effectiveTxList = if (!hasInwardTx) {
                val totalSold = medTx.filter { it.type == "SALE" || it.type == "ISSUE" }.sumOf { it.qty }
                val initialStock = med.stockQuantity + totalSold
                if (initialStock > 0) {
                    val rate = if (med.purchaseRate > 0.0) med.purchaseRate else med.price
                    val initialTx = StockTransactionEntity(
                        medicineId = med.id,
                        productName = med.productName,
                        companyName = if (med.companyName.isNotBlank()) med.companyName else med.manufacturerName,
                        type = "RECEIPT",
                        referenceInvoice = null,
                        qty = initialStock,
                        freeQty = med.freeQuantity,
                        rate = rate,
                        amount = initialStock * rate,
                        timestamp = if (med.createdAt > 0) med.createdAt else 1704067200000L,
                        dateFormatted = ""
                    )
                    medTx + initialTx
                } else medTx
            } else {
                medTx
            }

            // Transactions strictly before the selected start date
            val txBefore = effectiveTxList.filter { it.timestamp < startTime }
            val receiptQtyBefore = txBefore.filter { it.type == "PURCHASE" || it.type == "RECEIPT" }.sumOf { it.qty }
            val issueQtyBefore = txBefore.filter { it.type == "SALE" || it.type == "ISSUE" }.sumOf { it.qty + it.freeQty }
            val openingQty = (receiptQtyBefore - issueQtyBefore).coerceAtLeast(0)

            // Transactions within the selected period [startTime..endTime]
            val txPeriod = effectiveTxList.filter { it.timestamp in startTime..endTime }
            val receiptQty = txPeriod.filter { it.type == "PURCHASE" || it.type == "RECEIPT" }.sumOf { it.qty }
            val receiptAmount = txPeriod.filter { it.type == "PURCHASE" || it.type == "RECEIPT" }.sumOf { it.amount }
                .let { if (it > 0.0) it else receiptQty * (if (med.purchaseRate > 0.0) med.purchaseRate else med.price) }

            val issueQty = txPeriod.filter { it.type == "SALE" || it.type == "ISSUE" }.sumOf { it.qty + it.freeQty }
            val issueAmount = txPeriod.filter { it.type == "SALE" || it.type == "ISSUE" }.sumOf { it.amount }
                .let { if (it > 0.0) it else issueQty * (if (med.saleRate > 0.0) med.saleRate else med.price) }

            // Closing stock = opening + receipt - issue
            val closingQty = (openingQty + receiptQty - issueQty).coerceAtLeast(0)
            val pack = if (med.packagingType.isBlank()) {
                if (med.productType == "Syrup") "100 ml" else "1×10T"
            } else med.packagingType

            val itemRate = if (med.saleRate > 0.0) med.saleRate else med.price

            StockStatementRow(
                productName = med.productName,
                companyName = if (med.companyName.isNotBlank()) med.companyName else med.manufacturerName,
                packagingType = pack,
                openingQty = openingQty,
                openingAmount = openingQty * itemRate,
                receiptQty = receiptQty,
                receiptAmount = receiptAmount,
                issueQty = issueQty,
                issueAmount = issueAmount,
                closingQty = closingQty,
                closingAmount = closingQty * itemRate
            )
        }
    }

    fun getKhataRows(): List<KhataRow> {
        val sortedInvoices = invoices.value.sortedByDescending { it.invoiceNumber }
        val itemsMap = invoiceItems.value.groupBy { it.invoiceNumber }
        val rows = mutableListOf<KhataRow>()

        for (inv in sortedInvoices) {
            val items = itemsMap[inv.invoiceNumber] ?: emptyList()
            if (items.isEmpty()) {
                rows.add(
                    KhataRow(
                        invoiceNumber = inv.invoiceNumber,
                        date = inv.dateFormatted,
                        partyName = inv.customerName,
                        productName = "No items",
                        quantity = inv.totalQty,
                        free = inv.totalFree,
                        billAmount = inv.netAmount,
                        isFirstItemOfInvoice = true,
                        note = inv.note
                    )
                )
            } else {
                items.forEachIndexed { index, item ->
                    rows.add(
                        KhataRow(
                            invoiceNumber = inv.invoiceNumber,
                            date = inv.dateFormatted,
                            partyName = inv.customerName,
                            productName = item.productName,
                            quantity = item.qty,
                            free = item.freeQty,
                            billAmount = inv.netAmount,
                            isFirstItemOfInvoice = index == 0,
                            note = inv.note
                        )
                    )
                }
            }
        }
        return rows
    }

    fun triggerManualSync() {
        syncEngine.triggerAutomaticSync()
    }

    fun updateFirebaseProjectId(newId: String) {
        syncEngine.setFirebaseProjectId(newId)
    }

    fun updateDeviceName(name: String) {
        syncEngine.setDeviceName(name)
    }

    fun checkAppUpdate(onResult: ((AppUpdateState) -> Unit)? = null) {
        viewModelScope.launch {
            val state = repository.checkAppUpdateManually()
            onResult?.invoke(state)
        }
    }

    fun publishAppUpdate(versionName: String, versionCode: Int, releaseNotes: String, updateUrl: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.publishAppUpdate(versionName, versionCode, releaseNotes, updateUrl)
            onResult(success)
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncEngine.cleanup()
    }
}
