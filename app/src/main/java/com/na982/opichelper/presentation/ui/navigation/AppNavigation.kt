package com.na982.opichelper.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.na982.opichelper.presentation.ui.screen.MainScreen
import com.na982.opichelper.presentation.ui.screen.SettingsScreen
import kotlinx.coroutines.flow.StateFlow

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    permissionDenied: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                permissionDenied = permissionDenied
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
