package com.na982.opichelper.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.QuestionCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.data.audio.AudioRecorderImpl
import com.na982.opichelper.domain.audio.AudioRecorder
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer

import com.na982.opichelper.domain.usecase.RepeatListeningService


import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.data.repository.AudioFileManagerImpl
import kotlinx.coroutines.delay
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.audio.TtsPlaybackController

import com.na982.opichelper.domain.repository.AppExitState
import kotlinx.coroutines.Job
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.data.repository.UserPreferencesRepository
import com.na982.opichelper.domain.entity.UserLevel
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/**
 * 앱 전체 상태를 관리하는 통합 상태 클래스
 */
data class AppState(
    // 기본 UI 상태
    val currentQaItem: QaItem? = null,
    val currentCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList(),
    
    // 암기 관련 상태
    val memorizeLevels: List<String> = listOf("반복 듣기", "영작 테스트", "통암기"),
    val selectedMemorizeLevel: String = "",
    
    // 영작테스트 모드 상태
    val hasEnglishWritingTestMergedFile: Boolean = false,
    val isEnglishWritingTestMergedFilePlaying: Boolean = false,
    val englishWritingTestMergedFileHighlightIndex: Int? = null,
    
    // TTS 재생 상태
    val isPlaying: Boolean = false,
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    val questionHighlightIndex: Int? = null,
    val answerHighlightIndex: Int? = null,
    val answerKoHighlightIndex: Int? = null,
    val recordingHighlightIndex: Int? = null,
    
    // 카드 상태
    val isAnswerCardFlipped: Boolean = false,
    val isQuestionCardFlipped: Boolean = false,
    
    // 기타 상태
    val hasProgress: Boolean = false,
    val currentKoreanTtsService: String = "",
    
    // 사용자 레벨 상태
    val currentUserLevel: String = ""
)

/**
 * Composition Pattern을 사용한 MainViewModel
 * 여러 개의 작은 ViewModel을 조합하여 복잡성을 관리합니다.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val audioFileManager: AudioFileManager,
    private val qaDataManager: QaDataManager,
    private val ttsPlaybackController: TtsPlaybackController,
    private val progressTracker: MemorizeTestProgressTracker,

    private val repeatListeningService: RepeatListeningService,

    private val recordingTimeManager: RecordingTimeManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application
) : AndroidViewModel(application) {
    
    // 단일 StateFlow로 상태 관리 (각 ViewModel의 상태를 조합)
    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private val _hasEnglishWritingTestMergedFile = MutableStateFlow(false)
    val hasEnglishWritingTestMergedFile: StateFlow<Boolean> = _hasEnglishWritingTestMergedFile.asStateFlow()

    private val _isEnglishWritingTestMergedFilePlaying = MutableStateFlow(false)
    val isEnglishWritingTestMergedFilePlaying: StateFlow<Boolean> = _isEnglishWritingTestMergedFilePlaying.asStateFlow()

    private val _englishWritingTestMergedFileHighlightIndex = MutableStateFlow<Int?>(null)
    val englishWritingTestMergedFileHighlightIndex: StateFlow<Int?> = _englishWritingTestMergedFileHighlightIndex.asStateFlow()

    // 영작테스트 하이라이트 재생을 위한 Job 관리
    private var englishWritingTestHighlightJob: Job? = null

    private val _isQuestionCardFlipped = MutableStateFlow(false)
    val isQuestionCardFlipped: StateFlow<Boolean> = _isQuestionCardFlipped.asStateFlow()

    private var prefs: SharedPreferences? = null
    private val PREF_KEY_LAST_MEMORIZE_LEVEL = "last_memorize_level"
    private var _currentSentenceIndex = 0

    // 통암기 녹음 파일 관련 상태 - 제거 (MemorizationViewModel로 이동)
    // private val _hasFullMemorizationRecording = MutableStateFlow(false)
    // val hasFullMemorizationRecording: StateFlow<Boolean> = _hasFullMemorizationRecording.asStateFlow()

    // private val _isFullMemorizationRecordingPlaying = MutableStateFlow(false)
    // val isFullMemorizationRecordingPlaying: StateFlow<Boolean> = _isFullMemorizationRecordingPlaying.asStateFlow()

    // private val _fullMemorizationRecordingHighlightIndex = MutableStateFlow<Int?>(null)
    // val fullMemorizationRecordingHighlightIndex: StateFlow<Int?> = _fullMemorizationRecordingHighlightIndex.asStateFlow()

    init {
        initializeViewModel()
        setupStateCombination()
    }

    /**
     * ViewModel 초기화
     */
    private fun initializeViewModel() {
        try {
            prefs = getApplication<Application>().getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
            
            // TTS 오케스트레이터 설정
            val app = getApplication<Application>() as com.na982.opichelper.OPicHelperApplication
            ttsPlaybackController.setTtsOrchestrator(app.ttsOrchestrator)
            
            // 암기 레벨 로드
            loadMemorizeLevel()
            
            // QA 데이터 매니저 초기화 및 진행상황 복원
            viewModelScope.launch {
                try {
                    qaDataManager.init(getApplication())
                    progressTracker.restoreAllProgress()
                    Log.d("MainViewModel", "진행상황 복원 완료")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "진행상황 복원 실패", e)
                }
            }
            
            // 오래된 녹음 파일 정리
            viewModelScope.launch {
                // TODO: AudioFileManager를 통해 오래된 녹음 파일 정리
            }
        } catch (e: Exception) {
            // 테스트 환경에서는 Application이 제대로 설정되지 않을 수 있음
            // Log.w("MainViewModel", "초기화 중 오류 발생 (테스트 환경일 수 있음): ${e.message}")
        }
    }

    /**
     * QaDataManager와 TtsPlaybackController의 상태를 MainViewModel의 UI 상태와 동기화
     */
    private fun setupStateCombination() {
        viewModelScope.launch {
            // QaDataManager 상태 동기화
            combine(
                qaDataManager.currentQaItem,
                qaDataManager.currentCategory,
                qaDataManager.categories,
                qaDataManager.isLoading,
                qaDataManager.error
            ) { currentQaItem: QaItem?, currentCategory: String?, categories: List<String>, isLoading: Boolean, error: String? ->
                _uiState.value = _uiState.value.copy(
                    currentQaItem = currentQaItem,
                    currentCategory = currentCategory,
                    categories = categories,
                    isLoading = isLoading,
                    error = error
                )
            }.collect { }
        }
        
        viewModelScope.launch {
            // 사용자 레벨 상태 동기화
            userPreferencesRepository.userLevel.collect { userLevel ->
                _uiState.value = _uiState.value.copy(
                    currentUserLevel = userLevel.name
                )
            }
        }
        
        viewModelScope.launch {
            // TtsPlaybackController 상태 동기화
            combine(
                ttsPlaybackController.isPlaying,
                ttsPlaybackController.isQuestionPlaying,
                ttsPlaybackController.isAnswerPlaying,
                ttsPlaybackController.questionHighlightIndex,
                ttsPlaybackController.answerHighlightIndex,
                ttsPlaybackController.answerKoHighlightIndex,
                ttsPlaybackController.recordingHighlightIndex
            ) { values ->
                val isPlaying = values[0] as Boolean
                val isQuestionPlaying = values[1] as Boolean
                val isAnswerPlaying = values[2] as Boolean
                val questionHighlightIndex = values[3] as Int?
                val answerHighlightIndex = values[4] as Int?
                val answerKoHighlightIndex = values[5] as Int?
                val recordingHighlightIndex = values[6] as Int?
                
                _uiState.value = _uiState.value.copy(
                    isPlaying = isPlaying,
                    isQuestionPlaying = isQuestionPlaying,
                    isAnswerPlaying = isAnswerPlaying,
                    questionHighlightIndex = questionHighlightIndex,
                    answerHighlightIndex = answerHighlightIndex,
                    answerKoHighlightIndex = answerKoHighlightIndex,
                    recordingHighlightIndex = recordingHighlightIndex
                )
            }.collect { }
        }
        
        viewModelScope.launch {
            // ProgressTracker 상태 동기화
            progressTracker.hasProgress.collect { hasProgress ->
                _uiState.value = _uiState.value.copy(hasProgress = hasProgress)
            }
        }
    }

    // ===== 카드 관련 메서드들 (로컬에서 관리) =====
    
    fun setAnswerCardFlipped(isFlipped: Boolean) {
        _uiState.value = _uiState.value.copy(isAnswerCardFlipped = isFlipped)
        Log.d("MainViewModel", "답변 카드 뒤집기 상태 변경: $isFlipped")
    }

    fun setMergedAudioPlaying(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(
            isEnglishWritingTestMergedFilePlaying = isPlaying
        )
    }

    fun setQuestionCardFlipped(isFlipped: Boolean) {
        _isQuestionCardFlipped.value = isFlipped
        Log.d("MainViewModel", "질문 카드 뒤집기: $isFlipped")
    }

    fun setSelectedMemorizeLevel(level: String) {
        Log.d("MainViewModel", "암기레벨 변경: $level")
        _uiState.value = _uiState.value.copy(selectedMemorizeLevel = level)
        
        // SharedPreferences에 암기 레벨 저장
        prefs?.edit()?.putString(PREF_KEY_LAST_MEMORIZE_LEVEL, level)?.apply()
        Log.d("MainViewModel", "암기레벨 SharedPreferences 저장: $level")
        
        // 암기레벨 변경 시 MemorizationViewModel 상태 초기화
        viewModelScope.launch {
            try {
                // MemorizationViewModel의 상태를 초기화하는 이벤트 발생
                // 이는 MemorizationViewModel에서 감지하여 처리
                Log.d("MainViewModel", "암기레벨 변경으로 인한 상태 초기화 요청")
            } catch (e: Exception) {
                Log.e("MainViewModel", "암기레벨 변경 처리 실패", e)
            }
        }
    }

    fun setMemorizeTestRunning(isRunning: Boolean) {
        // 이 함수는 더 이상 필요하지 않으므로 제거
    }

    fun updateKoreanTtsServiceName(serviceName: String) {
        _uiState.value = _uiState.value.copy(currentKoreanTtsService = serviceName)
    }

    fun playMergedAudioFile() {
        // TODO: 영작테스트 병합 파일 재생 구현
    }

    fun playEnglishWritingTestMergedFile() {
        // 이전 하이라이트 Job이 실행 중이면 취소
        englishWritingTestHighlightJob?.cancel()
        
        englishWritingTestHighlightJob = viewModelScope.launch {
            try {
                val currentItem = qaDataManager.currentQaItem.value
                if (currentItem != null) {
                    val category = currentItem.category
                    val scriptIndex = qaDataManager.getCurrentIndex()
                    
                    Log.d("MainViewModel", "영작테스트 병합 파일 재생 시작: category='$category', scriptIndex=$scriptIndex")
                    
                    val mergedFile = audioFileManager.getEnglishWritingTestMergedFile(category, scriptIndex)
                    
                    if (mergedFile != null && mergedFile.exists()) {
                        Log.d("MainViewModel", "영작테스트 병합 파일 재생: ${mergedFile.absolutePath}")
                        
                        Log.d("MainViewModel", "녹음 시간 데이터 확인: category='$category', scriptIndex=$scriptIndex, 키: ${category}_${scriptIndex}")
                        
                        if (!recordingTimeManager.hasRecordingTimes(category, scriptIndex)) {
                            Log.d("MainViewModel", "녹음 시간 데이터가 없음 - 기본 하이라이트 사용")
                            
                            // 기본 하이라이트 사용 (처음부터)
                            playEnglishWritingTestMergedFileWithDefaultHighlight(mergedFile, currentItem)
                        } else {
                            Log.d("MainViewModel", "녹음 시간 데이터 사용 - 정확한 하이라이트 동기화")
                            // 저장된 녹음 시간 사용 (처음부터)
                            playEnglishWritingTestMergedFileWithExactHighlight(mergedFile, currentItem, category, scriptIndex)
                        }
                    } else {
                        Log.d("MainViewModel", "영작테스트 병합 파일이 존재하지 않음")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "영작테스트 병합 파일 재생 실패", e)
            } finally {
                // Job 완료 후 null로 설정
                englishWritingTestHighlightJob = null
            }
        }
    }

    private suspend fun playEnglishWritingTestMergedFileWithDefaultHighlight(mergedFile: File, currentItem: QaItem) {
        // 재생 상태 설정
        _isEnglishWritingTestMergedFilePlaying.value = true
        
        // 영문 카드로 설정
        setAnswerCardFlipped(false)
        
        // 실제 오디오 파일 재생 시작
        audioPlayer.playAudio(mergedFile.absolutePath)
        
        // 영문 텍스트를 문장 단위로 분리
        val answerText = getCurrentAnswer(currentItem)
        val sentences = answerText.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        
        // 각 문장에 대해 하이라이트 진행 (기본 시간)
        for (i in sentences.indices) {
            // Job이 취소되었는지 확인
            if (!currentCoroutineContext().isActive) {
                Log.d("MainViewModel", "영작테스트 Job이 취소됨 - 루프 종료")
                break
            }
            
            if (_isEnglishWritingTestMergedFilePlaying.value) {
                Log.d("MainViewModel", "영작테스트 하이라이트 설정: 문장 $i")
                _englishWritingTestMergedFileHighlightIndex.value = i
                
                // 기본 하이라이트 지속 시간
                val sentenceLength = sentences[i].length
                val highlightDuration = (sentenceLength * 50L).coerceAtLeast(1000L)
                
                Log.d("MainViewModel", "영작테스트 하이라이트 지속 시간: ${highlightDuration}ms")
                kotlinx.coroutines.delay(highlightDuration)
            } else {
                Log.d("MainViewModel", "영작테스트 재생이 중단됨 - 루프 종료")
                break
            }
        }
        
        // 재생 완료 후 상태 초기화
        _isEnglishWritingTestMergedFilePlaying.value = false
        _englishWritingTestMergedFileHighlightIndex.value = null
        
        Log.d("MainViewModel", "영작테스트 병합 파일 재생 완료 (기본 하이라이트)")
    }

    private suspend fun playEnglishWritingTestMergedFileWithExactHighlight(mergedFile: File, currentItem: QaItem, category: String, scriptIndex: Int) {
        Log.d("MainViewModel", "정확한 하이라이트 재생 시작: category=$category, scriptIndex=$scriptIndex")
        
        // 재생 상태 설정
        _isEnglishWritingTestMergedFilePlaying.value = true
        Log.d("MainViewModel", "영작테스트 재생 상태 설정: isEnglishWritingTestMergedFilePlaying=true")
        
        // 영문 카드로 설정
        setAnswerCardFlipped(false)
        
        // 실제 오디오 파일 재생 시작
        audioPlayer.playAudio(mergedFile.absolutePath)
        
        // 저장된 녹음 시간 가져오기
        val recordingTimes = recordingTimeManager.getAllRecordingTimes(category, scriptIndex)
        Log.d("MainViewModel", "저장된 녹음 시간: $recordingTimes")
        
        // 각 문장에 대해 정확한 녹음 시간으로 하이라이트 진행
        for (i in recordingTimes.indices) {
            // Job이 취소되었는지 확인
            if (!currentCoroutineContext().isActive) {
                Log.d("MainViewModel", "영작테스트 Job이 취소됨 - 루프 종료")
                break
            }
            
            // 재생 상태 재확인 (중단된 경우 루프 종료)
            if (!_isEnglishWritingTestMergedFilePlaying.value) {
                Log.d("MainViewModel", "영작테스트 재생이 중단됨 - 루프 종료")
                break
            }
            
            Log.d("MainViewModel", "영작테스트 정확한 하이라이트 설정: 문장 $i")
            _englishWritingTestMergedFileHighlightIndex.value = i
            
            // 저장된 실제 녹음 시간 사용
            val recordingTime = recordingTimes[i]
            Log.d("MainViewModel", "문장 $i 하이라이트 지속 시간: ${recordingTime}ms")
            
            kotlinx.coroutines.delay(recordingTime)
        }
        
        // 재생 완료 후 상태 초기화
        _isEnglishWritingTestMergedFilePlaying.value = false
        _englishWritingTestMergedFileHighlightIndex.value = null
        
        Log.d("MainViewModel", "영작테스트 병합 파일 재생 완료 (정확한 하이라이트)")
    }

    fun stopEnglishWritingTestMergedFile() {
        Log.d("MainViewModel", "영작테스트 병합 파일 재생 중단 시작")
        
        // 하이라이트 Job 취소
        englishWritingTestHighlightJob?.cancel()
        englishWritingTestHighlightJob = null
        
        // 재생 상태 초기화
        _isEnglishWritingTestMergedFilePlaying.value = false
        _englishWritingTestMergedFileHighlightIndex.value = null
        
        // 오디오 재생 중지
        audioPlayer.stop()
        
        Log.d("MainViewModel", "영작테스트 병합 파일 재생 중단 완료 - 하이라이트 인덱스 초기화")
    }

    fun checkEnglishWritingTestMergedFile() {
        viewModelScope.launch {
            val currentItem = qaDataManager.currentQaItem.value
            if (currentItem != null) {
                val category = currentItem.category
                val scriptIndex = qaDataManager.getCurrentIndex()
                
                Log.d("MainViewModel", "영작테스트 병합 파일 확인: category='$category', scriptIndex=$scriptIndex")
                
                // 파일 존재 여부 확인 (최대 3회 재시도)
                var hasFile = false
                var mergedFile: File? = null
                
                for (attempt in 1..3) {
                    hasFile = audioFileManager.hasEnglishWritingTestMergedFile(category, scriptIndex)
                    mergedFile = audioFileManager.getEnglishWritingTestMergedFile(category, scriptIndex)
                    
                    Log.d("MainViewModel", "영작테스트 병합 파일 확인 시도 $attempt: hasFile=$hasFile, mergedFile=${mergedFile?.absolutePath}")
                    
                    if (hasFile && mergedFile != null && mergedFile.exists()) {
                        Log.d("MainViewModel", "영작테스트 병합 파일 확인 성공")
                        break
                    } else if (attempt < 3) {
                        Log.d("MainViewModel", "영작테스트 병합 파일 확인 실패 - 재시도 대기")
                        delay(500L) // 재시도 전 대기
                    }
                }
                
                _hasEnglishWritingTestMergedFile.value = hasFile && mergedFile != null && mergedFile.exists()
                Log.d("MainViewModel", "영작테스트 병합 파일 최종 확인 결과: hasFile=${_hasEnglishWritingTestMergedFile.value}")
            }
        }
    }

    // ===== QA 데이터 관련 메서드들 (QaDataManager에 직접 위임) =====
    
    fun selectCategory(category: String) {
        viewModelScope.launch {
            qaDataManager.selectCategory(category)
        }
    }

    fun nextQaItem() {
        viewModelScope.launch {
            qaDataManager.nextQaItem()
        }
    }

    fun previousQaItem() {
        viewModelScope.launch {
            qaDataManager.previousQaItem()
        }
    }

    fun clearError() {
        qaDataManager.clearError()
    }

    fun getItemsInCategory(category: String): List<QaItem> {
        return qaDataManager.getItemsInCategory(category)
    }

    fun updateMergedAudioFileStatus() {
        // TODO: AudioFileManager를 통해 병합 오디오 파일 상태 업데이트
    }

    fun getCurrentMergedAudioFile(): java.io.File? {
        // TODO: AudioFileManager를 통해 현재 병합 오디오 파일 반환
        return null
    }

    fun playQuestion(question: String) {
        viewModelScope.launch {
            // 영작테스트 녹음 파일 재생 중단
            stopEnglishWritingTestMergedFile()
            
            // 다른 TTS 중지 후 새 질문 재생
            ttsPlaybackController.stopAllTts()
            ttsPlaybackController.playQuestion(question)
        }
    }

    fun playAnswer(answer: String) {
        viewModelScope.launch {
            // 영작테스트 녹음 파일 재생 중단
            stopEnglishWritingTestMergedFile()
            
            // 다른 TTS 중지 후 새 답변 재생
            ttsPlaybackController.stopAllTts()
            ttsPlaybackController.playAnswer(answer)
        }
    }
    
    /**
     * 현재 재생 중인 TTS만 중지 (토글)
     */
    fun stopCurrentTts() {
        viewModelScope.launch {
            Log.d("MainViewModel", "현재 TTS 중지")
            ttsPlaybackController.stopAllTts()
        }
    }
    
    /**
     * 모든 TTS 중지 및 정리 (백키 등)
     */
    fun stopAllTts() {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "모든 TTS 중지 시작")
                
                // 1. TTS 재생 중지
                ttsPlaybackController.stopAllTts()
                
                // 2. 영작테스트 관련 상태 초기화
                _isEnglishWritingTestMergedFilePlaying.value = false
                _englishWritingTestMergedFileHighlightIndex.value = null
                
                // 3. 하이라이트 초기화
                ttsPlaybackController.clearHighlight()
                
                Log.d("MainViewModel", "모든 TTS 중지 완료")
            } catch (e: Exception) {
                Log.e("MainViewModel", "TTS 중지 실패", e)
            }
        }
    }
    
    /**
     * 완전한 TTS 정리 (앱 종료 시 사용)
     */
    fun cleanupAllTts() {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "완전한 TTS 정리 시작")
                
                // 1. TTS 완전 정리
                ttsPlaybackController.cleanupTts()
                
                // 2. 모든 관련 상태 초기화
                _isEnglishWritingTestMergedFilePlaying.value = false
                _englishWritingTestMergedFileHighlightIndex.value = null
                
                Log.d("MainViewModel", "완전한 TTS 정리 완료")
            } catch (e: Exception) {
                Log.e("MainViewModel", "완전한 TTS 정리 실패", e)
            }
        }
    }
    
    /**
     * 동기적으로 TTS 정리 (백키 종료 시 사용)
     */
    fun cleanupAllTtsSync() {
        try {
            Log.d("MainViewModel", "동기적 TTS 정리 시작")
            
            // 1. TTS 강제 중지 (동기적으로)
            ttsPlaybackController.forceStopTts()
            ttsPlaybackController.clearHighlight()
            
            // 2. 모든 관련 상태 초기화
            _isEnglishWritingTestMergedFilePlaying.value = false
            _englishWritingTestMergedFileHighlightIndex.value = null
            
            Log.d("MainViewModel", "동기적 TTS 정리 완료")
        } catch (e: Exception) {
            Log.e("MainViewModel", "동기적 TTS 정리 실패", e)
        }
    }

    fun checkFullMemorizationRecording() {
        // MemorizationViewModel로 기능 이동 - 여기서는 호출만
        Log.d("MainViewModel", "통암기 녹음 파일 확인 요청 - MemorizationViewModel로 위임")
    }

    fun playFullMemorizationRecording() {
        // MemorizationViewModel로 기능 이동 - 여기서는 호출만
        Log.d("MainViewModel", "통암기 녹음 재생 요청 - MemorizationViewModel로 위임")
    }

    fun stopFullMemorizationRecording() {
        // MemorizationViewModel로 기능 이동 - 여기서는 호출만
        Log.d("MainViewModel", "통암기 녹음 재생 중지 요청 - MemorizationViewModel로 위임")
    }

    // ===== 앱 생명주기 관련 메서드들 =====
    
    fun cleanupOnAppExit() {
        Log.d("MainViewModel", "앱 종료 시 리소스 정리 시작")
        
        viewModelScope.launch {
            try {
                // 현재 상태 저장
                val selectedMemorizeLevel = _uiState.value.selectedMemorizeLevel
                
                // 암기 레벨이 이미 프리퍼런스에 저장되어 있으므로 추가 저장 불필요
                Log.d("MainViewModel", "앱 종료 시 상태 저장 - 선택된 레벨: $selectedMemorizeLevel")
                
                // 현재 활성화된 스크립트의 진행 상황 업데이트
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    val answerText = getCurrentAnswer(currentItem)
                    val totalSentences = answerText.split(".").size
                    
                    // 현재 진행상황을 가져와서 저장 (암기레벨별)
                    val currentProgress = progressTracker.getScriptProgress(currentItem.category, qaDataManager.getCurrentIndex(), selectedMemorizeLevel)
                    val currentSentenceIndex = if (currentProgress != null) {
                        currentProgress.currentSentenceIndex
                    } else {
                        0
                    }
                    
                    Log.d("MainViewModel", "앱 종료 시 현재 스크립트 진행 상황 업데이트 - 문장 인덱스: $currentSentenceIndex, 총 문장: $totalSentences")
                    
                    progressTracker.updateProgress(
                        category = currentItem.category,
                        scriptIndex = qaDataManager.getCurrentIndex(),
                        memorizeLevel = selectedMemorizeLevel,
                        currentSentenceIndex = currentSentenceIndex,
                        totalSentences = totalSentences,
                        isMemorizeTestRunning = false // 앱 종료 시에는 false로 설정
                    )
                }
                
                // 현재 인덱스 저장 추가
                val currentIndex = qaDataManager.getCurrentIndex()
                Log.d("MainViewModel", "앱 종료 시 현재 인덱스 저장: $currentIndex")
                // QaDataManager를 통해 인덱스 저장 (SharedPreferences에 저장됨)
                qaDataManager.saveCurrentIndex(currentIndex)
                
                progressTracker.persistChangedProgress()
                
                // 카드 상태 초기화
                setAnswerCardFlipped(false)
                setMergedAudioPlaying(false)
                
                Log.d("MainViewModel", "앱 종료 시 리소스 정리 완료")
            } catch (e: Exception) {
                Log.e("MainViewModel", "앱 종료 시 리소스 정리 중 오류", e)
            }
        }
    }

    fun onBackgroundMove() {
        Log.d("MainViewModel", "백그라운드로 이동 - TTS 일시 중지")
        viewModelScope.launch {
            try {
                // 1. 현재 TTS 상태 저장
                val wasPlaying = _uiState.value.isPlaying
                val wasQuestionPlaying = _uiState.value.isQuestionPlaying
                val wasAnswerPlaying = _uiState.value.isAnswerPlaying
                
                Log.d("MainViewModel", "백그라운드 이동 시 TTS 상태: playing=$wasPlaying, question=$wasQuestionPlaying, answer=$wasAnswerPlaying")
                
                // 2. TTS 일시 중지 (하지만 완전히 정리하지는 않음)
                if (wasPlaying) {
                    ttsPlaybackController.pauseTts()
                    Log.d("MainViewModel", "백그라운드 이동 - TTS 일시 중지 완료")
                }
                
                // 3. 하이라이트 상태 초기화 (설정화면에서는 보이지 않으므로)
                ttsPlaybackController.clearHighlight()
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "백그라운드 이동 처리 실패", e)
            }
        }
    }

    fun onForegroundReturn() {
        Log.d("MainViewModel", "포그라운드로 복귀 - TTS 상태 복원")
        viewModelScope.launch {
            try {
                // 1. TTS 재개 (일시 중지된 경우)
                val wasPlaying = _uiState.value.isPlaying
                if (wasPlaying) {
                    ttsPlaybackController.resumeTts()
                    Log.d("MainViewModel", "포그라운드 복귀 - TTS 재개 완료")
                }
                
                // 2. 상태 동기화
                Log.d("MainViewModel", "포그라운드 복귀 - 상태 동기화 완료")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "포그라운드 복귀 처리 실패", e)
            }
        }
    }

    // ===== 내부 헬퍼 메서드들 =====
    
    private fun loadMemorizeLevel() {
        val levels = listOf("반복 듣기", "영작 테스트", "통암기")
        
        val savedLevel = prefs?.getString(PREF_KEY_LAST_MEMORIZE_LEVEL, "")
        if (savedLevel != null && savedLevel.isNotEmpty()) {
            setSelectedMemorizeLevel(savedLevel)
        } else {
            setSelectedMemorizeLevel(levels.first())
        }
    }

    /**
     * 현재 사용자 레벨에 맞는 답변을 가져오기
     */
    fun getCurrentAnswer(qaItem: QaItem?): String {
        return qaDataManager.getCurrentAnswer(qaItem)
    }
    
    /**
     * 현재 사용자 레벨에 맞는 한국어 답변을 가져오기
     */
    fun getCurrentAnswerKo(qaItem: QaItem?): String {
        return qaDataManager.getCurrentAnswerKo(qaItem)
    }

    // 영작테스트 완료 후 녹음 시간 데이터 확인
    fun checkRecordingTimesAfterEnglishWritingTest() {
        viewModelScope.launch {
            val currentItem = qaDataManager.currentQaItem.value
            if (currentItem != null) {
                val category = currentItem.category
                val scriptIndex = qaDataManager.getCurrentIndex()
                
                Log.d("MainViewModel", "영작테스트 완료 후 녹음 시간 확인: category='$category', scriptIndex=$scriptIndex")
                
                val hasData = recordingTimeManager.hasRecordingTimes(category, scriptIndex)
                val allTimes = recordingTimeManager.getAllRecordingTimes(category, scriptIndex)
                
                Log.d("MainViewModel", "영작테스트 완료 후 결과: hasData=$hasData, allTimes=$allTimes")
            }
        }
    }
    
    /**
     * 사용자 레벨 설정
     */
    fun setUserLevel(level: UserLevel) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setUserLevel(level)
                Log.d("MainViewModel", "사용자 레벨 설정 완료: $level")
            } catch (e: Exception) {
                Log.e("MainViewModel", "사용자 레벨 설정 실패", e)
            }
        }
    }
} 