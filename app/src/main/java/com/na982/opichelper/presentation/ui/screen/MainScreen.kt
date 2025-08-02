package com.na982.opichelper.presentation.ui.screen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.presentation.ui.component.AnswerPlaySmartButton
import com.na982.opichelper.presentation.ui.component.MemorizeTestSmartButton
import com.na982.opichelper.presentation.ui.component.QuestionPlaySmartButton
import com.na982.opichelper.presentation.ui.component.RecordingPlaySmartButton
import com.na982.opichelper.presentation.ui.component.SmartButton
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.AnswerCard
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.AppTitle
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.CategorySelector
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.MemorizeLevelSelector
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.NavigationSection
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.QuestionCard
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.presentation.viewmodel.MemorizationViewModel
import com.na982.opichelper.ui.theme.OPicHelperThemeWithMemorizeLevel
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreenRefactored(
    viewModel: MainViewModel,
    memorizationViewModel: MemorizationViewModel? = null,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository
) {
    val context = LocalContext.current
    
    // 앱 초기화 (한 번만 실행)
    LaunchedEffect(Unit) {
        Log.d("MainScreen", "앱 초기화 시작")
        viewModel.initializeApp(context.applicationContext as android.app.Application)
    }
    
    // ===== 새로운 아키텍처 상태 =====
    val appState by viewModel.appState.collectAsState()
    val memorizationViewModelInstance = memorizationViewModel ?: hiltViewModel<MemorizationViewModel>()
    val selectedLevel = appState.selectedMemorizeLevel
    val currentQaItemState = appState.currentQaItem
    val currentCategoryState = appState.currentCategory

    // ===== 공통 상태 (모든 모드에서 사용) =====
    val isQuestionCardFlipped by viewModel.isQuestionCardFlipped().collectAsState()

    // ===== MemorizationViewModel UI 상태 =====
    val memorizationUiState by memorizationViewModelInstance.uiState.collectAsState()

    // ===== 반복 듣기 (Repeat Listening) =====
    val isRepeatListeningCardFlipped = memorizationUiState.isRepeatListeningCardFlipped

    // ===== 영작 테스트 (English Writing) =====
    val isEnglishWritingTestCardFlipped = memorizationUiState.isEnglishWritingTestCardFlipped

    // ===== 통암기 (Full Memorization) =====
    val isFullMemorizationQuestionPlaying = memorizationUiState.isFullMemorizationQuestionPlaying
    val isFullMemorizationRecording = memorizationUiState.isFullMemorizationRecording
    memorizationUiState.isFullMemorizationPlaying
    memorizationUiState.hasFullMemorizationRecording
    memorizationUiState.isFullMemorizationRecordingPlaying

    // ===== 편의 변수들 =====
    memorizationUiState.isMemorizeTestRunning
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

    // 영작테스트 모드 종료 시 병합 파일 확인
    LaunchedEffect(isEnglishWritingTestMode) {
        if (!isEnglishWritingTestMode) {
            Log.d("MainScreenRefactored", "영작테스트 모드 종료 - 병합 파일 확인")
            viewModel.checkEnglishWritingTestMergedFile()
        }
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
                currentLevel = userPreferencesRepository.getUserLevel().displayName,
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
                            levels = listOf("반복 듣기", "영작 테스트", "통암기"),
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
                    val questionHighlightIndex = appState.questionHighlightIndex
                    
                    Log.d("MainScreenRefactored", "질문 카드 하이라이트 상태: selectedLevel=$selectedLevel, appState.questionHighlightIndex=${appState.questionHighlightIndex}, finalHighlightIndex=$questionHighlightIndex")
                    Log.d("MainScreenRefactored", "질문 카드 하이라이트 상세: TTS 상태 - isQuestionPlaying=${appState.isQuestionPlaying}, isAnswerPlaying=${appState.isAnswerPlaying}, isPlaying=${appState.isPlaying}")
                    
                    QuestionCard(
                        currentQuestion = qaItem.questionEn,
                        currentQuestionKo = qaItem.questionKo,
                        highlightIndex = questionHighlightIndex,
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
                                viewModel.handleQuestionPlayClick()
                            },
                            onStopClick = {
                                viewModel.handleStopClick(ButtonFunction.QuestionPlay)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // 암기 테스트 스마트 버튼
                        SmartButton(
                            buttonConfig = memorizeTestButtonConfig,
                            onPlayClick = {
                                when (selectedLevel) {
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
                        // 하이라이트 상태는 AppState만 사용 (단일 진실 소스)
                        val finalHighlightIndex = when {
                            selectedLevel == "반복듣기" && appState.isAnswerCardFlipped -> appState.answerKoHighlightIndex
                            selectedLevel == "반복듣기" -> appState.answerHighlightIndex
                            else -> appState.answerHighlightIndex
                        }
                        val finalAnswerKoHighlightIndex = appState.answerKoHighlightIndex
                        val finalRecordingHighlightIndex = appState.recordingHighlightIndex
                        
                        Log.d("MainScreenRefactored", "하이라이트 상태 - selectedLevel: $selectedLevel, isAnswerCardFlipped: ${appState.isAnswerCardFlipped}")
                        Log.d("MainScreenRefactored", "하이라이트 상태 - finalHighlightIndex: $finalHighlightIndex, finalAnswerKoHighlightIndex: $finalAnswerKoHighlightIndex")
                        
                        AnswerCard(
                            currentAnswer = viewModel.getCurrentAnswer(qaItem),
                            currentAnswerKo = viewModel.getCurrentAnswerKo(qaItem),
                            highlightIndex = finalHighlightIndex,
                            answerKoHighlightIndex = finalAnswerKoHighlightIndex,
                            recordingHighlightIndex = finalRecordingHighlightIndex,
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
                                    when (selectedLevel) {
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