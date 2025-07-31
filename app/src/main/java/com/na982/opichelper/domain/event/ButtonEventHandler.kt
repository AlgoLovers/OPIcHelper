package com.na982.opichelper.domain.event


import com.na982.opichelper.domain.entity.*
import com.na982.opichelper.domain.usecase.*
import com.na982.opichelper.domain.state.AppStateManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import com.na982.opichelper.domain.audio.TtsController

/**
 * 버튼 이벤트 핸들러
 * 책임: 버튼 클릭 이벤트 처리, UseCase 호출, 상태 관리
 */
@Singleton
class ButtonEventHandler @Inject constructor(
    private val appStateManager: AppStateManager,
    private val executeRepeatListeningUseCase: ExecuteRepeatListeningUseCase,
    private val executeEnglishWritingTestUseCase: ExecuteEnglishWritingTestUseCase,
    private val executeFullMemorizationUseCase: ExecuteFullMemorizationUseCase,
    private val repeatListeningService: RepeatListeningService,
    private val ttsController: TtsController
) {
    
    /**
     * 버튼 이벤트 처리
     */
    suspend fun handleEvent(event: ButtonEvent): ButtonEventResult {
        return when (event) {
            is ButtonEvent.QuestionPlayClick -> handleQuestionPlayClick(event)
            is ButtonEvent.AnswerPlayClick -> handleAnswerPlayClick(event)
            is ButtonEvent.MemorizeTestClick -> handleMemorizeTestClick(event)
            is ButtonEvent.RecordingPlayClick -> handleRecordingPlayClick(event)
            is ButtonEvent.StopClick -> handleStopClick(event)
        }
    }
    
    private suspend fun handleQuestionPlayClick(event: ButtonEvent.QuestionPlayClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "질문 재생 이벤트 처리")
        
        // 1. 다른 작업 중단 (다른 버튼이 실행 중이면 중단)
        stopOtherOperations()
        
        // 2. 버튼 상태를 Loading으로 변경
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Loading)
        
        // 3. TTS 재생 (하이라이트 포함)
        ttsController.playQuestion(event.question)
        
        // 4. 버튼 상태를 Playing으로 변경
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)
        
        return ButtonEventResult.Success
    }
    
    private suspend fun handleAnswerPlayClick(event: ButtonEvent.AnswerPlayClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "답변 재생 이벤트 처리")
        
        // 1. 다른 작업 중단 (다른 버튼이 실행 중이면 중단)
        stopOtherOperations()
        
        // 2. 버튼 상태를 Loading으로 변경
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Loading)
        
        // 3. TTS 재생 (하이라이트 포함)
        ttsController.playAnswer(event.answer)
        
        // 4. 버튼 상태를 Playing으로 변경
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Playing)
        
        return ButtonEventResult.Success
    }
    
    private suspend fun handleMemorizeTestClick(event: ButtonEvent.MemorizeTestClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "암기 테스트 이벤트 처리")
        
        // 1. 버튼 상태를 Loading으로 변경
        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Loading)
        
        // 2. 암기 레벨에 따른 처리
        when (event.memorizeLevel) {
            com.na982.opichelper.domain.entity.MemorizeLevel.REPEAT_LISTENING -> {
                Log.d("ButtonEventHandler", "반복 듣기 모드 시작")
                val repeatListeningData = com.na982.opichelper.domain.entity.RepeatListeningData(
                    category = event.category,
                    scriptIndex = event.scriptIndex,
                    koreanAnswer = event.answerKo,
                    englishAnswer = event.answerEn
                )
                executeRepeatListeningUseCase.execute(
                    data = repeatListeningData,
                    uiCallback = object : com.na982.opichelper.domain.audio.RepeatListeningUiCallback {
                        override fun onCardFlip(isKorean: Boolean) {
                            Log.d("ButtonEventHandler", "반복듣기 카드 뒤집기: ${if (isKorean) "한글" else "영문"}")
                            // 카드 뒤집기 상태 업데이트
                            appStateManager.updateCardState(
                                isAnswerCardFlipped = isKorean
                            )
                        }
                        override fun onHighlight(index: Int?) {
                            Log.d("ButtonEventHandler", "반복듣기 영문 하이라이트: $index")
                            // 영문 하이라이트 상태 업데이트
                            appStateManager.updateHighlightState(
                                answerHighlightIndex = index
                            )
                            Log.d("ButtonEventHandler", "반복듣기 영문 하이라이트 상태 업데이트 완료: $index")
                        }
                        override fun onKoreanHighlight(index: Int?) {
                            Log.d("ButtonEventHandler", "반복듣기 한글 하이라이트: $index")
                            // 한글 하이라이트 상태 업데이트
                            appStateManager.updateHighlightState(
                                answerKoHighlightIndex = index
                            )
                            Log.d("ButtonEventHandler", "반복듣기 한글 하이라이트 상태 업데이트 완료: $index")
                        }
                        override fun onComplete() {
                            Log.d("ButtonEventHandler", "반복듣기 완료")
                            // 완료 시 버튼 상태를 Idle로 변경
                            appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
                            // 하이라이트 초기화
                            appStateManager.updateHighlightState(
                                answerHighlightIndex = null,
                                answerKoHighlightIndex = null
                            )
                            // 카드 상태 초기화
                            appStateManager.updateCardState(
                                isAnswerCardFlipped = false
                            )
                        }
                    }
                )
            }
            com.na982.opichelper.domain.entity.MemorizeLevel.ENGLISH_WRITING -> {
                Log.d("ButtonEventHandler", "영작 테스트 모드 시작")
                executeEnglishWritingTestUseCase.execute(
                    answerKo = event.answerKo,
                    answerEn = event.answerEn,
                    category = event.category,
                    scriptIndex = event.scriptIndex,
                    onCardFlip = { isKorean ->
                        // 카드 뒤집기 처리
                    },
                    onKoreanHighlight = { index ->
                        // 한글 하이라이트 처리
                    },
                    onRecordingHighlight = { index ->
                        // 녹음 하이라이트 처리
                    },
                    onRecordingStateChange = { isRecording ->
                        // 녹음 상태 변경
                    },
                    onMergedFileCreated = {
                        // 병합 파일 생성 완료 처리
                    }
                )
            }
            com.na982.opichelper.domain.entity.MemorizeLevel.FULL_MEMORIZATION -> {
                Log.d("ButtonEventHandler", "통암기 모드 시작")
                executeFullMemorizationUseCase.startFullMemorization(
                    category = event.category,
                    scriptIndex = event.scriptIndex,
                    onRecordingStateChange = { isRecording ->
                        // 녹음 상태 변경
                    },
                    onPlayingStateChange = { isPlaying ->
                        appStateManager.updateTtsPlayingState(isPlaying, false)
                    }
                )
            }
        }
        
        // 3. 버튼 상태를 Playing으로 변경
        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Playing)
        
        return ButtonEventResult.Success
    }
    
    private suspend fun handleRecordingPlayClick(event: ButtonEvent.RecordingPlayClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "녹음 재생 이벤트 처리")
        
        // 1. 버튼 상태를 Loading으로 변경
        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Loading)
        
        // 2. 녹음 재생 처리 (구현 필요)
        
        // 3. 버튼 상태를 Playing으로 변경
        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Playing)
        
        return ButtonEventResult.Success
    }
    
    private suspend fun handleStopClick(event: ButtonEvent.StopClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "중지 이벤트 처리")
        
        // 1. TTS 중지 (하이라이트 초기화 포함)
        ttsController.stopTts()
        
        // 2. 반복듣기 중지 (MemorizeTest 버튼인 경우)
        if (event.buttonFunction == ButtonFunction.MemorizeTest) {
            repeatListeningService.stopRepeatListening()
            
            // 반복듣기 중지 시 카드 상태 초기화 (하이라이트는 TtsController에서 처리)
            appStateManager.updateCardState(
                isAnswerCardFlipped = false
            )
            Log.d("ButtonEventHandler", "반복듣기 중지 - 카드 상태 초기화")
        }
        
        // 3. 버튼 상태를 Idle로 변경
        appStateManager.updateButtonState(event.buttonFunction, ButtonState.Idle)
        
        return ButtonEventResult.Success
    }

    /**
     * 다른 작업들을 중단하는 헬퍼 메서드
     */
    private suspend fun stopOtherOperations() {
        Log.d("ButtonEventHandler", "다른 작업들 중단")
        
        // 1. TTS 중지
        ttsController.stopTts()
        
        // 2. 반복듣기 중지
        repeatListeningService.stopRepeatListening()
        
        // 3. 모든 버튼 상태를 Idle로 변경
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
        
        // 4. 카드 상태 초기화
        appStateManager.updateCardState(
            isQuestionCardFlipped = false,
            isAnswerCardFlipped = false
        )
        
        Log.d("ButtonEventHandler", "다른 작업들 중단 완료")
    }
} 