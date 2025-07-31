package com.na982.opichelper.domain.audio

import android.util.Log
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.repository.QaDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 버튼 동작을 처리하는 클래스
 * 의존성 역전 원칙에 따라 버튼 동작을 추상화
 */
@Singleton
class ButtonActionHandler @Inject constructor(
    private val buttonStateManager: ButtonStateManager,
    private val ttsOrchestrator: TtsOrchestrator,
    private val qaDataManager: QaDataManager,
    private val executeFullMemorizationUseCase: com.na982.opichelper.domain.usecase.ExecuteFullMemorizationUseCase,
    private val executeRepeatListeningUseCase: com.na982.opichelper.domain.usecase.ExecuteRepeatListeningUseCase,
    private val executeEnglishWritingTestUseCase: com.na982.opichelper.domain.usecase.ExecuteEnglishWritingTestUseCase
) {
    
    private val coroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    
    /**
     * 질문 재생 버튼 클릭 처리
     */
    fun handleQuestionPlayClick(
        question: String, 
        isFullMemorizationMode: Boolean, 
        category: String,
        scriptIndex: Int,
        onStateChange: (Boolean, Boolean) -> Unit = { _, _ -> }
    ) {
        Log.d("ButtonActionHandler", "질문 재생 버튼 클릭 처리 시작")
        
        coroutineScope.launch {
            try {
                // 1. 버튼 상태를 Loading으로 변경
                buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Loading)
                
                // 2. 다른 작업 중단 (질문 재생 시에는 긴급 중지하지 않음)
                
                // 3. 통암기 모드인지 확인
                if (isFullMemorizationMode) {
                    Log.d("ButtonActionHandler", "통암기 모드 - 통암기 UseCase 시작")
                    // 통암기 모드에서는 UseCase를 통해 처리
                    executeFullMemorizationUseCase.startFullMemorization(
                        category = category,
                        scriptIndex = scriptIndex,
                        onRecordingStateChange = { isRecording ->
                            onStateChange(false, isRecording)
                        },
                        onPlayingStateChange = { isPlaying ->
                            onStateChange(isPlaying, false)
                        }
                    )
                } else {
                    Log.d("ButtonActionHandler", "일반 모드 - 질문 TTS 재생")
                    // 일반 모드에서는 TTS 재생
                    ttsOrchestrator.speak(question, null)
                    
                    // TTS 완료 후 버튼 상태를 Idle로 변경
                    // TTS 재생이 완료되면 자동으로 상태가 변경됨
                }
                
                // 4. 버튼 상태를 Playing으로 변경 (TTS 재생 중)
                buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)
                
                Log.d("ButtonActionHandler", "질문 재생 버튼 클릭 처리 완료")
            } catch (e: Exception) {
                Log.e("ButtonActionHandler", "질문 재생 버튼 클릭 처리 실패", e)
                buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Error)
            }
        }
    }
    
    /**
     * 답변 재생 버튼 클릭 처리
     */
    fun handleAnswerPlayClick(answer: String) {
        Log.d("ButtonActionHandler", "답변 재생 버튼 클릭 처리 시작")
        
        coroutineScope.launch {
            try {
                // 1. 버튼 상태를 Loading으로 변경
                buttonStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Loading)
                
                // 2. 다른 작업 중단
                
                // 3. 답변 TTS 재생
                ttsOrchestrator.speak(answer, null)
                
                // 4. 버튼 상태를 Playing으로 변경
                buttonStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Playing)
                
                Log.d("ButtonActionHandler", "답변 재생 버튼 클릭 처리 완료")
            } catch (e: Exception) {
                Log.e("ButtonActionHandler", "답변 재생 버튼 클릭 처리 실패", e)
                buttonStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Error)
            }
        }
    }
    
    /**
     * 암기 테스트 버튼 클릭 처리
     */
    fun handleMemorizeTestClick(
        memorizeLevel: MemorizeLevel, 
        category: String,
        scriptIndex: Int,
        answerKo: String = "",
        answerEn: String = "",
        onStateChange: (Boolean, Boolean) -> Unit = { _, _ -> }
    ) {
        Log.d("ButtonActionHandler", "암기 테스트 버튼 클릭 처리 시작 - 레벨: $memorizeLevel")
        
        coroutineScope.launch {
            try {
                // 1. 버튼 상태를 Loading으로 변경
                buttonStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Loading)
                
                // 2. 다른 작업 중단
                
                // 3. 암기 레벨에 따른 처리
                when (memorizeLevel) {
                    MemorizeLevel.REPEAT_LISTENING -> {
                        Log.d("ButtonActionHandler", "반복 듣기 모드 시작")
                        // UseCase를 통해 반복듣기 실행
                        val repeatListeningData = com.na982.opichelper.domain.entity.RepeatListeningData(
                            category = category,
                            scriptIndex = scriptIndex,
                            koreanAnswer = answerKo,
                            englishAnswer = answerEn
                        )
                        executeRepeatListeningUseCase.execute(
                            data = repeatListeningData,
                            uiCallback = object : RepeatListeningUiCallback {
                                override fun onCardFlip(isKorean: Boolean) {
                                    // 카드 뒤집기 처리
                                }
                                override fun onHighlight(index: Int?) {
                                    // 하이라이트 처리
                                }
                                override fun onKoreanHighlight(index: Int?) {
                                    // 한글 하이라이트 처리
                                }
                                override fun onComplete() {
                                    // 완료 처리
                                }
                            }
                        )
                    }
                    MemorizeLevel.ENGLISH_WRITING -> {
                        Log.d("ButtonActionHandler", "영작 테스트 모드 시작")
                        // UseCase를 통해 영작테스트 실행
                        executeEnglishWritingTestUseCase.execute(
                            answerKo = answerKo,
                            answerEn = answerEn,
                            category = category,
                            scriptIndex = scriptIndex,
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
                                onStateChange(false, isRecording)
                            },
                            onMergedFileCreated = {
                                // 병합 파일 생성 완료 처리
                            }
                        )
                    }
                    MemorizeLevel.FULL_MEMORIZATION -> {
                        Log.d("ButtonActionHandler", "통암기 모드 시작")
                        // UseCase를 통해 통암기 실행
                        executeFullMemorizationUseCase.startFullMemorization(
                            category = category,
                            scriptIndex = scriptIndex,
                            onRecordingStateChange = { isRecording ->
                                onStateChange(false, isRecording)
                            },
                            onPlayingStateChange = { isPlaying ->
                                onStateChange(isPlaying, false)
                            }
                        )
                    }
                }
                
                // 4. 버튼 상태를 Playing으로 변경
                buttonStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Playing)
                
                Log.d("ButtonActionHandler", "암기 테스트 버튼 클릭 처리 완료")
            } catch (e: Exception) {
                Log.e("ButtonActionHandler", "암기 테스트 버튼 클릭 처리 실패", e)
                buttonStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Error)
            }
        }
    }
    
    /**
     * 녹음 재생 버튼 클릭 처리
     */
    fun handleRecordingPlayClick(memorizeLevel: MemorizeLevel) {
        Log.d("ButtonActionHandler", "녹음 재생 버튼 클릭 처리 시작 - 레벨: $memorizeLevel")
        
        coroutineScope.launch {
            try {
                // 1. 버튼 상태를 Loading으로 변경
                buttonStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Loading)
                
                // 2. 다른 작업 중단
                
                // 3. 암기 레벨에 따른 녹음 재생
                when (memorizeLevel) {
                    MemorizeLevel.ENGLISH_WRITING -> {
                        Log.d("ButtonActionHandler", "영작테스트 녹음 재생")
                        // MainViewModel에 이벤트 발생
                        // 이는 MainViewModel에서 처리
                    }
                    MemorizeLevel.FULL_MEMORIZATION -> {
                        Log.d("ButtonActionHandler", "통암기 녹음 재생")
                        // MemorizationViewModel에 이벤트 발생
                        // 이는 MainViewModel에서 처리
                    }
                    else -> {
                        Log.w("ButtonActionHandler", "지원하지 않는 암기 레벨: $memorizeLevel")
                    }
                }
                
                // 4. 버튼 상태를 Playing으로 변경
                buttonStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Playing)
                
                Log.d("ButtonActionHandler", "녹음 재생 버튼 클릭 처리 완료")
            } catch (e: Exception) {
                Log.e("ButtonActionHandler", "녹음 재생 버튼 클릭 처리 실패", e)
                buttonStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Error)
            }
        }
    }
    
    /**
     * 버튼 중지 처리
     */
    fun handleStopClick(buttonType: ButtonFunction) {
        Log.d("ButtonActionHandler", "버튼 중지 처리 시작 - 타입: $buttonType")
        
        coroutineScope.launch {
            try {
                // 1. 해당 버튼의 작업 중단
                when (buttonType) {
                    is ButtonFunction.QuestionPlay -> {
                        ttsOrchestrator.stop()
                        buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
                    }
                    is ButtonFunction.AnswerPlay -> {
                        ttsOrchestrator.stop()
                        buttonStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
                    }
                    is ButtonFunction.MemorizeTest -> {
                        // MemorizationViewModel에 중지 이벤트 발생
                        // 이는 MainViewModel에서 처리
                        buttonStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
                    }
                    is ButtonFunction.RecordingPlay -> {
                        // MainViewModel에 중지 이벤트 발생
                        // 이는 MainViewModel에서 처리
                        buttonStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
                    }
                    is ButtonFunction.Stop -> {
                        // 긴급 중지 기능은 제거됨
                    }
                }
                
                Log.d("ButtonActionHandler", "버튼 중지 처리 완료")
            } catch (e: Exception) {
                Log.e("ButtonActionHandler", "버튼 중지 처리 실패", e)
                buttonStateManager.updateButtonState(buttonType, ButtonState.Error)
            }
        }
    }
    
    /**
     * TTS 재생 완료 시 버튼 상태 업데이트
     */
    fun onTtsPlaybackCompleted(buttonType: ButtonFunction) {
        Log.d("ButtonActionHandler", "TTS 재생 완료 - 버튼 상태 업데이트: $buttonType")
        buttonStateManager.updateButtonState(buttonType, ButtonState.Idle)
    }
    
    /**
     * 암기 테스트 완료 시 버튼 상태 업데이트
     */
    fun onMemorizeTestCompleted() {
        Log.d("ButtonActionHandler", "암기 테스트 완료 - 버튼 상태 업데이트")
        buttonStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
    }
    
    /**
     * 녹음 재생 완료 시 버튼 상태 업데이트
     */
    fun onRecordingPlaybackCompleted() {
        Log.d("ButtonActionHandler", "녹음 재생 완료 - 버튼 상태 업데이트")
        buttonStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
    }
} 