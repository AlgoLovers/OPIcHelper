package com.na982.opichelper.presentation.viewmodel

import android.util.Log
import com.na982.opichelper.domain.audio.RepeatListeningUiCallback
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase
import com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase
import com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase
import com.na982.opichelper.domain.usecase.GetLeveledAnswerUseCase
import com.na982.opichelper.domain.state.MemorizationProgressTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

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
 * MemorizationManager의 UI 상태를 나타내는 데이터 클래스
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


/**
 * 암기 테스트 관리 전담 클래스
 * 책임: 반복듣기, 영작테스트, 통암기 모드 관리
 */
@Singleton
class MemorizationManager @Inject constructor(
    private val executeRepeatListeningUseCase: StartRepeatListeningUseCase,
    private val executeEnglishWritingTestUseCase: StartEnglishWritingTestUseCase,
    private val executeFullMemorizationUseCase: StartFullMemorizationUseCase,
    private val qaDataRepository: QaDataRepository,
    private val ttsOrchestrator: TtsOrchestrator,
    private val getCurrentAnswerUseCase: GetLeveledAnswerUseCase,
    private val progressTracker: MemorizationProgressTracker
) {
    
    // UI 상태
    private val _uiState = MutableStateFlow(MemorizationUiState())
    val uiState: StateFlow<MemorizationUiState> = _uiState.asStateFlow()
    
    // 현재 실행 중인 작업
    private var currentUseCaseJob: Job? = null
    
    init {
        Log.d("MemorizationManager", "암기 테스트 매니저 초기화")
    }
    
    /**
     * 모드 시작
     */
    fun startMode(mode: CurrentMode) {
        Log.d("MemorizationManager", "모드 시작: $mode")
        
        _uiState.value = _uiState.value.copy(
            currentMode = mode,
            isRunning = true
        )
    }
    
    /**
     * 모드 중지
     */
    fun stopMode() {
        Log.d("MemorizationManager", "모드 중지")
        
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        
        _uiState.value = _uiState.value.copy(
            currentMode = CurrentMode.NONE,
            isRunning = false
        )
    }
    
    /**
     * 반복듣기 시작
     */
    fun startRepeatListening(category: String, scriptIndex: Int) {
        Log.d("MemorizationManager", "반복듣기 시작: category=$category, scriptIndex=$scriptIndex")
        
        startMode(CurrentMode.REPEAT_LISTENING)
        
        val uiCallback = object : RepeatListeningUiCallback {
            override fun onCardFlip(isKorean: Boolean) {
                _uiState.value = _uiState.value.copy(
                    isRepeatListeningCardFlipped = isKorean
                )
            }
            
            override fun onHighlight(index: Int?) {
                _uiState.value = _uiState.value.copy(
                    answerHighlightIndex = index
                )
            }
            
            override fun onKoreanHighlight(index: Int?) {
                _uiState.value = _uiState.value.copy(
                    answerKoHighlightIndex = index
                )
            }
            
            override fun onComplete() {
                Log.d("MemorizationManager", "반복듣기 완료")
                stopMode()
            }
        }
        
        // UseCase 실행을 위한 데이터 준비
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                val qaItem = qaDataRepository.getCurrentQaItem()
                if (qaItem != null) {
                    val data = RepeatListeningData(
                        category = category,
                        scriptIndex = scriptIndex,
                        koreanAnswer = getCurrentAnswerUseCase.getCurrentAnswerKo(qaItem),
                        englishAnswer = getCurrentAnswerUseCase.getCurrentAnswer(qaItem)
                    )
                    
                    executeRepeatListeningUseCase.execute(
                        data = data,
                        uiCallback = uiCallback
                    )
                }
            } catch (e: Exception) {
                Log.e("MemorizationManager", "반복듣기 시작 실패", e)
                stopMode()
            }
        }
    }
    
    /**
     * 영작테스트 시작
     */
    fun startEnglishWritingTest(category: String, scriptIndex: Int) {
        Log.d("MemorizationManager", "영작테스트 시작: category=$category, scriptIndex=$scriptIndex")
        
        startMode(CurrentMode.ENGLISH_WRITING)
        
        // UseCase 실행을 위한 데이터 준비
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                val qaItem = qaDataRepository.getCurrentQaItem()
                if (qaItem != null) {
                    val answerKo = getCurrentAnswerUseCase.getCurrentAnswerKo(qaItem)
                    val answerEn = getCurrentAnswerUseCase.getCurrentAnswer(qaItem)
                    
                    executeEnglishWritingTestUseCase.execute(
                        answerKo = answerKo,
                        answerEn = answerEn,
                        category = category,
                        scriptIndex = scriptIndex,
                        onCardFlip = { isKorean ->
                            _uiState.value = _uiState.value.copy(
                                isEnglishWritingTestCardFlipped = isKorean
                            )
                        },
                        onKoreanHighlight = { index ->
                            _uiState.value = _uiState.value.copy(
                                answerKoHighlightIndex = index
                            )
                        },
                        onRecordingHighlight = { index ->
                            _uiState.value = _uiState.value.copy(
                                recordingHighlightIndex = index
                            )
                        },
                        onRecordingStateChange = { isRecording ->
                            _uiState.value = _uiState.value.copy(
                                isEnglishWritingTestRecording = isRecording,
                                currentMode = if (isRecording) CurrentMode.ENGLISH_WRITING_RECORDING else CurrentMode.ENGLISH_WRITING
                            )
                        },
                        onMergedFileCreated = {
                            _uiState.value = _uiState.value.copy(
                                hasEnglishWritingTestRecording = true,
                                currentMode = CurrentMode.ENGLISH_WRITING_WITH_FILE
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("MemorizationManager", "영작테스트 시작 실패", e)
                stopMode()
            }
        }
    }
    
    /**
     * 통암기 시작
     */
    fun startFullMemorization(category: String, scriptIndex: Int) {
        Log.d("MemorizationManager", "통암기 시작: category=$category, scriptIndex=$scriptIndex")
        
        startMode(CurrentMode.FULL_MEMORIZATION)
        
        // UseCase 실행
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // 통암기 모드 진입 시 녹음 파일 상태 즉시 확인
                Log.d("MemorizationManager", "통암기 모드 진입 시 녹음 파일 상태 즉시 확인")
                checkFullMemorizationRecordingStatus()
                
                executeFullMemorizationUseCase.execute(
                    category = category,
                    scriptIndex = scriptIndex,
                    onRecordingStateChange = { isRecording ->
                        _uiState.value = _uiState.value.copy(
                            isFullMemorizationRecording = isRecording,
                            currentMode = if (isRecording) CurrentMode.FULL_MEMORIZATION_RECORDING else CurrentMode.FULL_MEMORIZATION
                        )
                    },
                    onPlayingStateChange = { isPlaying ->
                        _uiState.value = _uiState.value.copy(
                            isFullMemorizationPlaying = isPlaying,
                            currentMode = if (isPlaying) CurrentMode.FULL_MEMORIZATION_PLAYING else CurrentMode.FULL_MEMORIZATION
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("MemorizationManager", "통암기 시작 실패", e)
                stopMode()
            }
        }
    }
    
    /**
     * 현재 모드 중지
     */
    fun stopCurrentMode() {
        Log.d("MemorizationManager", "현재 모드 중지")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                when (_uiState.value.currentMode) {
                    CurrentMode.REPEAT_LISTENING -> {
                        executeRepeatListeningUseCase.stop()
                    }
                    CurrentMode.ENGLISH_WRITING,
                    CurrentMode.ENGLISH_WRITING_RECORDING,
                    CurrentMode.ENGLISH_WRITING_PLAYING,
                    CurrentMode.ENGLISH_WRITING_WITH_FILE -> {
                        // 영작테스트는 stop 메서드가 없으므로 무시
                    }
                    CurrentMode.FULL_MEMORIZATION,
                    CurrentMode.FULL_MEMORIZATION_RECORDING,
                    CurrentMode.FULL_MEMORIZATION_PLAYING,
                    CurrentMode.FULL_MEMORIZATION_WITH_FILE -> {
                        // 통암기는 stop 메서드가 없으므로 무시
                    }
                    else -> {
                        // 다른 모드는 무시
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationManager", "모드 중지 실패", e)
            } finally {
                stopMode()
            }
        }
    }
    
    /**
     * 통암기 녹음 중지
     */
    fun stopFullMemorizationRecording() {
        Log.d("MemorizationManager", "통암기 녹음 중지")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                executeFullMemorizationUseCase.stopRecording { isRecording ->
                    _uiState.value = _uiState.value.copy(
                        isFullMemorizationRecording = isRecording
                    )
                }
            } catch (e: Exception) {
                Log.e("MemorizationManager", "통암기 녹음 중지 실패", e)
            }
        }
    }
    
    /**
     * 통암기 녹음 재생
     */
    fun playFullMemorizationRecording() {
        Log.d("MemorizationManager", "통암기 녹음 재생")
        
        if (executeFullMemorizationUseCase.hasRecording()) {
            startMode(CurrentMode.FULL_MEMORIZATION_PLAYING)
            
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                try {
                    executeFullMemorizationUseCase.playRecording(
                        onPlayingStateChange = { isPlaying ->
                            _uiState.value = _uiState.value.copy(
                                isFullMemorizationPlaying = isPlaying
                            )
                            
                            if (!isPlaying) {
                                startMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
                            }
                        },
                        onHighlight = { index ->
                            _uiState.value = _uiState.value.copy(
                                recordingHighlightIndex = index
                            )
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MemorizationManager", "통암기 녹음 재생 실패", e)
                    stopMode()
                }
            }
        }
    }
    
    /**
     * 현재 상태 가져오기
     */
    fun getCurrentState(): MemorizationUiState {
        return _uiState.value
    }
    
    /**
     * 상태 초기화
     */
    fun resetState() {
        Log.d("MemorizationManager", "상태 초기화")
        
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        
        _uiState.value = MemorizationUiState()
    }
    
    /**
     * 통암기 녹음 파일 삭제
     */
    fun deleteFullMemorizationRecording() {
        Log.d("MemorizationManager", "통암기 녹음 파일 삭제")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                executeFullMemorizationUseCase.clearRecording()
                updateFullMemorizationRecordingStatus()
                Log.d("MemorizationManager", "통암기 녹음 파일 삭제 완료")
            } catch (e: Exception) {
                Log.e("MemorizationManager", "통암기 녹음 파일 삭제 실패", e)
            }
        }
    }
    
    /**
     * 통암기 녹음 파일 존재 여부 확인
     */
    suspend fun hasFullMemorizationRecording(): Boolean {
        return try {
            executeFullMemorizationUseCase.hasRecording()
        } catch (e: Exception) {
            Log.e("MemorizationManager", "통암기 녹음 파일 확인 실패", e)
            false
        }
    }
    
    /**
     * 통암기 녹음 상태 업데이트
     */
    fun updateFullMemorizationRecordingStatus() {
        Log.d("MemorizationManager", "통암기 녹음 상태 업데이트")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                val hasRecording = executeFullMemorizationUseCase.hasRecording()
                Log.d("MemorizationManager", "통암기 녹음 파일 상태 확인: hasRecording=$hasRecording")
                
                if (hasRecording) {
                    _uiState.value = _uiState.value.copy(
                        currentMode = CurrentMode.FULL_MEMORIZATION_WITH_FILE,
                        hasFullMemorizationRecording = true
                    )
                    Log.d("MemorizationManager", "통암기 녹음 파일 발견 - FULL_MEMORIZATION_WITH_FILE 모드로 변경")
                } else {
                    _uiState.value = _uiState.value.copy(
                        currentMode = CurrentMode.FULL_MEMORIZATION,
                        hasFullMemorizationRecording = false
                    )
                    Log.d("MemorizationManager", "통암기 녹음 파일 없음 - FULL_MEMORIZATION 모드로 변경")
                }
            } catch (e: Exception) {
                Log.e("MemorizationManager", "통암기 녹음 파일 상태 확인 실패", e)
            }
        }
    }
    
    /**
     * 통암기 모드 진입 시 즉시 녹음 파일 상태 확인 (동기)
     */
    private suspend fun checkFullMemorizationRecordingStatus() {
        try {
            val hasRecording = executeFullMemorizationUseCase.hasRecording()
            Log.d("MemorizationManager", "통암기 녹음 파일 상태 즉시 확인: hasRecording=$hasRecording")
            
            if (hasRecording) {
                _uiState.value = _uiState.value.copy(
                    currentMode = CurrentMode.FULL_MEMORIZATION_WITH_FILE,
                    hasFullMemorizationRecording = true
                )
                Log.d("MemorizationManager", "통암기 녹음 파일 발견 - 즉시 FULL_MEMORIZATION_WITH_FILE 모드로 변경")
            } else {
                _uiState.value = _uiState.value.copy(
                    currentMode = CurrentMode.FULL_MEMORIZATION,
                    hasFullMemorizationRecording = false
                )
                Log.d("MemorizationManager", "통암기 녹음 파일 없음 - 즉시 FULL_MEMORIZATION 모드로 변경")
            }
        } catch (e: Exception) {
            Log.e("MemorizationManager", "통암기 녹음 파일 상태 즉시 확인 실패", e)
        }
    }
} 