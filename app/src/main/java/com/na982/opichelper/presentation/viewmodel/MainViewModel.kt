package com.na982.opichelper.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.QuestionCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import com.na982.opichelper.domain.usecase.EnglishWritingTestService
import com.na982.opichelper.domain.usecase.FullMemorizationService
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.data.repository.AudioFileRepositoryImpl
import kotlinx.coroutines.delay
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.audio.TtsPlaybackController

import com.na982.opichelper.domain.repository.AppExitState
import kotlinx.coroutines.Job
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker

data class MainUiState(
    val currentQaItem: QaItem? = null,
    val currentCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList(),
    val memorizeLevels: List<String> = emptyList(),
    val selectedMemorizeLevel: String = "",
    val hasMergedAudioFile: Boolean = false, // 병합된 오디오 파일 존재 여부 추가
    val isMergedAudioPlaying: Boolean = false // 병합된 오디오 재생 상태 추가
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val audioFileManager: AudioFileManager,
    private val qaDataManager: QaDataManager,
    private val ttsPlaybackController: TtsPlaybackController,
    private val progressTracker: MemorizeTestProgressTracker,
    private val fullMemorizationService: FullMemorizationService,
    private val repeatListeningService: RepeatListeningService,
    private val englishWritingTestService: EnglishWritingTestService,
    application: Application
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var prefs: SharedPreferences? = null
    private val PREF_KEY_LAST_MEMORIZE_LEVEL = "last_memorize_level"

    // TtsPlaybackController에서 하이라이트 상태를 가져옴
    val questionHighlightIndex: StateFlow<Int?> = ttsPlaybackController.questionHighlightIndex
    val answerHighlightIndex: StateFlow<Int?> = ttsPlaybackController.answerHighlightIndex
    val answerKoHighlightIndex: StateFlow<Int?> = ttsPlaybackController.answerKoHighlightIndex
    val recordingHighlightIndex: StateFlow<Int?> = ttsPlaybackController.recordingHighlightIndex
    
    // 답변 카드 뒤집기 상태
    private val _isAnswerCardFlipped = MutableStateFlow(false)
    val isAnswerCardFlipped: StateFlow<Boolean> = _isAnswerCardFlipped
    
    // 현재 스크립트의 녹음 파일 존재 여부
    private val _hasRecordingFile = MutableStateFlow(false)
    val hasRecordingFile: StateFlow<Boolean> = _hasRecordingFile
    
    // 현재 사용 중인 한글 TTS 서비스 이름
    private val _currentKoreanTtsService = MutableStateFlow("")
    val currentKoreanTtsService: StateFlow<String> = _currentKoreanTtsService

    private val _memorizeLevels = MutableStateFlow<List<String>>(emptyList())
    val memorizeLevels: StateFlow<List<String>> = _memorizeLevels

    private val _selectedMemorizeLevel = MutableStateFlow("")
    val selectedMemorizeLevel: StateFlow<String> = _selectedMemorizeLevel

    private val _isMemorizeTestRunning = MutableStateFlow(false)
    val isMemorizeTestRunning: StateFlow<Boolean> = _isMemorizeTestRunning

    // TtsPlaybackController에서 재생 상태를 가져옴
    val isPlaying: StateFlow<Boolean> = ttsPlaybackController.isPlaying
    val isQuestionPlaying: StateFlow<Boolean> = ttsPlaybackController.isQuestionPlaying
    val isAnswerPlaying: StateFlow<Boolean> = ttsPlaybackController.isAnswerPlaying

    // ProgressTracker에서 진행 상태를 가져옴
    val hasProgress: StateFlow<Boolean> = progressTracker.hasProgress

    // UseCase 실행 Job 저장
    private var currentUseCaseJob: Job? = null
    
    // 현재 진행 중인 문장 인덱스 추적
    private var _currentSentenceIndex = 0
    
    // 통암기 관련 상태
    private val _isFullMemorizationMode = MutableStateFlow(false)
    val isFullMemorizationMode: StateFlow<Boolean> = _isFullMemorizationMode.asStateFlow()
    
    // 통암기 하이라이트 인덱스
    private val _fullMemorizationHighlightIndex = MutableStateFlow<Int?>(null)
    val fullMemorizationHighlightIndex: StateFlow<Int?> = _fullMemorizationHighlightIndex.asStateFlow()
    
    // 통암기 녹음/재생 상태
    private val _isFullMemorizationRecording = MutableStateFlow(false)
    val isFullMemorizationRecording: StateFlow<Boolean> = _isFullMemorizationRecording.asStateFlow()
    
    private val _isFullMemorizationPlaying = MutableStateFlow(false)
    val isFullMemorizationPlaying: StateFlow<Boolean> = _isFullMemorizationPlaying.asStateFlow()
    
    // 통암기 녹음 파일 존재 여부
    private val _hasFullMemorizationRecording = MutableStateFlow(false)
    val hasFullMemorizationRecording: StateFlow<Boolean> = _hasFullMemorizationRecording.asStateFlow()

    init {
        prefs = getApplication<Application>().getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
        
        // 모든 진행 상황 복원
        viewModelScope.launch {
            progressTracker.restoreAllProgress()
        }
        
        // QA 데이터 로딩
        qaDataManager.init(getApplication())
        
        // 암기 레벨 로드
        loadMemorizeLevel()
        
        // 녹음 파일 존재 여부 확인
        checkRecordingFileExists()
        
        // QaDataManager의 상태를 UI 상태와 동기화
        viewModelScope.launch {
            // QA 데이터 상태를 UI 상태로 복사
            qaDataManager.currentQaItem.collect { qaItem ->
                _uiState.value = _uiState.value.copy(currentQaItem = qaItem)
            }
        }
        
        viewModelScope.launch {
            qaDataManager.currentCategory.collect { category ->
                _uiState.value = _uiState.value.copy(currentCategory = category)
            }
        }
        
        viewModelScope.launch {
            qaDataManager.categories.collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
        
        viewModelScope.launch {
            qaDataManager.isLoading.collect { isLoading ->
                _uiState.value = _uiState.value.copy(isLoading = isLoading)
            }
        }
        
        viewModelScope.launch {
            qaDataManager.error.collect { error ->
                _uiState.value = _uiState.value.copy(error = error)
            }
        }
        
        // TTS 오케스트레이터 설정
        val application = getApplication<Application>() as com.na982.opichelper.OPicHelperApplication
        setTtsOrchestrator(application.ttsOrchestrator)
        
        // 앱 시작 시 오래된 녹음 파일들 정리
        viewModelScope.launch {
            audioFileManager.cleanupAllOldRecordings(1)
            Log.d("MainViewModel", "앱 시작 시 전체 녹음 파일 정리 완료")
            
            // 초기 녹음 파일 존재 여부 확인
            checkRecordingFileExists()
        }
    }

    fun setMemorizeLevel(level: String) {
        _selectedMemorizeLevel.value = level
        _uiState.value = _uiState.value.copy(selectedMemorizeLevel = level)
        
        // 통암기 모드 설정
        _isFullMemorizationMode.value = (level == "통암기")
        
        // 프리퍼런스에 암기 레벨 저장
        prefs?.edit()?.putString(PREF_KEY_LAST_MEMORIZE_LEVEL, level)?.apply()
        Log.d("MainViewModel", "암기 레벨 저장: $level, 통암기 모드: ${level == "통암기"}")
    }

    fun updateKoreanTtsServiceName(serviceName: String) {
        _currentKoreanTtsService.value = serviceName
        Log.d("MainViewModel", "한글 TTS 서비스 업데이트: $serviceName")
    }
    
    // TTS 서비스 상태 정보 업데이트
    fun updateTtsServiceStatus() {
        // TTS 매니저에서 서비스 상태 정보를 가져와서 업데이트
        // 이 부분은 TtsService에서 호출될 예정
    }

    fun setTtsOrchestrator(orchestrator: com.na982.opichelper.domain.audio.TtsOrchestrator) {
        ttsPlaybackController.setTtsOrchestrator(orchestrator)
        Log.d("MainViewModel", "TTS 오케스트레이터 설정 완료")
    }
    
    fun bindTtsService(context: Context, onKoreanTtsServiceUpdate: ((String) -> Unit)? = null) {
        ttsPlaybackController.bindTtsService(context, onKoreanTtsServiceUpdate)
    }
    
    fun unbindTtsService(context: Context) {
        ttsPlaybackController.unbindTtsService(context)
    }

    fun playQuestion(question: String) {
        // 암기 테스트 중지
        if (_isMemorizeTestRunning.value) {
            _isMemorizeTestRunning.value = false
            setAnswerCardFlipped(false)
            Log.d("MainViewModel", "암기 테스트 중지됨 (질문 재생 시작)")
        }
        
        ttsPlaybackController.playQuestion(question)
    }

    fun stopQuestion() {
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
        }
    }

    fun playAnswer(answer: String) {
        // 암기 테스트 중지
        if (_isMemorizeTestRunning.value) {
            _isMemorizeTestRunning.value = false
            setAnswerCardFlipped(false)
            Log.d("MainViewModel", "암기 테스트 중지됨 (답변 재생 시작)")
        }
        
        ttsPlaybackController.playAnswer(answer)
    }

    fun stopAnswer() {
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
        }
    }

    fun stopAllTts() {
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            _isMemorizeTestRunning.value = false
            setAnswerCardFlipped(false)
        }
    }

    // Hilt로 주입되므로 별도 설정 불필요
    // fun setAudioPlayer(player: AudioPlayer) {
    //     audioPlayer = player
    // }

    fun getCurrentMergedAudioFile(): java.io.File? {
        return audioFileManager.getLatestMergedAudioFile()
    }

    fun updateMergedAudioFileStatus() {
        val hasFile = getCurrentMergedAudioFile() != null
        _uiState.value = _uiState.value.copy(hasMergedAudioFile = hasFile)
        Log.d("MainViewModel", "병합된 오디오 파일 상태 업데이트: $hasFile")
    }

    fun playMergedAudioFile() {
        getCurrentMergedAudioFile()?.let { file ->
            ttsPlaybackController.playAudioFile(file)
        }
    }
    
    fun setAnswerCardFlipped(isFlipped: Boolean) {
        _isAnswerCardFlipped.value = isFlipped
        Log.d("MainViewModel", "답변 카드 뒤집기 상태 변경: $isFlipped")
    }
    

    
    fun checkRecordingFileExists() {
        viewModelScope.launch {
            val currentItem = qaDataManager.getCurrentQaItem()
            if (currentItem != null) {
                val currentIndex = qaDataManager.getCurrentIndex()
                val scriptId = "${currentItem.category}_$currentIndex"
                val hasFile = audioFileManager.hasRecordingFile(scriptId)
                _hasRecordingFile.value = hasFile
                Log.d("MainViewModel", "스크립트 $scriptId 녹음 파일 존재 여부: $hasFile")
            } else {
                _hasRecordingFile.value = false
            }
        }
    }

    /**
     * 앱 종료 시 모든 리소스 정리
     */
    fun cleanupOnAppExit() {
        Log.d("MainViewModel", "앱 종료 시 리소스 정리 시작")
        
        viewModelScope.launch {
            try {
                // 1. UseCase Job 취소
                currentUseCaseJob?.cancel()
                currentUseCaseJob = null
                Log.d("MainViewModel", "UseCase Job 취소됨")
                
                // 2. TTS 중지
                ttsPlaybackController.stopTts()
                
                // 3. 현재 상태 저장 (암기 테스트 중단 전에)
                val currentCategory = qaDataManager.currentCategory.value
                val currentIndex = qaDataManager.getCurrentIndex()
                val selectedMemorizeLevel = _selectedMemorizeLevel.value
                val isMemorizeTestRunning = _isMemorizeTestRunning.value
                
                // 현재 활성화된 스크립트의 진행 상황 업데이트
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null && isMemorizeTestRunning) {
                    val totalSentences = currentItem.answerKo.split(Regex("(?<=[.!?])\\s+")).size
                    
                    // 현재 진행 중인 문장 인덱스 (ProgressTracker에서 가져오기)
                    val currentProgress = progressTracker.getScriptProgress(currentCategory ?: "", currentIndex ?: 0)
                    val currentSentenceIndex = currentProgress?.currentSentenceIndex ?: 0
                    
                    Log.d("MainViewModel", "앱 종료 시 현재 스크립트 진행 상황 업데이트 - 문장 인덱스: $currentSentenceIndex, 총 문장: $totalSentences")
                    
                    progressTracker.updateProgress(
                        category = currentCategory ?: "",
                        scriptIndex = currentIndex ?: 0,
                        memorizeLevel = selectedMemorizeLevel ?: "",
                        currentSentenceIndex = currentSentenceIndex,
                        totalSentences = totalSentences,
                        isMemorizeTestRunning = isMemorizeTestRunning
                    )
                }
                
                // 변경된 진행 상황만 저장
                progressTracker.persistChangedProgress()
                
                // 4. 암기 테스트 중단 (상태 저장 후)
                if (_isMemorizeTestRunning.value) {
                    _isMemorizeTestRunning.value = false
                    setAnswerCardFlipped(false)
                    ttsPlaybackController.clearHighlight()
                    Log.d("MainViewModel", "앱 종료 시 암기 테스트 중단")
                }
                
                Log.d("MainViewModel", "앱 종료 시 리소스 정리 완료")
            } catch (e: Exception) {
                Log.e("MainViewModel", "앱 종료 시 리소스 정리 중 오류", e)
            }
        }
    }

    /**
     * 백그라운드 이동 시 상태 유지 (정리하지 않음)
     */
    fun onBackgroundMove() {
        Log.d("MainViewModel", "백그라운드로 이동 - UseCase 중단")
        
        // 암기 테스트 실행 중이면 중단
        if (_isMemorizeTestRunning.value) {
            viewModelScope.launch {
                ttsPlaybackController.stopTts()
                _isMemorizeTestRunning.value = false
                setAnswerCardFlipped(false)
                ttsPlaybackController.clearHighlight()
                Log.d("MainViewModel", "백그라운드 이동 시 암기 테스트 중단")
            }
        }
    }

    /**
     * 포그라운드 복귀 시 상태 확인
     */
    fun onForegroundReturn() {
        Log.d("MainViewModel", "포그라운드로 복귀 - 상태 확인")
        // 포그라운드로 복귀 시 상태 확인 (필요시 정리)
    }



    private fun loadMemorizeLevel() {
        val levels = listOf(
            "반복 듣기",
            "영작 테스트", 
            "통암기"
        )
        _memorizeLevels.value = levels
        _uiState.value = _uiState.value.copy(memorizeLevels = levels)
        
        // 저장된 암기 레벨 복원
        val savedLevel = prefs?.getString(PREF_KEY_LAST_MEMORIZE_LEVEL, "")
        if (savedLevel != null && savedLevel.isNotEmpty()) {
            setMemorizeLevel(savedLevel)
        } else {
            // 기본값 설정
            setMemorizeLevel(levels.first())
        }
    }



    fun selectCategory(category: String) {
        qaDataManager.selectCategory(category)
        // 카테고리 변경 시 진행 상황은 유지 (다른 카테고리로 돌아올 수 있음)
        // 녹음 파일 존재 여부 확인
        checkRecordingFileExists()
    }

    fun nextQaItem() {
        qaDataManager.nextQaItem()
        // 스크립트 변경 시 진행 상황은 유지 (다른 스크립트로 돌아올 수 있음)
        // 녹음 파일 존재 여부 확인
        checkRecordingFileExists()
    }

    fun clearError() {
        qaDataManager.clearError()
    }

    fun getItemsInCategory(category: String): List<QaItem> {
        return qaDataManager.getItemsInCategory(category)
    }

    // 이전 질문으로 이동
    fun previousQaItem() {
        qaDataManager.previousQaItem()
        // 스크립트 변경 시 진행 상황은 유지 (다른 스크립트로 돌아올 수 있음)
        // 녹음 파일 존재 여부 확인
        checkRecordingFileExists()
    }

    /**
     * 암기 테스트 버튼 클릭 처리
     */
    fun onMemorizeTestButtonClick() {
        viewModelScope.launch {
            try {
                // 이미 암기 테스트가 실행 중이면 종료
                if (_isMemorizeTestRunning.value) {
                    Log.d("MainViewModel", "암기 테스트 종료")
                    
                    // 기존 UseCase 중지
                    currentUseCaseJob?.cancel()
                    currentUseCaseJob = null
                    
                    // 상태 초기화
                    _isMemorizeTestRunning.value = false
                    _isFullMemorizationMode.value = false
                    setAnswerCardFlipped(false)
                    
                    // TTS 중지
                    ttsPlaybackController.stopTts()
                    
                    // 하이라이트 초기화
                    ttsPlaybackController.clearHighlight()
                    _fullMemorizationHighlightIndex.value = null
                    
                    return@launch
                }
                
                // 암기 테스트 시작
                val selectedLevel = _selectedMemorizeLevel.value
                Log.d("MainViewModel", "암기 테스트 시작: $selectedLevel")
                
                when (selectedLevel) {
                    "반복 듣기" -> {
                        startRepeatListening()
                    }
                    "영작 테스트" -> {
                        startEnglishWritingTest()
                    }
                    "통암기" -> {
                        startFullMemorizationMode()
                    }
                    else -> {
                        Log.w("MainViewModel", "알 수 없는 암기 레벨: $selectedLevel")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "암기 테스트 시작 실패", e)
                _isMemorizeTestRunning.value = false
            }
        }
    }

    /**
     * 통암기 모드 시작
     */
    fun startFullMemorizationMode() {
        viewModelScope.launch {
            try {
                // 기존 UseCase 중지
                currentUseCaseJob?.cancel()
                
                // 통암기 모드 활성화
                _isFullMemorizationMode.value = true
                _isMemorizeTestRunning.value = true
                
                // 카드 뒤집기 상태 초기화
                setAnswerCardFlipped(false)
                
                // TTS 중지
                ttsPlaybackController.stopTts()
                
                // 통암기 UseCase 시작
                currentUseCaseJob = viewModelScope.launch {
                    val category = qaDataManager.getCurrentCategory() ?: ""
                    val scriptIndex = qaDataManager.getCurrentIndex()
                    
                    fullMemorizationService.startFullMemorization(
                        category = category,
                        scriptIndex = scriptIndex,
                        onRecordingStateChange = { isRecording ->
                            _isFullMemorizationRecording.value = isRecording
                            Log.d("MainViewModel", "통암기 녹음 상태 변경: $isRecording")
                            
                            // 녹음 종료 시 파일 존재 여부 업데이트
                            if (!isRecording) {
                                viewModelScope.launch {
                                    updateFullMemorizationRecordingStatus()
                                }
                            }
                        },
                        onPlayingStateChange = { isPlaying ->
                            _isFullMemorizationPlaying.value = isPlaying
                            Log.d("MainViewModel", "통암기 재생 상태 변경: $isPlaying")
                        },
                        onHighlight = { index ->
                            _fullMemorizationHighlightIndex.value = index
                        }
                    )
                }
                
                Log.d("MainViewModel", "통암기 모드 시작")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "통암기 모드 시작 실패", e)
                _isFullMemorizationMode.value = false
                _isMemorizeTestRunning.value = false
            }
        }
    }
    
    /**
     * 통암기 녹음 종료
     */
    fun stopFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                fullMemorizationService.stopRecording()
                
                // 녹음 상태만 비활성화 (통암기 모드는 유지)
                _isFullMemorizationRecording.value = false
                _isMemorizeTestRunning.value = false
                
                // 하이라이트 초기화
                _fullMemorizationHighlightIndex.value = null
                
                Log.d("MainViewModel", "통암기 녹음 종료")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "통암기 녹음 종료 실패", e)
            }
        }
    }
    
    /**
     * 통암기 녹음 재생
     */
    fun playFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                // 카드를 영문으로 뒤집기
                setAnswerCardFlipped(false)
                
                fullMemorizationService.playRecording(
                    onPlayingStateChange = { isPlaying ->
                        // 재생 상태는 UseCase 내부에서 관리
                    },
                    onHighlight = { index ->
                        _fullMemorizationHighlightIndex.value = index
                    }
                )
                
                Log.d("MainViewModel", "통암기 녹음 재생")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "통암기 녹음 재생 실패", e)
                _fullMemorizationHighlightIndex.value = null
            }
        }
    }
    
    /**
     * 통암기 재생 중지
     */
    fun stopFullMemorizationPlaying() {
        viewModelScope.launch {
            try {
                fullMemorizationService.stopPlaying()
                _fullMemorizationHighlightIndex.value = null
                
                Log.d("MainViewModel", "통암기 재생 중지")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "통암기 재생 중지 실패", e)
            }
        }
    }
    
    /**
     * 통암기 녹음 파일 존재 여부 확인
     */
    suspend fun hasFullMemorizationRecording(): Boolean {
        return fullMemorizationService.hasRecordingFile()
    }
    
    /**
     * 통암기 녹음 파일 존재 여부 업데이트
     */
    private suspend fun updateFullMemorizationRecordingStatus() {
                    val hasRecording = fullMemorizationService.hasRecordingFile()
        _hasFullMemorizationRecording.value = hasRecording
        Log.d("MainViewModel", "통암기 녹음 파일 존재 여부 업데이트: $hasRecording")
    }
    
    /**
     * 통암기 녹음 파일 삭제
     */
    fun deleteFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                fullMemorizationService.deleteRecordingFile()
                Log.d("MainViewModel", "통암기 녹음 파일 삭제")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "통암기 녹음 파일 삭제 실패", e)
            }
        }
    }

    /**
     * 반복 듣기 시작
     */
    private fun startRepeatListening() {
        viewModelScope.launch {
            try {
                _isMemorizeTestRunning.value = true
                setAnswerCardFlipped(false)
                
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    Log.d("MainViewModel", "반복 듣기 Service 실행")
                    currentUseCaseJob = launch {
                        repeatListeningService.executeRepeatListeningTest(
                            answerKo = currentItem.answerKo,
                            answerEn = currentItem.answerEn,
                            onHighlight = { index ->
                                if (index != null) {
                                    // 현재 카드 상태에 따라 한글 또는 영문 하이라이트 설정
                                    if (_isAnswerCardFlipped.value) {
                                        // 카드가 한글로 뒤집혀 있으면 한글 하이라이트
                                        ttsPlaybackController.setAnswerKoHighlightIndex(index)
                                        Log.d("MainViewModel", "반복 듣기: 한글 하이라이트 설정: $index")
                                    } else {
                                        // 카드가 영문으로 뒤집혀 있으면 영문 하이라이트
                                        ttsPlaybackController.setAnswerHighlightIndex(index)
                                        Log.d("MainViewModel", "반복 듣기: 영문 하이라이트 설정: $index")
                                    }
                                } else {
                                    ttsPlaybackController.clearHighlight()
                                    Log.d("MainViewModel", "반복 듣기: 하이라이트 제거")
                                }
                            },
                            onCardFlip = { isKorean ->
                                setAnswerCardFlipped(isKorean)
                                Log.d("MainViewModel", "반복 듣기: 카드 뒤집기 - ${if (isKorean) "한글" else "영문"}")
                            },
                            category = currentItem.category,
                            scriptIndex = qaDataManager.getCurrentIndex()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "반복 듣기 시작 실패", e)
                _isMemorizeTestRunning.value = false
            }
        }
    }
    
    /**
     * 영작 테스트 시작
     */
    private fun startEnglishWritingTest() {
        viewModelScope.launch {
            try {
                _isMemorizeTestRunning.value = true
                setAnswerCardFlipped(false)
                
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    Log.d("MainViewModel", "영작 테스트 Service 실행")
                    currentUseCaseJob = launch {
                        englishWritingTestService.executeEnglishWritingTest(
                            answerKo = currentItem.answerKo,
                            answerEn = currentItem.answerEn,
                            onKoreanHighlight = { index ->
                                if (index != null) {
                                    // 한글 하이라이트 설정
                                    ttsPlaybackController.setAnswerKoHighlightIndex(index)
                                    Log.d("MainViewModel", "영작 테스트: 한글 하이라이트 설정: $index")
                                } else {
                                    ttsPlaybackController.clearHighlight()
                                    Log.d("MainViewModel", "영작 테스트: 한글 하이라이트 제거")
                                }
                            },
                            onEnglishHighlight = { index ->
                                if (index != null) {
                                    // 영문 하이라이트 설정
                                    ttsPlaybackController.setAnswerHighlightIndex(index)
                                    Log.d("MainViewModel", "영작 테스트: 영문 하이라이트 설정: $index")
                                } else {
                                    ttsPlaybackController.clearHighlight()
                                    Log.d("MainViewModel", "영작 테스트: 영문 하이라이트 제거")
                                }
                            },
                            onRecordingHighlight = { index ->
                                if (index != null) {
                                    // 녹음 하이라이트 설정 (더 강한 하이라이트)
                                    ttsPlaybackController.setRecordingHighlightIndex(index)
                                    Log.d("MainViewModel", "영작 테스트: 녹음 하이라이트 설정: $index")
                                } else {
                                    ttsPlaybackController.clearHighlight()
                                    Log.d("MainViewModel", "영작 테스트: 녹음 하이라이트 제거")
                                }
                            },
                            onCardFlip = { isKorean ->
                                setAnswerCardFlipped(isKorean)
                                Log.d("MainViewModel", "영작 테스트: 카드 뒤집기 - ${if (isKorean) "한글" else "영문"}")
                            },
                            category = currentItem.category,
                            scriptIndex = qaDataManager.getCurrentIndex()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "영작 테스트 시작 실패", e)
                _isMemorizeTestRunning.value = false
            }
        }
    }

} 