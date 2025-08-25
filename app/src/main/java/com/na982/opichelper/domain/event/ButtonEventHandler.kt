package com.na982.opichelper.domain.event

import android.util.Log
import com.na982.opichelper.domain.manager.IAudioControlManager
import com.na982.opichelper.domain.manager.ProgressManager
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.domain.strategy.MemorizationStrategyFactory
import com.na982.opichelper.domain.strategy.MemorizationUiCallback
import javax.inject.Inject
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * 버튼 이벤트 처리기
 * 모든 버튼 클릭 이벤트를 처리하고 적절한 액션을 수행
 */
@ViewModelScoped
class ButtonEventHandler @Inject constructor(
    private val audioControlManager: IAudioControlManager,
    private val appStateManager: AppStateManager,
    private val strategyFactory: MemorizationStrategyFactory,
    private val progressManager: ProgressManager,
    private val startRepeatListeningUseCase: com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase
) {
    
    /**
     * 버튼 이벤트 처리
     */
    suspend fun handleEvent(
        event: ButtonEvent,
        playRecordingUseCase: com.na982.opichelper.domain.usecase.PlayRecordingUseCase
    ): ButtonEventResult {
        return when (event) {
            is ButtonEvent.QuestionPlayClick -> handleQuestionPlayClick(event)
            is ButtonEvent.AnswerPlayClick -> handleAnswerPlayClick(event)
            is ButtonEvent.MemorizeTestClick -> handleMemorizeTestClick(event)
            is ButtonEvent.RecordingPlayClick -> handleRecordingPlayClick(event, playRecordingUseCase)
            is ButtonEvent.StopClick -> handleStopClick(event)
        }
    }
    
    /**
     * PlayRecordingUseCase가 필요하지 않은 이벤트들을 위한 오버로드
     */
    suspend fun handleEvent(event: ButtonEvent): ButtonEventResult {
        return when (event) {
            is ButtonEvent.QuestionPlayClick -> handleQuestionPlayClick(event)
            is ButtonEvent.AnswerPlayClick -> handleAnswerPlayClick(event)
            is ButtonEvent.MemorizeTestClick -> handleMemorizeTestClick(event)
            is ButtonEvent.StopClick -> handleStopClick(event)
            is ButtonEvent.RecordingPlayClick -> {
                // 녹음 재생은 PlayRecordingUseCase가 필요하므로 예외 발생
                throw IllegalStateException("녹음 재생 이벤트는 PlayRecordingUseCase와 함께 호출되어야 합니다")
            }
        }
    }
    
    /**
     * 질문 재생 이벤트 처리
     */
    private suspend fun handleQuestionPlayClick(event: ButtonEvent.QuestionPlayClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "질문 재생 이벤트 처리")

        // 1. 다른 작업 중단 (다른 버튼이 실행 중이면 중단)
        stopOtherOperations()

        // 2. 버튼 상태를 Playing으로 변경 (Loading 건너뛰기)
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // 3. 오디오 재생 요청 (콜백 기반)
        audioControlManager.playQuestion(event.qaItem) {
            // 4. 재생 완료 시 버튼 상태를 Idle로 변경
            appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
        }

        return ButtonEventResult.Success
    }
    
    /**
     * 답변 재생 이벤트 처리
     */
    private suspend fun handleAnswerPlayClick(event: ButtonEvent.AnswerPlayClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "답변 재생 이벤트 처리")
        
        // 1. 다른 작업 중단 (다른 버튼이 실행 중이면 중단)
        stopOtherOperations()

        // 2. 버튼 상태를 Playing으로 변경 (Loading 건너뛰기)
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Playing)

        // 3. 오디오 재생 요청 (콜백 기반)
        audioControlManager.playAnswer(event.qaItem) {
            // 4. 재생 완료 시 버튼 상태를 Idle로 변경
            appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
        }
        
        return ButtonEventResult.Success
    }
    
    /**
     * 암기 테스트 이벤트 처리
     */
    private suspend fun handleMemorizeTestClick(event: ButtonEvent.MemorizeTestClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "암기 테스트 이벤트 처리 시작 - 레벨: ${event.memorizeLevel}")
        
        // 1. 다른 작업 중단 (다른 버튼이 실행 중이면 중단)
        stopOtherOperations()
        
        // 2. 버튼 상태를 Loading으로 변경
        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Playing)
        
        // 3. 전략 팩토리에서 적절한 전략 가져오기
        val strategy = strategyFactory.getStrategy(event.memorizeLevel)
        
        // 5. 선택된 전략 실행
        strategy.execute(
            category = event.category,
            scriptIndex = event.scriptIndex,
            answerKo = event.answerKo,
            answerEn = event.answerEn,
            uiCallback = createMemorizationUiCallback()
        )
        
        Log.d("ButtonEventHandler", "암기 테스트 이벤트 처리 완료")
        return ButtonEventResult.Success
    }
    
    /**
     * 녹음 재생 이벤트 처리
     */
    private suspend fun handleRecordingPlayClick(
        event: ButtonEvent.RecordingPlayClick,
        playRecordingUseCase: com.na982.opichelper.domain.usecase.PlayRecordingUseCase
    ): ButtonEventResult {
        Log.d("ButtonEventHandler", "녹음 재생 이벤트 처리 시작 - 레벨: ${event.memorizeLevel}")
        
        // 1. 다른 작업 중단 (다른 버튼이 실행 중이면 중단)
        stopOtherOperations()
        
        // 2. 버튼 상태를 Playing으로 변경 (Loading 건너뛰기)
        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Playing)
        
        // 3. 녹음 재생 전용 UseCase 실행 (이벤트 기반)
        playRecordingUseCase.execute(
            memorizeLevel = event.memorizeLevel,
            category = event.category,
            scriptIndex = event.scriptIndex,
            onCompletion = {
                Log.d("ButtonEventHandler", "녹음 재생 완료")
                appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
            }
        )
        
        Log.d("ButtonEventHandler", "녹음 재생 이벤트 처리 완료")
        return ButtonEventResult.Success
    }
    
    /**
     * 중지 이벤트 처리
     */
    private suspend fun handleStopClick(event: ButtonEvent.StopClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "중지 이벤트 처리: ${event.buttonFunction}")
        
        // 1. 모든 오디오 중지 (Infrastructure Layer 호출)
        audioControlManager.stopAllAudio()
        
        // 2. 모든 버튼 상태를 Idle로 변경 (Domain Layer에서 처리)
        resetAllButtonStates()
        
        return ButtonEventResult.Success
    }
    
    /**
     * 다른 작업 중단
     */
    private suspend fun stopOtherOperations() {
        Log.d("ButtonEventHandler", "다른 작업 중단")
        
        // 반복듣기 UseCase 중지 (코루틴 취소)
        startRepeatListeningUseCase.stop()
        
        // 모든 오디오 중지
        audioControlManager.stopAllAudio()
        
        // 모든 버튼 상태를 Idle로 변경
        resetAllButtonStates()
    }
    
    /**
     * 모든 버튼 상태를 Idle로 초기화 (Domain Layer 책임)
     */
    private suspend fun resetAllButtonStates() {
        Log.d("ButtonEventHandler", "모든 버튼 상태를 Idle로 초기화")
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
    }
    
    /**
     * 암기 전략에서 사용할 UI 콜백 생성
     */
    private fun createMemorizationUiCallback(): MemorizationUiCallback {
        return object : MemorizationUiCallback {
            override fun onCardFlip(isKorean: Boolean) {
                Log.d("ButtonEventHandler", "카드 뒤집기: ${if (isKorean) "한글" else "영문"}")
                appStateManager.updateCardState(isAnswerCardFlipped = isKorean)
            }
            
            override fun onHighlight(index: Int) {
                Log.d("ButtonEventHandler", "영문 하이라이트: $index")
                appStateManager.updateHighlightState(
                    questionHighlightIndex = -1,
                    answerHighlightIndex = index,
                    answerKoHighlightIndex = -1
                )
            }
            
            override fun onKoreanHighlight(index: Int) {
                Log.d("ButtonEventHandler", "한글 하이라이트: $index")
                appStateManager.updateHighlightState(
                    questionHighlightIndex = -1,
                    answerHighlightIndex = -1,
                    answerKoHighlightIndex = index
                )
            }
            
            override fun onRecordingHighlight(index: Int) {
                Log.d("ButtonEventHandler", "녹음 하이라이트: $index")
                appStateManager.updateHighlightState(
                    questionHighlightIndex = -1,
                    answerHighlightIndex = -1,
                    answerKoHighlightIndex = -1,
                    recordingHighlightIndex = index
                )
            }
            
            override fun onRecordingStateChange(isRecording: Boolean) {
                Log.d("ButtonEventHandler", "녹음 상태 변경: $isRecording")
                appStateManager.updateRecordingState(isRecording)
            }
            
            override fun onPlayingStateChange(isPlaying: Boolean) {
                Log.d("ButtonEventHandler", "재생 상태 변경: $isPlaying")
                appStateManager.updateTtsPlayingState(isPlaying = isPlaying)
            }
            
            override fun onMergedFileCreated() {
                Log.d("ButtonEventHandler", "병합 파일 생성 완료")
                // 영작테스트 완료 처리
                progressManager.onEnglishWritingTestCompleted()
            }
            
            override fun onComplete() {
                Log.d("ButtonEventHandler", "암기 테스트 완료 - 버튼 상태를 Idle로 변경")
                appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
            }
        }
    }
} 