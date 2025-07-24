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
import com.na982.opichelper.data.repository.AudioFileManagerImpl
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
    val isMergedAudioPlaying: Boolean = false, // 병합된 오디오 재생 상태 추가
    val isAnswerCardFlipped: Boolean = false, // 답변 카드 뒤집기 상태
    val hasRecordingFile: Boolean = false, // 현재 스크립트의 녹음 파일 존재 여부
    val currentKoreanTtsService: String = "", // 현재 사용 중인 한글 TTS 서비스 이름
    val isMemorizeTestRunning: Boolean = false, // 암기 테스트 실행 중 여부
    val isFullMemorizationMode: Boolean = false, // 통암기 모드 활성화 여부
    val fullMemorizationHighlightIndex: Int? = null, // 통암기 하이라이트 인덱스
    val isFullMemorizationRecording: Boolean = false, // 통암기 녹음 상태
    val isFullMemorizationPlaying: Boolean = false, // 통암기 재생 상태
    val hasFullMemorizationRecording: Boolean = false, // 통암기 녹음 파일 존재 여부
    val questionHighlightIndex: Int? = null, // TTS 재생 상태
    val answerHighlightIndex: Int? = null, // TTS 재생 상태
    val answerKoHighlightIndex: Int? = null, // TTS 재생 상태
    val recordingHighlightIndex: Int? = null, // TTS 재생 상태
    val isPlaying: Boolean = false, // TTS 재생 상태
    val isQuestionPlaying: Boolean = false, // TTS 재생 상태
    val isAnswerPlaying: Boolean = false, // TTS 재생 상태
    val hasProgress: Boolean = false // 진행 상태
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

    // UseCase 실행 Job 저장
    private var currentUseCaseJob: Job? = null
    
    // 현재 진행 중인 문장 인덱스 추적
    private var _currentSentenceIndex = 0

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
        val app = getApplication<Application>() as com.na982.opichelper.OPicHelperApplication
        setTtsOrchestrator(app.ttsOrchestrator)
        
        // 앱 시작 시 오래된 녹음 파일들 정리
        viewModelScope.launch {
            audioFileManager.cleanupAllOldRecordings(1)
            Log.d("MainViewModel", "앱 시작 시 전체 녹음 파일 정리 완료")
            
            // 초기 녹음 파일 존재 여부 확인
            checkRecordingFileExists()
        }

        // 각 StateFlow/MutableStateFlow의 collect를 통해 _uiState를 갱신
        viewModelScope.launch {
            ttsPlaybackController.questionHighlightIndex.collect { idx ->
                _uiState.value = _uiState.value.copy(questionHighlightIndex = idx)
            }
        }
        viewModelScope.launch {
            ttsPlaybackController.answerHighlightIndex.collect { idx ->
                _uiState.value = _uiState.value.copy(answerHighlightIndex = idx)
            }
        }
        viewModelScope.launch {
            ttsPlaybackController.answerKoHighlightIndex.collect { idx ->
                _uiState.value = _uiState.value.copy(answerKoHighlightIndex = idx)
            }
        }
        viewModelScope.launch {
            ttsPlaybackController.recordingHighlightIndex.collect { idx ->
                _uiState.value = _uiState.value.copy(recordingHighlightIndex = idx)
            }
        }
        viewModelScope.launch {
            ttsPlaybackController.isPlaying.collect { playing ->
                _uiState.value = _uiState.value.copy(isPlaying = playing)
            }
        }
        viewModelScope.launch {
            ttsPlaybackController.isQuestionPlaying.collect { playing ->
                _uiState.value = _uiState.value.copy(isQuestionPlaying = playing)
            }
        }
        viewModelScope.launch {
            ttsPlaybackController.isAnswerPlaying.collect { playing ->
                _uiState.value = _uiState.value.copy(isAnswerPlaying = playing)
            }
        }
        viewModelScope.launch {
            progressTracker.hasProgress.collect { has ->
                _uiState.value = _uiState.value.copy(hasProgress = has)
            }
        }
    }

    fun setMemorizeLevel(level: String) {
        _uiState.value = _uiState.value.copy(
            selectedMemorizeLevel = level,
            isFullMemorizationMode = (level == "통암기")
        )
        
        // 프리퍼런스에 암기 레벨 저장
        prefs?.edit()?.putString(PREF_KEY_LAST_MEMORIZE_LEVEL, level)?.apply()
        Log.d("MainViewModel", "암기 레벨 저장: $level, 통암기 모드: ${level == "통암기"}")
    }

    fun updateKoreanTtsServiceName(serviceName: String) {
        _uiState.value = _uiState.value.copy(currentKoreanTtsService = serviceName)
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
        if (_uiState.value.isMemorizeTestRunning) {
            _uiState.value = _uiState.value.copy(isMemorizeTestRunning = false)
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
        if (_uiState.value.isMemorizeTestRunning) {
            _uiState.value = _uiState.value.copy(isMemorizeTestRunning = false)
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
            _uiState.value = _uiState.value.copy(isMemorizeTestRunning = false)
            setAnswerCardFlipped(false)
        }
    }

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
        _uiState.value = _uiState.value.copy(isAnswerCardFlipped = isFlipped)
        Log.d("MainViewModel", "답변 카드 뒤집기 상태 변경: $isFlipped")
    }
    
    fun checkRecordingFileExists() {
        viewModelScope.launch {
            val currentItem = qaDataManager.getCurrentQaItem()
            if (currentItem != null) {
                val currentIndex = qaDataManager.getCurrentIndex()
                val scriptId = "${currentItem.category}_$currentIndex"
                val hasFile = audioFileManager.hasRecordingFile(scriptId)
                _uiState.value = _uiState.value.copy(hasRecordingFile = hasFile)
                Log.d("MainViewModel", "스크립트 $scriptId 녹음 파일 존재 여부: $hasFile")
            } else {
                _uiState.value = _uiState.value.copy(hasRecordingFile = false)
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
                val selectedMemorizeLevel = _uiState.value.selectedMemorizeLevel
                val isMemorizeTestRunning = _uiState.value.isMemorizeTestRunning
                
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
                if (_uiState.value.isMemorizeTestRunning) {
                    _uiState.value = _uiState.value.copy(isMemorizeTestRunning = false)
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
        if (_uiState.value.isMemorizeTestRunning) {
            viewModelScope.launch {
                ttsPlaybackController.stopTts()
                _uiState.value = _uiState.value.copy(isMemorizeTestRunning = false)
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
                if (_uiState.value.isMemorizeTestRunning) {
                    Log.d("MainViewModel", "암기 테스트 종료")
                    
                    // 기존 UseCase 중지
                    currentUseCaseJob?.cancel()
                    currentUseCaseJob = null
                    
                    // 상태 초기화
                    _uiState.value = _uiState.value.copy(
                        isMemorizeTestRunning = false,
                        isFullMemorizationMode = false,
                        fullMemorizationHighlightIndex = null,
                        isFullMemorizationRecording = false,
                        isFullMemorizationPlaying = false
                    )
                    setAnswerCardFlipped(false)
                    
                    // TTS 중지
                    ttsPlaybackController.stopTts()
                    
                    // 하이라이트 초기화
                    ttsPlaybackController.clearHighlight()
                    
                    return@launch
                }
                
                // 암기 테스트 시작
                val selectedLevel = _uiState.value.selectedMemorizeLevel
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
                _uiState.value = _uiState.value.copy(isMemorizeTestRunning = false)
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
                _uiState.value = _uiState.value.copy(
                    isFullMemorizationMode = true,
                    isMemorizeTestRunning = true
                )
                
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
                            _uiState.value = _uiState.value.copy(isFullMemorizationRecording = isRecording)
                            Log.d("MainViewModel", "통암기 녹음 상태 변경: $isRecording")
                            
                            // 녹음 종료 시 파일 존재 여부 업데이트
                            if (!isRecording) {
                                viewModelScope.launch {
                                    updateFullMemorizationRecordingStatus()
                                }
                            }
                        },
                        onPlayingStateChange = { isPlaying ->
                            _uiState.value = _uiState.value.copy(isFullMemorizationPlaying = isPlaying)
                            Log.d("MainViewModel", "통암기 재생 상태 변경: $isPlaying")
                        },
                        onHighlight = { index ->
                            _uiState.value = _uiState.value.copy(fullMemorizationHighlightIndex = index)
                        }
                    )
                }
                
                Log.d("MainViewModel", "통암기 모드 시작")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "통암기 모드 시작 실패", e)
                _uiState.value = _uiState.value.copy(
                    isFullMemorizationMode = false,
                    isMemorizeTestRunning = false
                )
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
                _uiState.value = _uiState.value.copy(
                    isFullMemorizationRecording = false,
                    isMemorizeTestRunning = false,
                    fullMemorizationHighlightIndex = null
                )
                
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
                        _uiState.value = _uiState.value.copy(isFullMemorizationPlaying = isPlaying)
                    },
                    onHighlight = { index ->
                        _uiState.value = _uiState.value.copy(fullMemorizationHighlightIndex = index)
                    }
                )
                
                Log.d("MainViewModel", "통암기 녹음 재생")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "통암기 녹음 재생 실패", e)
                _uiState.value = _uiState.value.copy(fullMemorizationHighlightIndex = null)
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
                _uiState.value = _uiState.value.copy(fullMemorizationHighlightIndex = null)
                
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
        _uiState.value = _uiState.value.copy(hasFullMemorizationRecording = hasRecording)
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
                _uiState.value = _uiState.value.copy(isMemorizeTestRunning = true)
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
                                    if (_uiState.value.isAnswerCardFlipped) {
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
                _uiState.value = _uiState.value.copy(isMemorizeTestRunning = false)
            }
        }
    }
    
    /**
     * 영작 테스트 시작
     */
    private fun startEnglishWritingTest() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isMemorizeTestRunning = true)
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
                _uiState.value = _uiState.value.copy(isMemorizeTestRunning = false)
            }
        }
    }
} 