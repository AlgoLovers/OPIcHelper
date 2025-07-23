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

class MainViewModel : AndroidViewModel {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val itemsByCategory: MutableMap<String, List<QaItem>> = mutableMapOf()
    private val itemIndexByCategory: MutableMap<String, Int> = mutableMapOf()

    private var prefs: SharedPreferences? = null
    private val PREF_KEY_LAST_CATEGORY = "last_category"
    private val PREF_KEY_LAST_INDEX = "last_index"
    private val PREF_KEY_LAST_MEMORIZE_LEVEL = "last_memorize_level"

    private val _questionHighlightIndex = MutableStateFlow<Int?>(null)
    val questionHighlightIndex: StateFlow<Int?> = _questionHighlightIndex
    private val _answerHighlightIndex = MutableStateFlow<Int?>(null)
    val answerHighlightIndex: StateFlow<Int?> = _answerHighlightIndex
    
    // 한글 답변 하이라이트 상태
    private val _answerKoHighlightIndex = MutableStateFlow<Int?>(null)
    val answerKoHighlightIndex: StateFlow<Int?> = _answerKoHighlightIndex
    
    // 답변 카드 뒤집기 상태
    private val _isAnswerCardFlipped = MutableStateFlow(false)
    val isAnswerCardFlipped: StateFlow<Boolean> = _isAnswerCardFlipped
    
    // 녹음 하이라이트 상태
    private val _recordingHighlightIndex = MutableStateFlow<Int?>(null)
    val recordingHighlightIndex: StateFlow<Int?> = _recordingHighlightIndex
    
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

    // TTS 관련 상태
    private val _isQuestionPlaying = MutableStateFlow(false)
    val isQuestionPlaying: StateFlow<Boolean> = _isQuestionPlaying

    private val _isAnswerPlaying = MutableStateFlow(false)
    val isAnswerPlaying: StateFlow<Boolean> = _isAnswerPlaying

    // TTS 플레이어 참조
    private var ttsPlayer: TtsPlayer? = null

    // 오디오 플레이어 참조
    private var audioPlayer: AudioPlayer? = null

    // 오디오 녹음 관련
    private var audioRecorder: AudioRecorder? = null
    private var audioFileRepository: AudioFileRepository? = null

    // 병합 오디오 상태 변경 콜백
    private var onMergedAudioStateChange: ((Boolean) -> Unit)? = null

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

    fun setTtsPlayer(player: TtsPlayer?) {
        ttsPlayer = player
        // TTS 서비스에 하이라이트 콜백 설정
        if (player is com.na982.opichelper.presentation.ui.component.TtsService) {
            player.setHighlightCallback(object : com.na982.opichelper.presentation.ui.component.TtsService.HighlightCallback {
                override fun onQuestionHighlight(index: Int?) {
                    Log.d("MainViewModel", "Question highlight changed to: $index")
                    _questionHighlightIndex.value = index
                }
                override fun onAnswerHighlight(index: Int?) {
                    Log.d("MainViewModel", "Answer highlight changed to: $index")
                    _answerHighlightIndex.value = index
                }
            })
        }
    }

    fun playQuestion(question: String) {
        viewModelScope.launch {
            try {
                // 암기 테스트 중지
                if (_isMemorizeTestRunning.value) {
                    _isMemorizeTestRunning.value = false
                    setAnswerCardFlipped(false)
                    _answerKoHighlightIndex.value = null
                    _recordingHighlightIndex.value = null
                    Log.d("MainViewModel", "암기 테스트 중지됨 (질문 재생 시작)")
                }
                
                // 다른 재생 중지 및 하이라이트 초기화 (항상 실행)
                ttsPlayer?.stop()
                _isAnswerPlaying.value = false
                _answerHighlightIndex.value = null
                Log.d("MainViewModel", "답변 재생 중지됨 (질문 재생 시작)")
                
                _isQuestionPlaying.value = true
                ttsPlayer?.speakWithHighlight(question) { index ->
                    _questionHighlightIndex.value = index
                }
                _isQuestionPlaying.value = false
                _questionHighlightIndex.value = null
                Log.d("MainViewModel", "질문 재생 완료")
            } catch (e: Exception) {
                Log.e("MainViewModel", "질문 재생 실패", e)
                _isQuestionPlaying.value = false
                _questionHighlightIndex.value = null
            }
        }
    }

    fun stopQuestion() {
        viewModelScope.launch {
            try {
                ttsPlayer?.stop()
                _isQuestionPlaying.value = false
                _questionHighlightIndex.value = null
                _answerHighlightIndex.value = null
                _answerKoHighlightIndex.value = null
                _recordingHighlightIndex.value = null
                Log.d("MainViewModel", "질문 재생 중지")
            } catch (e: Exception) {
                Log.e("MainViewModel", "질문 재생 중지 실패", e)
            }
        }
    }

    fun playAnswer(answer: String) {
        viewModelScope.launch {
            try {
                // 암기 테스트 중지
                if (_isMemorizeTestRunning.value) {
                    _isMemorizeTestRunning.value = false
                    setAnswerCardFlipped(false)
                    _answerKoHighlightIndex.value = null
                    _recordingHighlightIndex.value = null
                    Log.d("MainViewModel", "암기 테스트 중지됨 (답변 재생 시작)")
                }
                
                // 다른 재생 중지 및 하이라이트 초기화 (항상 실행)
                ttsPlayer?.stop()
                _isQuestionPlaying.value = false
                _questionHighlightIndex.value = null
                Log.d("MainViewModel", "질문 재생 중지됨 (답변 재생 시작)")
                
                _isAnswerPlaying.value = true
                ttsPlayer?.speakWithHighlight(answer) { index ->
                    _answerHighlightIndex.value = index
                }
                _isAnswerPlaying.value = false
                _answerHighlightIndex.value = null
                Log.d("MainViewModel", "답변 재생 완료")
            } catch (e: Exception) {
                Log.e("MainViewModel", "답변 재생 실패", e)
                _isAnswerPlaying.value = false
                _answerHighlightIndex.value = null
            }
        }
    }

    fun stopAnswer() {
        viewModelScope.launch {
            try {
                ttsPlayer?.stop()
                _isAnswerPlaying.value = false
                _questionHighlightIndex.value = null
                _answerHighlightIndex.value = null
                _answerKoHighlightIndex.value = null
                _recordingHighlightIndex.value = null
                Log.d("MainViewModel", "답변 재생 중지")
            } catch (e: Exception) {
                Log.e("MainViewModel", "답변 재생 중지 실패", e)
            }
        }
    }

    fun stopAllTts() {
        viewModelScope.launch {
            try {
                ttsPlayer?.stop()
                _isQuestionPlaying.value = false
                _isAnswerPlaying.value = false
                _isMemorizeTestRunning.value = false
                _questionHighlightIndex.value = null
                _answerHighlightIndex.value = null
                _answerKoHighlightIndex.value = null
                _recordingHighlightIndex.value = null
                setAnswerCardFlipped(false)
                Log.d("MainViewModel", "모든 TTS 재생 중지")
            } catch (e: Exception) {
                Log.e("MainViewModel", "TTS 재생 중지 실패", e)
            }
        }
    }

    fun setAudioPlayer(player: AudioPlayer) {
        audioPlayer = player
    }

    fun setMergedAudioStateChangeCallback(callback: (Boolean) -> Unit) {
        onMergedAudioStateChange = callback
    }

    fun getCurrentMergedAudioFile(): java.io.File? {
        return audioFileRepository?.getLatestMergedAudioFile()
    }

    fun updateMergedAudioFileStatus() {
        val hasFile = getCurrentMergedAudioFile() != null
        _uiState.value = _uiState.value.copy(hasMergedAudioFile = hasFile)
        Log.d("MainViewModel", "병합된 오디오 파일 상태 업데이트: $hasFile")
    }

    fun playMergedAudioFile() {
        // 이미 재생 중이면 중지
        if (_uiState.value.isMergedAudioPlaying) {
            audioPlayer?.stop()
            _uiState.value = _uiState.value.copy(isMergedAudioPlaying = false)
            onMergedAudioStateChange?.invoke(false)
            Log.d("MainViewModel", "병합 오디오 재생 중지")
            return
        }
        
        // 다른 재생 중지 및 하이라이트 초기화
        if (_isQuestionPlaying.value || _isAnswerPlaying.value) {
            ttsPlayer?.stop()
            _isQuestionPlaying.value = false
            _isAnswerPlaying.value = false
            _questionHighlightIndex.value = null
            _answerHighlightIndex.value = null
            _answerKoHighlightIndex.value = null
            _recordingHighlightIndex.value = null
            Log.d("MainViewModel", "기존 TTS 재생 중지됨 (병합 오디오 재생 시작)")
        }
        
        getCurrentMergedAudioFile()?.let { file ->
            _uiState.value = _uiState.value.copy(isMergedAudioPlaying = true)
            onMergedAudioStateChange?.invoke(true) // 재생 시작
            audioPlayer?.play(file) {
                Log.d("MainViewModel", "병합된 오디오 파일 재생 완료")
                _uiState.value = _uiState.value.copy(isMergedAudioPlaying = false)
                onMergedAudioStateChange?.invoke(false) // 재생 완료
            }
        }
    }

    fun setQuestionHighlightIndex(index: Int?) {
        _questionHighlightIndex.value = index
    }
    fun setAnswerHighlightIndex(index: Int?) {
        _answerHighlightIndex.value = index
    }
    
    fun setAnswerKoHighlightIndex(index: Int?) {
        _answerKoHighlightIndex.value = index
        Log.d("MainViewModel", "한글 답변 하이라이트 상태 변경: $index")
    }
    
    fun setAnswerCardFlipped(isFlipped: Boolean) {
        _isAnswerCardFlipped.value = isFlipped
        Log.d("MainViewModel", "답변 카드 뒤집기 상태 변경: $isFlipped")
    }
    
    fun setRecordingHighlightIndex(index: Int?) {
        _recordingHighlightIndex.value = index
        Log.d("MainViewModel", "녹음 하이라이트 상태 변경: $index")
    }
    
    fun checkRecordingFileExists() {
        viewModelScope.launch {
            val currentItem = _uiState.value.currentQaItem
            if (currentItem != null) {
                val currentIndex = itemIndexByCategory[currentItem.category] ?: 0
                val scriptId = "${currentItem.category}_$currentIndex"
                val hasFile = audioFileRepository?.hasRecordingFile(scriptId) ?: false
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
                // 모든 TTS 재생 중지
                stopAllTts()
                
                // 오디오 플레이어 중지
                audioPlayer?.stop()
                
                // 모든 하이라이트 초기화
                _questionHighlightIndex.value = null
                _answerHighlightIndex.value = null
                _answerKoHighlightIndex.value = null
                _recordingHighlightIndex.value = null
                
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

    // Primary constructor for test (inject data)
    constructor(itemsByCategory: Map<String, List<QaItem>>) : super(Application()) {
        this.itemsByCategory.putAll(itemsByCategory)
        for (category in itemsByCategory.keys) {
            itemIndexByCategory[category] = 0
        }
        _uiState.value = _uiState.value.copy(categories = itemsByCategory.keys.toList())
    }

    // Secondary constructor for production (load from assets)
    constructor(application: Application) : this(mutableMapOf()) {
        prefs = application.getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
        audioRecorder = AudioRecorderImpl(application)
        audioFileRepository = AudioFileRepositoryImpl(application)
        loadQaItemsFromAssets(application)
        loadMemorizeLevel()
        restoreLastCategory()
        
        // 앱 시작 시 오래된 녹음 파일들 정리
        viewModelScope.launch {
            audioFileRepository?.cleanupAllOldRecordings(1)
            Log.d("MainViewModel", "앱 시작 시 전체 녹음 파일 정리 완료")
            
            // 초기 녹음 파일 존재 여부 확인
            checkRecordingFileExists()
        }
    }

    private fun loadQaItemsFromAssets(application: Application) {
        val context = application
        val gson = Gson()
        // Define the desired category order and display names
        val categoryDisplayNames = listOf(
            "집", "음악", "집에서 보내는 휴가", "영화", "레스토랑", "해변", "인터넷", "산업,커리어", "은행", "교통", "패션", "가족,친구", "가구", "예약", "명절"
        )
        // Map display names to asset file names (for 레스토랑, use restaurants asset)
        val categoryAssetMap = mapOf(
            "집" to "home",
            "음악" to "music",
            "집에서 보내는 휴가" to "home_vacation",
            "영화" to "movie",
            "레스토랑" to "restaurants",
            "해변" to "beach",
            "인터넷" to "internet",
            "산업,커리어" to "industry_career",
            "은행" to "bank",
            "교통" to "transportation",
            "패션" to "fashion",
            "가족,친구" to "family_friends",
            "가구" to "furniture",
            "예약" to "reservation",
            "명절" to "holiday"
        )
        val categories = categoryDisplayNames
        for (displayName in categoryDisplayNames) {
            val assetKey = categoryAssetMap[displayName] ?: displayName
            val fileName = "qa_${assetKey}.json"
            try {
                val assetList = context.assets.list("")?.toList() ?: emptyList()
                if (assetList.contains(fileName)) {
                    val inputStream = context.assets.open(fileName)
                    val reader = InputStreamReader(inputStream)
                    val type = object : TypeToken<List<QaItemAsset>>() {}.type
                    val assetItems: List<QaItemAsset> = gson.fromJson(reader, type)
                    val items = assetItems.map {
                        QaItem(
                            id = it.id ?: "",
                            category = displayName,
                            questionEn = it.question_en,
                            questionKo = it.question_ko,
                            answerEn = it.answer_en,
                            answerKo = it.answer_ko
                        )
                    }
                    itemsByCategory[displayName] = items
                    itemIndexByCategory[displayName] = 0
                } else {
                    // No asset file yet, initialize with empty list
                    itemsByCategory[displayName] = emptyList()
                    itemIndexByCategory[displayName] = 0
                }
            } catch (e: Exception) {
                itemsByCategory[displayName] = emptyList()
                itemIndexByCategory[displayName] = 0
            }
        }
        _uiState.value = _uiState.value.copy(categories = categories)
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

    private fun restoreLastCategory() {
        val lastCategory = prefs?.getString(PREF_KEY_LAST_CATEGORY, null)
        val lastIndex = prefs?.getInt(PREF_KEY_LAST_INDEX, 0) ?: 0
        if (lastCategory != null && itemsByCategory.containsKey(lastCategory)) {
            val items = itemsByCategory[lastCategory] ?: emptyList()
            val safeIndex = if (items.isNotEmpty()) lastIndex.coerceIn(0, items.size - 1) else 0
            itemIndexByCategory[lastCategory] = safeIndex
            if (items.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    currentQaItem = items[safeIndex],
                    currentCategory = lastCategory,
                    isLoading = false,
                    error = null
                )
                // 녹음 파일 존재 여부 확인
                checkRecordingFileExists()
            } else {
                _uiState.value = _uiState.value.copy(
                    currentQaItem = null,
                    currentCategory = lastCategory,
                    isLoading = false,
                    error = "해당 카테고리에 질문이 없습니다."
                )
            }
        }
    }

    fun selectCategory(category: String) {
        prefs?.edit()?.putString(PREF_KEY_LAST_CATEGORY, category)?.apply()
        val items = itemsByCategory[category] ?: emptyList()
        val index = itemIndexByCategory[category] ?: 0
        prefs?.edit()?.putInt(PREF_KEY_LAST_INDEX, index)?.apply()
        if (items.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                currentQaItem = items[index],
                currentCategory = category,
                isLoading = false,
                error = null
            )
            // 녹음 파일 존재 여부 확인
            checkRecordingFileExists()
        } else {
            _uiState.value = _uiState.value.copy(
                currentQaItem = null,
                currentCategory = category,
                isLoading = false,
                error = "해당 카테고리에 질문이 없습니다."
            )
        }
    }

    fun nextQaItem() {
        Log.d("MainViewModel", "nextQaItem called")
        val category = _uiState.value.currentCategory ?: run {
            Log.w("MainViewModel", "No current category")
            return
        }
        val items = itemsByCategory[category] ?: run {
            Log.w("MainViewModel", "No items for category: $category")
            return
        }
        if (items.isEmpty()) {
            Log.w("MainViewModel", "Items list is empty for category: $category")
            return
        }
        
        val currentIndex = itemIndexByCategory[category] ?: 0
        val nextIndex = (currentIndex + 1) % items.size
        
        Log.d("MainViewModel", "Moving from index $currentIndex to $nextIndex in category $category")
        
        itemIndexByCategory[category] = nextIndex
        prefs?.edit()?.putInt(PREF_KEY_LAST_INDEX, nextIndex)?.apply()
        _uiState.value = _uiState.value.copy(
            currentQaItem = items[nextIndex],
            isLoading = false,
            error = null
        )
        
        // 녹음 파일 존재 여부 확인
        checkRecordingFileExists()
        
        Log.d("MainViewModel", "Successfully moved to next question: ${items[nextIndex].questionEn.take(50)}...")
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getItemsInCategory(category: String): List<QaItem> {
        return itemsByCategory[category] ?: emptyList()
    }

    // 이전 질문으로 이동
    fun previousQaItem() {
        Log.d("MainViewModel", "previousQaItem called")
        val category = _uiState.value.currentCategory ?: run {
            Log.w("MainViewModel", "No current category")
            return
        }
        val items = itemsByCategory[category] ?: run {
            Log.w("MainViewModel", "No items for category: $category")
            return
        }
        if (items.isEmpty()) {
            Log.w("MainViewModel", "Items list is empty for category: $category")
            return
        }
        
        val currentIndex = itemIndexByCategory[category] ?: 0
        val previousIndex = if (currentIndex > 0) currentIndex - 1 else items.size - 1
        
        Log.d("MainViewModel", "Moving from index $currentIndex to $previousIndex in category $category")
        
        itemIndexByCategory[category] = previousIndex
        prefs?.edit()?.putInt(PREF_KEY_LAST_INDEX, previousIndex)?.apply()
        _uiState.value = _uiState.value.copy(
            currentQaItem = items[previousIndex],
            isLoading = false,
            error = null
        )
        
        // 녹음 파일 존재 여부 확인
        checkRecordingFileExists()
        
        Log.d("MainViewModel", "Successfully moved to previous question: ${items[previousIndex].questionEn.take(50)}...")
    }

    fun onMemorizeTestButtonClick(
        ttsPlayer: TtsPlayer,
        answerKo: String,
        answerEn: String,
        onHighlight: (Int?) -> Unit
    ) {
        Log.d("MainViewModel", "onMemorizeTestButtonClick 호출됨, level=${selectedMemorizeLevel.value}")
        
        // 이미 암기 테스트가 실행 중이면 종료
        if (_isMemorizeTestRunning.value) {
            ttsPlayer.stop()
            _isMemorizeTestRunning.value = false
            _questionHighlightIndex.value = null
            _answerHighlightIndex.value = null
            _answerKoHighlightIndex.value = null
            _recordingHighlightIndex.value = null
            setAnswerCardFlipped(false)
            Log.d("MainViewModel", "암기 테스트 중지됨")
            return
        }
        
        viewModelScope.launch {
            // 다른 재생 중지 및 하이라이트 초기화
            if (_isQuestionPlaying.value || _isAnswerPlaying.value) {
                ttsPlayer.stop()
                _isQuestionPlaying.value = false
                _isAnswerPlaying.value = false
                _questionHighlightIndex.value = null
                _answerHighlightIndex.value = null
                _answerKoHighlightIndex.value = null
                _recordingHighlightIndex.value = null
                Log.d("MainViewModel", "기존 재생 중지됨 (암기 테스트 시작)")
            }
            
            // 암기 테스트 시작 시 답변 카드를 한글 페이지로 뒤집기
            setAnswerCardFlipped(true)
            
            // 암기 테스트 실행 중 상태로 설정
            _isMemorizeTestRunning.value = true
            
            when (selectedMemorizeLevel.value) {
                "반복 듣기" -> {
                    Log.d("MainViewModel", "반복 듣기 UseCase 실행")
                    val useCase = RepeatListeningUseCase(
                        answerKo = answerKo,
                        answerEn = answerEn,
                        ttsPlayer = ttsPlayer,
                        onHighlight = onHighlight,
                        repeatCount = 5
                    )
                    useCase.execute()
                    
                    // 암기 테스트 완료 후 상태 초기화
                    _isMemorizeTestRunning.value = false
                    setAnswerCardFlipped(false)
                    _questionHighlightIndex.value = null
                    _answerHighlightIndex.value = null
                    _answerKoHighlightIndex.value = null
                    _recordingHighlightIndex.value = null
                    Log.d("MainViewModel", "반복 듣기 완료: 상태 초기화")
                }
                "영작 테스트" -> {
                    Log.d("MainViewModel", "영작 테스트 UseCase 실행")
                    if (audioRecorder == null || audioFileRepository == null) {
                        Log.e("MainViewModel", "audioRecorder 또는 audioFileRepository가 null입니다. 영작 테스트 실행 불가")
                        return@launch
                    }
                    val currentItem = _uiState.value.currentQaItem
                    val currentIndex = itemIndexByCategory[currentItem?.category] ?: 0
                    val scriptId = "${currentItem?.category}_$currentIndex"
                    
                    val useCase = EnglishWritingTestUseCase(
                        answerEn = answerEn,
                        answerKo = answerKo,
                        scriptId = scriptId,
                        ttsPlayer = ttsPlayer,
                        audioRecorder = audioRecorder!!,
                        audioFileRepository = audioFileRepository!!,
                        onAutoFlip = {
                            // 답변 카드를 한글 페이지로 뒤집기
                            setAnswerCardFlipped(true)
                            Log.d("MainViewModel", "영작 테스트: 답변 카드를 한글 페이지로 뒤집음")
                        },
                        onKoreanHighlight = { index ->
                            // 한글 하이라이트 설정
                            setAnswerKoHighlightIndex(index)
                            Log.d("MainViewModel", "영작 테스트: 한글 하이라이트 설정: $index")
                        },
                        onRecordingHighlight = { index ->
                            // 녹음 하이라이트 설정
                            setRecordingHighlightIndex(index)
                            Log.d("MainViewModel", "영작 테스트: 녹음 하이라이트 설정: $index")
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
                    setAnswerHighlightIndex(null)
                    setAnswerKoHighlightIndex(null)
                    setRecordingHighlightIndex(null)
                    Log.d("MainViewModel", "영작 테스트 완료: 답변 카드 상태 복원")
                    
                    // 오래된 녹음 파일들 정리
                    if (currentItem != null) {
                        audioFileRepository?.cleanupOldRecordings(scriptId, 1)
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

    data class QaItemAsset(
        val id: String? = null,
        val question_en: String,
        val question_ko: String,
        val answer_en: String,
        val answer_ko: String
    )
} 