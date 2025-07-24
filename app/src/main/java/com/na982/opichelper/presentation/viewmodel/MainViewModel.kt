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
import com.na982.opichelper.domain.usecase.MemorizeTestUseCase
import com.na982.opichelper.domain.usecase.RepeatListeningUseCase
import com.na982.opichelper.domain.usecase.EnglishWritingTestUseCase
import com.na982.opichelper.domain.usecase.FullMemorizationUseCase
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.repository.AudioFileRepository
import com.na982.opichelper.data.repository.AudioFileRepositoryImpl
import kotlinx.coroutines.delay
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.usecase.MemorizeTestState

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
    private val audioFileRepository: AudioFileRepository,
    private val qaDataRepository: QaDataRepository,
    private val ttsPlaybackController: TtsPlaybackController,
    private val memorizeTestState: MemorizeTestState,
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



    init {
        prefs = getApplication<Application>().getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
        qaDataRepository.init(getApplication())
        loadMemorizeLevel()
        
        // TTS 오케스트레이터 설정
        val application = getApplication<Application>() as com.na982.opichelper.OPicHelperApplication
        setTtsOrchestrator(application.ttsOrchestrator)
        
        // 진행 상태 복원
        viewModelScope.launch {
            memorizeTestState.restoreProgress()
        }
        
        // QaDataRepository의 상태를 UI 상태와 동기화
        viewModelScope.launch {
            // QA 데이터 상태를 UI 상태로 복사
            qaDataRepository.currentQaItem.collect { qaItem ->
                _uiState.value = _uiState.value.copy(currentQaItem = qaItem)
            }
        }
        
        viewModelScope.launch {
            qaDataRepository.currentCategory.collect { category ->
                _uiState.value = _uiState.value.copy(currentCategory = category)
            }
        }
        
        viewModelScope.launch {
            qaDataRepository.categories.collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
        
        viewModelScope.launch {
            qaDataRepository.isLoading.collect { isLoading ->
                _uiState.value = _uiState.value.copy(isLoading = isLoading)
            }
        }
        
        viewModelScope.launch {
            qaDataRepository.error.collect { error ->
                _uiState.value = _uiState.value.copy(error = error)
            }
        }
        
        // 앱 시작 시 오래된 녹음 파일들 정리
        viewModelScope.launch {
            audioFileRepository.cleanupAllOldRecordings(1)
            Log.d("MainViewModel", "앱 시작 시 전체 녹음 파일 정리 완료")
            
            // 초기 녹음 파일 존재 여부 확인
            checkRecordingFileExists()
        }
    }

    fun setMemorizeLevel(level: String) {
        _selectedMemorizeLevel.value = level
        _uiState.value = _uiState.value.copy(selectedMemorizeLevel = level)
        // 프리퍼런스에 암기 레벨 저장
        prefs?.edit()?.putString(PREF_KEY_LAST_MEMORIZE_LEVEL, level)?.apply()
        Log.d("MainViewModel", "암기 레벨 저장: $level")
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
        return audioFileRepository.getLatestMergedAudioFile()
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
            val currentItem = qaDataRepository.getCurrentQaItem()
            if (currentItem != null) {
                val currentIndex = qaDataRepository.getCurrentIndex()
                val scriptId = "${currentItem.category}_$currentIndex"
                val hasFile = audioFileRepository.hasRecordingFile(scriptId)
                _hasRecordingFile.value = hasFile
                Log.d("MainViewModel", "스크립트 $scriptId 녹음 파일 존재 여부: $hasFile")
            } else {
                _hasRecordingFile.value = false
            }
        }
    }

    /**
     * 앱 종료 시 모든 리소스를 정리하는 함수
     * MainActivity에서 호출됨
     */
    fun cleanupOnAppExit() {
        Log.d("MainViewModel", "앱 종료 시 리소스 정리 시작")
        
        viewModelScope.launch {
            try {
                // TtsPlaybackController 정리
                ttsPlaybackController.stopTts()
                
                // 답변 카드 상태 초기화
                setAnswerCardFlipped(false)
                
                // 암기 테스트 중지
                _isMemorizeTestRunning.value = false
                
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
        Log.d("MainViewModel", "백그라운드로 이동 - 상태 유지")
        // 백그라운드로 이동 시에는 TTS와 하이라이트 상태 유지
    }

    /**
     * 포그라운드 복귀 시 상태 확인
     */
    fun onForegroundReturn() {
        Log.d("MainViewModel", "포그라운드로 복귀 - 상태 확인")
        // 포그라운드로 복귀 시 상태 확인 (필요시 정리)
    }



    private fun loadMemorizeLevel() {
        val levels = listOf("반복 듣기", "영작 테스트", "통암기")
        _memorizeLevels.value = levels
        
        // 프리퍼런스에서 저장된 암기 레벨 로드
        val savedLevel = prefs?.getString(PREF_KEY_LAST_MEMORIZE_LEVEL, null)
        if (savedLevel != null && levels.contains(savedLevel)) {
            setMemorizeLevel(savedLevel)
            Log.d("MainViewModel", "저장된 암기 레벨 로드: $savedLevel")
        } else if (_selectedMemorizeLevel.value.isEmpty() && levels.isNotEmpty()) {
            setMemorizeLevel(levels[0])
            Log.d("MainViewModel", "기본 암기 레벨 설정: ${levels[0]}")
        }
        
        _uiState.value = _uiState.value.copy(memorizeLevels = levels)
    }



    fun selectCategory(category: String) {
        qaDataRepository.selectCategory(category)
        // 녹음 파일 존재 여부 확인
        checkRecordingFileExists()
    }

    fun nextQaItem() {
        qaDataRepository.nextQaItem()
        // 녹음 파일 존재 여부 확인
        checkRecordingFileExists()
    }

    fun clearError() {
        qaDataRepository.clearError()
    }

    fun getItemsInCategory(category: String): List<QaItem> {
        return qaDataRepository.getItemsInCategory(category)
    }

    // 이전 질문으로 이동
    fun previousQaItem() {
        qaDataRepository.previousQaItem()
        // 녹음 파일 존재 여부 확인
        checkRecordingFileExists()
    }

    fun onMemorizeTestButtonClick(
        answerKo: String,
        answerEn: String,
        onHighlight: (Int?) -> Unit
    ) {
        Log.d("MainViewModel", "onMemorizeTestButtonClick 호출됨, level=${selectedMemorizeLevel.value}")
        
        // 이미 암기 테스트가 실행 중이면 종료
        if (_isMemorizeTestRunning.value) {
            viewModelScope.launch {
                ttsPlaybackController.stopTts()
            }
            _isMemorizeTestRunning.value = false
            setAnswerCardFlipped(false)
            Log.d("MainViewModel", "암기 테스트 중지됨")
            return
        }
        
        viewModelScope.launch {
            // 다른 재생 중지 및 하이라이트 초기화
            if (isPlaying.value) {
                ttsPlaybackController.stopTts()
                Log.d("MainViewModel", "기존 재생 중지됨 (암기 테스트 시작)")
            }
            
            // 암기 테스트 시작 시 답변 카드를 한글 페이지로 뒤집기
            setAnswerCardFlipped(true)
            
            // 암기 테스트 실행 중 상태로 설정
            _isMemorizeTestRunning.value = true
            
            when (selectedMemorizeLevel.value) {
                "반복 듣기" -> {
                    Log.d("MainViewModel", "반복 듣기 UseCase 실행")
                    val currentItem = qaDataRepository.getCurrentQaItem()
                    val currentIndex = qaDataRepository.getCurrentIndex()
                    
                    // 진행 상태 시작
                    memorizeTestState.startProgress(
                        category = currentItem?.category ?: "",
                        qaItemId = currentItem?.id ?: "",
                        testType = "반복 듣기",
                        totalSentences = answerKo.split(Regex("(?<=[.!?])\\s+")).size
                    )
                    
                    val useCase = RepeatListeningUseCase(
                        answerKo = answerKo,
                        answerEn = answerEn,
                        ttsPlayer = ttsPlaybackController.getTtsPlayer(),
                        onHighlight = { index ->
                            // TtsPlaybackController의 하이라이트 상태와 동기화
                            if (index != null) {
                                // 현재 카드가 한글로 뒤집혀 있으면 한글 하이라이트, 아니면 영문 하이라이트
                                if (_isAnswerCardFlipped.value) {
                                    // 한글 답변 하이라이트 (답변 카드의 한글 부분)
                                    ttsPlaybackController.setAnswerKoHighlightIndex(index)
                                } else {
                                    // 영문 답변 하이라이트 (답변 카드의 영문 부분)
                                    ttsPlaybackController.setAnswerHighlightIndex(index)
                                }
                            } else {
                                // 하이라이트 제거
                                ttsPlaybackController.clearHighlight()
                            }
                            onHighlight(index) // 원래 콜백도 호출
                        },
                        onCardFlip = { isKorean ->
                            setAnswerCardFlipped(isKorean)
                            Log.d("MainViewModel", "반복 듣기: 카드 뒤집기 - ${if (isKorean) "한글" else "영문"}")
                        },
                        memorizeTestState = memorizeTestState,
                        category = currentItem?.category ?: "",
                        qaItemId = currentItem?.id ?: "",
                        repeatCount = 5
                    )
                    useCase.execute()
                    
                    // 암기 테스트 완료 후 상태 초기화
                    _isMemorizeTestRunning.value = false
                    setAnswerCardFlipped(false)
                    ttsPlaybackController.clearHighlight()
                    Log.d("MainViewModel", "반복 듣기 완료: 상태 초기화")
                }
                "영작 테스트" -> {
                    Log.d("MainViewModel", "영작 테스트 UseCase 실행")
                    val currentItem = qaDataRepository.getCurrentQaItem()
                    val currentIndex = qaDataRepository.getCurrentIndex()
                    val scriptId = "${currentItem?.category}_$currentIndex"
                    
                    // 진행 상태 시작
                    memorizeTestState.startProgress(
                        category = currentItem?.category ?: "",
                        qaItemId = currentItem?.id ?: "",
                        testType = "영작 테스트",
                        totalSentences = answerEn.split(Regex("(?<=[.!?])\\s+")).size
                    )
                    
                    val useCase = EnglishWritingTestUseCase(
                        answerEn = answerEn,
                        answerKo = answerKo,
                        scriptId = scriptId,
                        ttsPlayer = ttsPlaybackController.getTtsPlayer(),
                        audioRecorder = audioRecorder,
                        audioFileRepository = audioFileRepository,
                        memorizeTestState = memorizeTestState,
                        category = currentItem?.category ?: "",
                        qaItemId = currentItem?.id ?: "",
                        onAutoFlip = {
                            // 답변 카드를 한글 페이지로 뒤집기
                            setAnswerCardFlipped(true)
                            Log.d("MainViewModel", "영작 테스트: 답변 카드를 한글 페이지로 뒤집음")
                        },
                        onKoreanHighlight = { index ->
                            // 한글 하이라이트 설정
                            if (index != null) {
                                ttsPlaybackController.setAnswerKoHighlightIndex(index)
                                Log.d("MainViewModel", "영작 테스트: 한글 하이라이트 설정: $index")
                            } else {
                                ttsPlaybackController.clearHighlight()
                                Log.d("MainViewModel", "영작 테스트: 한글 하이라이트 제거")
                            }
                        },
                        onRecordingHighlight = { index ->
                            // 녹음 하이라이트 설정 (더 강한 하이라이트)
                            if (index != null) {
                                ttsPlaybackController.setRecordingHighlightIndex(index)
                                Log.d("MainViewModel", "영작 테스트: 녹음 하이라이트 설정: $index")
                            } else {
                                ttsPlaybackController.clearHighlight()
                                Log.d("MainViewModel", "영작 테스트: 녹음 하이라이트 제거")
                            }
                        },
                        onMergedFileCreated = { mergedFile ->
                            Log.d("MainViewModel", "병합된 오디오 파일 생성됨: ${mergedFile.absolutePath}")
                            updateMergedAudioFileStatus() // UI 상태 업데이트
                        }
                    )
                    useCase.execute()
                    
                    // 영작 테스트 완료 후 답변 카드를 원래 상태로 복원
                    _isMemorizeTestRunning.value = false
                    setAnswerCardFlipped(false)
                    ttsPlaybackController.clearHighlight()
                    Log.d("MainViewModel", "영작 테스트 완료: 답변 카드 상태 복원")
                    
                    // 오래된 녹음 파일들 정리
                    if (currentItem != null) {
                        audioFileRepository.cleanupOldRecordings(scriptId, 1)
                        Log.d("MainViewModel", "스크립트 $scriptId 오래된 녹음 파일들 정리 완료")
                    }
                    
                    // 녹음 파일 존재 여부 다시 확인
                    checkRecordingFileExists()
                }
                // TODO: 통암기도 동일하게 생성
                "통암기" -> {
                    Log.d("MainViewModel", "통암기 UseCase 실행 (미구현)")
                    // val useCase = FullMemorizationUseCase(...)
                }
                else -> {
                    Log.w("MainViewModel", "알 수 없는 암기 레벨: ${selectedMemorizeLevel.value}")
                }
            }
        }
    }


} 