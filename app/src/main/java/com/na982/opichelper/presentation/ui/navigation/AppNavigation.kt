package com.na982.opichelper.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.na982.opichelper.presentation.ui.screen.MainScreen
import com.na982.opichelper.presentation.ui.screen.SettingsScreen
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.presentation.viewmodel.MemorizationViewModel

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            val mainViewModel: MainViewModel = hiltViewModel()
            val memorizationViewModel: MemorizationViewModel = hiltViewModel()
            
            MainScreen(
                viewModel = mainViewModel,
                memorizationViewModel = memorizationViewModel,
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onLogout = {
                    // 로그아웃 로직 구현
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
} 