package com.na982.opichelper.domain.manager

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
import dagger.hilt.android.scopes.ViewModelScoped

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
    val answerHighlightIndex: Int = -1,
    val answerKoHighlightIndex: Int = -1,
    val recordingHighlightIndex: Int = -1,
    
    // 이벤트 상태들
    val englishWritingTestCompleted: Boolean = false,
    val stopEnglishWritingTestMergedFilePlaying: Boolean = false
)


/**
 * 암기 테스트 관리 전담 클래스
 * 책임: 반복듣기, 영작테스트, 통암기 모드 관리
 */
@ViewModelScoped
class MemorizationManager @Inject constructor(
    private val executeRepeatListeningUseCase: StartRepeatListeningUseCase,
    private val executeEnglishWritingTestUseCase: StartEnglishWritingTestUseCase,
    private val executeFullMemorizationUseCase: StartFullMemorizationUseCase,
    private val getCurrentAnswerUseCase: GetLeveledAnswerUseCase,
    private val progressTracker: com.na982.opichelper.domain.state.MemorizationProgressTracker
) : IMemorizationManager {
    
    // UI 상태
    private val _uiState = MutableStateFlow(MemorizationUiState())
    override val uiState: StateFlow<MemorizationUiState> = _uiState.asStateFlow()
    
    // 현재 실행 중인 작업
    private var currentJob: Job? = null
    
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
        
        currentJob?.cancel()
        currentJob = null
        
        _uiState.value = _uiState.value.copy(
            currentMode = CurrentMode.NONE,
            isRunning = false
        )
    }
    
    /**
     * 반복듣기 시작
     */
    override fun startRepeatListening(category: String, scriptIndex: Int) {
        Log.d("MemorizationManager", "반복듣기 시작: $category, $scriptIndex")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // 기존 작업 중단
                stopCurrentMode()
                
                // UI 상태 업데이트
                _uiState.value = _uiState.value.copy(
                    isRunning = true,
                    currentMode = CurrentMode.REPEAT_LISTENING,
                    isRepeatListeningMode = true,
                    isRepeatListeningRunning = true,
                    repeatListeningCurrentRepeat = 0
                )
                
                // UseCase 실행은 별도로 구현 필요
                Log.d("MemorizationManager", "반복듣기 시작 완료")
                
            } catch (e: Exception) {
                Log.e("MemorizationManager", "반복듣기 시작 실패", e)
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    currentMode = CurrentMode.NONE,
                    isRepeatListeningMode = false,
                    isRepeatListeningRunning = false
                )
            }
        }
    }
    
    /**
     * 영작테스트 시작
     */
    override fun startEnglishWritingTest(category: String, scriptIndex: Int) {
        Log.d("MemorizationManager", "영작테스트 시작: $category, $scriptIndex")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // 기존 작업 중단
                stopCurrentMode()
                
                // UI 상태 업데이트
                _uiState.value = _uiState.value.copy(
                    isRunning = true,
                    currentMode = CurrentMode.ENGLISH_WRITING,
                    isEnglishWritingTestMode = true,
                    isEnglishWritingTestRunning = true
                )
                
                // UseCase 실행은 별도로 구현 필요
                Log.d("MemorizationManager", "영작테스트 시작 완료")
                
            } catch (e: Exception) {
                Log.e("MemorizationManager", "영작테스트 시작 실패", e)
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    currentMode = CurrentMode.NONE,
                    isEnglishWritingTestMode = false,
                    isEnglishWritingTestRunning = false
                )
            }
        }
    }
    
    /**
     * 통암기 시작
     */
    override fun startFullMemorization(category: String, scriptIndex: Int) {
        Log.d("MemorizationManager", "통암기 시작: $category, $scriptIndex")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // 기존 작업 중단
                stopCurrentMode()
                
                // UI 상태 업데이트
                _uiState.value = _uiState.value.copy(
                    isRunning = true,
                    currentMode = CurrentMode.FULL_MEMORIZATION,
                    isFullMemorizationMode = true
                )
                
                // UseCase 실행은 별도로 구현 필요
                Log.d("MemorizationManager", "통암기 시작 완료")
                
            } catch (e: Exception) {
                Log.e("MemorizationManager", "통암기 시작 실패", e)
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    currentMode = CurrentMode.NONE,
                    isFullMemorizationMode = false
                )
            }
        }
    }
    
    /**
     * 현재 모드 중지
     */
    override fun stopCurrentMode() {
        Log.d("MemorizationManager", "현재 모드 중지")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // 현재 작업 중단
                currentJob?.cancel()
                currentJob = null
                
                // UI 상태 초기화
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    currentMode = CurrentMode.NONE,
                    isRepeatListeningMode = false,
                    isRepeatListeningRunning = false,
                    isEnglishWritingTestMode = false,
                    isEnglishWritingTestRunning = false,
                    isFullMemorizationMode = false
                )
                
                Log.d("MemorizationManager", "현재 모드 중지 완료")
                
            } catch (e: Exception) {
                Log.e("MemorizationManager", "모드 중지 실패", e)
            }
        }
    }
    
    /**
     * 영작테스트 완료 처리
     */
    override fun onEnglishWritingTestCompleted() {
        Log.d("MemorizationManager", "영작테스트 완료 처리")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // UI 상태 업데이트 - 영작테스트 완료
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    currentMode = CurrentMode.NONE,
                    isEnglishWritingTestMode = false,
                    isEnglishWritingTestRunning = false,
                    englishWritingTestCompleted = true
                )
                
                Log.d("MemorizationManager", "영작테스트 완료 상태 업데이트 완료")
                
            } catch (e: Exception) {
                Log.e("MemorizationManager", "영작테스트 완료 처리 실패", e)
            }
        }
    }
    
    /**
     * 통암기 녹음 중지
     */
    override fun stopFullMemorizationRecording() {
        Log.d("MemorizationManager", "통암기 녹음 중지")
        // 구현은 UseCase에서 처리
    }
    
    /**
     * 통암기 녹음 재생
     */
    override fun playFullMemorizationRecording() {
        Log.d("MemorizationManager", "통암기 녹음 재생")
        // 구현은 UseCase에서 처리
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
    override fun resetState() {
        Log.d("MemorizationManager", "상태 초기화")
        
        currentJob?.cancel()
        currentJob = null
        
        _uiState.value = MemorizationUiState()
    }
    
    /**
     * 통암기 녹음 파일 삭제
     */
    override fun deleteFullMemorizationRecording() {
        Log.d("MemorizationManager", "통암기 녹음 파일 삭제")
        // 구현은 UseCase에서 처리
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

    /**
     * 에러 상태 초기화
     */
    override fun clearError() {
        Log.d("MemorizationManager", "에러 상태 초기화")
        // 에러 상태는 UI State에서 관리되므로 별도 처리 불필요
    }
} 