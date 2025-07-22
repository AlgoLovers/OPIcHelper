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

data class MainUiState(
    val currentQaItem: QaItem? = null,
    val currentCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList(),
    val memorizeLevels: List<String> = emptyList(),
    val selectedMemorizeLevel: String = "",
    val hasMergedAudioFile: Boolean = false // 병합된 오디오 파일 존재 여부 추가
)

class MainViewModel : AndroidViewModel {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val itemsByCategory: MutableMap<String, List<QaItem>> = mutableMapOf()
    private val itemIndexByCategory: MutableMap<String, Int> = mutableMapOf()

    private var prefs: SharedPreferences? = null
    private val PREF_KEY_LAST_CATEGORY = "last_category"
    private val PREF_KEY_LAST_INDEX = "last_index"

    private val _questionHighlightIndex = MutableStateFlow<Int?>(null)
    val questionHighlightIndex: StateFlow<Int?> = _questionHighlightIndex
    private val _answerHighlightIndex = MutableStateFlow<Int?>(null)
    val answerHighlightIndex: StateFlow<Int?> = _answerHighlightIndex

    private val _memorizeLevels = MutableStateFlow<List<String>>(emptyList())
    val memorizeLevels: StateFlow<List<String>> = _memorizeLevels

    private val _selectedMemorizeLevel = MutableStateFlow("")
    val selectedMemorizeLevel: StateFlow<String> = _selectedMemorizeLevel

    fun setMemorizeLevel(level: String) {
        _selectedMemorizeLevel.value = level
        _uiState.value = _uiState.value.copy(selectedMemorizeLevel = level)
    }

    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null
    private var audioFileRepository: AudioFileRepository? = null
    private var currentRecordingFile: java.io.File? = null
    private var onMergedAudioStateChange: ((Boolean) -> Unit)? = null // 병합된 오디오 재생 상태 콜백

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
        getCurrentMergedAudioFile()?.let { file ->
            onMergedAudioStateChange?.invoke(true) // 재생 시작
            audioPlayer?.play(file) {
                Log.d("MainViewModel", "병합된 오디오 파일 재생 완료")
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
        if (_selectedMemorizeLevel.value.isEmpty() && levels.isNotEmpty()) {
            setMemorizeLevel(levels[0])
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
        
        Log.d("MainViewModel", "Successfully moved to previous question: ${items[previousIndex].questionEn.take(50)}...")
    }

    fun onMemorizeTestButtonClick(
        ttsPlayer: TtsPlayer,
        answerKo: String,
        answerEn: String,
        onHighlight: (Int?) -> Unit
    ) {
        Log.d("MainViewModel", "onMemorizeTestButtonClick 호출됨, level=${selectedMemorizeLevel.value}")
        viewModelScope.launch {
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
                }
                "영작 테스트" -> {
                    Log.d("MainViewModel", "영작 테스트 UseCase 실행")
                    if (audioRecorder == null || audioFileRepository == null) {
                        Log.e("MainViewModel", "audioRecorder 또는 audioFileRepository가 null입니다. 영작 테스트 실행 불가")
                        return@launch
                    }
                    val useCase = EnglishWritingTestUseCase(
                        answerEn = answerEn,
                        answerKo = answerKo,
                        ttsPlayer = ttsPlayer,
                        audioRecorder = audioRecorder!!,
                        audioFileRepository = audioFileRepository!!,
                        onAutoFlip = null, // 필요시 콜백 구현
                        onMergedFileCreated = { mergedFile ->
                            Log.d("MainViewModel", "병합된 오디오 파일 생성됨: ${mergedFile.absolutePath}")
                            updateMergedAudioFileStatus() // UI 상태 업데이트
                        }
                    )
                    useCase.execute()
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