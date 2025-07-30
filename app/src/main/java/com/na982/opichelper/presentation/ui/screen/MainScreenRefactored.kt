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
import com.na982.opichelper.domain.entity.QuestionCategory
import com.na982.opichelper.presentation.viewmodel.MainViewModelRefactored
import com.na982.opichelper.presentation.viewmodel.AppState
import androidx.compose.ui.platform.LocalContext
import com.na982.opichelper.domain.entity.QaItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.activity.compose.BackHandler
import android.util.Log
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.*
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.entity.PlaybackState
import com.na982.opichelper.domain.entity.PlayType
import com.na982.opichelper.presentation.viewmodel.MemorizationViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.na982.opichelper.presentation.viewmodel.CurrentMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.na982.opichelper.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme
import com.na982.opichelper.presentation.ui.component.*
import com.na982.opichelper.domain.entity.*
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.*

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreenRefactored(
    viewModel: MainViewModelRefactored,
    memorizationViewModel: MemorizationViewModel? = null,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {}
) {
    // ===== 새로운 아키텍처 상태 =====
    val appState by viewModel.appState.collectAsState()
    val memorizationViewModelInstance = memorizationViewModel ?: hiltViewModel<MemorizationViewModel>()
    val memorizeLevels by memorizationViewModelInstance.memorizeLevels.collectAsState()
    val selectedLevel = appState.selectedMemorizeLevel
    val currentQaItemState = appState.currentQaItem
    val currentCategoryState = appState.currentCategory

    // ===== 공통 상태 (모든 모드에서 사용) =====
    val currentMode by memorizationViewModelInstance.currentMode.collectAsState()
    val isQuestionCardFlipped by viewModel.isQuestionCardFlipped().collectAsState()

    // ===== MemorizationViewModel UI 상태 =====
    val memorizationUiState by memorizationViewModelInstance.uiState.collectAsState()

    // ===== 반복 듣기 (Repeat Listening) =====
    val isRepeatListeningCardFlipped = memorizationUiState.isRepeatListeningCardFlipped

    // ===== 영작 테스트 (English Writing) =====
    val hasEnglishWritingTestMergedFile by viewModel.hasEnglishWritingTestMergedFile().collectAsState()
    val englishWritingTestCompleted by memorizationViewModelInstance.englishWritingTestCompleted.collectAsState()
    val isEnglishWritingTestMergedFilePlaying by viewModel.isEnglishWritingTestMergedFilePlaying().collectAsState()
    val englishWritingTestMergedFileHighlightIndex by viewModel.englishWritingTestMergedFileHighlightIndex().collectAsState()
    val stopEnglishWritingTestMergedFilePlaying by memorizationViewModelInstance.stopEnglishWritingTestMergedFilePlaying.collectAsState()
    val isEnglishWritingTestCardFlipped = memorizationUiState.isEnglishWritingTestCardFlipped

    // ===== 통암기 (Full Memorization) =====
    val fullMemorizationHighlightIndex by memorizationViewModelInstance.fullMemorizationHighlightIndex.collectAsState()
    val isFullMemorizationQuestionPlaying = memorizationUiState.isFullMemorizationQuestionPlaying
    val isFullMemorizationRecording = memorizationUiState.isFullMemorizationRecording
    val isFullMemorizationPlaying = memorizationUiState.isFullMemorizationPlaying
    val hasFullMemorizationRecording = memorizationUiState.hasFullMemorizationRecording
    val isFullMemorizationRecordingPlaying = memorizationUiState.isFullMemorizationRecordingPlaying

    // ===== 편의 변수들 =====
    val isMemorizeTestRunning = memorizationUiState.isMemorizeTestRunning
    val isFullMemorizationMode = memorizationUiState.isFullMemorizationMode
    val isEnglishWritingTestMode = memorizationUiState.isEnglishWritingTestMode

    // ===== 새로운 스마트 버튼 시스템 =====
    val questionButtonConfig = viewModel.getButtonConfig(ButtonFunction.QuestionPlay)
    val answerButtonConfig = viewModel.getButtonConfig(ButtonFunction.AnswerPlay)
    val memorizeTestButtonConfig = viewModel.getButtonConfig(ButtonFunction.MemorizeTest)
    val recordingPlayButtonConfig = viewModel.getButtonConfig(ButtonFunction.RecordingPlay)
    
    // ===== 편의 변수들 =====
    val qaItem = currentQaItemState
    val category = currentCategoryState
    val currentIndex = appState.currentIndex
    val totalCount = appState.totalCount

    // 앱 재시작 시 MemorizationViewModel 상태 초기화
    LaunchedEffect(Unit) {
        Log.d("MainScreenRefactored", "MainScreen 시작 - MemorizationViewModel 상태 초기화")
        memorizationViewModelInstance.resetStateOnAppRestart()
    }
    
    // 스크립트 변경 시 영작테스트 병합 파일 확인
    LaunchedEffect(appState.currentQaItem) {
        if (appState.currentQaItem != null) {
            Log.d("MainScreenRefactored", "스크립트 변경 감지 - 영작테스트 병합 파일 확인")
            viewModel.checkEnglishWritingTestMergedFile()
        }
    }

    // 영작테스트 완료 시 병합 파일 확인
    LaunchedEffect(englishWritingTestCompleted) {
        if (englishWritingTestCompleted) {
            Log.d("MainScreenRefactored", "영작테스트 완료 이벤트 감지 - 병합 파일 확인 시작")
            delay(500L) // 파일 시스템 동기화 추가 대기
            viewModel.checkEnglishWritingTestMergedFile()
            memorizationViewModelInstance.resetEnglishWritingTestCompleted()
            Log.d("MainScreenRefactored", "영작테스트 완료 이벤트 처리 완료")
        }
    }

    // 영작테스트 모드 종료 시 병합 파일 확인
    LaunchedEffect(isEnglishWritingTestMode) {
        if (!isEnglishWritingTestMode) {
            Log.d("MainScreenRefactored", "영작테스트 모드 종료 - 병합 파일 확인")
            viewModel.checkEnglishWritingTestMergedFile()
        }
    }

    // 영작테스트 녹음 파일 재생 중단 이벤트 처리
    LaunchedEffect(stopEnglishWritingTestMergedFilePlaying) {
        if (stopEnglishWritingTestMergedFilePlaying) {
            Log.d("MainScreenRefactored", "영작테스트 녹음 파일 재생 중단 이벤트 감지")
            viewModel.handleStopClick(com.na982.opichelper.domain.entity.ButtonFunction.RecordingPlay)
            memorizationViewModelInstance.resetStopEnglishWritingTestMergedFilePlaying()
            Log.d("MainScreenRefactored", "영작테스트 녹음 파일 재생 중단 처리 완료")
        }
    }

    // 암기레벨 변경 감지 및 상태 초기화
    LaunchedEffect(selectedLevel) {
        Log.d("MainScreenRefactored", "암기레벨 변경 감지: $selectedLevel")
        memorizationViewModelInstance.onMemorizeLevelChanged()
    }

    // 다크 테마 감지
    val isDarkTheme = isSystemInDarkTheme()

    // 암기레벨별 동적 테마 적용
    OPicHelperThemeWithMemorizeLevel(
        darkTheme = isDarkTheme,
        memorizeLevel = selectedLevel
    ) {
        // 백키 처리
        BackHandler {
            Log.d("MainScreenRefactored", "백키 감지 - 인터럽트 처리")
            viewModel.handleBackPress()
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 앱 제목 (설정 버튼 포함)
            AppTitle(
                currentLevel = appState.currentQaItem?.let { qaItem ->
                    when {
                        qaItem.answers.containsKey(com.na982.opichelper.domain.entity.UserLevel.IM) -> "IM"
                        qaItem.answers.containsKey(com.na982.opichelper.domain.entity.UserLevel.IH) -> "IH"
                        else -> "AL"
                    }
                } ?: "AL",
                onSettingsClick = {
                    Log.d("MainScreenRefactored", "AppTitle 내 설정 버튼 클릭됨")
                    viewModel.handleSettingsEnter()
                    onSettingsClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // 카테고리/암기레벨 선택 영역 (1:1 비율 Row)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 카테고리 선택기 + 설명
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        CategorySelector(
                            selectedCategory = category ?: "",
                            categories = listOf("은행", "해변", "가족친구", "패션", "가구", "휴일", "집휴가", "집", "산업직업", "인터넷", "영화", "음악", "예약", "레스토랑", "교통"),
                            onCategorySelected = {
                                viewModel.handleCategoryChange(it)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📚 학습할 주제를 선택하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 암기레벨 선택기 + 설명
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        MemorizeLevelSelector(
                            selectedLevel = selectedLevel,
                            levels = memorizeLevels,
                            onLevelSelected = {
                                viewModel.handleMemorizeLevelChange(it)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🎯 학습 방법을 선택하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 메인 콘텐츠 영역
            when {
                currentQaItemState == null -> {
                    // 로딩 상태
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                qaItem != null -> {
                    // 질문 카드 (기존 아름다운 디자인 복원)
                    QuestionCard(
                        currentQuestion = qaItem.questionEn,
                        currentQuestionKo = qaItem.questionKo,
                        highlightIndex = when {
                            selectedLevel == "통암기" -> fullMemorizationHighlightIndex
                            else -> appState.questionHighlightIndex
                        },
                        currentIndex = currentIndex,
                        totalCount = totalCount,
                        isFlipped = isQuestionCardFlipped,
                        currentCategory = category ?: "",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Log.d("MainScreenRefactored", "질문 카드 상태: isFlipped=$isQuestionCardFlipped, isFullMemorizationMode=$isFullMemorizationMode")

                    // 질문 카드 바로 아래 버튼들
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 질문 재생 스마트 버튼
                        QuestionPlaySmartButton(
                            buttonConfig = questionButtonConfig,
                            onPlayClick = {
                                // 현재 선택된 암기레벨에 따라 모드 결정
                                val isFullMemorizationModeSelected = selectedLevel == "통암기"
                                viewModel.handleQuestionPlayClick()
                            },
                            onStopClick = {
                                viewModel.handleStopClick(ButtonFunction.QuestionPlay)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // 암기 테스트 스마트 버튼
                        MemorizeTestSmartButton(
                            buttonConfig = memorizeTestButtonConfig,
                            onPlayClick = {
                                val memorizeLevel = when (selectedLevel) {
                                    "반복 듣기" -> MemorizeLevel.REPEAT_LISTENING
                                    "영작 테스트" -> MemorizeLevel.ENGLISH_WRITING
                                    "통암기" -> MemorizeLevel.FULL_MEMORIZATION
                                    else -> MemorizeLevel.REPEAT_LISTENING
                                }
                                viewModel.handleMemorizeTestClick()
                            },
                            onStopClick = {
                                viewModel.handleStopClick(ButtonFunction.MemorizeTest)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 답변 카드 (통암기 모드에서 질문 재생 중이거나 녹음 중일 때는 숨김)
                    if (!isFullMemorizationMode || (!isFullMemorizationQuestionPlaying && !isFullMemorizationRecording)) {
                        // 답변 카드 (기존 아름다운 디자인 복원)
                        AnswerCard(
                            currentAnswer = viewModel.getCurrentAnswer(qaItem),
                            currentAnswerKo = viewModel.getCurrentAnswerKo(qaItem),
                            highlightIndex = when {
                                isRepeatListeningCardFlipped -> memorizationUiState.answerKoHighlightIndex
                                else -> memorizationUiState.answerHighlightIndex
                            },
                            answerKoHighlightIndex = memorizationUiState.answerKoHighlightIndex,
                            recordingHighlightIndex = memorizationUiState.recordingHighlightIndex,
                            isFlipped = when {
                                isRepeatListeningCardFlipped -> true
                                isEnglishWritingTestCardFlipped -> true
                                else -> appState.isAnswerCardFlipped
                            },
                            isRepeatListeningCardFlipped = isRepeatListeningCardFlipped,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 답변 카드 아래 버튼들
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 답변 재생 스마트 버튼
                            AnswerPlaySmartButton(
                                buttonConfig = answerButtonConfig,
                                                            onPlayClick = {
                                viewModel.handleAnswerPlayClick()
                            },
                                onStopClick = {
                                    viewModel.handleStopClick(ButtonFunction.AnswerPlay)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // 녹음 재생 스마트 버튼 (조건부 표시)
                            RecordingPlaySmartButton(
                                buttonConfig = recordingPlayButtonConfig,
                                onPlayClick = {
                                    val memorizeLevel = when (selectedLevel) {
                                        "반복 듣기" -> MemorizeLevel.REPEAT_LISTENING
                                        "영작 테스트" -> MemorizeLevel.ENGLISH_WRITING
                                        "통암기" -> MemorizeLevel.FULL_MEMORIZATION
                                        else -> MemorizeLevel.REPEAT_LISTENING
                                    }
                                    viewModel.handleRecordingPlayClick()
                                },
                                onStopClick = {
                                    viewModel.handleStopClick(ButtonFunction.RecordingPlay)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 통암기 모드에서 녹음 중일 때 표시할 UI
                    if (isFullMemorizationMode && isFullMemorizationRecording) {
                        // 통암기 녹음 중 UI (기존 디자인 복원)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "통암기 녹음 중",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "답변을 녹음하고 있습니다...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        memorizationViewModelInstance.stopFullMemorizationRecording()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("중지")
                                }
                            }
                        }
                    }
                }
            }

            // 스크립트 네비게이션 (기존 아름다운 디자인 복원) - 맨 아래로 이동
            NavigationSection(
                onPreviousQuestion = {
                    viewModel.handleScriptChange()
                    viewModel.previousQaItem()
                },
                onNextQuestion = {
                    viewModel.handleScriptChange()
                    viewModel.nextQaItem()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
} 