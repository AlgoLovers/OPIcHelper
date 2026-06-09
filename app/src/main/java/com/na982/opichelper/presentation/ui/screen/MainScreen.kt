package com.na982.opichelper.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.na982.opichelper.presentation.viewmodel.PlaybackViewModel
import com.na982.opichelper.presentation.viewmodel.QaBrowserViewModel
import com.na982.opichelper.presentation.viewmodel.RepeatListeningViewModel
import com.na982.opichelper.presentation.viewmodel.EnglishWritingTestViewModel
import com.na982.opichelper.presentation.viewmodel.FullMemorizationViewModel
import com.na982.opichelper.presentation.viewmodel.OnboardingViewModel
import com.na982.opichelper.presentation.viewmodel.MemorizationController
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.entity.CurrentMode
import com.na982.opichelper.domain.entity.ModeGroup
import androidx.compose.ui.unit.sp
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.na982.opichelper.presentation.ui.component.OnboardingDialog
import com.na982.opichelper.presentation.ui.component.PipPermissionDialog
import com.na982.opichelper.presentation.ui.component.SearchDialog
import com.na982.opichelper.presentation.ui.component.openPipSettings
import com.na982.opichelper.presentation.ui.component.EditScriptBottomSheet
import com.na982.opichelper.presentation.viewmodel.EditScriptViewModel
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    qaViewModel: QaBrowserViewModel = hiltViewModel(),
    repeatListeningViewModel: RepeatListeningViewModel = hiltViewModel(),
    englishWritingTestViewModel: EnglishWritingTestViewModel = hiltViewModel(),
    fullMemorizationViewModel: FullMemorizationViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    permissionDenied: StateFlow<Boolean> = remember { MutableStateFlow(false) }
) {
    val playbackState by playbackViewModel.uiState.collectAsState()
    val qaState by qaViewModel.uiState.collectAsState()
    val coordinator = repeatListeningViewModel.modeCoordinator
    val coordinatorMode by coordinator.currentMode.collectAsState()
    val coordinatorRunning by coordinator.isRunning.collectAsState()
    val repeatListeningState by repeatListeningViewModel.uiState.collectAsState()
    val englishWritingTestState by englishWritingTestViewModel.uiState.collectAsState()
    val fullMemorizationState by fullMemorizationViewModel.uiState.collectAsState()

    val selectedLevel = qaState.selectedMemorizeLevel
    val showOnboarding = remember { mutableStateOf(!onboardingViewModel.isOnboardingCompleted()) }
    val showPipGuide = remember { mutableStateOf(!onboardingViewModel.isPipGuideCompleted()) }
    val showSearch = remember { mutableStateOf(false) }
    val editScriptState = remember { mutableStateOf<EditScriptState?>(null) }
    val context = LocalContext.current
    val isFullMemorizationPlaying by remember {
        derivedStateOf { coordinatorMode == CurrentMode.FULL_MEMORIZATION_PLAYING }
    }
    val isFullMemorizationRecording by remember {
        derivedStateOf { coordinatorMode == CurrentMode.FULL_MEMORIZATION_RECORDING }
    }
    val isFullMemorizationQuestionPlaying by remember {
        derivedStateOf { coordinatorMode == CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING }
    }

    val memorizationController = remember(
        repeatListeningViewModel, englishWritingTestViewModel, fullMemorizationViewModel
    ) {
        MemorizationController(
            mapOf(
                ModeGroup.REPEAT_LISTENING to repeatListeningViewModel,
                ModeGroup.ENGLISH_WRITING to englishWritingTestViewModel,
                ModeGroup.FULL_MEMORIZATION to fullMemorizationViewModel
            )
        )
    }

    MainScreenSideEffects(
        selectedLevel = selectedLevel,
        currentQaItem = qaState.currentQaItem,
        isRepeatListeningPlaying = repeatListeningState.isPlaying,
        fullMemorizationSentenceEn = fullMemorizationState.currentSentenceEn,
        fullMemorizationSentenceKo = fullMemorizationState.currentSentenceKo,
        memorizationController = memorizationController,
        repeatListeningViewModel = repeatListeningViewModel,
        playbackViewModel = playbackViewModel
    )

    val isDarkTheme = isSystemInDarkTheme()

    OPicHelperThemeWithMemorizeLevel(
        darkTheme = isDarkTheme,
        memorizeLevel = selectedLevel
    ) {
        val qaItem = qaState.currentQaItem
        val category = qaState.currentCategory
        val itemsInCategory = remember(category) {
            category?.let { qaViewModel.getItemsInCategory(it) } ?: emptyList()
        }
        val currentIndex = remember(category to qaItem) {
            if (category != null && qaItem != null) {
                val index = itemsInCategory.indexOfFirst { it.id == qaItem.id }
                if (index >= 0) index + 1 else 0
            } else 0
        }
        val totalCount = itemsInCategory.size

        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            val scope = rememberCoroutineScope()

            MainScreenSnackbarCollector(
                snackbarHostState = snackbarHostState,
                eventFlows = listOf(
                    playbackViewModel.events,
                    repeatListeningViewModel.events,
                    englishWritingTestViewModel.events,
                    fullMemorizationViewModel.events,
                    qaViewModel.events
                ),
                permissionDenied = permissionDenied,
                completedCount = qaState.completedCount,
                isOnboardingVisible = showOnboarding.value
            )

            // 온보딩 다이얼로그
            if (showOnboarding.value) {
                OnboardingDialog(
                    onStartClick = {
                        onboardingViewModel.setOnboardingCompleted()
                        showOnboarding.value = false
                    }
                )
            }

            // PiP 권한 안내 다이얼로그
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

            // 검색 다이얼로그
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

            // 스크립트 편집 BottomSheet
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

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            AppTitle(
                currentLevel = qaState.currentUserLevel,
                onSettingsClick = onNavigateToSettings,
                onSearchClick = { showSearch.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            CategoryLevelRow(
                selectedCategory = category ?: "",
                categories = qaState.categories,
                selectedLevel = selectedLevel,
                onCategorySelected = {
                    playbackViewModel.stopTts()
                    qaViewModel.selectCategory(it)
                },
                onLevelSelected = { qaViewModel.setSelectedMemorizeLevel(it) }
            )

            when {
                qaState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                qaState.error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = qaState.error ?: "",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                qaItem == null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (category == null) "카테고리를 선택하면 질문이 표시됩니다" else "이 카테고리에 질문이 없습니다",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    QuestionCard(
                        currentQuestion = qaItem.questionEn,
                        currentQuestionKo = qaItem.questionKo,
                        highlightIndex = resolveQuestionHighlightIndex(
                            coordinatorGroup = coordinatorMode.group,
                            isFullMemorizationPlaying = isFullMemorizationPlaying,
                            fullMemorizationHighlightIndex = fullMemorizationState.highlightIndex,
                            playbackQuestionHighlightIndex = playbackState.questionHighlight.index
                        ),
                        currentIndex = currentIndex,
                        totalCount = totalCount,
                        completedCount = qaState.completedCount,
                        currentCategory = category ?: "",
                        onEdit = {
                            editScriptState.value = EditScriptState(
                                qaItem = qaItem,
                                isQuestion = true,
                                level = qaViewModel.getCurrentUserLevel(),
                                scriptIndex = qaViewModel.getCurrentIndex(),
                                entityId = "${qaItem.category}_${qaItem.id}_${qaViewModel.getCurrentUserLevel().name}"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    QuestionActionRow(
                        isQuestionPlaying = playbackState.isQuestionPlaying,
                        questionEn = qaItem.questionEn,
                        coordinatorGroup = coordinatorMode.group,
                        isFullMemorizationQuestionPlaying = isFullMemorizationQuestionPlaying,
                        isFullMemorizationRecording = isFullMemorizationRecording,
                        selectedLevel = selectedLevel,
                        isRepeatListeningPlaying = repeatListeningState.isPlaying,
                        isCoordinatorRunning = coordinatorRunning,
                        coordinator = coordinator,
                        memorizationController = memorizationController,
                        onPlayQuestion = {
                            memorizationController.stopCurrent(coordinator)
                            playbackViewModel.playQuestion(qaItem.questionEn)
                        },
                        onStopTts = { playbackViewModel.stopTts() },
                        onStartFullMemorization = { fullMemorizationViewModel.start() },
                        onStopFullMemorizationRecording = { fullMemorizationViewModel.stopRecording() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AnswerSection(
                        qaItem = qaItem,
                        coordinatorGroup = coordinatorMode.group,
                        isFullMemorizationQuestionPlaying = isFullMemorizationQuestionPlaying,
                        isFullMemorizationRecording = isFullMemorizationRecording,
                        isFullMemorizationPlaying = isFullMemorizationPlaying,
                        fullMemorizationHighlightIndex = fullMemorizationState.highlightIndex,
                        playbackState = playbackState,
                        repeatListeningState = repeatListeningState,
                        englishWritingTestIsCardFlipped = englishWritingTestState.isCardFlipped,
                        selectedLevel = selectedLevel,
                        currentMode = coordinatorMode,
                        coordinator = coordinator,
                        memorizationController = memorizationController,
                        playbackViewModel = playbackViewModel,
                        fullMemorizationViewModel = fullMemorizationViewModel,
                        currentAnswer = qaViewModel.getCurrentAnswer(qaItem),
                        currentAnswerKo = qaViewModel.getCurrentAnswerKo(qaItem),
                        currentUserLevel = qaViewModel.getCurrentUserLevel(),
                        currentIndex = qaViewModel.getCurrentIndex(),
                        qaItemCategory = qaItem.category,
                        qaItemId = qaItem.id,
                        answerPlayCount = qaState.answerPlayCount,
                        hasFullMemorizationRecording = fullMemorizationState.hasRecordingFile,
                        onEditScript = { editScriptState.value = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationSection(
                        onPreviousQuestion = {
                            playbackViewModel.stopTts()
                            qaViewModel.previousQaItem()
                        },
                        onNextQuestion = {
                            playbackViewModel.stopTts()
                            qaViewModel.nextQaItem()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    }
}
