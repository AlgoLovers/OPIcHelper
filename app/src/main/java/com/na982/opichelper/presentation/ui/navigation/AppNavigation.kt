package com.na982.opichelper.presentation.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.na982.opichelper.presentation.ui.screen.LoginScreen
import com.na982.opichelper.presentation.ui.screen.MainScreen
import com.na982.opichelper.presentation.ui.screen.SettingsScreen
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.presentation.viewmodel.MemorizationViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.na982.opichelper.data.repository.AuthRepository
import com.na982.opichelper.data.repository.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
    object Settings : Screen("settings")
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthRepositoryEntryPoint {
    fun authRepository(): AuthRepository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserPreferencesRepositoryEntryPoint {
    fun userPreferencesRepository(): UserPreferencesRepository
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                AuthRepositoryEntryPoint::class.java
            )
            val authRepository = entryPoint.authRepository()
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                authRepository = authRepository
            )
        }
        
        composable(Screen.Main.route) {
            // 각 Composable에서 자신의 ViewModel을 생성
            val mainViewModel: MainViewModel = hiltViewModel()
            val memorizationViewModel: MemorizationViewModel = hiltViewModel()
            
            // 기존 MainScreen 사용
            MainScreen(
                viewModel = mainViewModel,
                memorizationViewModel = memorizationViewModel,
                onSettingsClick = {
                    android.util.Log.d("AppNavigation", "설정 화면으로 네비게이션 시작")
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            android.util.Log.d("AppNavigation", "설정 화면 컴포저블 시작")
            val context = androidx.compose.ui.platform.LocalContext.current
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                UserPreferencesRepositoryEntryPoint::class.java
            )
            val userPreferencesRepository = entryPoint.userPreferencesRepository()
            SettingsScreen(
                onBackPressed = {
                    android.util.Log.d("AppNavigation", "설정 화면에서 뒤로가기")
                    navController.popBackStack()
                },
                onLogout = {
                    android.util.Log.d("AppNavigation", "설정 화면에서 로그아웃")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userPreferencesRepository = userPreferencesRepository
            )
        }
    }
} 