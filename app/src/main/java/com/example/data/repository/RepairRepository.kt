package com.example.data.repository

import android.content.Context
import android.telephony.SmsManager
import com.example.data.AppDatabase
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.BrandEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.FaultEntity
import com.example.data.entity.ModelEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.PriceEntity
import com.example.data.entity.RepairEntity
import com.example.data.entity.RepairItemEntity
import com.example.data.entity.RepairTypeEntity
import com.example.data.entity.StatusHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RepairRepository(private val db: AppDatabase, private val context: Context) {

    private val repairDao = db.repairDao()
    private val repairItemDao = db.repairItemDao()
    private val paymentDao = db.paymentDao()
    private val statusHistoryDao = db.statusHistoryDao()
    private val customerDao = db.customerDao()
    private val brandDao = db.brandDao()
    private val modelDao = db.modelDao()
    private val faultDao = db.faultDao()
    private val repairTypeDao = db.repairTypeDao()
    private val priceDao = db.priceDao()
    private val settingsDao = db.settingsDao()

    val allRepairs: Flow<List<RepairEntity>> = repairDao.getAllRepairsFlow()
    val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPaymentsFlow()
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomersFlow()
    val allBrands: Flow<List<BrandEntity>> = brandDao.getAllBrandsFlow()
    val allFaults: Flow<List<FaultEntity>> = faultDao.getAllFaultsFlow()
    val allRepairTypes: Flow<List<RepairTypeEntity>> = repairTypeDao.getAllRepairTypesFlow()
    val allPrices: Flow<List<PriceEntity>> = priceDao.getAllPricesFlow()
    val settingsFlow: Flow<AppSettingsEntity?> = settingsDao.getSettingsFlow()

    fun getModelsForBrand(brand: String): Flow<List<ModelEntity>> = modelDao.getModelsForBrand(brand)

    fun getItemsForRepair(repairId: Long): Flow<List<RepairItemEntity>> = repairItemDao.getItemsForRepair(repairId)

    fun getPaymentsForRepair(repairId: Long): Flow<List<PaymentEntity>> = paymentDao.getPaymentsForRepair(repairId)

    fun getHistoryForRepair(repairId: Long): Flow<List<StatusHistoryEntity>> = statusHistoryDao.getHistoryForRepair(repairId)

    suspend fun getRepairById(id: Long): RepairEntity? = repairDao.getRepairById(id)

    suspend fun getRepairByJobNumber(jobNumber: String): RepairEntity? {
        val normalized = normalizeJobNumber(jobNumber)
        return repairDao.getRepairByJobNumber(normalized)
    }

    suspend fun getRepairsForCustomer(phone: String): List<RepairEntity> = repairDao.getRepairsByCustomerPhone(phone)

    suspend fun getSettings(): AppSettingsEntity {
        return settingsDao.getSettings() ?: AppSettingsEntity()
    }

    suspend fun updateSettings(settings: AppSettingsEntity) {
        settingsDao.insertOrUpdate(settings)
    }

    // 4-Digit Job Number Generator
    suspend fun generateNextJobNumber(): String = withContext(Dispatchers.IO) {
        val settings = getSettings()
        val allNumbers = repairDao.getAllJobNumbers()
        var maxNum = settings.startingJobNumber - 1

        for (numStr in allNumbers) {
            val num = numStr.toIntOrNull()
            if (num != null && num > maxNum) {
                maxNum = num
            }
        }
        val next = maxNum + 1
        String.format(Locale.US, "%04d", next)
    }

    fun normalizeJobNumber(raw: String): String {
        val digitsOnly = raw.replace(Regex("[^0-9]"), "")
        if (digitsOnly.isEmpty()) return ""
        val num = digitsOnly.toIntOrNull() ?: 0
        return String.format(Locale.US, "%04d", num)
    }

    // Customer History Lookup by Phone
    suspend fun getCustomerSummaryByPhone(phone: String): Pair<CustomerEntity?, List<RepairEntity>> = withContext(Dispatchers.IO) {
        val clean = phone.filter { it.isDigit() }
        if (clean.length < 3) return@withContext null to emptyList()
        val allRepairs = repairDao.getAllRepairsDirect()
        val matchingRepairs = allRepairs.filter { repair ->
            val repClean = repair.customerPhone.filter { it.isDigit() }
            repClean.isNotEmpty() && (repClean == clean || repClean.endsWith(clean) || clean.endsWith(repClean))
        }
        val allCustomers = customerDao.getAllCustomersDirect()
        val matchingCustomer = allCustomers.find { customer ->
            val custClean = customer.phone.filter { it.isDigit() }
            custClean.isNotEmpty() && (custClean == clean || custClean.endsWith(clean) || clean.endsWith(custClean))
        }
        matchingCustomer to matchingRepairs
    }

    // Price Lookup
    suspend fun lookupPrice(brand: String, model: String, repairType: String): Double? = withContext(Dispatchers.IO) {
        val entity = priceDao.findPrice(brand.trim(), model.trim(), repairType.trim())
        entity?.price
    }

    suspend fun savePrice(brand: String, model: String, repairType: String, price: Double) = withContext(Dispatchers.IO) {
        priceDao.insertPrice(
            PriceEntity(
                brand = brand.trim(),
                model = model.trim(),
                repairType = repairType.trim(),
                price = price
            )
        )
    }

    suspend fun deletePrice(price: PriceEntity) = withContext(Dispatchers.IO) {
        priceDao.deletePrice(price)
    }

    // Autocomplete Master Data
    suspend fun addBrandIfNew(name: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext false
        val existing = brandDao.getAllBrandsDirect()
        if (existing.none { it.name.equals(trimmed, ignoreCase = true) }) {
            brandDao.insertBrand(BrandEntity(name = trimmed))
            true
        } else false
    }

    suspend fun addModelIfNew(brand: String, modelName: String): Boolean = withContext(Dispatchers.IO) {
        val bTrimmed = brand.trim()
        val mTrimmed = modelName.trim()
        if (bTrimmed.isEmpty() || mTrimmed.isEmpty()) return@withContext false
        val existing = modelDao.getModelsForBrandDirect(bTrimmed)
        if (existing.none { it.modelName.equals(mTrimmed, ignoreCase = true) }) {
            modelDao.insertModel(ModelEntity(brandName = bTrimmed, modelName = mTrimmed))
            true
        } else false
    }

    suspend fun addFaultIfNew(name: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext false
        val existing = faultDao.getAllFaultsDirect()
        if (existing.none { it.name.equals(trimmed, ignoreCase = true) }) {
            faultDao.insertFault(FaultEntity(name = trimmed))
            true
        } else false
    }

    suspend fun addRepairTypeIfNew(name: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext false
        val existing = repairTypeDao.getAllRepairTypesDirect()
        if (existing.none { it.name.equals(trimmed, ignoreCase = true) }) {
            repairTypeDao.insertRepairType(RepairTypeEntity(name = trimmed))
            true
        } else false
    }

    // Creating a New Repair
    suspend fun createRepair(
        customerName: String,
        customerPhone: String,
        brand: String,
        model: String,
        imei: String,
        deviceColour: String,
        accessories: String,
        fault: String,
        technicianNotes: String,
        remarks: String,
        repairItems: List<Pair<String, Double>>,
        initialPaymentAmount: Double
    ): RepairEntity = withContext(Dispatchers.IO) {
        val jobNumber = generateNextJobNumber()
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(now))
        val timeStr = timeFormat.format(Date(now))

        val totalPrice = repairItems.sumOf { it.second }
        val amountPaid = initialPaymentAmount
        val balance = (totalPrice - amountPaid).coerceAtLeast(0.0)

        val paymentStatus = when {
            totalPrice > 0 && amountPaid >= totalPrice -> "PAID"
            amountPaid > 0 -> "PARTIALLY PAID"
            else -> "UNPAID"
        }

        // Automatic delivery if fully paid
        val initialStatus = if (totalPrice > 0 && amountPaid >= totalPrice) "DELIVERED" else "RECEIVED"
        val deliveredTimestamp = if (initialStatus == "DELIVERED") now else null

        val repairEntity = RepairEntity(
            jobNumber = jobNumber,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            brand = brand.trim(),
            model = model.trim(),
            imei = imei.trim(),
            deviceColour = deviceColour.trim(),
            accessories = accessories.trim(),
            fault = fault.trim(),
            technicianNotes = technicianNotes.trim(),
            remarks = remarks.trim(),
            totalPrice = totalPrice,
            amountPaid = amountPaid,
            balance = balance,
            paymentStatus = paymentStatus,
            status = initialStatus,
            receivedDate = dateStr,
            receivedTime = timeStr,
            receivedTimestamp = now,
            deliveredTimestamp = deliveredTimestamp,
            readySmsSent = false
        )

        val repairId = repairDao.insertRepair(repairEntity)
        val createdRepair = repairEntity.copy(id = repairId)

        // Insert repair items
        val itemEntities = repairItems.map {
            RepairItemEntity(repairId = repairId, repairType = it.first.trim(), price = it.second)
        }
        repairItemDao.insertItems(itemEntities)

        // Insert initial payment if any
        if (initialPaymentAmount > 0) {
            paymentDao.insertPayment(
                PaymentEntity(
                    repairId = repairId,
                    paymentNumber = 1,
                    amount = initialPaymentAmount,
                    date = dateStr,
                    time = timeStr,
                    timestamp = now
                )
            )
        }

        // Insert status history
        statusHistoryDao.insertHistory(
            StatusHistoryEntity(
                repairId = repairId,
                status = initialStatus,
                date = dateStr,
                time = timeStr,
                timestamp = now,
                notes = if (initialStatus == "DELIVERED") "Delivered on receipt (Paid in full)" else "Job registered"
            )
        )

        // Save / update customer
        customerDao.insertCustomer(
            CustomerEntity(
                phone = customerPhone.trim(),
                name = customerName.trim(),
                lastVisitTimestamp = now
            )
        )

        // Save any custom brand/model/fault/repairType into master data
        addBrandIfNew(brand)
        addModelIfNew(brand, model)
        addFaultIfNew(fault)
        repairItems.forEach { addRepairTypeIfNew(it.first) }

        createdRepair
    }

    // Payment Operations
    suspend fun addPayment(repairId: Long, amount: Double): RepairEntity? = withContext(Dispatchers.IO) {
        val repair = repairDao.getRepairById(repairId) ?: return@withContext null
        if (amount <= 0) return@withContext repair

        val existingPayments = paymentDao.getPaymentsForRepairDirect(repairId)
        val nextNumber = existingPayments.size + 1
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(now))
        val timeStr = timeFormat.format(Date(now))

        paymentDao.insertPayment(
            PaymentEntity(
                repairId = repairId,
                paymentNumber = nextNumber,
                amount = amount,
                date = dateStr,
                time = timeStr,
                timestamp = now
            )
        )

        recalculateAndSaveRepair(repairId)
    }

    suspend fun updatePayment(paymentId: Long, newAmount: Double): RepairEntity? = withContext(Dispatchers.IO) {
        val allPayments = paymentDao.getAllPaymentsDirect()
        val payment = allPayments.find { it.id == paymentId } ?: return@withContext null
        if (newAmount <= 0) return@withContext null

        paymentDao.updatePayment(payment.copy(amount = newAmount))
        recalculateAndSaveRepair(payment.repairId)
    }

    suspend fun deletePayment(paymentId: Long): RepairEntity? = withContext(Dispatchers.IO) {
        val allPayments = paymentDao.getAllPaymentsDirect()
        val payment = allPayments.find { it.id == paymentId } ?: return@withContext null
        val repairId = payment.repairId

        paymentDao.deletePaymentById(paymentId)
        recalculateAndSaveRepair(repairId)
    }

    // Add / Remove Repair Items
    suspend fun addRepairItem(repairId: Long, repairType: String, price: Double): RepairEntity? = withContext(Dispatchers.IO) {
        repairItemDao.insertItem(
            RepairItemEntity(repairId = repairId, repairType = repairType.trim(), price = price)
        )
        addRepairTypeIfNew(repairType)
        recalculateAndSaveRepair(repairId)
    }

    suspend fun removeRepairItem(itemId: Long, repairId: Long): RepairEntity? = withContext(Dispatchers.IO) {
        val items = repairItemDao.getItemsForRepairDirect(repairId)
        val item = items.find { it.id == itemId } ?: return@withContext null
        repairItemDao.deleteItem(item)
        recalculateAndSaveRepair(repairId)
    }

    suspend fun updateRepairItem(itemId: Long, repairId: Long, newRepairType: String, newPrice: Double): RepairEntity? = withContext(Dispatchers.IO) {
        val items = repairItemDao.getItemsForRepairDirect(repairId)
        val item = items.find { it.id == itemId } ?: return@withContext null
        repairItemDao.updateItem(item.copy(repairType = newRepairType.trim(), price = newPrice))
        addRepairTypeIfNew(newRepairType)
        recalculateAndSaveRepair(repairId)
    }

    // Recalculates payments, balance, payment status, and automatic delivery status
    private suspend fun recalculateAndSaveRepair(repairId: Long): RepairEntity? {
        val repair = repairDao.getRepairById(repairId) ?: return null
        val items = repairItemDao.getItemsForRepairDirect(repairId)
        val payments = paymentDao.getPaymentsForRepairDirect(repairId)

        val totalPrice = items.sumOf { it.price }
        val totalPaid = payments.sumOf { it.amount }
        val balance = (totalPrice - totalPaid).coerceAtLeast(0.0)

        val paymentStatus = when {
            totalPrice > 0 && totalPaid >= totalPrice -> "PAID"
            totalPaid > 0 -> "PARTIALLY PAID"
            else -> "UNPAID"
        }

        var newStatus = repair.status
        var deliveredTimestamp = repair.deliveredTimestamp
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Automation Rule: If Total Paid >= Total Repair Price -> DELIVERED
        if (totalPrice > 0 && totalPaid >= totalPrice) {
            if (repair.status != "DELIVERED") {
                newStatus = "DELIVERED"
                deliveredTimestamp = now
                statusHistoryDao.insertHistory(
                    StatusHistoryEntity(
                        repairId = repairId,
                        status = "DELIVERED",
                        date = dateFormat.format(Date(now)),
                        time = timeFormat.format(Date(now)),
                        timestamp = now,
                        notes = "Auto-marked DELIVERED: Full payment received"
                    )
                )
            }
        } else {
            // If it was DELIVERED purely because of full payment, and now underpaid (e.g. price increased or payment removed)
            if (repair.status == "DELIVERED" && totalPrice > totalPaid) {
                // Return to appropriate status (READY if was ready, or REPAIRING)
                newStatus = "READY"
                statusHistoryDao.insertHistory(
                    StatusHistoryEntity(
                        repairId = repairId,
                        status = "READY",
                        date = dateFormat.format(Date(now)),
                        time = timeFormat.format(Date(now)),
                        timestamp = now,
                        notes = "Status updated to READY due to pending balance"
                    )
                )
            }
        }

        val updated = repair.copy(
            totalPrice = totalPrice,
            amountPaid = totalPaid,
            balance = balance,
            paymentStatus = paymentStatus,
            status = newStatus,
            deliveredTimestamp = deliveredTimestamp
        )

        repairDao.updateRepair(updated)
        return updated
    }

    // Status Changes & Automatic SMS
    suspend fun updateStatus(repairId: Long, newStatus: String, notes: String = ""): RepairEntity? = withContext(Dispatchers.IO) {
        val repair = repairDao.getRepairById(repairId) ?: return@withContext null
        if (repair.status == newStatus) return@withContext repair

        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(now))
        val timeStr = timeFormat.format(Date(now))

        var readySmsSent = repair.readySmsSent
        val deliveredTimestamp = if (newStatus == "DELIVERED") now else repair.deliveredTimestamp

        // Automatic SMS when transitioning to READY
        if (newStatus == "READY" && !repair.readySmsSent) {
            val settings = getSettings()
            if (settings.autoReadySms) {
                val sent = sendSms(repair, settings)
                if (sent) readySmsSent = true
            }
        }

        val updated = repair.copy(
            status = newStatus,
            deliveredTimestamp = deliveredTimestamp,
            readySmsSent = readySmsSent
        )

        repairDao.updateRepair(updated)
        statusHistoryDao.insertHistory(
            StatusHistoryEntity(
                repairId = repairId,
                status = newStatus,
                date = dateStr,
                time = timeStr,
                timestamp = now,
                notes = notes.ifEmpty { "Status changed to $newStatus" }
            )
        )

        updated
    }

    // Send SMS (Automatic or Manual "SEND SMS AGAIN")
    suspend fun sendReadySmsExplicit(repair: RepairEntity): Boolean = withContext(Dispatchers.IO) {
        val settings = getSettings()
        sendSms(repair, settings)
    }

    private fun sendSms(repair: RepairEntity, settings: AppSettingsEntity): Boolean {
        return try {
            val balanceStr = if (repair.balance <= 0) "0" else String.format(Locale.US, "%,.0f", repair.balance)
            val totalStr = String.format(Locale.US, "%,.0f", repair.totalPrice)

            val text = settings.smsTemplate
                .replace("{customer_name}", repair.customerName)
                .replace("{job_number}", repair.jobNumber)
                .replace("{total_price}", totalStr)
                .replace("{balance}", balanceStr)
                .replace("{model}", "${repair.brand} ${repair.model}")
                .replace("{shop_name}", settings.shopName)
                .replace("{shop_phone}", settings.shopPhone)

            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(text)
            smsManager.sendMultipartTextMessage(repair.customerPhone, null, parts, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Full Repair Details Updating
    suspend fun updateRepairDetails(
        repairId: Long,
        customerName: String,
        customerPhone: String,
        brand: String,
        model: String,
        imei: String,
        deviceColour: String,
        accessories: String,
        fault: String,
        technicianNotes: String,
        remarks: String
    ) = withContext(Dispatchers.IO) {
        val repair = repairDao.getRepairById(repairId) ?: return@withContext
        val updated = repair.copy(
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            brand = brand.trim(),
            model = model.trim(),
            imei = imei.trim(),
            deviceColour = deviceColour.trim(),
            accessories = accessories.trim(),
            fault = fault.trim(),
            technicianNotes = technicianNotes.trim(),
            remarks = remarks.trim()
        )
        repairDao.updateRepair(updated)
        customerDao.insertCustomer(
            CustomerEntity(
                phone = customerPhone.trim(),
                name = customerName.trim(),
                lastVisitTimestamp = System.currentTimeMillis()
            )
        )
        addBrandIfNew(brand)
        addModelIfNew(brand, model)
        addFaultIfNew(fault)
    }

    suspend fun deleteRepair(repairId: Long) = withContext(Dispatchers.IO) {
        repairItemDao.deleteItemsForRepair(repairId)
        paymentDao.deletePaymentsForRepair(repairId)
        statusHistoryDao.deleteHistoryForRepair(repairId)
        repairDao.deleteRepairById(repairId)
    }

    // Master Data Deletion/Editing
    suspend fun deleteBrand(brand: BrandEntity) = withContext(Dispatchers.IO) { brandDao.deleteBrand(brand) }
    suspend fun deleteModel(model: ModelEntity) = withContext(Dispatchers.IO) { modelDao.deleteModel(model) }
    suspend fun deleteFault(fault: FaultEntity) = withContext(Dispatchers.IO) { faultDao.deleteFault(fault) }
    suspend fun deleteRepairType(type: RepairTypeEntity) = withContext(Dispatchers.IO) { repairTypeDao.deleteRepairType(type) }

    // Direct access for backup & restore
    suspend fun getAllDataForBackup() = withContext(Dispatchers.IO) {
        BackupData(
            repairs = repairDao.getAllRepairsDirect(),
            repairItems = repairItemDao.getAllRepairItemsDirect(),
            payments = paymentDao.getAllPaymentsDirect(),
            statusHistory = statusHistoryDao.getAllHistoryDirect(),
            customers = customerDao.getAllCustomersDirect(),
            brands = brandDao.getAllBrandsDirect(),
            models = modelDao.getAllModelsDirect(),
            faults = faultDao.getAllFaultsDirect(),
            repairTypes = repairTypeDao.getAllRepairTypesDirect(),
            prices = priceDao.getAllPricesDirect(),
            settings = settingsDao.getSettings() ?: AppSettingsEntity()
        )
    }

    suspend fun restoreData(backup: BackupData) = withContext(Dispatchers.IO) {
        for (b in backup.brands) brandDao.insertBrand(b)
        for (m in backup.models) modelDao.insertModel(m)
        for (f in backup.faults) faultDao.insertFault(f)
        for (rt in backup.repairTypes) repairTypeDao.insertRepairType(rt)
        for (p in backup.prices) priceDao.insertPrice(p)
        for (c in backup.customers) customerDao.insertCustomer(c)
        for (r in backup.repairs) repairDao.insertRepair(r)
        for (ri in backup.repairItems) repairItemDao.insertItem(ri)
        for (pm in backup.payments) paymentDao.insertPayment(pm)
        for (sh in backup.statusHistory) statusHistoryDao.insertHistory(sh)
        settingsDao.insertOrUpdate(backup.settings)
    }
}

data class BackupData(
    val repairs: List<RepairEntity>,
    val repairItems: List<RepairItemEntity>,
    val payments: List<PaymentEntity>,
    val statusHistory: List<StatusHistoryEntity>,
    val customers: List<CustomerEntity>,
    val brands: List<BrandEntity>,
    val models: List<ModelEntity>,
    val faults: List<FaultEntity>,
    val repairTypes: List<RepairTypeEntity>,
    val prices: List<PriceEntity>,
    val settings: AppSettingsEntity
)
