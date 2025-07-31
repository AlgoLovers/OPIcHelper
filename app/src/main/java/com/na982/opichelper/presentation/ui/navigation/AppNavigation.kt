package com.na982.opichelper.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.na982.opichelper.presentation.ui.screen.MainScreenRefactored
import com.na982.opichelper.presentation.ui.screen.SettingsScreen
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.presentation.viewmodel.MemorizationViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryPoint {
    fun userPreferencesRepository(): com.na982.opichelper.domain.repository.UserPreferencesRepository
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            val mainViewModel: MainViewModel = hiltViewModel()
            val memorizationViewModel: MemorizationViewModel = hiltViewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                SettingsEntryPoint::class.java
            )
            val userPreferencesRepository = entryPoint.userPreferencesRepository()
            
            MainScreenRefactored(
                viewModel = mainViewModel,
                memorizationViewModel = memorizationViewModel,
                userPreferencesRepository = userPreferencesRepository,
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }
        
        composable("settings") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                SettingsEntryPoint::class.java
            )
            val userPreferencesRepository = entryPoint.userPreferencesRepository()
            
            SettingsScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onLogout = {
                    // 로그아웃 로직 구현
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                userPreferencesRepository = userPreferencesRepository
            )
        }
    }
} 