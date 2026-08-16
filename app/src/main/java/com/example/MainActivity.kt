package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.data.CropRecordRepository
import com.example.data.GardenPlanningRepository
import com.example.security.AppLockManager
import com.example.ui.AgriCropMainScreen
import com.example.ui.AppThemeMode
import com.example.ui.AttendanceViewModel
import com.example.ui.AttendanceViewModelFactory
import com.example.ui.CropViewModel
import com.example.ui.CropViewModelFactory
import com.example.ui.theme.MyApplicationTheme

import com.example.data.NotificationRepository
import com.example.notifications.NotificationHelper
import com.example.ui.NotificationViewModel
import com.example.ui.NotificationViewModelFactory

import com.example.data.FirestoreSyncManager
import kotlinx.coroutines.Dispatchers
import android.os.Build
import android.view.WindowManager
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure window privacy flag is cleared so screenshots, screen recording, and sharing work seamlessly
        window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

        com.example.util.SafeFirebase.init(this)

        appLockManager = AppLockManager.getInstance(applicationContext)
        appLockManager.applySecureWindowFlag(this)

        // Programmatically request maximum display refresh rate (120Hz / 90Hz / 144Hz)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val defaultDisplay = window?.windowManager?.defaultDisplay
                @Suppress("DEPRECATION")
                val modes = defaultDisplay?.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    window?.attributes = window?.attributes?.apply {
                        preferredDisplayModeId = maxMode.modeId
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback if device display mode is restricted
        }

        enableEdgeToEdge()

        NotificationHelper.createNotificationChannels(this)

        val database = AppDatabase.getDatabase(this, lifecycleScope)
        val gardenPlanningRepository = GardenPlanningRepository(database.gardenPlanningDao(), database.farmerContactDao(), database.recycleBinDao())
        val cropRepository = CropRecordRepository(database.cropRecordDao(), database.farmerContactDao(), database.recycleBinDao(), database.inventoryDao())
        val cropFactory = CropViewModelFactory(cropRepository, gardenPlanningRepository)
        val cropViewModel = ViewModelProvider(this, cropFactory)[CropViewModel::class.java]

        val attendanceRepository = AttendanceRepository(database.attendanceDao())
        val attendanceFactory = AttendanceViewModelFactory(attendanceRepository)
        val attendanceViewModel = ViewModelProvider(this, attendanceFactory)[AttendanceViewModel::class.java]

        val notificationRepository = NotificationRepository(database.notificationDao())
        val notificationFactory = NotificationViewModelFactory(notificationRepository)
        val notificationViewModel = ViewModelProvider(this, notificationFactory)[NotificationViewModel::class.java]

        // Asynchronously sync Cloud Firestore database records (Local Plants, Imported Plants, Pruning, Site Visit, Rootstocks, Bookings, Attendance & Inventory)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val syncManager = FirestoreSyncManager()
                syncManager.syncFromCloudToLocal(database.cropRecordDao(), database.attendanceDao())
                syncManager.syncInventoryFromCloudToLocal(database.inventoryDao())
            } catch (e: Exception) {
                // Log or ignore network error during initial offline sync
            }
        }

        cropViewModel.loadThemeSettings(this)

        cropViewModel.setOnBookingSavedListener { farmerName, serviceType, serialNo, expectedDelivery ->
            notificationViewModel.sendBookingConfirmation(
                context = this,
                farmerName = farmerName,
                serviceType = serviceType,
                serialNo = serialNo
            )
            if (expectedDelivery.isNotBlank()) {
                notificationViewModel.sendReminder(
                    context = this,
                    title = "Expected Delivery Reminder",
                    message = "Delivery for $farmerName ($serviceType) is scheduled for $expectedDelivery."
                )
            }
        }

        setContent {
            val themeMode by cropViewModel.themeMode.collectAsState()
            val accentHex by cropViewModel.accentColorHex.collectAsState()

            val accentColor = androidx.compose.runtime.remember(accentHex) {
                try {
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(accentHex))
                } catch (e: Exception) {
                    com.example.ui.theme.AgriRedPrimary
                }
            }

            MyApplicationTheme(
                themeMode = themeMode,
                accentColor = accentColor
            ) {
                AgriCropMainScreen(
                    viewModel = cropViewModel,
                    attendanceViewModel = attendanceViewModel,
                    notificationViewModel = notificationViewModel,
                    appLockManager = appLockManager
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appLockManager.isInitialized) {
            appLockManager.applySecureWindowFlag(this)
        }
    }
}

