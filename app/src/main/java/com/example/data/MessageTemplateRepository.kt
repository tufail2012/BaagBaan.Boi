package com.example.data

import android.content.Context
import android.util.Log
import com.example.util.SafeFirebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object MessageTemplateRepository {
    private const val TAG = "MsgTemplateRepo"
    private const val COLLECTION_APP_CONFIG = "app_config"
    private const val DOC_MESSAGE_TEMPLATES = "message_templates"

    val DEFINED_TEMPLATES = listOf(
        MessageTemplate(
            id = "booking_confirmation_official",
            name = "Official Digital Receipt (Booking Confirmation)",
            category = "Bookings",
            description = "Detailed official receipt with customer, order & payment breakdown",
            defaultText = """
                🧾 {{businessName}}
                Registration Number: {{registrationNumber}}
                {{businessAddress}}
                Contacts: {{businessContact}}

                OFFICIAL DIGITAL RECEIPT / BOOKING CONFIRMATION
                ----------------------------------
                FARMER / CUSTOMER DETAILS:
                • Receipt / Serial #: {{serialNumber}}
                • Booking Date: {{bookingDate}}
                • Customer Name: {{farmerName}}
                • Contact Phone: {{contactNumber}}
                • Address: {{address}}
                • Orchard / Location: {{location}}

                ORDER & SERVICE DETAILS:
                • Category: {{serviceCategory}}
                • Variety / Item: {{plantVariety}}
                • Quantity: {{quantity}} plants
                • Expected Delivery: {{expectedDelivery}}

                PAYMENT BREAKDOWN:
                • Total Amount: {{totalAmount}}
                • Advance Paid: {{amountPaid}}
                • Balance Due: {{remainingBalance}}
                • Payment Status: {{paymentStatus}}
                • Account No: {{accountNumber}}
                • IFSC Code: {{ifscCode}}
                • Account Holder: {{accountHolderName}}
                ----------------------------------
                Thank you for choosing Baagbaan Boi!
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "businessName", "registrationNumber", "businessAddress", "businessContact",
                "serialNumber", "bookingDate", "farmerName", "contactNumber", "address", "location",
                "serviceCategory", "plantVariety", "quantity", "expectedDelivery",
                "totalAmount", "amountPaid", "remainingBalance", "paymentStatus",
                "accountNumber", "ifscCode", "accountHolderName"
            ),
            sampleData = mapOf(
                "serialNumber" to "REC-00101",
                "bookingDate" to "15 Aug 2026",
                "farmerName" to "Ghulam Mohammad",
                "contactNumber" to "+919876543210",
                "address" to "Ramnagri, Shopian",
                "location" to "Upper Orchard Block B",
                "serviceCategory" to "Apple Plants",
                "plantVariety" to "Gala Schnico Red",
                "quantity" to "500",
                "expectedDelivery" to "November 2026",
                "totalAmount" to "₹2,50,000",
                "amountPaid" to "₹1,00,000",
                "remainingBalance" to "₹1,50,000",
                "paymentStatus" to "Advance Paid"
            )
        ),
        MessageTemplate(
            id = "payment_reminder",
            name = "Standard Payment Reminder",
            category = "Payments",
            description = "Friendly payment reminder with financial breakdown",
            defaultText = """
                Dear {{farmerName}},

                This is a friendly payment reminder for your {{serviceCategory}} booking ({{serialNumber}} - {{plantVariety}}).

                • Total Amount: {{totalAmount}}
                • Amount Paid: {{amountPaid}}
                • Remaining Balance: {{remainingBalance}}
                • Payment Status: {{paymentStatus}}

                Please clear the balance of {{remainingBalance}} at your earliest convenience. Thank you!
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "farmerName", "serviceCategory", "serialNumber", "plantVariety",
                "totalAmount", "amountPaid", "remainingBalance", "paymentStatus"
            ),
            sampleData = mapOf(
                "farmerName" to "Ghulam Mohammad",
                "serviceCategory" to "Apple Plants",
                "serialNumber" to "REC-00101",
                "plantVariety" to "Gala Schnico Red",
                "totalAmount" to "₹2,50,000",
                "amountPaid" to "₹1,00,000",
                "remainingBalance" to "₹1,50,000",
                "paymentStatus" to "Partial"
            )
        ),
        MessageTemplate(
            id = "thank_you_note",
            name = "Thank You Note",
            category = "Bookings",
            description = "Appreciation note after booking confirmation",
            defaultText = """
                Dear {{farmerName}},

                Thank you for booking {{serviceCategory}} ({{plantVariety}}) with us!

                Ref #: {{serialNumber}} | Quantity: {{quantity}} plants
                Booking Date: {{bookingDate}}

                We appreciate your trust in our nursery and wish you a fruitful harvest season!
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "farmerName", "serviceCategory", "plantVariety", "serialNumber", "quantity", "bookingDate"
            ),
            sampleData = mapOf(
                "farmerName" to "Ghulam Mohammad",
                "serviceCategory" to "Apple Plants",
                "plantVariety" to "Gala Schnico Red",
                "serialNumber" to "REC-00101",
                "quantity" to "500",
                "bookingDate" to "15 Aug 2026"
            )
        ),
        MessageTemplate(
            id = "delivery_tracking",
            name = "Delivery Tracking Schedule",
            category = "Tracking",
            description = "Fulfillment and delivery date notification",
            defaultText = """
                Dear {{farmerName}},

                Your {{serviceCategory}} order ({{serialNumber}} - {{plantVariety}}, Qty: {{quantity}}) is scheduled for delivery/fulfillment.

                • Expected Delivery: {{expectedDelivery}}
                • Remaining Balance: {{remainingBalance}}
                • Address: {{address}}
                • Orchard Location: {{location}}

                Thank you for choosing our agricultural service!
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "farmerName", "serviceCategory", "serialNumber", "plantVariety", "quantity",
                "expectedDelivery", "remainingBalance", "address", "location"
            ),
            sampleData = mapOf(
                "farmerName" to "Ghulam Mohammad",
                "serviceCategory" to "Apple Plants",
                "serialNumber" to "REC-00101",
                "plantVariety" to "Gala Schnico Red",
                "quantity" to "500",
                "expectedDelivery" to "15 Nov 2026",
                "remainingBalance" to "₹1,50,000",
                "address" to "Ramnagri, Shopian",
                "location" to "Main Orchard"
            )
        ),
        MessageTemplate(
            id = "quick_payment_reminder",
            name = "Quick Payment Reminder (One-Liner)",
            category = "Payments",
            description = "Compact WhatsApp reminder for pending payment dashboard",
            defaultText = """Dear {{farmerName}}, this is a gentle payment reminder regarding your booking (#{{serialNumber}} - {{serviceType}}). You have an outstanding balance of {{amountDue}}. Kindly arrange payment at your earliest convenience. Thank you! - {{businessName}}""",
            supportedPlaceholders = listOf(
                "farmerName", "serialNumber", "serviceType", "amountDue", "businessName"
            ),
            sampleData = mapOf(
                "farmerName" to "Ghulam Mohammad",
                "serialNumber" to "101",
                "serviceType" to "Apple Plants",
                "amountDue" to "₹1,50,000"
            )
        ),
        MessageTemplate(
            id = "whatsapp_booking_short",
            name = "WhatsApp Booking Confirmation (Card Summary)",
            category = "Bookings",
            description = "Structured summary sent from booking detail screen",
            defaultText = """
                🌱 *{{businessName}} - Booking Confirmation*

                Serial No: #{{serialNumber}}
                Farmer Name: {{farmerName}}
                Category: {{serviceType}}
                Variety: {{plantVariety}}
                Quantity: {{quantity}} Units

                💰 *Payment Summary:*
                Total Amount: {{totalAmount}}
                Paid So Far: {{amountPaid}}
                Remaining Balance: {{remainingBalance}}
                Status: {{paymentStatus}}

                Booking Date: {{bookingDate}}
                Thank you for choosing Baagbaan Boi!
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "businessName", "serialNumber", "farmerName", "serviceType", "plantVariety",
                "quantity", "totalAmount", "amountPaid", "remainingBalance", "paymentStatus", "bookingDate"
            ),
            sampleData = mapOf(
                "serialNumber" to "101",
                "farmerName" to "Ghulam Mohammad",
                "serviceType" to "Apple Plants",
                "plantVariety" to "Gala Schnico Red",
                "quantity" to "500",
                "totalAmount" to "₹2,50,000",
                "amountPaid" to "₹1,00,000",
                "remainingBalance" to "₹1,50,000",
                "paymentStatus" to "Advance Paid",
                "bookingDate" to "15 Aug 2026"
            )
        ),
        MessageTemplate(
            id = "sms_booking_confirmation",
            name = "SMS Booking Confirmation",
            category = "Bookings",
            description = "Compact plain SMS confirmation",
            defaultText = """
                {{businessName}} - Booking Confirmation
                Serial No: #{{serialNumber}}
                Farmer: {{farmerName}}
                Service: {{serviceType}} ({{plantVariety}})
                Total: {{totalAmount}} | Paid: {{amountPaid}} | Balance: {{remainingBalance}}
                Status: {{paymentStatus}}
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "businessName", "serialNumber", "farmerName", "serviceType", "plantVariety",
                "totalAmount", "amountPaid", "remainingBalance", "paymentStatus"
            ),
            sampleData = mapOf(
                "serialNumber" to "101",
                "farmerName" to "Ghulam Mohammad",
                "serviceType" to "Apple Plants",
                "plantVariety" to "Gala Schnico Red",
                "totalAmount" to "₹2,50,000",
                "amountPaid" to "₹1,00,000",
                "remainingBalance" to "₹1,50,000",
                "paymentStatus" to "Advance Paid"
            )
        ),
        MessageTemplate(
            id = "whatsapp_tracking_details",
            name = "WhatsApp Installment Tracking Details",
            category = "Tracking",
            description = "Detailed log of all payment installments and delivery status",
            defaultText = """
                📦 *{{businessName}} - Booking Tracking Details*

                Serial No: #{{serialNumber}}
                Customer: {{farmerName}}
                Service: {{serviceType}}
                Item / Variety: {{plantVariety}}
                Quantity: {{quantity}} Units
                Location: {{location}}
                Expected Delivery: {{expectedDelivery}}

                💳 *Installment Payment Tracking:*
                Total Record Value: {{totalAmount}}
                Total Paid So Far: {{amountPaid}}
                Remaining Balance Due: {{remainingBalance}}
                Status: {{paymentStatus}}

                📜 *Payment History Log:*
                {{paymentHistory}}

                Thank you for trusting Baagbaan Boi!
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "businessName", "serialNumber", "farmerName", "serviceType", "plantVariety",
                "quantity", "location", "expectedDelivery", "totalAmount", "amountPaid",
                "remainingBalance", "paymentStatus", "paymentHistory"
            ),
            sampleData = mapOf(
                "serialNumber" to "101",
                "farmerName" to "Ghulam Mohammad",
                "serviceType" to "Apple Plants",
                "plantVariety" to "Gala Schnico Red",
                "quantity" to "500",
                "location" to "Ramnagri, Shopian",
                "expectedDelivery" to "15 Nov 2026",
                "totalAmount" to "₹2,50,000",
                "amountPaid" to "₹1,00,000",
                "remainingBalance" to "₹1,50,000",
                "paymentStatus" to "Advance Paid",
                "paymentHistory" to "1. 10 Aug 2026: ₹50,000 (Advance - Cash)\n2. 15 Aug 2026: ₹50,000 (UPI)"
            )
        ),
        MessageTemplate(
            id = "simple_booking_confirmation",
            name = "Simple Booking Confirmation (Quick Modal)",
            category = "Bookings",
            description = "Quick popup template option 1 in WhatsApp dialog",
            defaultText = """
                Hello {{farmerName}},

                Your booking for *{{serviceType}}* with *{{businessName}}* is confirmed!

                • Farmer Name: {{farmerName}}
                • Service: {{serviceType}}
                • Total Amount: {{totalAmount}}
                • Status: {{paymentStatus}}

                Thank you for choosing {{businessName}}!
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "farmerName", "serviceType", "businessName", "totalAmount", "paymentStatus"
            ),
            sampleData = mapOf(
                "farmerName" to "Ghulam Mohammad",
                "serviceType" to "Apple Plants",
                "totalAmount" to "₹2,50,000",
                "paymentStatus" to "Confirmed"
            )
        ),
        MessageTemplate(
            id = "simple_payment_reminder",
            name = "Simple Payment Reminder (Quick Modal)",
            category = "Payments",
            description = "Quick popup template option 2 in WhatsApp dialog",
            defaultText = """
                Dear {{farmerName}},

                This is a friendly payment reminder regarding your *{{serviceType}}* service with *{{businessName}}*.

                • Total Amount: {{totalAmount}}
                • Amount Paid: {{amountPaid}}
                • Pending Balance: {{remainingBalance}}

                Kindly complete the remaining payment at your earliest convenience. Thank you!
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "farmerName", "serviceType", "businessName", "totalAmount", "amountPaid", "remainingBalance"
            ),
            sampleData = mapOf(
                "farmerName" to "Ghulam Mohammad",
                "serviceType" to "Apple Plants",
                "totalAmount" to "₹2,50,000",
                "amountPaid" to "₹1,00,000",
                "remainingBalance" to "₹1,50,000"
            )
        ),
        MessageTemplate(
            id = "digital_receipt_summary",
            name = "Digital Receipt Text Summary",
            category = "Bookings",
            description = "Quick popup template option 3 in WhatsApp dialog accompanying image",
            defaultText = """
                🌾 *{{businessName}} - OFFICIAL DIGITAL RECEIPT* 🌾
                Registration Number: {{registrationNumber}}

                • Receipt #: {{serialNumber}}
                • Date: {{date}}
                • Customer Name: {{farmerName}}
                • Service Category: {{serviceType}}
                • Total Amount: {{totalAmount}}
                • Amount Paid: {{amountPaid}}
                • Balance Due: {{remainingBalance}}
                • Payment Status: {{paymentStatus}}
                • Account No: {{accountNumber}}
                • IFSC Code: {{ifscCode}}

                Thank you for doing business with {{businessName}}!
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "businessName", "registrationNumber", "serialNumber", "date", "farmerName",
                "serviceType", "totalAmount", "amountPaid", "remainingBalance", "paymentStatus",
                "accountNumber", "ifscCode"
            ),
            sampleData = mapOf(
                "serialNumber" to "REC-00101",
                "date" to "15 Aug 2026",
                "farmerName" to "Ghulam Mohammad",
                "serviceType" to "Apple Plants",
                "totalAmount" to "₹2,50,000",
                "amountPaid" to "₹1,00,000",
                "remainingBalance" to "₹1,50,000",
                "paymentStatus" to "Advance Paid"
            )
        ),
        MessageTemplate(
            id = "garden_booking_note",
            name = "Garden Planning Booking Note",
            category = "Garden",
            description = "WhatsApp message sent for garden design & planning bookings",
            defaultText = """Dear {{farmerName}}, regarding your Garden Planning booking (#{{serialNumber}}): Total Cost {{totalCost}}, Payment Status: {{paymentStatus}}.""",
            supportedPlaceholders = listOf(
                "farmerName", "serialNumber", "totalCost", "paymentStatus"
            ),
            sampleData = mapOf(
                "farmerName" to "Ghulam Mohammad",
                "serialNumber" to "GP-201",
                "totalCost" to "₹75,000",
                "paymentStatus" to "Confirmed"
            )
        ),
        MessageTemplate(
            id = "garden_tracking_note",
            name = "Garden Planning Tracking Note",
            category = "Garden",
            description = "WhatsApp tracking status message for garden planning",
            defaultText = """Dear {{farmerName}}, tracking details for your Garden Planning booking (#{{serialNumber}}): Status - {{paymentStatus}}, Delivery expected: {{expectedDelivery}}.""",
            supportedPlaceholders = listOf(
                "farmerName", "serialNumber", "paymentStatus", "expectedDelivery"
            ),
            sampleData = mapOf(
                "farmerName" to "Ghulam Mohammad",
                "serialNumber" to "GP-201",
                "paymentStatus" to "In Progress",
                "expectedDelivery" to "20 Nov 2026"
            )
        ),
        MessageTemplate(
            id = "worker_payroll_receipt",
            name = "Worker Payroll Digital Receipt",
            category = "Attendance",
            description = "Official wage & attendance receipt shared with workers",
            defaultText = """
                🧾 *{{businessName}} - OFFICIAL DIGITAL RECEIPT*
                Registration Number: {{registrationNumber}}
                ------------------------------------------
                📅 *Date:* {{date}}
                👤 *Worker Name:* {{workerName}}
                📞 *Phone:* {{phoneNumber}}

                📊 *ATTENDANCE SUMMARY*
                • Present Days: {{presentDays}}
                • Absent Days: {{absentDays}}
                • Daily Rate: {{dailyRate}}

                💰 *PAYROLL BREAKDOWN*
                • Gross Earnings: {{grossEarnings}}
                • Total Advances Paid: {{totalAdvance}}
                • Net Balance Remaining: {{netBalance}}
                • Account No: {{accountNumber}}
                • IFSC Code: {{ifscCode}}
                {{advancePaymentsHistory}}
                ------------------------------------------
                This is an official receipt of {{businessName}}.
            """.trimIndent(),
            supportedPlaceholders = listOf(
                "businessName", "registrationNumber", "date", "workerName", "phoneNumber",
                "presentDays", "absentDays", "dailyRate", "grossEarnings", "totalAdvance",
                "netBalance", "accountNumber", "ifscCode", "advancePaymentsHistory"
            ),
            sampleData = mapOf(
                "date" to "15 Aug 2026, 10:00 AM",
                "workerName" to "Tariq Ahmad",
                "phoneNumber" to "+919876543210",
                "presentDays" to "24",
                "absentDays" to "2",
                "dailyRate" to "₹600/day",
                "grossEarnings" to "₹14,400",
                "totalAdvance" to "₹4,000",
                "netBalance" to "₹10,400",
                "advancePaymentsHistory" to "\n📝 *RECORDED ADVANCE PAYMENTS*\n• 01 Aug 2026: ₹2,000\n• 08 Aug 2026: ₹2,000"
            )
        )
    )

    private val defaultMap: Map<String, String> = DEFINED_TEMPLATES.associate { it.id to it.defaultText }
    private const val PREFS_NAME = "AgriCropMessageTemplatePrefs"
    private const val COLLECTION_USERS = "users"

    private val _templatesState = MutableStateFlow<Map<String, String>>(defaultMap)
    val templatesState: StateFlow<Map<String, String>> = _templatesState.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null
    private var isListening = false

    private fun getPrefs(context: Context?): android.content.SharedPreferences? {
        return context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadFromPrefs(context: Context?) {
        val prefs = getPrefs(context) ?: return
        val all = prefs.all
        if (all.isNotEmpty()) {
            val merged = defaultMap.toMutableMap()
            all.forEach { (k, v) ->
                if (v is String && v.isNotBlank()) {
                    merged[k] = v
                }
            }
            _templatesState.value = merged
        }
    }

    private fun saveToPrefs(templateId: String, content: String, context: Context?) {
        val prefs = getPrefs(context) ?: return
        prefs.edit().putString(templateId, content).apply()
    }

    private fun getDocumentRef(context: Context? = null): DocumentReference? {
        val uid = SafeFirebase.auth?.currentUser?.uid ?: return null
        val firestore: FirebaseFirestore = SafeFirebase.getDb(context) ?: return null
        return firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_APP_CONFIG).document(DOC_MESSAGE_TEMPLATES)
    }

    @Synchronized
    fun startListening(context: Context? = null) {
        loadFromPrefs(context)

        if (isListening && listenerRegistration != null) return

        val docRef = getDocumentRef(context)
        if (docRef == null) {
            Log.d(TAG, "Unauthenticated or Firestore DB unavailable, retaining templates in memory & preferences.")
            return
        }

        try {
            listenerRegistration?.remove()
            isListening = true

            listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(TAG, "Permission denied for remote MessageTemplates; using local storage.")
                    } else {
                        Log.w(TAG, "SnapshotListener note on message templates: ${error.message}")
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        val merged = defaultMap.toMutableMap()
                        data.forEach { (key, value) ->
                            if (value is String && value.isNotBlank()) {
                                merged[key] = value
                                saveToPrefs(key, value, context)
                            }
                        }
                        _templatesState.value = merged
                        Log.d(TAG, "Realtime MessageTemplates updated from Firestore (${merged.size} templates)")
                    }
                } else if (snapshot != null && !snapshot.exists()) {
                    Log.i(TAG, "Document $COLLECTION_APP_CONFIG/$DOC_MESSAGE_TEMPLATES not found. Seeding with default templates...")
                    docRef.set(_templatesState.value)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully seeded message templates to Firestore.")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to seed templates: ${e.message}")
                        }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach snapshot listener for message templates: ${e.message}")
            isListening = false
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        isListening = false
    }

    fun getTemplateText(templateId: String): String {
        return _templatesState.value[templateId] ?: defaultMap[templateId] ?: ""
    }

    fun getDefaultText(templateId: String): String {
        return defaultMap[templateId] ?: ""
    }

    fun renderTemplate(
        templateId: String,
        data: Map<String, String> = emptyMap(),
        customTemplateString: String? = null
    ): String {
        val rawTemplate = customTemplateString ?: getTemplateText(templateId)
        val businessInfo = BusinessInfoRepository.currentBusinessInfo

        // Combine default business info values with user-supplied data
        val resolvedMap = mutableMapOf<String, String>()
        resolvedMap["businessName"] = businessInfo.businessName
        resolvedMap["businessTagline"] = businessInfo.tagline
        resolvedMap["businessAddress"] = businessInfo.address
        resolvedMap["businessContact"] = businessInfo.contactNumbers.joinToString(", ")
        resolvedMap["accountNumber"] = businessInfo.accountNumber
        resolvedMap["accountHolderName"] = businessInfo.accountHolderName
        resolvedMap["ifscCode"] = businessInfo.ifscCode
        resolvedMap["registrationNumber"] = businessInfo.registrationNumber

        // Overlay with provided data
        data.forEach { (k, v) ->
            resolvedMap[k] = v
        }

        var result = rawTemplate
        // Replace all {{placeholder}} occurrences
        val regex = Regex("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}")
        result = regex.replace(result) { matchResult ->
            val placeholderKey = matchResult.groupValues[1]
            resolvedMap[placeholderKey] ?: matchResult.value
        }

        return result
    }

    suspend fun saveTemplate(
        templateId: String,
        newContent: String,
        context: Context? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        saveToPrefs(templateId, newContent, context)
        val current = _templatesState.value.toMutableMap()
        current[templateId] = newContent
        _templatesState.value = current

        val docRef = getDocumentRef(context)
        if (docRef != null) {
            try {
                val updatePayload = mapOf(templateId to newContent)
                docRef.set(updatePayload, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Remote sync for template $templateId deferred/failed: ${e.message}")
            }
        }
        Result.success(Unit)
    }

    suspend fun resetTemplate(
        templateId: String,
        context: Context? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val def = getDefaultText(templateId)
        saveTemplate(templateId, def, context)
    }
}
