package com.na982.opichelper.presentation.ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.presentation.ui.component.EditScriptBottomSheet
import com.na982.opichelper.presentation.ui.component.OnboardingDialog
import com.na982.opichelper.presentation.ui.component.PipPermissionDialog
import com.na982.opichelper.presentation.ui.component.SearchDialog
import com.na982.opichelper.presentation.ui.component.openPipSettings
import com.na982.opichelper.presentation.viewmodel.OnboardingViewModel
import com.na982.opichelper.presentation.viewmodel.QaBrowserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainScreenDialogs(
    showOnboarding: MutableState<Boolean>,
    showPipGuide: MutableState<Boolean>,
    showSearch: MutableState<Boolean>,
    editScriptState: MutableState<EditScriptState?>,
    onboardingViewModel: OnboardingViewModel,
    qaViewModel: QaBrowserViewModel,
    context: Context,
    scope: CoroutineScope
) {
    if (showOnboarding.value) {
        OnboardingDialog(
            onStartClick = {
                onboardingViewModel.setOnboardingCompleted()
                showOnboarding.value = false
            }
        )
    }

    if (!showOnboarding.value && showPipGuide.value) {
        PipPermissionDialog(
            onDismiss = {
                onboardingViewModel.setPipGuideCompleted()
                showPipGuide.value = false
            },
            onOpenSettings = {
                onboardingViewModel.setPipGuideCompleted()
                showPipGuide.value = false
                openPipSettings(context)
            }
        )
    }

    if (showSearch.value) {
        SearchDialog(
            onDismiss = { showSearch.value = false },
            onResultClick = { item ->
                showSearch.value = false
                scope.launch {
                    qaViewModel.navigateToItem(item)
                }
            },
            searchQuery = { query -> qaViewModel.search(query) }
        )
    }

    editScriptState.value?.let { editState ->
        EditScriptBottomSheet(
            qaItem = editState.qaItem,
            isQuestion = editState.isQuestion,
            level = editState.level,
            scriptIndex = editState.scriptIndex,
            entityId = editState.entityId,
            onDismiss = { editScriptState.value = null }
        )
    }
}
