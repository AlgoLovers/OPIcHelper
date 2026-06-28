package com.na982.opichelper.presentation.ui.screen

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

@Composable
fun MainScreenSnackbarCollector(
    snackbarHostState: SnackbarHostState,
    eventFlows: List<Flow<String>>,
    permissionDenied: Flow<Boolean>,
    completedCount: Int,
    isOnboardingVisible: Boolean
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        merge(*eventFlows.toTypedArray()).collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    val isPermissionDenied by permissionDenied.collectAsState(initial = false)
    var hasShownPermissionDenied by remember { mutableStateOf(false) }
    LaunchedEffect(isPermissionDenied) {
        if (isPermissionDenied && !hasShownPermissionDenied) {
            hasShownPermissionDenied = true
            snackbarHostState.showSnackbar(
                "녹음 권한이 필요합니다. 설정에서 권한을 허용해주세요.",
                duration = SnackbarDuration.Long
            )
        }
    }

    var hasShownResumePrompt by remember { mutableStateOf(false) }
    LaunchedEffect(completedCount) {
        if (completedCount > 0 && !isOnboardingVisible && !hasShownResumePrompt) {
            hasShownResumePrompt = true
            snackbarHostState.showSnackbar(
                "이전 위치에서 이어서 듣기 가능",
                duration = SnackbarDuration.Short
            )
        }
    }
}
