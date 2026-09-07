package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.PharmaDao
import com.example.data.local.entity.AdminUserEntity
import com.example.data.local.entity.DoctorEntity
import com.example.data.local.entity.GuestLoginEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.MedicineEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PatientEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.local.entity.SettingsEntity
import com.example.data.local.entity.StockTransactionEntity
import com.example.data.local.entity.SyncOperationEntity
import com.example.data.local.entity.TombstoneEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SettingsEntity::class,
        MedicineEntity::class,
        PartyEntity::class,
        DoctorEntity::class,
        PatientEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        StockTransactionEntity::class,
        AdminUserEntity::class,
        GuestLoginEntity::class,
        SyncOperationEntity::class,
        TombstoneEntity::class,
        PurchaseInvoiceEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pharmaDao(): PharmaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN packagingType TEXT NOT NULL DEFAULT '1×10T'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS admin_users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        mobileNumber TEXT NOT NULL,
                        password TEXT NOT NULL,
                        createdByMobile TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS guest_logins (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mobileNumber TEXT NOT NULL,
                        loginTimestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN composition TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_operations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        retryCount INTEGER NOT NULL,
                        errorMessage TEXT,
                        deviceId TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_tombstones (
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL,
                        deviceId TEXT NOT NULL,
                        userMobile TEXT NOT NULL,
                        PRIMARY KEY(entityType, entityId)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchase_invoices (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        invoiceNumber TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        dateFormatted TEXT NOT NULL,
                        companyName TEXT NOT NULL,
                        companyGst TEXT NOT NULL,
                        companyPhone TEXT NOT NULL,
                        itemsSummary TEXT NOT NULL,
                        totalQty INTEGER NOT NULL,
                        taxableAmount REAL NOT NULL,
                        gstRatePercent REAL NOT NULL,
                        cgstAmount REAL NOT NULL,
                        sgstAmount REAL NOT NULL,
                        totalGstAmount REAL NOT NULL,
                        totalAmount REAL NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pharmabill_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.pharmaDao())
                }
            }
        }

        suspend fun populateInitialData(dao: PharmaDao) {
            val settings = dao.getSettings()
            if (settings == null) {
                dao.insertOrUpdateSettings(
                    SettingsEntity(
                        id = 1,
                        businessName = "LifeCare Pharmacy",
                        address = "Station Road, Burdwan, West Bengal - 713101",
                        gstNumber = "19AABCL1234F1Z5",
                        dlNumber = "WB-BUR-2024-987654",
                        contactNumber = "9002625428",
                        adminPassword = "654321",
                        adminMobile = "9002625428"
                    )
                )

                // Add some default starter medicines
                dao.insertMedicine(
                    MedicineEntity(
                        productName = "PARACETAMOL 650MG",
                        productType = "Tablet",
                        packagingType = "1×15T",
                        composition = "Paracetamol 650mg",
                        manufacturerName = "MICRO LABS",
                        companyName = "DOLO",
                        batchNumber = "DL2401",
                        mfgDate = "01/24",
                        expiryDate = "12/26",
                        mrp = 30.50,
                        price = 24.40,
                        purchaseRate = 18.00,
                        saleRate = 24.40,
                        gstPercent = 12.0,
                        stockQuantity = 200,
                        rackLocation = "Rack A-1",
                        freeQuantity = 0,
                        lowStockLevel = 20
                    )
                )
                dao.insertMedicine(
                    MedicineEntity(
                        productName = "AZITHROMYCIN 500MG",
                        productType = "Tablet",
                        packagingType = "1×5T",
                        composition = "Azithromycin 500mg",
                        manufacturerName = "CIPLA",
                        companyName = "AZIWIN",
                        batchNumber = "AZ8921",
                        mfgDate = "02/24",
                        expiryDate = "01/27",
                        mrp = 119.50,
                        price = 95.00,
                        purchaseRate = 72.00,
                        saleRate = 95.00,
                        gstPercent = 12.0,
                        stockQuantity = 50,
                        rackLocation = "Rack B-2",
                        freeQuantity = 0,
                        lowStockLevel = 10
                    )
                )
                dao.insertMedicine(
                    MedicineEntity(
                        productName = "PANTOPRAZOLE 40MG",
                        productType = "Capsule",
                        packagingType = "1×10C",
                        composition = "Pantoprazole 40mg",
                        manufacturerName = "ALKEM",
                        companyName = "PAN-40",
                        batchNumber = "PN4412",
                        mfgDate = "03/24",
                        expiryDate = "02/27",
                        mrp = 95.00,
                        price = 76.00,
                        purchaseRate = 55.00,
                        saleRate = 76.00,
                        gstPercent = 12.0,
                        stockQuantity = 120,
                        rackLocation = "Rack A-3",
                        freeQuantity = 0,
                        lowStockLevel = 15
                    )
                )

                // Starter Party
                dao.insertParty(
                    PartyEntity(
                        partyName = "M/s Maa Tara Medico Agency",
                        dlNumber = "WB-BUR-DL-8877",
                        gstPanNumber = "19AABCM8877P1Z2",
                        contactNumber = "9832109876",
                        address = "Khadan Road, Burdwan"
                    )
                )

                // Starter Doctor
                dao.insertDoctor(
                    DoctorEntity(
                        doctorName = "Dr. S. K. Mukherjee",
                        qualification = "MBBS, MD (Medicine)",
                        doctorType = "Allopathic",
                        phoneNumber = "9434102938",
                        address = "Near Sadar Hospital, Burdwan"
                    )
                )

                // Starter Patient
                dao.insertPatient(
                    PatientEntity(
                        patientName = "Subhash Bose",
                        phoneNumber = "9876541230",
                        doctorName = "Dr. S. K. Mukherjee",
                        address = "Nilpur, Burdwan"
                    )
                )

                // Starter Purchase Invoices for GST
                dao.insertPurchaseInvoice(
                    PurchaseInvoiceEntity(
                        invoiceNumber = "PUR-2024-001",
                        date = System.currentTimeMillis() - 86400000L * 3,
                        dateFormatted = "04/09/2026",
                        companyName = "MICRO LABS LTD",
                        companyGst = "19AABCM5432K1Z8",
                        companyPhone = "9800112233",
                        itemsSummary = "PARACETAMOL 650MG (200 Strips)",
                        totalQty = 200,
                        taxableAmount = 3600.0,
                        gstRatePercent = 12.0,
                        cgstAmount = 216.0,
                        sgstAmount = 216.0,
                        totalGstAmount = 432.0,
                        totalAmount = 4032.0,
                        note = "Stock Purchase Dolo 650"
                    )
                )

                dao.insertPurchaseInvoice(
                    PurchaseInvoiceEntity(
                        invoiceNumber = "PUR-2024-002",
                        date = System.currentTimeMillis() - 86400000L * 2,
                        dateFormatted = "05/09/2026",
                        companyName = "CIPLA PHARMACEUTICALS",
                        companyGst = "19AABCC1122J1Z4",
                        companyPhone = "9833445566",
                        itemsSummary = "AZITHROMYCIN 500MG (50 Strips)",
                        totalQty = 50,
                        taxableAmount = 3600.0,
                        gstRatePercent = 12.0,
                        cgstAmount = 216.0,
                        sgstAmount = 216.0,
                        totalGstAmount = 432.0,
                        totalAmount = 4032.0,
                        note = "Stock Purchase Aziwin 500"
                    )
                )

                dao.insertPurchaseInvoice(
                    PurchaseInvoiceEntity(
                        invoiceNumber = "PUR-2024-003",
                        date = System.currentTimeMillis() - 86400000L,
                        dateFormatted = "06/09/2026",
                        companyName = "ALKEM LABORATORIES",
                        companyGst = "19AABCA9988H1Z1",
                        companyPhone = "9877889900",
                        itemsSummary = "PANTOPRAZOLE 40MG (120 Caps)",
                        totalQty = 120,
                        taxableAmount = 6600.0,
                        gstRatePercent = 12.0,
                        cgstAmount = 396.0,
                        sgstAmount = 396.0,
                        totalGstAmount = 792.0,
                        totalAmount = 7392.0,
                        note = "Stock Purchase Pan 40"
                    )
                )
            }
        }
    }
}
