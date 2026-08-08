package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CropRecord
import com.example.data.UserBooking
import com.example.data.calculateRemainingBalance
import com.example.data.calculateTotalAmount
import com.example.data.isPaymentCleared
import com.example.ui.CropViewModel
import com.example.ui.UserDashboardViewModel

@Composable
fun AgriDashboardScreen(
    viewModel: CropViewModel,
    userDashboardViewModel: UserDashboardViewModel,
    currentUserEmail: String? = null,
    onBack: () -> Unit,
    onNavigateToCategory: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val allRecords by viewModel.allRecords.collectAsState()
    val rawBookings by userDashboardViewModel.rawBookings.collectAsState()
    val isDark = isAppInDarkMode()

    val accountEmail = currentUserEmail ?: "thokertufail20@gmail.com"

    var selectedFilterTab by remember { mutableStateOf("All") }

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

    // Financial Metrics
    val totalRevenue = remember(allRecords) {
        allRecords.sumOf { it.calculateTotalAmount() }
    }
    val totalPaid = remember(allRecords) {
        allRecords.sumOf { it.amountPaid }
    }
    val totalRemaining = remember(allRecords) {
        allRecords.sumOf { it.calculateRemainingBalance() }
    }

    val fullyPaidCount = remember(allRecords) {
        allRecords.count { it.isPaymentCleared() }
    }
    val advancePaidCount = remember(allRecords) {
        allRecords.count { !it.isPaymentCleared() && it.amountPaid > 0 }
    }
    val pendingCount = remember(allRecords) {
        allRecords.count { !it.isPaymentCleared() && it.amountPaid <= 0 }
    }

    val paidRatio = if (totalRevenue > 0) (totalPaid / totalRevenue).toFloat().coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Dashboard Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "AgriCrop Operations Dashboard",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Comprehensive Operations & Financial Overview",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { userDashboardViewModel.refreshUser() },
                        modifier = Modifier.testTag("dashboard_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // 1. Account & System Banner Card
                item {
                    AccountBannerCard(
                        email = accountEmail,
                        totalEntriesCount = allRecords.size + rawBookings.size,
                        totalVolume = totalRevenue,
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
                        totalRecordsCount = allRecords.size,
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
                                tint = MaterialTheme.colorScheme.primary,
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
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 4. Summaries Grid (2 columns or styled stacked cards for max readability)
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
                            icon = Icons.Default.Park,
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

                        // Farmer Bookings
                        CategorySummaryCard(
                            title = "Farmer Bookings",
                            icon = Icons.Default.PlaylistAddCheck,
                            badgeColor = Color(0xFF00897B),
                            recordsCount = rawBookings.size,
                            totalQuantity = rawBookings.sumOf { it.quantity ?: 1 },
                            unitLabel = "Booked Items",
                            totalValue = 0.0,
                            remainingBalance = 0.0,
                            paidCount = rawBookings.size,
                            onClick = { onNavigateToCategory?.invoke("Bookings") },
                            isDark = isDark
                        )
                    }
                }

                // 5. Varieties & Inventory Snapshot
                item {
                    VarietyDistributionCard(allRecords = allRecords, isDark = isDark)
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
                            val tabs = listOf("All", "Pending", "Paid", "Bookings")
                            items(tabs) { tab ->
                                FilterChip(
                                    selected = selectedFilterTab == tab,
                                    onClick = { selectedFilterTab = tab },
                                    label = { Text(tab, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                if (selectedFilterTab == "Bookings") {
                    if (rawBookings.isEmpty()) {
                        item {
                            EmptyStateCard(message = "No farmer bookings registered yet.")
                        }
                    } else {
                        items(rawBookings.take(8)) { booking ->
                            BookingLogItemCard(booking = booking, isDark = isDark)
                        }
                    }
                } else {
                    if (filteredLogRecords.isEmpty()) {
                        item {
                            EmptyStateCard(message = "No records found matching current filter.")
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

@Composable
private fun AccountBannerCard(
    email: String,
    totalEntriesCount: Int,
    totalVolume: Double,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
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
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User Account",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Operations Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2E7D32)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AgriCrop Cloud Sync Enabled",
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
                    color = MaterialTheme.colorScheme.primary
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
    isDark: Boolean
) {
    val currencyFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0))
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Financial & Payment Status Breakdown",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${(paidRatio * 100).toInt()}% Collected",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                    value = "₹${currencyFormat.format(totalRevenue.toLong())}",
                    icon = Icons.Default.TrendingUp,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Paid / Received
                FinancialMetricBox(
                    label = "Amount Paid",
                    value = "₹${currencyFormat.format(totalPaid.toLong())}",
                    icon = Icons.Default.Payments,
                    accentColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )

                // Remaining Balance
                FinancialMetricBox(
                    label = "Remaining",
                    value = "₹${currencyFormat.format(totalRemaining.toLong())}",
                    icon = Icons.Default.ReceiptLong,
                    accentColor = if (totalRemaining > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32),
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
                    Text("${(paidRatio * 100).toInt()}% Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

            Divider(color = if (isDark) Color(0xFF333333) else Color(0xFFF0F0F0))

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
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEEEE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2E2E2E) else Color(0xFFE5E5E5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )

                        Text(
                            text = "$count",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = record.serialNumber.ifBlank { "SN-${record.id}" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = record.serviceType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
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
private fun BookingLogItemCard(
    booking: UserBooking,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0F0F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = booking.type,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00897B)
                    )
                    if (booking.season.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF00897B).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = booking.season,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF00897B),
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
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
}
