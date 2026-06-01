package com.na982.opichelper.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.na982.opichelper.presentation.ui.component.PipOverlay
import com.na982.opichelper.presentation.ui.screen.MainScreen
import com.na982.opichelper.presentation.ui.screen.SettingsScreen
import com.na982.opichelper.presentation.viewmodel.PlaybackViewModel
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
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val pipState by playbackViewModel.pipState.collectAsState()

    if (pipState.isPipMode) {
        PipOverlay(
            sentenceEn = pipState.currentSentenceEn,
            sentenceKo = pipState.currentSentenceKo,
            hasCompleted = pipState.hasCompleted,
            modifier = Modifier.fillMaxSize()
        )
    } else {
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
}
