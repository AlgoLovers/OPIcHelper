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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.*
import com.na982.opichelper.presentation.viewmodel.MemorizationViewModel
import com.na982.opichelper.presentation.viewmodel.isFullMemorizationQuestionPlaying
import com.na982.opichelper.presentation.viewmodel.isFullMemorizationRecording
import com.na982.opichelper.presentation.viewmodel.isFullMemorizationPlaying
import com.na982.opichelper.presentation.viewmodel.isFullMemorizationRecordingPlaying
import com.na982.opichelper.presentation.viewmodel.isMemorizeTestRunning
import com.na982.opichelper.presentation.viewmodel.isEnglishWritingTestMode
import androidx.hilt.navigation.compose.hiltViewModel
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    qaViewModel: QaBrowserViewModel = hiltViewModel(),
    memorizationViewModel: MemorizationViewModel? = null,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
) {
    val playbackState by playbackViewModel.uiState.collectAsState()
    val qaState by qaViewModel.uiState.collectAsState()
    val memorizationViewModelInstance = memorizationViewModel ?: hiltViewModel<MemorizationViewModel>()
    val memorizationUiState by memorizationViewModelInstance.uiState.collectAsState()
    val memorizeLevels = memorizationUiState.memorizeLevels
    val selectedLevel = qaState.selectedMemorizeLevel
    val currentQaItemState = qaState.currentQaItem
    val categories = qaState.categories
    val currentCategoryState = qaState.currentCategory

    val isQuestionCardFlipped = memorizationUiState.isQuestionCardFlipped
    val isRepeatListeningCardFlipped = memorizationUiState.isRepeatListeningCardFlipped

    val hasEnglishWritingTestMergedFile = playbackState.hasEnglishWritingTestMergedFile
    val englishWritingTestCompleted = memorizationUiState.englishWritingTestCompleted
    val isEnglishWritingTestMergedFilePlaying = playbackState.isEnglishWritingTestMergedFilePlaying
    val englishWritingTestMergedFileHighlightIndex = playbackState.englishWritingTestMergedFileHighlightIndex
    val stopEnglishWritingTestMergedFilePlaying = memorizationUiState.stopEnglishWritingTestMergedFilePlaying
    val isEnglishWritingTestCardFlipped = memorizationUiState.isEnglishWritingTestCardFlipped

    val fullMemorizationHighlightIndex = memorizationUiState.fullMemorizationHighlightIndex
    val isFullMemorizationQuestionPlaying = memorizationUiState.isFullMemorizationQuestionPlaying
    val isFullMemorizationRecording = memorizationUiState.isFullMemorizationRecording
    val isFullMemorizationPlaying = memorizationUiState.isFullMemorizationPlaying
    val hasFullMemorizationRecording = memorizationUiState.hasFullMemorizationRecordingFile
    val isFullMemorizationRecordingPlaying = memorizationUiState.isFullMemorizationRecordingPlaying

    val isMemorizeTestRunning = memorizationUiState.isMemorizeTestRunning
    val isFullMemorizationMode = MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.FULL_MEMORIZATION
    val isEnglishWritingTestMode = memorizationUiState.isEnglishWritingTestMode

    LaunchedEffect(Unit) {
        memorizationViewModelInstance.resetStateOnAppRestart()
    }

    LaunchedEffect(qaState.currentQaItem) {
        playbackViewModel.checkEnglishWritingTestMergedFile()
    }

    LaunchedEffect(englishWritingTestCompleted) {
        if (englishWritingTestCompleted) {
            playbackViewModel.checkEnglishWritingTestMergedFile()
            memorizationViewModelInstance.resetEnglishWritingTestCompleted()
        }
    }

    LaunchedEffect(isEnglishWritingTestMode) {
        if (!isEnglishWritingTestMode) {
            playbackViewModel.checkEnglishWritingTestMergedFile()
        }
    }

    LaunchedEffect(stopEnglishWritingTestMergedFilePlaying) {
        if (stopEnglishWritingTestMergedFilePlaying) {
            playbackViewModel.stopEnglishWritingTestMergedFile()
            memorizationViewModelInstance.resetStopEnglishWritingTestMergedFilePlaying()
        }
    }

    LaunchedEffect(selectedLevel) {
        memorizationViewModelInstance.onMemorizeLevelChanged()
    }

    val isDarkTheme = isSystemInDarkTheme()

    OPicHelperThemeWithMemorizeLevel(
        darkTheme = isDarkTheme,
        memorizeLevel = selectedLevel
    ) {
        BackHandler(enabled = false) {}

        val qaItem = currentQaItemState
        val category = currentCategoryState
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
                            categories = categories,
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
                            levels = memorizeLevels,
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
                            (isFullMemorizationMode && isFullMemorizationPlaying) -> fullMemorizationHighlightIndex
                            else -> playbackState.questionHighlightIndex
                        },
                        currentIndex = currentIndex,
                        totalCount = totalCount,
                        isFlipped = isQuestionCardFlipped,
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
                                memorizationViewModelInstance.stopMemorization()
                                playbackViewModel.playQuestion(qaItem.questionEn)
                            },
                            onStopClick = { playbackViewModel.stopTts() },
                            modifier = Modifier.weight(1f)
                        )

                        if (isFullMemorizationMode) {
                            FullMemorizationRecordingButton(
                                isQuestionPlaying = isFullMemorizationQuestionPlaying,
                                isRecording = isFullMemorizationRecording,
                                onStartRecording = { memorizationViewModelInstance.startFullMemorizationMode() },
                                onStopRecording = { memorizationViewModelInstance.stopFullMemorizationRecording() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (MemorizeLevel.fromDisplayName(selectedLevel) != MemorizeLevel.FULL_MEMORIZATION) {
                                        playbackViewModel.stopTts()
                                    }
                                    memorizationViewModelInstance.onMemorizeTestButtonClick(selectedLevel)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMemorizeTestRunning)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = when {
                                        isMemorizeTestRunning -> "${selectedLevel} 종료"
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
                                (isFullMemorizationMode && isFullMemorizationPlaying) || isFullMemorizationRecordingPlaying -> fullMemorizationHighlightIndex
                                isEnglishWritingTestMergedFilePlaying -> englishWritingTestMergedFileHighlightIndex
                                else -> playbackState.answerHighlightIndex
                            },
                            answerKoHighlightIndex = playbackState.answerKoHighlightIndex,
                            recordingHighlightIndex = playbackState.recordingHighlightIndex,
                            isFlipped = when {
                                isEnglishWritingTestMode -> isEnglishWritingTestCardFlipped
                                isEnglishWritingTestMergedFilePlaying -> false
                                isRepeatListeningCardFlipped -> isRepeatListeningCardFlipped
                                else -> playbackState.isAnswerCardFlipped
                            },
                            isRepeatListeningCardFlipped = isRepeatListeningCardFlipped,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        RecordingAnimation(
                            isRecording = isFullMemorizationRecording,
                            onStopRecording = { memorizationViewModelInstance.stopFullMemorizationRecording() }
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
                                memorizationViewModelInstance.stopMemorization()
                                qaItem.let { playbackViewModel.playAnswer(qaViewModel.getCurrentAnswer(it)) }
                            },
                            onStopClick = { playbackViewModel.stopTts() },
                            modifier = Modifier.weight(1f)
                        )

                        MemorizeLevelPlaybackButton(
                            selectedLevel = selectedLevel,
                            onPlayEnglishWritingTest = { playbackViewModel.playEnglishWritingTestMergedFile() },
                            onStopEnglishWritingTest = { playbackViewModel.stopEnglishWritingTestMergedFile() },
                            onPlayFullMemorization = { memorizationViewModelInstance.playFullMemorizationRecording() },
                            onStopFullMemorization = { memorizationViewModelInstance.stopFullMemorizationPlaying() },
                            hasEnglishWritingTestMergedFile = hasEnglishWritingTestMergedFile,
                            isEnglishWritingTestMergedFilePlaying = isEnglishWritingTestMergedFilePlaying,
                            hasFullMemorizationRecording = hasFullMemorizationRecording,
                            isFullMemorizationRecordingPlaying = isFullMemorizationRecordingPlaying,
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
