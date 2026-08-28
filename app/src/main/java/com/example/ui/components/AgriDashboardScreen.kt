package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.data.UserBooking
import com.example.data.calculateRemainingBalance
import com.example.data.calculateTotalAmount
import com.example.data.isPaymentCleared
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.ui.components.BrandedPullToRefreshBox
import com.example.ui.CropViewModel
import com.example.ui.GardenPlanningViewModel
import com.example.ui.UserDashboardViewModel
import com.example.ui.theme.getSectionAccentColor

@Composable
fun AgriDashboardScreen(
    viewModel: CropViewModel,
    userDashboardViewModel: UserDashboardViewModel,
    gardenPlanningViewModel: GardenPlanningViewModel? = null,
    currentUserEmail: String? = null,
    onBack: () -> Unit,
    onNavigateToCategory: ((String) -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val allRecords by viewModel.allRecords.collectAsState()
    val gardenEntries by (gardenPlanningViewModel?.allEntries ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val rawBookings by userDashboardViewModel.rawBookings.collectAsState()
    val accentColorHex by viewModel.accentColorHex.collectAsState()
    val isDark = isAppInDarkMode()

    val parsedPaletteColor = remember(accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(accentColorHex))
        } catch (e: Exception) {
            null
        }
    }
    val dashboardAccent = getSectionAccentColor(
        "Dashboard",
        customPaletteColor = parsedPaletteColor,
        defaultColor = MaterialTheme.colorScheme.primary
    )

    val context = LocalContext.current
    var currentUser by remember {
        mutableStateOf(
            try {
                com.example.util.SafeFirebase.getAuth(context)?.currentUser
            } catch (e: Throwable) {
                null
            }
        )
    }

    DisposableEffect(context) {
        val auth = try {
            com.example.util.SafeFirebase.getAuth(context)
        } catch (e: Throwable) {
            null
        }
        val listener = FirebaseAuth.AuthStateListener { a ->
            currentUser = a.currentUser
        }
        auth?.addAuthStateListener(listener)
        onDispose {
            auth?.removeAuthStateListener(listener)
        }
    }

    var selectedFilterTab by remember { mutableStateOf("All") }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var isInitialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(allRecords, gardenEntries) {
        if (allRecords.isNotEmpty() || gardenEntries.isNotEmpty()) {
            isInitialLoading = false
        } else {
            kotlinx.coroutines.delay(300)
            isInitialLoading = false
        }
    }

    val filteredLogRecords = remember(allRecords, rawBookings, selectedFilterTab) {
        when (selectedFilterTab) {
            "Pending" -> allRecords.filter { !it.isPaymentCleared() }
            "Paid" -> allRecords.filter { it.isPaymentCleared() }
            "Bookings" -> emptyList()
            else -> allRecords
        }
    }

    // Computations for CropRecords
    val importedRecords = remember(allRecords) {
        allRecords.filter { it.serviceType.equals("Imported", ignoreCase = true) }
    }
    val localRecords = remember(allRecords) {
        allRecords.filter { it.serviceType.equals("Local Plants", ignoreCase = true) }
    }
    val rootstockRecords = remember(allRecords) {
        allRecords.filter { it.serviceType.equals("Rootstocks", ignoreCase = true) || it.serviceType.contains("Rootstock", ignoreCase = true) }
    }
    val siteVisitRecords = remember(allRecords) {
        allRecords.filter { it.serviceType.equals("Site Visit", ignoreCase = true) }
    }
    val pruningRecords = remember(allRecords) {
        allRecords.filter { it.serviceType.equals("Pruning", ignoreCase = true) }
    }

    // Garden Planning Financial Metrics
    val gardenRevenue = remember(gardenEntries) { gardenEntries.sumOf { it.totalCost } }
    val gardenPaid = remember(gardenEntries) { gardenEntries.sumOf { it.amountPaid } }
    val gardenRemaining = remember(gardenEntries) { gardenEntries.sumOf { it.remainingBalance } }

    val gardenFullyPaidCount = remember(gardenEntries) { gardenEntries.count { it.paymentStatus == "Fully Paid" } }
    val gardenAdvancePaidCount = remember(gardenEntries) { gardenEntries.count { it.paymentStatus == "Advance Paid" } }
    val gardenPendingCount = remember(gardenEntries) { gardenEntries.count { it.paymentStatus == "Pending" || it.paymentStatus == "Unpaid" } }

    // Aggregate Financial Metrics
    val totalRevenue = remember(allRecords, gardenEntries) {
        allRecords.sumOf { it.calculateTotalAmount() } + gardenRevenue
    }
    val totalPaid = remember(allRecords, gardenEntries) {
        allRecords.sumOf { it.amountPaid } + gardenPaid
    }
    val totalRemaining = remember(allRecords, gardenEntries) {
        allRecords.sumOf { it.calculateRemainingBalance() } + gardenRemaining
    }

    val fullyPaidCount = remember(allRecords, gardenEntries) {
        allRecords.count { it.isPaymentCleared() } + gardenFullyPaidCount
    }
    val advancePaidCount = remember(allRecords, gardenEntries) {
        allRecords.count { !it.isPaymentCleared() && it.amountPaid > 0 } + gardenAdvancePaidCount
    }
    val pendingCount = remember(allRecords, gardenEntries) {
        allRecords.count { !it.isPaymentCleared() && it.amountPaid <= 0 } + gardenPendingCount
    }
    val totalRecordsCount = remember(allRecords, gardenEntries) {
        allRecords.size + gardenEntries.size
    }

    val paidRatio = if (totalRevenue > 0) (totalPaid / totalRevenue).toFloat().coerceIn(0f, 1f) else 0f

    val dashboardBgBrush = remember(isDark, dashboardAccent) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    dashboardAccent.copy(alpha = 0.05f),
                    Color(0xFF0B1120),
                    Color(0xFF060911)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8FAFC),
                    dashboardAccent.copy(alpha = 0.035f),
                    Color(0xFFF1F5F9),
                    Color(0xFFFFFFFF)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(dashboardBgBrush)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Floating Glass Top Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .glassCardBackground(
                        isDark = isDark,
                        accentColor = dashboardAccent,
                        shape = CircleShape
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("dashboard_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = dashboardAccent
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(dashboardAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Dashboard Icon",
                                tint = dashboardAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "AgriCrop Operations Dashboard",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Comprehensive Operations & Financial Overview",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (onNavigateToSettings != null) {
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("dashboard_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings & Security",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { userDashboardViewModel.refreshUser() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("dashboard_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Data",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            BrandedPullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (isRefreshing) return@BrandedPullToRefreshBox
                    isRefreshing = true
                    coroutineScope.launch {
                        try {
                            userDashboardViewModel.refreshUser()
                            delay(600)
                        } catch (_: Exception) {
                        } finally {
                            isRefreshing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }

                // 1. Account & System Banner Card
                item {
                    AccountBannerCard(
                        currentUser = currentUser,
                        totalEntriesCount = totalRecordsCount,
                        totalVolume = totalRevenue,
                        accentColor = dashboardAccent,
                        isDark = isDark
                    )
                }

                // 2. Financial Overview & Payment Breakdown Card
                item {
                    FinancialBreakdownCard(
                        totalRevenue = totalRevenue,
                        totalPaid = totalPaid,
                        totalRemaining = totalRemaining,
                        paidRatio = paidRatio,
                        fullyPaidCount = fullyPaidCount,
                        advancePaidCount = advancePaidCount,
                        pendingCount = pendingCount,
                        totalRecordsCount = totalRecordsCount,
                        accentColor = dashboardAccent,
                        isDark = isDark
                    )
                }

                // 3. Category Summaries Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = dashboardAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Operational Category Summaries",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "6 Modules",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = dashboardAccent
                        )
                    }
                }

                // 4. Summaries Grid
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Imported Plants
                        CategorySummaryCard(
                            title = "Imported Plants",
                            icon = Icons.Default.LocalShipping,
                            badgeColor = Color(0xFF0288D1),
                            recordsCount = importedRecords.size,
                            totalQuantity = importedRecords.sumOf { it.quantity },
                            unitLabel = "Plants",
                            totalValue = importedRecords.sumOf { it.calculateTotalAmount() },
                            remainingBalance = importedRecords.sumOf { it.calculateRemainingBalance() },
                            paidCount = importedRecords.count { it.isPaymentCleared() },
                            onClick = { onNavigateToCategory?.invoke("Imported") },
                            isDark = isDark
                        )

                        // Local Plants
                        CategorySummaryCard(
                            title = "Local Plants",
                            icon = Icons.Outlined.LocalFlorist,
                            badgeColor = Color(0xFF2E7D32),
                            recordsCount = localRecords.size,
                            totalQuantity = localRecords.sumOf { it.quantity },
                            unitLabel = "Saplings",
                            totalValue = localRecords.sumOf { it.calculateTotalAmount() },
                            remainingBalance = localRecords.sumOf { it.calculateRemainingBalance() },
                            paidCount = localRecords.count { it.isPaymentCleared() },
                            onClick = { onNavigateToCategory?.invoke("Local Plants") },
                            isDark = isDark
                        )

                        // Rootstock Inventories
                        CategorySummaryCard(
                            title = "Rootstock Inventories",
                            icon = Icons.Default.Spa,
                            badgeColor = Color(0xFFED6C02),
                            recordsCount = rootstockRecords.size,
                            totalQuantity = rootstockRecords.sumOf { it.quantity },
                            unitLabel = "Rootstocks",
                            totalValue = rootstockRecords.sumOf { it.calculateTotalAmount() },
                            remainingBalance = rootstockRecords.sumOf { it.calculateRemainingBalance() },
                            paidCount = rootstockRecords.count { it.isPaymentCleared() },
                            onClick = { onNavigateToCategory?.invoke("Rootstocks") },
                            isDark = isDark
                        )

                        // Site Visit Observations
                        CategorySummaryCard(
                            title = "Site Visit Observations",
                            icon = Icons.Outlined.Assignment,
                            badgeColor = Color(0xFF9C27B0),
                            recordsCount = siteVisitRecords.size,
                            totalQuantity = siteVisitRecords.sumOf { it.landAreaAcres.toInt().coerceAtLeast(1) },
                            unitLabel = "Acres Visited",
                            totalValue = siteVisitRecords.sumOf { it.calculateTotalAmount() },
                            remainingBalance = siteVisitRecords.sumOf { it.calculateRemainingBalance() },
                            paidCount = siteVisitRecords.count { it.isPaymentCleared() },
                            onClick = { onNavigateToCategory?.invoke("Site Visit") },
                            isDark = isDark
                        )

                        // Pruning Records
                        CategorySummaryCard(
                            title = "Pruning Records",
                            icon = Icons.Default.ContentCut,
                            badgeColor = Color(0xFFD32F2F),
                            recordsCount = pruningRecords.size,
                            totalQuantity = pruningRecords.sumOf { it.quantity },
                            unitLabel = "Trees / Acres",
                            totalValue = pruningRecords.sumOf { it.calculateTotalAmount() },
                            remainingBalance = pruningRecords.sumOf { it.calculateRemainingBalance() },
                            paidCount = pruningRecords.count { it.isPaymentCleared() },
                            onClick = { onNavigateToCategory?.invoke("Pruning") },
                            isDark = isDark
                        )

                        // Garden Planning
                        CategorySummaryCard(
                            title = "Garden Planning",
                            icon = Icons.Default.Park,
                            badgeColor = Color(0xFF00897B),
                            recordsCount = gardenEntries.size,
                            totalQuantity = gardenEntries.sumOf { (it.totalKanalArea * it.plantsPerKanal).toInt() },
                            unitLabel = "Plants (Calculated)",
                            totalValue = gardenRevenue,
                            remainingBalance = gardenRemaining,
                            paidCount = gardenFullyPaidCount,
                            onClick = { onNavigateToCategory?.invoke("Garden Planning") },
                            isDark = isDark
                        )
                    }
                }

                // 5. Varieties & Inventory Snapshot
                item {
                    VarietyDistributionCard(
                        allRecords = allRecords,
                        accentColor = dashboardAccent,
                        isDark = isDark
                    )
                }

                // 6. Filterable Activity Log / Recent Records
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recent Operations Log",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tabs = listOf("All", "Pending", "Paid", "Garden Planning")
                            items(tabs) { tab ->
                                FilterChip(
                                    selected = selectedFilterTab == tab,
                                    onClick = { selectedFilterTab = tab },
                                    label = { Text(tab, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = dashboardAccent,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                if (selectedFilterTab == "Garden Planning") {
                    if (gardenEntries.isEmpty()) {
                        if (isInitialLoading) {
                            items(3) {
                                SkeletonCard(isDark = isDark, lineCount = 3, hasActionRow = false)
                            }
                        } else {
                            item {
                                EmptyStateCard(message = "No Garden Planning entries registered yet.", isDark = isDark)
                            }
                        }
                    } else {
                        items(gardenEntries.take(8)) { entry ->
                            GardenLogItemCard(entry = entry, isDark = isDark)
                        }
                    }
                } else {
                    if (filteredLogRecords.isEmpty()) {
                        if (isInitialLoading) {
                            items(3) {
                                SkeletonCard(isDark = isDark, lineCount = 3, hasActionRow = false)
                            }
                        } else {
                            item {
                                EmptyStateCard(message = "No records found matching current filter.", isDark = isDark)
                            }
                        }
                    } else {
                        items(filteredLogRecords.take(10)) { record ->
                            RecordLogItemCard(record = record, isDark = isDark)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
            }
        }
    }
}

@Composable
private fun AccountBannerCard(
    currentUser: FirebaseUser?,
    totalEntriesCount: Int,
    totalVolume: Double,
    accentColor: Color,
    isDark: Boolean
) {
    val primaryText = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.takeIf { it.isNotBlank() }
        ?: "Guest"

    val secondaryText = if (!currentUser?.displayName.isNullOrBlank() && !currentUser?.email.isNullOrBlank()) {
        currentUser!!.email!!
    } else if (currentUser != null) {
        "AgriCrop Cloud Sync Enabled"
    } else {
        "Local Guest Session"
    }

    val photoUrl = currentUser?.photoUrl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                isDark = isDark,
                accentColor = accentColor,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Account",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (currentUser != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                        ) {
                            Text(
                                text = if (currentUser != null) "ACTIVE" else "GUEST",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = secondaryText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$totalEntriesCount",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Text(
                    text = "Total Entries",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FinancialBreakdownCard(
    totalRevenue: Double,
    totalPaid: Double,
    totalRemaining: Double,
    paidRatio: Float,
    fullyPaidCount: Int,
    advancePaidCount: Int,
    pendingCount: Int,
    totalRecordsCount: Int,
    accentColor: Color,
    isDark: Boolean
) {
    val currencyFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                isDark = isDark,
                accentColor = accentColor,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Financial & Payment Status Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = "${(paidRatio * 100).toInt()}% Collected",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Summary 3-Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Volume
                FinancialMetricBox(
                    label = "Total Volume",
                    targetValue = totalRevenue,
                    formatter = { "₹${currencyFormat.format(it.toLong())}" },
                    icon = Icons.Default.TrendingUp,
                    accentColor = accentColor,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                // Paid / Received
                FinancialMetricBox(
                    label = "Amount Paid",
                    targetValue = totalPaid,
                    formatter = { "₹${currencyFormat.format(it.toLong())}" },
                    icon = Icons.Default.Payments,
                    accentColor = Color(0xFF2E7D32),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                // Remaining Balance
                FinancialMetricBox(
                    label = "Remaining",
                    targetValue = totalRemaining,
                    formatter = { "₹${currencyFormat.format(it.toLong())}" },
                    icon = Icons.Default.ReceiptLong,
                    accentColor = if (totalRemaining > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Payment Collection Progress", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(paidRatio * 100).toInt()}% Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
                LinearProgressIndicator(
                    progress = paidRatio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF2E7D32),
                    trackColor = Color(0xFFD32F2F).copy(alpha = 0.25f)
                )
            }

            Divider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))

            // Payment Status Counts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatusBadgeCount(
                    label = "Fully Paid",
                    count = fullyPaidCount,
                    total = totalRecordsCount,
                    color = Color(0xFF2E7D32),
                    icon = Icons.Default.CheckCircle
                )

                StatusBadgeCount(
                    label = "Advance Paid",
                    count = advancePaidCount,
                    total = totalRecordsCount,
                    color = Color(0xFFED6C02),
                    icon = Icons.Default.HourglassEmpty
                )

                StatusBadgeCount(
                    label = "Pending",
                    count = pendingCount,
                    total = totalRecordsCount,
                    color = Color(0xFFD32F2F),
                    icon = Icons.Default.ReceiptLong
                )
            }
        }
    }
}

@Composable
private fun FinancialMetricBox(
    label: String,
    targetValue: Double? = null,
    formatter: ((Double) -> String)? = null,
    value: String = "",
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassCardBackground(
                isDark = isDark,
                accentColor = accentColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (targetValue != null && formatter != null) {
                CountUpText(
                    targetValue = targetValue,
                    formatter = formatter,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusBadgeCount(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(text = "$count entries", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategorySummaryCard(
    title: String,
    icon: ImageVector,
    badgeColor: Color,
    recordsCount: Int,
    totalQuantity: Int,
    unitLabel: String,
    totalValue: Double,
    remainingBalance: Double,
    paidCount: Int,
    onClick: () -> Unit,
    isDark: Boolean
) {
    val currencyFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                isDark = isDark,
                accentColor = badgeColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = badgeColor, modifier = Modifier.size(22.dp))
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$recordsCount entries",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor
                        )
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalQuantity $unitLabel",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (totalValue > 0) {
                    Text(
                        text = "₹${currencyFormat.format(totalValue.toLong())}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (remainingBalance > 0) {
                        Text(
                            text = "Due: ₹${currencyFormat.format(remainingBalance.toLong())}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    } else {
                        Text(
                            text = "Cleared",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                } else {
                    Text(
                        text = "$recordsCount Total",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                    Text(
                        text = "Active Module",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VarietyDistributionCard(
    allRecords: List<CropRecord>,
    accentColor: Color,
    isDark: Boolean
) {
    val topVarieties = remember(allRecords) {
        allRecords.groupingBy { it.plantVariety.ifBlank { "Unspecified" } }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    if (topVarieties.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                isDark = isDark,
                accentColor = accentColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Top Operational Varieties",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val maxCount = topVarieties.firstOrNull()?.second ?: 1
                topVarieties.forEach { (variety, count) ->
                    val ratio = (count.toFloat() / maxCount).coerceIn(0.1f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = variety,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(120.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        LinearProgressIndicator(
                            progress = ratio,
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.2f)
                        )

                        Text(
                            text = "$count",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordLogItemCard(
    record: CropRecord,
    isDark: Boolean
) {
    val currencyFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")) }
    val isCleared = record.isPaymentCleared()
    val remaining = record.calculateRemainingBalance()
    val serviceAccent = getSectionAccentColor(record.serviceType, defaultColor = MaterialTheme.colorScheme.primary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                isDark = isDark,
                accentColor = serviceAccent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = record.serialNumber.ifBlank { "SN-${record.id}" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = serviceAccent
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = serviceAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = record.serviceType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = serviceAccent,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = record.farmerName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${record.plantVariety} • Qty: ${record.quantity}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${currencyFormat.format(record.calculateTotalAmount().toLong())}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = CircleShape,
                    color = if (isCleared) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFD32F2F).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isCleared) "PAID" else "DUE ₹${currencyFormat.format(remaining.toLong())}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCleared) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GardenLogItemCard(
    entry: GardenPlanningEntry,
    isDark: Boolean
) {
    val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
    val gardenAccent = Color(0xFF00897B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                isDark = isDark,
                accentColor = gardenAccent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Garden Planning",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = gardenAccent
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = gardenAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "#${entry.serialNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = gardenAccent,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.farmerName.ifBlank { "Garden Entry" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val infoText = buildString {
                    if (entry.plantVariety.isNotBlank()) append(entry.plantVariety).append(" • ")
                    append("${entry.totalKanalArea} Kanals (${entry.plantsPerKanal}/Kanal)")
                }
                Text(
                    text = infoText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(entry.totalCost),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                val isCleared = entry.paymentStatus == "Fully Paid"
                val isAdvance = entry.paymentStatus == "Advance Paid"
                Surface(
                    shape = CircleShape,
                    color = if (isCleared) Color(0xFF2E7D32).copy(alpha = 0.15f) else if (isAdvance) Color(0xFFED6C02).copy(alpha = 0.15f) else Color(0xFFD32F2F).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isCleared) "PAID" else if (isAdvance) "ADVANCE" else "DUE ${currencyFormat.format(entry.remainingBalance.toLong())}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCleared) Color(0xFF2E7D32) else if (isAdvance) Color(0xFFED6C02) else Color(0xFFD32F2F),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingLogItemCard(
    booking: UserBooking,
    isDark: Boolean
) {
    val bookingAccent = getSectionAccentColor(booking.type, defaultColor = Color(0xFF00897B))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                isDark = isDark,
                accentColor = bookingAccent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = booking.type,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = bookingAccent
                    )
                    if (booking.season.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = bookingAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = booking.season,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = bookingAccent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = booking.farmerName.ifBlank { "Farmer Booking" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${booking.itemName} ${booking.variety}".trim(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Qty: ${booking.quantity ?: 1}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = booking.bookingDate.ifBlank { "Registered" },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    isDark: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                isDark = isDark,
                accentColor = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
