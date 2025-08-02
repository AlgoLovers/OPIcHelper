package com.na982.opichelper.domain.button

import android.util.Log
import com.na982.opichelper.domain.audio.AudioFileManager
import com.na982.opichelper.domain.button.ButtonStateManager
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase
import com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase
import com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 버튼 액션 핸들러
 * 책임: 버튼 클릭 시 실제 동작 처리
 */
@Singleton
class ButtonActionHandler @Inject constructor(
    private val buttonStateManager: ButtonStateManager,
    private val ttsController: TtsController,
    private val qaDataRepository: QaDataRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val executeFullMemorizationUseCase: StartFullMemorizationUseCase,
    private val executeRepeatListeningUseCase: StartRepeatListeningUseCase,
    private val executeEnglishWritingTestUseCase: StartEnglishWritingTestUseCase
) {
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    /**
     * 질문 재생 버튼 클릭 처리
     */
    fun handleQuestionPlayClick(
        question: String,
        category: String,
        scriptIndex: Int,
        onStateChange: (Boolean, Boolean) -> Unit = { _, _ -> }
    ) {
        Log.d("ButtonActionHandler", "질문 재생 버튼 클릭 처리 시작")
        
        coroutineScope.launch {
            try {
                // 1. 버튼 상태를 Loading으로 변경
                buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Loading)
                
                // 2. 다른 작업 중단
                
                // 3. 선택된 암기레벨에 따른 처리
                val selectedLevel = "반복듣기" // 임시로 하드코딩
                if (selectedLevel == "통암기") {
                    Log.d("ButtonActionHandler", "통암기 모드 - 통암기 UseCase 시작")
                    // 통암기 모드에서는 UseCase를 통해 처리
                    executeFullMemorizationUseCase.execute(
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
                    // 일반 모드에서는 TTS 재생 (하이라이트 포함)
                    ttsController.playQuestion(question)
                    
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
                
                // 3. 답변 TTS 재생 (하이라이트 포함)
                ttsController.playAnswer(answer)
                
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
                            uiCallback = object : com.na982.opichelper.domain.audio.RepeatListeningUiCallback {
                                override fun onCardFlip(isKorean: Boolean) {
                                    // 카드 뒤집기 처리
                                }
                                override fun onHighlight(index: Int) {
                                    // 하이라이트 처리
                                }
                                override fun onKoreanHighlight(index: Int) {
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
                        executeFullMemorizationUseCase.execute(
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
                        ttsController.stopAllTts()
                        buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
                    }
                    is ButtonFunction.AnswerPlay -> {
                        ttsController.stopAllTts()
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
                        // 모든 작업 중단
                        ttsController.stopAllTts()
                        buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
                        buttonStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
                        buttonStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
                        buttonStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
                    }
                }
                
                Log.d("ButtonActionHandler", "버튼 중지 처리 완료")
            } catch (e: Exception) {
                Log.e("ButtonActionHandler", "버튼 중지 처리 실패", e)
            }
        }
    }
    
    /**
     * 현재 QA 아이템 가져오기
     */
    fun getCurrentQaItem(): QaItem? {
        return qaDataRepository.getCurrentQaItem()
    }
    
    /**
     * 선택된 암기레벨 가져오기
     */
    fun getSelectedMemorizeLevel(): String {
        return "반복듣기" // 임시로 하드코딩
    }
} 