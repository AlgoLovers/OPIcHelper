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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.toModeGroup
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
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.entity.QaItem

data class EditScriptState(
    val qaItem: QaItem,
    val isQuestion: Boolean,
    val level: UserLevel,
    val scriptIndex: Int,
    val entityId: String
)


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

    // 레벨 변경 시 모든 모드 정지
    LaunchedEffect(selectedLevel) {
        memorizationController.onLevelChangedAll()
    }

    // QA 아이템 변경 시 반복듣기 이어서 듣기 위치 갱신
    LaunchedEffect(qaState.currentQaItem) {
        if (!repeatListeningState.isPlaying) {
            repeatListeningViewModel.refreshResumeIndex()
        }
    }

    // 반복듣기 모드 진입 시 이어서 듣기 위치 초기 갱신
    LaunchedEffect(Unit) {
        repeatListeningViewModel.refreshResumeIndex()
    }

    // 통암기 모드 문장 텍스트를 PlaybackViewModel로 릴레이 (PiP용)
    LaunchedEffect(fullMemorizationState.currentSentenceEn, fullMemorizationState.currentSentenceKo) {
        playbackViewModel.setFullMemorizationSentence(
            fullMemorizationState.currentSentenceEn,
            fullMemorizationState.currentSentenceKo
        )
    }

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

            // 에러 이벤트 Snackbar 수집
            LaunchedEffect(Unit) {
                kotlinx.coroutines.flow.merge(
                    playbackViewModel.events,
                    repeatListeningViewModel.events,
                    englishWritingTestViewModel.events,
                    fullMemorizationViewModel.events,
                    qaViewModel.events
                ).collect { message ->
                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                }
            }

            // 권한 거부 피드백
            val isPermissionDenied by permissionDenied.collectAsState()
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

            // 이어서 듣기 프롬프트 (최초 1회만)
            var hasShownResumePrompt by remember { mutableStateOf(false) }
            LaunchedEffect(qaState.completedCount) {
                if (qaState.completedCount > 0 && !showOnboarding.value && !hasShownResumePrompt) {
                    hasShownResumePrompt = true
                    snackbarHostState.showSnackbar(
                        "이전 위치에서 이어서 듣기 가능",
                        duration = SnackbarDuration.Short
                    )
                }
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        CategorySelector(
                            selectedCategory = category ?: "",
                            categories = qaState.categories,
                            onCategorySelected = {
                                playbackViewModel.stopTts()
                                qaViewModel.selectCategory(it)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📚 학습할 주제를 선택하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        MemorizeLevelSelector(
                            levels = MemorizeLevel.allDisplayNames,
                            selectedLevel = selectedLevel,
                            onLevelSelected = { qaViewModel.setSelectedMemorizeLevel(it) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🎯 학습 난이도를 선택하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

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
                        highlightIndex = when {
                            (coordinatorMode.group == ModeGroup.FULL_MEMORIZATION && isFullMemorizationPlaying) -> fullMemorizationState.highlightIndex
                            else -> playbackState.questionHighlight.index
                        },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuestionPlayButton(
                            isPlaying = playbackState.isQuestionPlaying,
                            onPlayClick = {
                                stopCurrentMemorization(coordinator, memorizationController)
                                playbackViewModel.playQuestion(qaItem.questionEn)
                            },
                            onStopClick = { playbackViewModel.stopTts() },
                            modifier = Modifier.weight(1f)
                        )

                        if (coordinatorMode.group == ModeGroup.FULL_MEMORIZATION) {
                            FullMemorizationRecordingButton(
                                isQuestionPlaying = isFullMemorizationQuestionPlaying,
                                isRecording = isFullMemorizationRecording,
                                onStartRecording = { fullMemorizationViewModel.start() },
                                onStopRecording = { fullMemorizationViewModel.stopRecording() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (MemorizeLevel.fromDisplayName(selectedLevel) != MemorizeLevel.FULL_MEMORIZATION) {
                                        playbackViewModel.stopTts()
                                    }
                                    if (repeatListeningState.isPlaying || coordinatorRunning) {
                                        stopCurrentMemorization(coordinator, memorizationController)
                                    } else {
                                        onMemorizeTestButtonClick(
                                            selectedLevel, memorizationController
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (repeatListeningState.isPlaying || coordinatorRunning)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = when {
                                        repeatListeningState.isPlaying || coordinatorRunning -> "${selectedLevel} 종료"
                                        MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.ENGLISH_WRITING -> "부분암기 테스트"
                                        MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.FULL_MEMORIZATION -> "통암기"
                                        else -> selectedLevel.ifEmpty { "암기 테스트" }
                                    },
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (coordinatorMode.group != ModeGroup.FULL_MEMORIZATION || (!isFullMemorizationQuestionPlaying && !isFullMemorizationRecording)) {
                        AnswerCard(
                            currentAnswer = qaViewModel.getCurrentAnswer(qaItem),
                            currentAnswerKo = qaViewModel.getCurrentAnswerKo(qaItem),
                            highlightIndex = when {
                                coordinatorMode.group == ModeGroup.FULL_MEMORIZATION && isFullMemorizationPlaying -> fullMemorizationState.highlightIndex
                                playbackState.isEnglishWritingTestMergedFilePlaying -> playbackState.englishWritingTestMergedFileHighlightIndex
                                else -> playbackState.answerHighlight.index
                            },
                            answerKoHighlightIndex = playbackState.answerKoHighlight.index,
                            recordingHighlightIndex = playbackState.recordingHighlight.index,
                            resumeHighlightIndex = if (!repeatListeningState.isPlaying) repeatListeningState.resumeSentenceIndex else null,
                            isFlipped = when {
                                coordinatorMode.group == ModeGroup.ENGLISH_WRITING -> englishWritingTestState.isCardFlipped
                                playbackState.isEnglishWritingTestMergedFilePlaying -> false
                                repeatListeningState.isCardFlipped -> repeatListeningState.isCardFlipped
                                else -> false
                            },
                            onEdit = {
                                editScriptState.value = EditScriptState(
                                    qaItem = qaItem,
                                    isQuestion = false,
                                    level = qaViewModel.getCurrentUserLevel(),
                                    scriptIndex = qaViewModel.getCurrentIndex(),
                                    entityId = "${qaItem.category}_${qaItem.id}_${qaViewModel.getCurrentUserLevel().name}"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        RecordingAnimation(
                            isRecording = isFullMemorizationRecording,
                            onStopRecording = { fullMemorizationViewModel.stopRecording() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isRepeatListening = MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.REPEAT_LISTENING
                        AnswerPlayButton(
                            isPlaying = playbackState.isAnswerPlaying,
                            repeatCount = qaState.answerPlayCount,
                            onPlayClick = {
                                stopCurrentMemorization(coordinator, memorizationController)
                                qaItem.let { playbackViewModel.playAnswer(qaViewModel.getCurrentAnswer(it)) }
                            },
                            onStopClick = { playbackViewModel.stopTts() },
                            modifier = if (isRepeatListening) Modifier.fillMaxWidth() else Modifier.weight(1f)
                        )

                        if (!isRepeatListening) {
                            MemorizeLevelPlaybackButton(
                                selectedLevel = selectedLevel,
                                onPlayEnglishWritingTest = { playbackViewModel.playEnglishWritingTestMergedFile() },
                                onStopEnglishWritingTest = { playbackViewModel.stopEnglishWritingTestMergedFile() },
                                onPlayFullMemorization = { fullMemorizationViewModel.playRecording() },
                                onStopFullMemorization = { fullMemorizationViewModel.stopPlaying() },
                                hasEnglishWritingTestMergedFile = playbackState.hasEnglishWritingTestMergedFile,
                                isEnglishWritingTestMergedFilePlaying = playbackState.isEnglishWritingTestMergedFilePlaying,
                                hasFullMemorizationRecording = fullMemorizationState.hasRecordingFile,
                                isFullMemorizationPlaying = coordinatorMode == CurrentMode.FULL_MEMORIZATION_PLAYING,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

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

private fun onMemorizeTestButtonClick(
    selectedLevel: String,
    memorizationController: MemorizationController
) {
    val level = MemorizeLevel.fromDisplayName(selectedLevel)
    memorizationController.startForGroup(level.toModeGroup())
}

private fun stopCurrentMemorization(
    coordinator: MemorizationModeCoordinator,
    memorizationController: MemorizationController
) {
    memorizationController.stopForGroup(coordinator.currentMode.value.group)
}
