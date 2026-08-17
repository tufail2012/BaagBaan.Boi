package com.example.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.data.calculateRemainingBalance
import com.example.util.SafeFirebase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PendingPaymentsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val authUser = try {
            SafeFirebase.getAuth(context)?.currentUser
        } catch (e: Exception) {
            null
        }

        if (authUser == null) {
            provideContent {
                SignedOutWidgetContent(context)
            }
            return
        }

        // Fetch records safely from local Room database
        val db = AppDatabase.getDatabase(context)
        val cropRecords = try {
            db.cropRecordDao().getAllRecordsList()
        } catch (e: Exception) {
            emptyList<CropRecord>()
        }
        val gardenEntries = try {
            db.gardenPlanningDao().getAllEntriesList()
        } catch (e: Exception) {
            emptyList<GardenPlanningEntry>()
        }

        // Exact query/filter logic from PaymentRemindersDialog
        var pendingCount = 0
        var totalOutstanding = 0.0

        cropRecords.forEach { crop ->
            val remDue = crop.calculateRemainingBalance()
            val isPending = !crop.paymentStatus.equals("Fully Paid", ignoreCase = true) && remDue > 0.01
            if (isPending) {
                pendingCount++
                totalOutstanding += remDue
            }
        }

        gardenEntries.forEach { garden ->
            val remDue = (garden.totalCost - garden.amountPaid).coerceAtLeast(0.0)
            val isPending = !garden.paymentStatus.equals("Fully Paid", ignoreCase = true) && remDue > 0.01
            if (isPending) {
                pendingCount++
                totalOutstanding += remDue
            }
        }

        val nearestUpcomingDate = findNearestUpcomingDate(cropRecords, gardenEntries)

        val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        numberFormat.maximumFractionDigits = 0
        val formattedTotal = numberFormat.format(totalOutstanding)

        provideContent {
            WidgetContent(
                context = context,
                pendingCount = pendingCount,
                formattedTotal = formattedTotal,
                nearestUpcomingDate = nearestUpcomingDate
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun SignedOutWidgetContent(context: Context) {
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1E293B)))
                .cornerRadius(16.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(clickIntent)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_apple_logo),
                        contentDescription = "App Icon",
                        modifier = GlanceModifier.size(24.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "BAAGBAAN BOI",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF86EFAC)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "Sign in to Baagbaan Boi to see updates",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(
        context: Context,
        pendingCount: Int,
        formattedTotal: String,
        nearestUpcomingDate: String?
    ) {
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.baagbaan.boi.ACTION_OPEN_PAYMENT_REMINDERS"
            data = Uri.parse("baagbaanboi://payments")
            putExtra("OPEN_PAYMENT_REMINDERS", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val bgColor = if (pendingCount > 0) Color(0xFF131D24) else Color(0xFF0F201B)
        val accentColor = if (pendingCount > 0) Color(0xFFFF6B6B) else Color(0xFF4ADE80)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(bgColor))
                .cornerRadius(18.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(clickIntent))
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header: Branding + Status Chip
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_apple_logo),
                        contentDescription = "App Icon",
                        modifier = GlanceModifier.size(22.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "BAAGBAAN BOI",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFE2E8F0)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(if (pendingCount > 0) Color(0x33FF6B6B) else Color(0x334ADE80)))
                            .cornerRadius(10.dp)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (pendingCount > 0) "$pendingCount Pending" else "All Clear",
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Body: Main Amount & Description or Empty State
                if (pendingCount > 0) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "Pending Dues",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF94A3B8)),
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = formattedTotal,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (nearestUpcomingDate != null) {
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Next Delivery",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFF94A3B8)),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = nearestUpcomingDate,
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFF38BDF8)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Footer hint
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap to open Payment Reminders →",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF64748B)),
                                fontSize = 10.sp
                            )
                        )
                    }
                } else {
                    // Empty State: Calm & clear
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No pending payments",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = if (nearestUpcomingDate != null) "Next delivery: $nearestUpcomingDate" else "All customer balances are fully settled",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF86EFAC)),
                                fontSize = 11.sp
                            )
                        )
                    }

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap to view orders →",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF64748B)),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }

    private fun findNearestUpcomingDate(
        cropRecords: List<CropRecord>,
        gardenEntries: List<GardenPlanningEntry>
    ): String? {
        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("dd MMM yyyy", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US)
        )

        val outFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = nowCal.time

        var nearestDate: Date? = null

        fun checkDateStr(dateStr: String?) {
            if (dateStr.isNullOrBlank()) return
            for (sdf in dateFormats) {
                try {
                    val parsed = sdf.parse(dateStr.trim())
                    if (parsed != null && !parsed.before(today)) {
                        if (nearestDate == null || parsed.before(nearestDate)) {
                            nearestDate = parsed
                        }
                        break
                    }
                } catch (_: Exception) {
                }
            }
        }

        cropRecords.forEach { crop ->
            checkDateStr(crop.expectedDelivery)
            if (crop.expectedDelivery.isBlank()) {
                checkDateStr(crop.bookingDate)
            }
        }

        gardenEntries.forEach { garden ->
            checkDateStr(garden.expectedDelivery)
            if (garden.expectedDelivery.isBlank()) {
                checkDateStr(garden.bookingDate)
            }
        }

        return nearestDate?.let { outFormat.format(it) }
    }
}
