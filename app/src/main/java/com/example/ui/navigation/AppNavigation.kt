package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.FirestoreSyncManager
import com.example.ui.AgriCropMainScreen
import com.example.ui.AttendanceViewModel
import com.example.ui.CropViewModel
import com.example.ui.NotificationViewModel
import com.example.ui.UserDashboardViewModel
import com.example.ui.components.AgriDashboardScreen
import com.example.ui.components.GlobalSearchResultsScreen
import com.example.ui.components.LoginScreen
import com.example.ui.components.attendance.AttendanceMainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NavRoutes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val DASHBOARD = "dashboard"
    const val ATTENDANCE = "attendance"
    const val GLOBAL_SEARCH = "global_search"
}

@Composable
fun AppNavigation(
    cropViewModel: CropViewModel,
    attendanceViewModel: AttendanceViewModel,
    notificationViewModel: NotificationViewModel? = null,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = com.example.util.SafeFirebase.getAuth(context)
    val userDashboardViewModel = remember { UserDashboardViewModel() }
    val db = remember { AppDatabase.getDatabase(context, coroutineScope) }

    val startDestination = remember {
        val user = auth?.currentUser
        if (user != null && user.isEmailVerified) {
            NavRoutes.MAIN
        } else {
            NavRoutes.LOGIN
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { userEmail ->
                    userDashboardViewModel.refreshUser()
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            FirestoreSyncManager().syncFromCloudToLocal(db.cropRecordDao(), db.attendanceDao())
                        } catch (_: Exception) {}
                    }
                    navController.navigate(NavRoutes.MAIN) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                onContinueAsGuest = {
                    navController.navigate(NavRoutes.MAIN) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.MAIN) {
            AgriCropMainScreen(
                viewModel = cropViewModel,
                attendanceViewModel = attendanceViewModel,
                notificationViewModel = notificationViewModel,
                userDashboardViewModel = userDashboardViewModel,
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.LOGIN)
                },
                onNavigateToDashboard = {
                    navController.navigate(NavRoutes.DASHBOARD)
                },
                onNavigateToAttendance = {
                    navController.navigate(NavRoutes.ATTENDANCE)
                },
                onNavigateToGlobalSearch = {
                    navController.navigate(NavRoutes.GLOBAL_SEARCH)
                },
                onLogout = {
                    auth?.signOut()
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.DASHBOARD) {
            val currentUser = auth?.currentUser
            AgriDashboardScreen(
                viewModel = cropViewModel,
                userDashboardViewModel = userDashboardViewModel,
                currentUserEmail = currentUser?.email,
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToCategory = { category ->
                    cropViewModel.selectServiceCategory(category)
                    navController.popBackStack(NavRoutes.MAIN, inclusive = false)
                }
            )
        }

        composable(NavRoutes.ATTENDANCE) {
            AttendanceMainScreen(
                viewModel = attendanceViewModel,
                onNavigateBackToMain = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.GLOBAL_SEARCH) {
            GlobalSearchResultsScreen(
                viewModel = cropViewModel,
                onBack = {
                    cropViewModel.closeGlobalSearch()
                    navController.popBackStack()
                }
            )
        }
    }
}
