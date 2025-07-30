package com.na982.opichelper.domain.event

import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.usecase.ExecuteFullMemorizationUseCase
import com.na982.opichelper.domain.usecase.ExecuteRepeatListeningUseCase
import com.na982.opichelper.domain.usecase.ExecuteEnglishWritingTestUseCase
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.ButtonFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 버튼 이벤트를 처리하는 클래스
 * 단일 책임: 이벤트 처리만 담당
 */
@Singleton
class ButtonEventHandler @Inject constructor(
    private val appStateManager: AppStateManager,
    private val ttsPlaybackController: TtsPlaybackController,
    private val executeFullMemorizationUseCase: ExecuteFullMemorizationUseCase,
    private val executeRepeatListeningUseCase: ExecuteRepeatListeningUseCase,
    private val executeEnglishWritingTestUseCase: ExecuteEnglishWritingTestUseCase
) {
    
    private val coroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    
    /**
     * 버튼 이벤트 처리
     */
    suspend fun handleEvent(event: ButtonEvent): ButtonEventResult {
        return try {
            Log.d("ButtonEventHandler", "이벤트 처리 시작: $event")
            
            when (event) {
                is ButtonEvent.QuestionPlayClick -> handleQuestionPlayClick(event)
                is ButtonEvent.AnswerPlayClick -> handleAnswerPlayClick(event)
                is ButtonEvent.MemorizeTestClick -> handleMemorizeTestClick(event)
                is ButtonEvent.RecordingPlayClick -> handleRecordingPlayClick(event)
                is ButtonEvent.StopClick -> handleStopClick(event)
            }
        } catch (e: Exception) {
            Log.e("ButtonEventHandler", "이벤트 처리 실패: $event", e)
            ButtonEventResult.Error(e.message ?: "알 수 없는 오류")
        }
    }
    
    private suspend fun handleQuestionPlayClick(event: ButtonEvent.QuestionPlayClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "질문 재생 이벤트 처리")
        
        // 1. 버튼 상태를 Loading으로 변경
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Loading)
        
        // 2. 통암기 모드인지 확인
        if (event.isFullMemorizationMode) {
            Log.d("ButtonEventHandler", "통암기 모드 - 통암기 UseCase 시작")
            executeFullMemorizationUseCase.startFullMemorization(
                category = event.category,
                scriptIndex = event.scriptIndex,
                onRecordingStateChange = { isRecording ->
                    // 녹음 상태 업데이트
                },
                onPlayingStateChange = { isPlaying ->
                    appStateManager.updateTtsPlayingState(isPlaying, false)
                }
            )
        } else {
            Log.d("ButtonEventHandler", "일반 모드 - 질문 TTS 재생")
            ttsPlaybackController.playQuestion(event.question)
        }
        
        // 3. 버튼 상태를 Playing으로 변경
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)
        
        return ButtonEventResult.Success
    }
    
    private suspend fun handleAnswerPlayClick(event: ButtonEvent.AnswerPlayClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "답변 재생 이벤트 처리")
        
        // 1. 버튼 상태를 Loading으로 변경
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Loading)
        
        // 2. 답변 TTS 재생
        ttsPlaybackController.playAnswer(event.answer)
        
        // 3. 버튼 상태를 Playing으로 변경
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
        
        // 1. TTS 중지
        ttsPlaybackController.stopTts()
        
        // 2. 버튼 상태를 Idle로 변경
        appStateManager.updateButtonState(event.buttonFunction, ButtonState.Idle)
        
        return ButtonEventResult.Success
    }
} 