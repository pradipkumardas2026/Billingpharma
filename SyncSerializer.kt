package com.example.data.sync

import com.example.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject

object SyncSerializer {

    fun medicineToJson(m: MedicineEntity): String {
        val json = JSONObject()
        json.put("id", m.id)
        json.put("productName", m.productName)
        json.put("productType", m.productType)
        json.put("packagingType", m.packagingType)
        json.put("composition", m.composition)
        json.put("manufacturerName", m.manufacturerName)
        json.put("companyName", m.companyName)
        json.put("batchNumber", m.batchNumber)
        json.put("mfgDate", m.mfgDate)
        json.put("expiryDate", m.expiryDate)
        json.put("mrp", m.mrp)
        json.put("price", m.price)
        json.put("purchaseRate", m.purchaseRate)
        json.put("saleRate", m.saleRate)
        json.put("gstPercent", m.gstPercent)
        json.put("stockQuantity", m.stockQuantity)
        json.put("rackLocation", m.rackLocation)
        json.put("freeQuantity", m.freeQuantity)
        json.put("lowStockLevel", m.lowStockLevel)
        json.put("createdAt", m.createdAt)
        return json.toString()
    }

    fun partyToJson(p: PartyEntity): String {
        val json = JSONObject()
        json.put("id", p.id)
        json.put("partyName", p.partyName)
        json.put("dlNumber", p.dlNumber)
        json.put("gstPanNumber", p.gstPanNumber)
        json.put("contactNumber", p.contactNumber)
        json.put("address", p.address)
        return json.toString()
    }

    fun doctorToJson(d: DoctorEntity): String {
        val json = JSONObject()
        json.put("id", d.id)
        json.put("doctorName", d.doctorName)
        json.put("qualification", d.qualification)
        json.put("doctorType", d.doctorType)
        json.put("phoneNumber", d.phoneNumber)
        json.put("address", d.address)
        return json.toString()
    }

    fun patientToJson(p: PatientEntity): String {
        val json = JSONObject()
        json.put("id", p.id)
        json.put("patientName", p.patientName)
        json.put("phoneNumber", p.phoneNumber)
        json.put("doctorName", p.doctorName)
        json.put("address", p.address)
        return json.toString()
    }

    fun settingsToJson(s: SettingsEntity): String {
        val json = JSONObject()
        json.put("id", s.id)
        json.put("businessName", s.businessName)
        json.put("address", s.address)
        json.put("contactNumber", s.contactNumber)
        json.put("dlNumber", s.dlNumber)
        json.put("gstNumber", s.gstNumber)
        json.put("adminPassword", s.adminPassword)
        json.put("adminMobile", s.adminMobile)
        return json.toString()
    }

    fun invoiceToJson(inv: InvoiceEntity, items: List<InvoiceItemEntity>): String {
        val json = JSONObject()
        json.put("invoiceNumber", inv.invoiceNumber)
        json.put("date", inv.date)
        json.put("dateFormatted", inv.dateFormatted)
        json.put("customerType", inv.customerType)
        json.put("customerId", inv.customerId)
        json.put("customerName", inv.customerName)
        json.put("customerAddress", inv.customerAddress)
        json.put("customerDl", inv.customerDl)
        json.put("customerGstPan", inv.customerGstPan)
        json.put("customerPhone", inv.customerPhone)
        json.put("doctorDetails", inv.doctorDetails)
        json.put("totalMrpValue", inv.totalMrpValue)
        json.put("itemCount", inv.itemCount)
        json.put("totalQty", inv.totalQty)
        json.put("totalFree", inv.totalFree)
        json.put("totalAmount", inv.totalAmount)
        json.put("lessDiscount", inv.lessDiscount)
        json.put("cgstAmount", inv.cgstAmount)
        json.put("sgstAmount", inv.sgstAmount)
        json.put("adjustmentAmount", inv.adjustmentAmount)
        json.put("netAmount", inv.netAmount)
        json.put("paidAmount", inv.paidAmount)
        json.put("dueAmount", inv.dueAmount)
        json.put("amountInWords", inv.amountInWords)
        json.put("note", inv.note)

        val itemsArray = JSONArray()
        for (item in items) {
            val itemJson = JSONObject()
            itemJson.put("id", item.id)
            itemJson.put("invoiceNumber", item.invoiceNumber)
            itemJson.put("slNo", item.slNo)
            itemJson.put("medicineId", item.medicineId)
            itemJson.put("productName", item.productName)
            itemJson.put("manufacturer", item.manufacturer)
            itemJson.put("pack", item.pack)
            itemJson.put("batchNo", item.batchNo)
            itemJson.put("expDate", item.expDate)
            itemJson.put("qty", item.qty)
            itemJson.put("freeQty", item.freeQty)
            itemJson.put("mrp", item.mrp)
            itemJson.put("price", item.price)
            itemJson.put("discountPercent", item.discountPercent)
            itemJson.put("bonusPercent", item.bonusPercent)
            itemJson.put("sgstPercent", item.sgstPercent)
            itemJson.put("cgstPercent", item.cgstPercent)
            itemJson.put("netRate", item.netRate)
            itemJson.put("itemTotalAmount", item.itemTotalAmount)
            itemsArray.put(itemJson)
        }
        json.put("items", itemsArray)
        return json.toString()
    }

    fun guestLoginToJson(g: GuestLoginEntity): String {
        val json = JSONObject()
        json.put("id", g.id)
        json.put("mobileNumber", g.mobileNumber)
        json.put("loginTimestamp", g.loginTimestamp)
        return json.toString()
    }

    fun adminUserToJson(a: AdminUserEntity): String {
        val json = JSONObject()
        json.put("id", a.id)
        json.put("name", a.name)
        json.put("mobileNumber", a.mobileNumber)
        json.put("password", a.password)
        json.put("createdByMobile", a.createdByMobile)
        json.put("createdAt", a.createdAt)
        return json.toString()
    }
}
