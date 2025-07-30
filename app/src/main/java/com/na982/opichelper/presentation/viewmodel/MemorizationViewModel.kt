package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.audio.RepeatListeningUiCallback
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.usecase.*
import com.na982.opichelper.domain.usecase.ExecuteEnglishWritingTestUseCase
import com.na982.opichelper.domain.usecase.ExecuteRepeatListeningUseCase
import com.na982.opichelper.domain.usecase.FullMemorizationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.delay

/**
 * 현재 실행 중인 모드를 나타내는 enum
 */
enum class CurrentMode {
    NONE,                           // 실행 중이 아님
    
    // 기본 재생 모드들
    QUESTION_PLAY,                  // 질문1회재생
    ANSWER_PLAY,                    // 답변1회재생
    
    // 암기 테스트 모드들
    REPEAT_LISTENING,              // 암기레벨중반복듣기
    
    // 영작테스트 모드들 (세부 상태 포함)
    ENGLISH_WRITING,               // 영작테스트 모드 (기본)
    ENGLISH_WRITING_RECORDING,     // 영작테스트 녹음 중
    ENGLISH_WRITING_PLAYING,       // 영작테스트 재생 중
    ENGLISH_WRITING_WITH_FILE,     // 영작테스트 (녹음 파일 존재)
    
    // 통암기 모드들 (세부 상태 포함)
    FULL_MEMORIZATION,             // 통암기 모드 (기본)
    FULL_MEMORIZATION_QUESTION_PLAYING, // 통암기 질문 재생 중
    FULL_MEMORIZATION_RECORDING,   // 통암기 녹음 중
    FULL_MEMORIZATION_PLAYING,     // 통암기 재생 중
    FULL_MEMORIZATION_WITH_FILE    // 통암기 (녹음 파일 존재)
}

/**
 * MemorizationViewModel의 UI 상태를 나타내는 데이터 클래스
 */
data class MemorizationUiState(
    val isRunning: Boolean = false,
    val currentMode: CurrentMode = CurrentMode.NONE,
    
    // 반복듣기 상태
    val isRepeatListeningCardFlipped: Boolean = false,
    val isRepeatListeningRunning: Boolean = false,
    val isRepeatListeningMode: Boolean = false,
    val repeatListeningCurrentRepeat: Int = 0, // 현재 반복 횟수
    val repeatListeningTotalRepeats: Int = 5, // 총 반복 횟수
    
    // 영작테스트 상태
    val isEnglishWritingTestCardFlipped: Boolean = false,
    val isEnglishWritingTestRunning: Boolean = false,
    val isEnglishWritingTestMode: Boolean = false,
    val isEnglishWritingTestRecording: Boolean = false,
    val isEnglishWritingTestPlaying: Boolean = false,
    val hasEnglishWritingTestRecording: Boolean = false,
    
    // 통암기 상태
    val isFullMemorizationMode: Boolean = false,
    val isFullMemorizationQuestionPlaying: Boolean = false, // 질문 재생 중 상태 추가
    val isFullMemorizationRecording: Boolean = false,
    val isFullMemorizationPlaying: Boolean = false,
    val hasFullMemorizationRecording: Boolean = false,
    val isFullMemorizationRecordingPlaying: Boolean = false,
    
    // 편의 변수들
    val isMemorizeTestRunning: Boolean = false,
    
    // 하이라이트 인덱스들
    val answerHighlightIndex: Int? = null,
    val answerKoHighlightIndex: Int? = null,
    val recordingHighlightIndex: Int? = null,
    
    // 이벤트 상태들
    val englishWritingTestCompleted: Boolean = false,
    val stopEnglishWritingTestMergedFilePlaying: Boolean = false
)

@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val ttsPlaybackController: TtsPlaybackController,
    private val qaDataManager: QaDataManager,
    private val executeRepeatListeningUseCase: ExecuteRepeatListeningUseCase,
    private val executeEnglishWritingTestUseCase: ExecuteEnglishWritingTestUseCase,
    private val executeFullMemorizationUseCase: FullMemorizationUseCase,
    private val progressTracker: MemorizeTestProgressTracker
) : ViewModel(), RepeatListeningUiCallback {
    // 상태 StateFlow들
    private val _memorizeLevels = MutableStateFlow(listOf("반복 듣기", "영작 테스트", "통암기"))
    val memorizeLevels: StateFlow<List<String>> = _memorizeLevels.asStateFlow()

    // === 핵심 상태 변수들 ===
    // 실행 상태 (실행 중인지 여부)
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // 현재 모드
    private val _currentMode = MutableStateFlow(CurrentMode.NONE)
    val currentMode: StateFlow<CurrentMode> = _currentMode.asStateFlow()

    // === 이벤트 상태 변수들 ===
    // 영작테스트 완료 시 병합 파일 생성 이벤트
    private val _englishWritingTestCompleted = MutableStateFlow(false)
    val englishWritingTestCompleted: StateFlow<Boolean> = _englishWritingTestCompleted.asStateFlow()

    // 영작테스트 녹음 파일 재생 중단 이벤트
    private val _stopEnglishWritingTestMergedFilePlaying = MutableStateFlow(false)
    val stopEnglishWritingTestMergedFilePlaying: StateFlow<Boolean> = _stopEnglishWritingTestMergedFilePlaying.asStateFlow()

    // === UI 상태 관리 ===
    private val _uiState = MutableStateFlow(MemorizationUiState())
    val uiState: StateFlow<MemorizationUiState> = _uiState.asStateFlow()

    // 하이라이트 인덱스는 UseCase의 StateFlow만 구독
    val fullMemorizationHighlightIndex: StateFlow<Int?> = executeFullMemorizationUseCase.highlightIndex

    private var currentUseCaseJob: Job? = null

    // UI 상태 업데이트 함수
    private fun updateUiState() {
        Log.d("MemorizationViewModel", "updateUiState 호출: currentMode=${_currentMode.value}, isRunning=${_isRunning.value}")
        val newState = MemorizationUiState(
            isRunning = _isRunning.value,
            currentMode = _currentMode.value,
            
            // 반복듣기 상태
            isRepeatListeningCardFlipped = _currentMode.value == CurrentMode.REPEAT_LISTENING && _isRunning.value,
            isRepeatListeningRunning = _currentMode.value == CurrentMode.REPEAT_LISTENING && _isRunning.value,
            isRepeatListeningMode = _currentMode.value == CurrentMode.REPEAT_LISTENING,
            repeatListeningCurrentRepeat = 0,
            repeatListeningTotalRepeats = 5,
            
            // 영작테스트 상태
            isEnglishWritingTestCardFlipped = _currentMode.value == CurrentMode.ENGLISH_WRITING && _isRunning.value,
            isEnglishWritingTestRunning = _currentMode.value == CurrentMode.ENGLISH_WRITING && _isRunning.value,
            isEnglishWritingTestMode = _currentMode.value in listOf(
                CurrentMode.ENGLISH_WRITING,
                CurrentMode.ENGLISH_WRITING_RECORDING,
                CurrentMode.ENGLISH_WRITING_PLAYING,
                CurrentMode.ENGLISH_WRITING_WITH_FILE
            ),
            isEnglishWritingTestRecording = _currentMode.value == CurrentMode.ENGLISH_WRITING_RECORDING,
            isEnglishWritingTestPlaying = _currentMode.value == CurrentMode.ENGLISH_WRITING_PLAYING,
            hasEnglishWritingTestRecording = _currentMode.value == CurrentMode.ENGLISH_WRITING_WITH_FILE,
            
            // 통암기 상태
            isFullMemorizationMode = _currentMode.value in listOf(
                CurrentMode.FULL_MEMORIZATION,
                CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING,
                CurrentMode.FULL_MEMORIZATION_RECORDING,
                CurrentMode.FULL_MEMORIZATION_PLAYING,
                CurrentMode.FULL_MEMORIZATION_WITH_FILE
            ),
            isFullMemorizationQuestionPlaying = _currentMode.value == CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING,
            isFullMemorizationRecording = _currentMode.value == CurrentMode.FULL_MEMORIZATION_RECORDING,
            isFullMemorizationPlaying = _currentMode.value == CurrentMode.FULL_MEMORIZATION_PLAYING,
            hasFullMemorizationRecording = _currentMode.value in listOf(
                CurrentMode.FULL_MEMORIZATION_WITH_FILE,
                CurrentMode.FULL_MEMORIZATION_PLAYING
            ),
            isFullMemorizationRecordingPlaying = _currentMode.value == CurrentMode.FULL_MEMORIZATION_PLAYING,
            
            // 편의 변수들
            isMemorizeTestRunning = _isRunning.value,
            
            // 하이라이트 인덱스들 (TtsPlaybackController에서 가져옴)
            answerHighlightIndex = ttsPlaybackController.answerHighlightIndex.value,
            answerKoHighlightIndex = ttsPlaybackController.answerKoHighlightIndex.value,
            recordingHighlightIndex = ttsPlaybackController.recordingHighlightIndex.value,
            
            // 이벤트 상태들
            englishWritingTestCompleted = _englishWritingTestCompleted.value,
            stopEnglishWritingTestMergedFilePlaying = _stopEnglishWritingTestMergedFilePlaying.value
        )
        
        Log.d("MemorizationViewModel", "updateUiState 완료: isFullMemorizationMode=${newState.isFullMemorizationMode}, hasFullMemorizationRecording=${newState.hasFullMemorizationRecording}")
        _uiState.value = newState
    }

    // 편의 함수들
    private fun startMode(mode: CurrentMode) {
        Log.d("MemorizationViewModel", "startMode 호출: $mode")
        _currentMode.value = mode
        _isRunning.value = true
        updateUiState()
        Log.d("MemorizationViewModel", "startMode 완료: currentMode=${_currentMode.value}, isRunning=${_isRunning.value}")
    }

    private fun stopMode() {
        _isRunning.value = false
        _currentMode.value = CurrentMode.NONE
        updateUiState()
    }

    private fun isModeRunning(mode: CurrentMode): Boolean {
        return _isRunning.value && _currentMode.value == mode
    }

    fun onMemorizeTestButtonClick(selectedLevel: String) {
        viewModelScope.launch {
            try {
                Log.d("MemorizationViewModel", "onMemorizeTestButtonClick 호출됨 - selectedLevel: '$selectedLevel'")
                
                when (selectedLevel) {
                    "반복 듣기" -> {
                        Log.d("MemorizationViewModel", "반복 듣기 모드 선택됨")
                        if (_uiState.value.isRepeatListeningRunning) {
                            Log.d("MemorizationViewModel", "반복 듣기 실행 중 - 스탑 실행")
                            stopRepeatListening()
                        } else {
                            Log.d("MemorizationViewModel", "반복 듣기 모드 선택됨")
                            startMode(CurrentMode.REPEAT_LISTENING)
                            ttsPlaybackController.stopTts()
                            ttsPlaybackController.clearHighlight()
                            startRepeatListening()
                        }
                    }
                    "영작 테스트" -> {
                        // 영작테스트가 이미 실행 중인지 확인
                        if (_uiState.value.isEnglishWritingTestRunning) {
                            Log.d("MemorizationViewModel", "영작 테스트 실행 중 - 스탑 실행")
                            stopEnglishWritingTest()
                        } else {
                            Log.d("MemorizationViewModel", "영작 테스트 모드 선택됨")
                            startMode(CurrentMode.ENGLISH_WRITING)
                            ttsPlaybackController.stopTts()
                            ttsPlaybackController.clearHighlight()
                            // 영작테스트 녹음 파일 재생 중단 이벤트 발생
                            _stopEnglishWritingTestMergedFilePlaying.value = true
                            startEnglishWritingTest()
                        }
                    }
                    "통암기" -> {
                        Log.d("MemorizationViewModel", "통암기 모드 선택됨 - 진입 시작")
                        Log.d("MemorizationViewModel", "통암기 모드 진입 시작")
                        
                        startMode(CurrentMode.FULL_MEMORIZATION)
                        startFullMemorizationMode()
                        
                        Log.d("MemorizationViewModel", "통암기 모드 진입 완료")
                    }
                    else -> {
                        Log.w("MemorizationViewModel", "알 수 없는 암기 레벨: '$selectedLevel'")
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "암기 테스트 시작 실패", e)
                stopMode()
            }
        }
    }

    fun startFullMemorizationMode() {
        viewModelScope.launch {
            try {
                Log.d("MemorizationViewModel", "startFullMemorizationMode 호출됨")
                currentUseCaseJob?.cancel()
                
                // 통암기 모드 진입 시 녹음 파일 상태 즉시 확인
                Log.d("MemorizationViewModel", "통암기 모드 진입 시 녹음 파일 상태 즉시 확인")
                checkFullMemorizationRecordingStatus()
                
                val category = qaDataManager.getCurrentCategory() ?: ""
                val scriptIndex = qaDataManager.getCurrentIndex()
                Log.d("MemorizationViewModel", "통암기 서비스 시작: category=$category, scriptIndex=$scriptIndex")
                
                executeFullMemorizationUseCase.startFullMemorization(
                    category = category,
                    scriptIndex = scriptIndex,
                    onRecordingStateChange = { isRecording ->
                        if (isRecording) {
                            startMode(CurrentMode.FULL_MEMORIZATION_RECORDING)
                        } else {
                            Log.d("MemorizationViewModel", "FULL_MEMORIZATION_RECORDING 모드로 전환")
                            // 질문 재생 완료 후 녹음 시작
                            startMode(CurrentMode.FULL_MEMORIZATION_RECORDING)
                        }
                    },
                    onPlayingStateChange = { isPlaying ->
                        Log.d("MemorizationViewModel", "onPlayingStateChange 콜백 호출: isPlaying=$isPlaying")
                        if (isPlaying) {
                            Log.d("MemorizationViewModel", "FULL_MEMORIZATION_QUESTION_PLAYING 모드로 전환")
                            startMode(CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING)
                        } else {
                            Log.d("MemorizationViewModel", "FULL_MEMORIZATION_RECORDING 모드로 전환")
                            // 질문 재생 완료 후 녹음 시작
                            startMode(CurrentMode.FULL_MEMORIZATION_RECORDING)
                        }
                    }
                )
                
                Log.d("MemorizationViewModel", "통암기 모드 시작")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 모드 시작 실패", e)
                stopMode()
            }
        }
    }

    fun stopFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                executeFullMemorizationUseCase.stopRecording()
                updateFullMemorizationRecordingStatus()
                Log.d("MemorizationViewModel", "통암기 녹음 종료")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 종료 실패", e)
            }
        }
    }

    fun playFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                if (executeFullMemorizationUseCase.hasRecording()) {
                    startMode(CurrentMode.FULL_MEMORIZATION_PLAYING)
                    Log.d("MemorizationViewModel", "통암기 녹음 재생 시작")
                    executeFullMemorizationUseCase.playRecordingWithHighlight { isPlaying ->
                        Log.d("MemorizationViewModel", "통암기 녹음 재생 상태 변경: isPlaying=$isPlaying")
                        if (!isPlaying) {
                            startMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
                            Log.d("MemorizationViewModel", "통암기 녹음 재생 완료 - 모드 복원")
                        }
                    }
                    Log.d("MemorizationViewModel", "통암기 녹음 재생 완료")
                } else {
                    Log.d("MemorizationViewModel", "통암기 녹음 파일이 존재하지 않음")
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 재생 실패", e)
                startMode(CurrentMode.FULL_MEMORIZATION)
            }
        }
    }

    fun stopFullMemorizationPlaying() {
        viewModelScope.launch {
            try {
                // 재생 중지 기능은 UseCase에서 제거되었으므로 단순히 모드만 변경
                startMode(CurrentMode.FULL_MEMORIZATION)
                updateFullMemorizationRecordingStatus()
                Log.d("MemorizationViewModel", "통암기 재생 중지")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 재생 중지 실패", e)
                startMode(CurrentMode.FULL_MEMORIZATION) // 재생 중지 실패 시 통암기 모드로 복귀
            }
        }
    }

    suspend fun hasFullMemorizationRecording(): Boolean {
        return _uiState.value.hasFullMemorizationRecording
    }

    fun updateFullMemorizationRecordingStatus() {
        viewModelScope.launch {
            try {
                val hasRecording = executeFullMemorizationUseCase.hasRecording()
                Log.d("MemorizationViewModel", "통암기 녹음 파일 상태 확인: hasRecording=$hasRecording")
                
                if (hasRecording) {
                    _currentMode.value = CurrentMode.FULL_MEMORIZATION_WITH_FILE
                    Log.d("MemorizationViewModel", "통암기 녹음 파일 발견 - FULL_MEMORIZATION_WITH_FILE 모드로 변경")
                } else {
                    _currentMode.value = CurrentMode.FULL_MEMORIZATION
                    Log.d("MemorizationViewModel", "통암기 녹음 파일 없음 - FULL_MEMORIZATION 모드로 변경")
                }
                updateUiState()
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 파일 상태 확인 실패", e)
            }
        }
    }
    
    /**
     * 통암기 모드 진입 시 즉시 녹음 파일 상태 확인 (동기)
     */
    private suspend fun checkFullMemorizationRecordingStatus() {
        try {
            val hasRecording = executeFullMemorizationUseCase.hasRecording()
            Log.d("MemorizationViewModel", "통암기 녹음 파일 상태 즉시 확인: hasRecording=$hasRecording")
            
            if (hasRecording) {
                _currentMode.value = CurrentMode.FULL_MEMORIZATION_WITH_FILE
                Log.d("MemorizationViewModel", "통암기 녹음 파일 발견 - 즉시 FULL_MEMORIZATION_WITH_FILE 모드로 변경")
            } else {
                _currentMode.value = CurrentMode.FULL_MEMORIZATION
                Log.d("MemorizationViewModel", "통암기 녹음 파일 없음 - 즉시 FULL_MEMORIZATION 모드로 변경")
            }
            updateUiState()
        } catch (e: Exception) {
            Log.e("MemorizationViewModel", "통암기 녹음 파일 상태 즉시 확인 실패", e)
        }
    }
    
    /**
     * 앱 재시작 시 상태 초기화
     */
    fun resetStateOnAppRestart() {
        Log.d("MemorizationViewModel", "앱 재시작 시 상태 초기화")
        
        // 모든 상태를 초기값으로 리셋
        _currentMode.value = CurrentMode.NONE
        _isRunning.value = false
        
        // UI 상태 업데이트
        updateUiState()
        
        Log.d("MemorizationViewModel", "상태 초기화 완료: currentMode=${_currentMode.value}, isRunning=${_isRunning.value}")
    }

    fun deleteFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                executeFullMemorizationUseCase.clearRecording()
                updateFullMemorizationRecordingStatus()
                Log.d("MemorizationViewModel", "통암기 녹음 파일 삭제")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 파일 삭제 실패", e)
            }
        }
    }

    private fun startRepeatListening() {
        viewModelScope.launch {
            try {
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    Log.d("MemorizationViewModel", "반복 듣기 Service 실행")
                    currentUseCaseJob = launch {
                        val repeatListeningData = RepeatListeningData(
                            category = currentItem.category,
                            scriptIndex = qaDataManager.getCurrentIndex(),
                            koreanAnswer = qaDataManager.getCurrentAnswerKo(currentItem),
                            englishAnswer = qaDataManager.getCurrentAnswer(currentItem)
                        )
                        executeRepeatListeningUseCase.execute(
                            data = repeatListeningData,
                            uiCallback = this@MemorizationViewModel
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "반복 듣기 시작 실패", e)
                stopMode()
            }
        }
    }

    private fun startEnglishWritingTest() {
        val currentItem = qaDataManager.currentQaItem.value
        if (currentItem != null) {
            val scriptIndex = qaDataManager.getCurrentIndex()
            
            currentUseCaseJob = viewModelScope.launch {
                try {
                    executeEnglishWritingTestUseCase.execute(
                        answerKo = qaDataManager.getCurrentAnswerKo(currentItem),
                        answerEn = qaDataManager.getCurrentAnswer(currentItem),
                        category = currentItem.category,
                        scriptIndex = scriptIndex,
                        onCardFlip = { isKorean ->
                            // 영작테스트 카드 뒤집기 상태 업데이트
                            _uiState.value = _uiState.value.copy(
                                isEnglishWritingTestCardFlipped = isKorean
                            )
                            Log.d("MemorizationViewModel", "영작 테스트: 카드 뒤집기 - ${if (isKorean) "한글" else "영문"}")
                        },
                        onKoreanHighlight = { index ->
                            if (index != null) {
                                ttsPlaybackController.setAnswerKoHighlightIndex(index)
                            } else {
                                ttsPlaybackController.clearHighlight()
                            }
                        },
                        onRecordingHighlight = { index ->
                            if (index != null) {
                                ttsPlaybackController.setRecordingHighlightIndex(index)
                            } else {
                                ttsPlaybackController.clearHighlight()
                            }
                        },
                        onRecordingStateChange = { isRecording ->
                            // 영작테스트 녹음 상태는 currentMode로 관리
                            if (!isRecording) {
                                viewModelScope.launch {
                                    updateFullMemorizationRecordingStatus()
                                }
                            }
                        },
                        onMergedFileCreated = {
                            viewModelScope.launch {
                                _englishWritingTestCompleted.value = true
                                Log.d("MemorizationViewModel", "영작테스트 병합 파일 생성 완료 - 이벤트 발생")
                                
                                // 영작테스트 모드 종료
                                stopMode()
                                _uiState.value = _uiState.value.copy(
                                    isEnglishWritingTestRunning = false,
                                    isEnglishWritingTestMode = false
                                )
                                
                                // 녹음 시간 데이터 확인
                                // mainViewModel.checkRecordingTimesAfterEnglishWritingTest() // Removed MainViewModel dependency
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MemorizationViewModel", "영작 테스트 실행 중 오류", e)
                } finally {
                    stopMode()
                    // 카드 뒤집기 상태는 currentMode로 관리
                }
            }
        }
    }

    fun stopMemorization() {
        Log.d("MemorizationViewModel", "암기 테스트 전체 중단 시작")
        
        // 현재 실행 중인 모드에 따라 적절한 중단 함수 호출
        when {
            _uiState.value.isRepeatListeningRunning -> {
                Log.d("MemorizationViewModel", "반복듣기 모드 중단")
                stopRepeatListening()
            }
            _uiState.value.isEnglishWritingTestRunning -> {
                Log.d("MemorizationViewModel", "영작테스트 모드 중단")
                stopEnglishWritingTest()
            }
            _uiState.value.isFullMemorizationMode -> {
                Log.d("MemorizationViewModel", "통암기 모드 중단")
                // 통암기는 진행상황 저장하지 않음
                currentUseCaseJob?.cancel()
                currentUseCaseJob = null
                stopMode()
                viewModelScope.launch {
                    ttsPlaybackController.stopTts()
                    ttsPlaybackController.clearHighlight()
                }
            }
            else -> {
                Log.d("MemorizationViewModel", "실행 중인 암기 모드 없음")
                currentUseCaseJob?.cancel()
                currentUseCaseJob = null
                stopMode()
                viewModelScope.launch {
                    ttsPlaybackController.stopTts()
                    ttsPlaybackController.clearHighlight()
                }
            }
        }
        
        Log.d("MemorizationViewModel", "암기 테스트 전체 중단 완료")
    }

    fun stopRepeatListening() {
        Log.d("MemorizationViewModel", "반복듣기 중단 시작")
        
        // 1. 현재 실행 중인 작업 중단
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        stopMode()
        
        // 2. 카드 상태 초기화 (영문으로 복원)
        _uiState.value = _uiState.value.copy(
            isRepeatListeningCardFlipped = false
        )
        
        // 3. TTS 완전 중지 및 하이라이트 정리
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            ttsPlaybackController.clearHighlight()
        }
        
        // 3. 반복듣기 진행상황 저장
        viewModelScope.launch {
            try {
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    val currentProgress = progressTracker.getScriptProgress(currentItem.category, qaDataManager.getCurrentIndex(), "반복 듣기")
                    if (currentProgress != null) {
                        progressTracker.persistChangedProgress()
                        Log.d("MemorizationViewModel", "반복듣기 중단 시 진행상황 저장: ${currentProgress.currentSentenceIndex}")
                    }
                }
                Log.d("MemorizationViewModel", "반복듣기 중단 완료")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "반복듣기 진행상황 저장 실패", e)
            }
        }
    }

    fun resetEnglishWritingTestCompleted() {
        _englishWritingTestCompleted.value = false
    }

    fun resetStopEnglishWritingTestMergedFilePlaying() {
        _stopEnglishWritingTestMergedFilePlaying.value = false
    }

    fun stopEnglishWritingTest() {
        Log.d("MemorizationViewModel", "영작테스트 중단 시작")
        
        // 1. 현재 실행 중인 영작테스트 작업 중단
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        stopMode()
        
        // 2. 카드 상태 초기화 (영문으로 복원)
        _uiState.value = _uiState.value.copy(
            isEnglishWritingTestCardFlipped = false
        )
        
        // 3. TTS 및 하이라이트 정리
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            ttsPlaybackController.clearHighlight()
        }
        
        // 3. 영작테스트 진행상황 저장
        viewModelScope.launch {
            try {
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    val currentProgress = progressTracker.getScriptProgress(currentItem.category, qaDataManager.getCurrentIndex(), "영작 테스트")
                    if (currentProgress != null) {
                        progressTracker.persistChangedProgress()
                        Log.d("MemorizationViewModel", "영작테스트 중단 시 진행상황 저장: ${currentProgress.currentSentenceIndex}")
                    }
                }
                Log.d("MemorizationViewModel", "영작테스트 중단 완료")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "영작테스트 진행상황 저장 실패", e)
            }
        }
    }

    /**
     * 암기레벨 변경 시 상태 초기화 및 새로운 암기레벨 진행상황 확인
     */
    fun onMemorizeLevelChanged() {
        viewModelScope.launch {
            try {
                Log.d("MemorizationViewModel", "암기레벨 변경 감지 - 상태 초기화")
                
                // 현재 스크립트의 진행상황 저장
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    // 현재 활성화된 암기레벨 확인
                    val currentLevel = when {
                        _uiState.value.isRepeatListeningMode -> "반복 듣기"
                        _uiState.value.isEnglishWritingTestMode -> "영작 테스트"
                        _uiState.value.isFullMemorizationMode -> "통암기"
                        else -> "반복 듣기" // 기본값
                    }
                    
                    // 현재 진행상황 저장
                    val currentProgress = progressTracker.getScriptProgress(
                        currentItem.category, 
                        qaDataManager.getCurrentIndex(), 
                        currentLevel
                    )
                    
                    if (currentProgress != null) {
                        progressTracker.persistChangedProgress()
                        Log.d("MemorizationViewModel", "암기레벨 변경 시 진행상황 저장: $currentLevel - 문장 ${currentProgress.currentSentenceIndex}/${currentProgress.totalSentences}")
                    }
                }
                
                // 현재 실행 중인 작업 중단
                currentUseCaseJob?.cancel()
                currentUseCaseJob = null
                
                // 기본 상태 초기화 (통암기 모드는 유지)
                stopMode()
                
                _englishWritingTestCompleted.value = false
                _stopEnglishWritingTestMergedFilePlaying.value = false
                
                // TTS 중단
                ttsPlaybackController.stopTts()
                ttsPlaybackController.clearHighlight()
                
                // 새로운 암기레벨의 진행상황 확인 및 로드
                if (currentItem != null) {
                    // 현재 활성화된 암기레벨 확인
                    val selectedLevel = when {
                        _uiState.value.isRepeatListeningMode -> "반복 듣기"
                        _uiState.value.isEnglishWritingTestMode -> "영작 테스트"
                        _uiState.value.isFullMemorizationMode -> "통암기"
                        else -> "반복 듣기" // 기본값
                    }
                    
                    val currentProgress = progressTracker.getScriptProgress(
                        currentItem.category, 
                        qaDataManager.getCurrentIndex(), 
                        selectedLevel
                    )
                    
                    if (currentProgress != null) {
                        Log.d("MemorizationViewModel", "새로운 암기레벨 진행상황 발견: $selectedLevel - 문장 ${currentProgress.currentSentenceIndex}/${currentProgress.totalSentences}")
                    } else {
                        Log.d("MemorizationViewModel", "새로운 암기레벨 진행상황 없음: $selectedLevel")
                    }
                }
                
                Log.d("MemorizationViewModel", "암기레벨 변경으로 인한 상태 초기화 완료")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "암기레벨 변경 처리 실패", e)
            }
        }
    }

    // 스크립트 변경 시 통암기 녹음 파일 존재 여부 확인
    init {
        viewModelScope.launch {
            qaDataManager.currentQaItem.collect { currentItem ->
                if (currentItem != null) {
                    Log.d("MemorizationViewModel", "스크립트 변경 감지 - 통암기 녹음 파일 상태 확인")
                    updateFullMemorizationRecordingStatus()
                }
            }
        }
    }

    // === RepeatListeningUiCallback 인터페이스 구현 ===
    override fun onCardFlip(isKorean: Boolean) {
        // 반복듣기 카드 뒤집기 상태 업데이트
        _uiState.value = _uiState.value.copy(
            isRepeatListeningCardFlipped = isKorean
        )
        Log.d("MemorizationViewModel", "반복 듣기: 카드 뒤집기 - ${if (isKorean) "한글" else "영문"}")
    }

    override fun onHighlight(index: Int?) {
        if (index != null) {
            ttsPlaybackController.setAnswerHighlightIndex(index)
            Log.d("MemorizationViewModel", "반복 듣기: 영문 하이라이트 설정: $index")
        } else {
            ttsPlaybackController.clearHighlight()
            Log.d("MemorizationViewModel", "반복 듣기: 영문 하이라이트 제거")
        }
    }

    override fun onKoreanHighlight(index: Int?) {
        if (index != null) {
            ttsPlaybackController.setAnswerKoHighlightIndex(index)
            Log.d("MemorizationViewModel", "반복 듣기: 한글 하이라이트 설정: $index")
        } else {
            ttsPlaybackController.clearHighlight()
            Log.d("MemorizationViewModel", "반복 듣기: 한글 하이라이트 제거")
        }
    }

    override fun onComplete() {
        // 반복듣기 완료 시 상태 초기화
        stopMode()
        Log.d("MemorizationViewModel", "반복 듣기 완료 - 상태 초기화")
    }
} 