package com.na982.opichelper.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.na982.opichelper.presentation.viewmodel.PlaybackViewModel
import com.na982.opichelper.presentation.viewmodel.QaBrowserViewModel
import com.na982.opichelper.presentation.viewmodel.RepeatListeningViewModel
import com.na982.opichelper.presentation.viewmodel.EnglishWritingTestViewModel
import com.na982.opichelper.presentation.viewmodel.FullMemorizationViewModel
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.presentation.viewmodel.CurrentMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    qaViewModel: QaBrowserViewModel = hiltViewModel(),
    repeatListeningViewModel: RepeatListeningViewModel = hiltViewModel(),
    englishWritingTestViewModel: EnglishWritingTestViewModel = hiltViewModel(),
    fullMemorizationViewModel: FullMemorizationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
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
    val isFullMemorizationMode by remember {
        derivedStateOf { MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.FULL_MEMORIZATION }
    }
    val isEnglishWritingTestMode by remember {
        derivedStateOf {
            coordinatorMode in setOf(
                CurrentMode.ENGLISH_WRITING,
                CurrentMode.ENGLISH_WRITING_RECORDING,
                CurrentMode.ENGLISH_WRITING_PLAYING,
                CurrentMode.ENGLISH_WRITING_WITH_FILE
            )
        }
    }
    val isFullMemorizationPlaying by remember {
        derivedStateOf { coordinatorMode == CurrentMode.FULL_MEMORIZATION_PLAYING }
    }
    val isFullMemorizationRecording by remember {
        derivedStateOf { coordinatorMode == CurrentMode.FULL_MEMORIZATION_RECORDING }
    }
    val isFullMemorizationQuestionPlaying by remember {
        derivedStateOf { coordinatorMode == CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING }
    }

    // 레벨 변경 시 모든 모드 정지
    LaunchedEffect(selectedLevel) {
        repeatListeningViewModel.onLevelChanged()
        englishWritingTestViewModel.onLevelChanged()
        fullMemorizationViewModel.onLevelChanged()
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

    val isDarkTheme = isSystemInDarkTheme()

    OPicHelperThemeWithMemorizeLevel(
        darkTheme = isDarkTheme,
        memorizeLevel = selectedLevel
    ) {
        BackHandler(enabled = false) {}

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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            AppTitle(
                currentLevel = qaState.currentUserLevel,
                onSettingsClick = onNavigateToSettings,
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
                            text = qaState.error!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                qaItem != null -> {
                    QuestionCard(
                        currentQuestion = qaItem.questionEn,
                        currentQuestionKo = qaItem.questionKo,
                        highlightIndex = when {
                            (isFullMemorizationMode && isFullMemorizationPlaying) -> fullMemorizationState.highlightIndex
                            else -> playbackState.questionHighlightIndex
                        },
                        currentIndex = currentIndex,
                        totalCount = totalCount,
                        isFlipped = false,
                        currentCategory = category ?: "",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuestionPlayButton(
                            currentQuestion = qaItem.questionEn,
                            isPlaying = playbackState.isQuestionPlaying,
                            onPlayClick = {
                                stopCurrentMemorization(coordinator, repeatListeningViewModel, englishWritingTestViewModel, fullMemorizationViewModel)
                                playbackViewModel.playQuestion(qaItem.questionEn)
                            },
                            onStopClick = { playbackViewModel.stopTts() },
                            modifier = Modifier.weight(1f)
                        )

                        if (isFullMemorizationMode) {
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
                                        stopCurrentMemorization(coordinator, repeatListeningViewModel, englishWritingTestViewModel, fullMemorizationViewModel)
                                    } else {
                                        onMemorizeTestButtonClick(
                                            selectedLevel, repeatListeningViewModel, englishWritingTestViewModel, fullMemorizationViewModel
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

                    if (!isFullMemorizationMode || (!isFullMemorizationQuestionPlaying && !isFullMemorizationRecording)) {
                        AnswerCard(
                            currentAnswer = qaViewModel.getCurrentAnswer(qaItem),
                            currentAnswerKo = qaViewModel.getCurrentAnswerKo(qaItem),
                            highlightIndex = when {
                                (isFullMemorizationMode && isFullMemorizationPlaying) || (coordinatorMode == CurrentMode.FULL_MEMORIZATION_PLAYING) -> fullMemorizationState.highlightIndex
                                playbackState.isEnglishWritingTestMergedFilePlaying -> playbackState.englishWritingTestMergedFileHighlightIndex
                                else -> playbackState.answerHighlightIndex
                            },
                            answerKoHighlightIndex = playbackState.answerKoHighlightIndex,
                            recordingHighlightIndex = playbackState.recordingHighlightIndex,
                            resumeHighlightIndex = if (!repeatListeningState.isPlaying) repeatListeningState.resumeSentenceIndex else null,
                            isFlipped = when {
                                isEnglishWritingTestMode -> englishWritingTestState.isCardFlipped
                                playbackState.isEnglishWritingTestMergedFilePlaying -> false
                                repeatListeningState.isCardFlipped -> repeatListeningState.isCardFlipped
                                else -> playbackState.isAnswerCardFlipped
                            },
                            isRepeatListeningCardFlipped = repeatListeningState.isCardFlipped,
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
                        AnswerPlayButton(
                            currentAnswer = qaViewModel.getCurrentAnswer(qaItem),
                            isPlaying = playbackState.isAnswerPlaying,
                            onPlayClick = {
                                stopCurrentMemorization(coordinator, repeatListeningViewModel, englishWritingTestViewModel, fullMemorizationViewModel)
                                qaItem.let { playbackViewModel.playAnswer(qaViewModel.getCurrentAnswer(it)) }
                            },
                            onStopClick = { playbackViewModel.stopTts() },
                            modifier = Modifier.weight(1f)
                        )

                        MemorizeLevelPlaybackButton(
                            selectedLevel = selectedLevel,
                            onPlayEnglishWritingTest = { playbackViewModel.playEnglishWritingTestMergedFile() },
                            onStopEnglishWritingTest = { playbackViewModel.stopEnglishWritingTestMergedFile() },
                            onPlayFullMemorization = { fullMemorizationViewModel.playRecording() },
                            onStopFullMemorization = { fullMemorizationViewModel.stopPlaying() },
                            hasEnglishWritingTestMergedFile = playbackState.hasEnglishWritingTestMergedFile,
                            isEnglishWritingTestMergedFilePlaying = playbackState.isEnglishWritingTestMergedFilePlaying,
                            hasFullMemorizationRecording = fullMemorizationState.hasRecordingFile,
                            isFullMemorizationRecordingPlaying = coordinatorMode == CurrentMode.FULL_MEMORIZATION_PLAYING,
                            modifier = Modifier.weight(1f)
                        )
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
    repeatListeningViewModel: RepeatListeningViewModel,
    englishWritingTestViewModel: EnglishWritingTestViewModel,
    fullMemorizationViewModel: FullMemorizationViewModel
) {
    when (MemorizeLevel.fromDisplayName(selectedLevel)) {
        MemorizeLevel.REPEAT_LISTENING -> repeatListeningViewModel.start()
        MemorizeLevel.ENGLISH_WRITING -> englishWritingTestViewModel.start()
        MemorizeLevel.FULL_MEMORIZATION -> fullMemorizationViewModel.start()
    }
}

private fun stopCurrentMemorization(
    coordinator: MemorizationModeCoordinator,
    repeatListeningViewModel: RepeatListeningViewModel,
    englishWritingTestViewModel: EnglishWritingTestViewModel,
    fullMemorizationViewModel: FullMemorizationViewModel
) {
    val currentMode = coordinator.currentMode.value
    when {
        currentMode == CurrentMode.REPEAT_LISTENING -> repeatListeningViewModel.stop()
        currentMode in setOf(CurrentMode.ENGLISH_WRITING, CurrentMode.ENGLISH_WRITING_RECORDING,
            CurrentMode.ENGLISH_WRITING_PLAYING, CurrentMode.ENGLISH_WRITING_WITH_FILE) -> englishWritingTestViewModel.stop()
        currentMode in setOf(CurrentMode.FULL_MEMORIZATION, CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING,
            CurrentMode.FULL_MEMORIZATION_RECORDING, CurrentMode.FULL_MEMORIZATION_PLAYING,
            CurrentMode.FULL_MEMORIZATION_WITH_FILE) -> fullMemorizationViewModel.stop()
    }
}
