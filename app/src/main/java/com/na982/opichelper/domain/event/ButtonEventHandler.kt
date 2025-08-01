package com.na982.opichelper.domain.event


import android.util.Log
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.audio.AudioFileManager
import com.na982.opichelper.domain.state.AppStateManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 버튼 이벤트 핸들러
 * 책임: 버튼 클릭 이벤트 처리, UseCase 호출, 상태 관리
 */
@Singleton
class ButtonEventHandler @Inject constructor(
    private val ttsController: TtsController,
    private val repeatListeningUseCase: com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase,
    private val executeEnglishWritingTestUseCase: com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase,
    private val executeFullMemorizationUseCase: com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase,
    private val appStateManager: AppStateManager,
    private val recordingAudioPlayer: RecordingAudioPlayer,
    private val audioFileManager: AudioFileManager
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

        // 2. 버튼 상태를 Playing으로 변경
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // 3. TTS 재생 (하이라이트 포함)
        ttsController.playQuestion(event.question)

        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)

        return ButtonEventResult.Success
    }
    
    private suspend fun handleAnswerPlayClick(event: ButtonEvent.AnswerPlayClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "답변 재생 이벤트 처리")
        
        // 1. 다른 작업 중단 (다른 버튼이 실행 중이면 중단)
        stopOtherOperations()

        // 2. 버튼 상태를 Playing으로 변경
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Playing)

        // 3. TTS 재생 (하이라이트 포함)
        ttsController.playAnswer(event.answer)

        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
        
        return ButtonEventResult.Success
    }
    
    private suspend fun handleMemorizeTestClick(event: ButtonEvent.MemorizeTestClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "암기 테스트 이벤트 처리 시작 - 레벨: ${event.memorizeLevel}")
        
        // 1. 버튼 상태를 Loading으로 변경
        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Loading)
        
        // 2. 암기 레벨에 따른 처리
        when (event.memorizeLevel) {
            com.na982.opichelper.domain.entity.MemorizeLevel.REPEAT_LISTENING -> {
                Log.d("ButtonEventHandler", "반복 듣기 모드 시작")
                // 3. 버튼 상태를 Playing으로 변경
                appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Playing)
                val repeatListeningData = com.na982.opichelper.domain.entity.RepeatListeningData(
                    category = event.category,
                    scriptIndex = event.scriptIndex,
                    koreanAnswer = event.answerKo,
                    englishAnswer = event.answerEn
                )
                repeatListeningUseCase.execute(
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
                                questionHighlightIndex = -1,
                                answerHighlightIndex = index ?: -1,
                                answerKoHighlightIndex = -1
                            )
                        }
                        override fun onKoreanHighlight(index: Int?) {
                            Log.d("ButtonEventHandler", "반복듣기 한글 하이라이트: $index")
                            // 한글 하이라이트 상태 업데이트
                            appStateManager.updateHighlightState(
                                questionHighlightIndex = -1,
                                answerHighlightIndex = -1,
                                answerKoHighlightIndex = index ?: -1
                            )
                        }
                        override fun onComplete() {
                            Log.d("ButtonEventHandler", "반복듣기 완료")
                            // 완료 시 버튼 상태를 Idle로 변경
                            appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
                            // 카드 상태 초기화
                            appStateManager.updateCardState(
                                isAnswerCardFlipped = false
                            )
                        }
                    }
                )
            }
            com.na982.opichelper.domain.entity.MemorizeLevel.ENGLISH_WRITING -> {
                Log.d("ButtonEventHandler", "영작 테스트 모드 시작 - 카테고리: ${event.category}, 스크립트: ${event.scriptIndex}")
                Log.d("ButtonEventHandler", "영작 테스트 데이터 - 한글: ${event.answerKo.take(50)}..., 영문: ${event.answerEn.take(50)}...")
                appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Playing)
                executeEnglishWritingTestUseCase.execute(
                    answerKo = event.answerKo,
                    answerEn = event.answerEn,
                    category = event.category,
                    scriptIndex = event.scriptIndex,
                    onCardFlip = { isKorean ->
                        Log.d("ButtonEventHandler", "영작테스트 카드 뒤집기: ${if (isKorean) "한글" else "영문"}")
                        // 카드 뒤집기 상태 업데이트
                        appStateManager.updateCardState(
                            isAnswerCardFlipped = isKorean
                        )
                    },
                    onKoreanHighlight = { index ->
                        Log.d("ButtonEventHandler", "영작테스트 한글 하이라이트: $index")
                        // 한글 하이라이트 상태 업데이트
                        appStateManager.updateHighlightState(
                            questionHighlightIndex = -1,
                            answerHighlightIndex = -1,
                            answerKoHighlightIndex = index ?: -1
                        )
                    },
                    onRecordingHighlight = { index ->
                        Log.d("ButtonEventHandler", "영작테스트 녹음 하이라이트: $index")
                        // 녹음 하이라이트 상태 업데이트
                        appStateManager.updateHighlightState(
                            questionHighlightIndex = -1,
                            answerHighlightIndex = -1,
                            answerKoHighlightIndex = -1,
                            recordingHighlightIndex = index ?: -1
                        )
                    },
                    onRecordingStateChange = { isRecording ->
                        Log.d("ButtonEventHandler", "영작테스트 녹음 상태 변경: $isRecording")
                        // 녹음 상태 업데이트
                        appStateManager.updateRecordingState(isRecording)
                    },
                    onMergedFileCreated = {
                        Log.d("ButtonEventHandler", "영작테스트 병합 파일 생성 완료")
                        // 병합 파일 생성 완료 처리
                        appStateManager.updateMergedFileCreated(true)
                        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
                    }
                )
            }
            com.na982.opichelper.domain.entity.MemorizeLevel.FULL_MEMORIZATION -> {
                Log.d("ButtonEventHandler", "통암기 모드 시작")
                executeFullMemorizationUseCase.execute(
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

            
        Log.d("ButtonEventHandler", "암기 테스트 이벤트 처리 완료")
        return ButtonEventResult.Success
    }
    
    private suspend fun handleRecordingPlayClick(event: ButtonEvent.RecordingPlayClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "녹음 재생 이벤트 처리")
        
        // 1. 버튼 상태를 Loading으로 변경
        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Loading)
        
        // 2. 녹음 재생 처리
        try {
            // 현재 상태에서 카테고리와 스크립트 인덱스 가져오기
            val currentCategory = appStateManager.currentCategory
            val currentScriptIndex = appStateManager.currentIndex

            Log.d("ButtonEventHandler", "현재 상태 - 카테고리: $currentCategory, 스크립트: $currentScriptIndex")

            // null 체크
            if (currentCategory == null) {
                Log.e("ButtonEventHandler", "현재 카테고리가 null입니다.")
                appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
                return ButtonEventResult.Success
            }

            // AudioFileManager를 통해 영작테스트 병합 파일 가져오기
            val mergedFile = audioFileManager.getEnglishWritingTestMergedFile(currentCategory, currentScriptIndex)
            
            if (mergedFile != null && mergedFile.exists()) {
                Log.d("ButtonEventHandler", "영작테스트 병합 파일 발견: ${mergedFile.absolutePath}")
                
                recordingAudioPlayer.playRecording(
                    filePath = mergedFile.absolutePath,
                    onHighlight = { index ->
                        Log.d("ButtonEventHandler", "녹음 재생 하이라이트: $index")
                        // 영문 하이라이트 상태 업데이트
                        appStateManager.updateHighlightState(
                            questionHighlightIndex = -1,
                            answerHighlightIndex = index ?: -1,
                            answerKoHighlightIndex = -1,
                            recordingHighlightIndex = -1
                        )
                    },
                    onCompletion = {
                        Log.d("ButtonEventHandler", "병합된 녹음 재생 완료")
                        // 재생 완료 시 버튼 상태를 Idle로 변경
                        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
                    }
                )
                
                // 3. 버튼 상태를 Playing으로 변경
                appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Playing)
                
            } else {
                Log.e("ButtonEventHandler", "영작테스트 병합 파일을 찾을 수 없음: category=$currentCategory, scriptIndex=$currentScriptIndex")
                appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
            }
            
        } catch (e: Exception) {
            Log.e("ButtonEventHandler", "녹음 재생 실패", e)
            // 오류 시 버튼 상태를 Idle로 변경
            appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
        }
        
        return ButtonEventResult.Success
    }
    
    private suspend fun handleStopClick(event: ButtonEvent.StopClick): ButtonEventResult {
        Log.d("ButtonEventHandler", "중지 이벤트 처리")
        
        // 1. TTS 중지 (하이라이트 초기화 포함)
        ttsController.stopTts()
        
        // 2. 반복듣기 중지 (MemorizeTest 버튼인 경우)
        if (event.buttonFunction == ButtonFunction.MemorizeTest) {
            repeatListeningUseCase.stop()
            
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
        repeatListeningUseCase.stop()
        
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