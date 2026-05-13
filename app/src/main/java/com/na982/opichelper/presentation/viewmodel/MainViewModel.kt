package com.na982.opichelper.presentation.viewmodel

import android.util.Log
import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.usecase.PlayMergedFileUseCase
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppState(
    val currentQaItem: QaItem? = null,
    val currentCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList(),

    val memorizeLevels: List<String> = MemorizeLevel.allDisplayNames,
    val selectedMemorizeLevel: String = MemorizeLevel.REPEAT_LISTENING.displayName,

    val hasEnglishWritingTestMergedFile: Boolean = false,
    val isEnglishWritingTestMergedFilePlaying: Boolean = false,
    val englishWritingTestMergedFileHighlightIndex: Int? = null,

    val isPlaying: Boolean = false,
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    val questionHighlightIndex: Int? = null,
    val answerHighlightIndex: Int? = null,
    val answerKoHighlightIndex: Int? = null,
    val recordingHighlightIndex: Int? = null,

    val isAnswerCardFlipped: Boolean = false,
    val isQuestionCardFlipped: Boolean = false,

    val hasProgress: Boolean = false,
    val currentKoreanTtsService: String = "",
    val currentUserLevel: String = ""
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val qaDataManager: QaDataManager,
    private val ttsPlaybackController: TtsPlaybackController,
    private val progressTracker: MemorizeTestProgressTracker,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val playMergedFileUseCase: PlayMergedFileUseCase,
    private val ttsOrchestrator: TtsOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    val hasEnglishWritingTestMergedFile: StateFlow<Boolean> = playMergedFileUseCase.hasFile
    val isEnglishWritingTestMergedFilePlaying: StateFlow<Boolean> = playMergedFileUseCase.isPlaying
    val englishWritingTestMergedFileHighlightIndex: StateFlow<Int?> = playMergedFileUseCase.highlightIndex

    private val _isQuestionCardFlipped = MutableStateFlow(false)
    val isQuestionCardFlipped: StateFlow<Boolean> = _isQuestionCardFlipped.asStateFlow()

    init {
        initializeViewModel()
        setupStateCombination()
    }

    private fun initializeViewModel() {
        try {
            ttsPlaybackController.setTtsOrchestrator(ttsOrchestrator)

            loadMemorizeLevel()

            viewModelScope.launch {
                try {
                    qaDataManager.init()
                    progressTracker.restoreAllProgress()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "진행상황 복원 실패", e)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun setupStateCombination() {
        viewModelScope.launch {
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
            userPreferencesRepository.userLevel.collect { userLevel ->
                _uiState.value = _uiState.value.copy(currentUserLevel = userLevel.name)
            }
        }

        viewModelScope.launch {
            combine(
                ttsPlaybackController.isPlaying,
                ttsPlaybackController.isQuestionPlaying,
                ttsPlaybackController.isAnswerPlaying,
                ttsPlaybackController.questionHighlightIndex,
                ttsPlaybackController.answerHighlightIndex,
                ttsPlaybackController.answerKoHighlightIndex,
                ttsPlaybackController.recordingHighlightIndex
            ) { values ->
                _uiState.value = _uiState.value.copy(
                    isPlaying = values[0] as Boolean,
                    isQuestionPlaying = values[1] as Boolean,
                    isAnswerPlaying = values[2] as Boolean,
                    questionHighlightIndex = values[3] as Int?,
                    answerHighlightIndex = values[4] as Int?,
                    answerKoHighlightIndex = values[5] as Int?,
                    recordingHighlightIndex = values[6] as Int?
                )
            }.collect { }
        }

        viewModelScope.launch {
            progressTracker.hasProgress.collect { hasProgress ->
                _uiState.value = _uiState.value.copy(hasProgress = hasProgress)
            }
        }

        viewModelScope.launch {
            combine(
                playMergedFileUseCase.hasFile,
                playMergedFileUseCase.isPlaying,
                playMergedFileUseCase.highlightIndex
            ) { hasFile, isPlaying, highlightIndex ->
                _uiState.value = _uiState.value.copy(
                    hasEnglishWritingTestMergedFile = hasFile,
                    isEnglishWritingTestMergedFilePlaying = isPlaying,
                    englishWritingTestMergedFileHighlightIndex = highlightIndex
                )
            }.collect { }
        }
    }

    // ===== 카드 상태 =====

    fun setAnswerCardFlipped(isFlipped: Boolean) {
        _uiState.value = _uiState.value.copy(isAnswerCardFlipped = isFlipped)
    }

    fun setMergedAudioPlaying(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(isEnglishWritingTestMergedFilePlaying = isPlaying)
    }

    fun setQuestionCardFlipped(isFlipped: Boolean) {
        _isQuestionCardFlipped.value = isFlipped
    }

    fun setSelectedMemorizeLevel(level: String) {
        _uiState.value = _uiState.value.copy(selectedMemorizeLevel = level)
        userPreferencesRepository.setMemorizeLevel(level)
    }

    fun updateKoreanTtsServiceName(serviceName: String) {
        _uiState.value = _uiState.value.copy(currentKoreanTtsService = serviceName)
    }

    // ===== 영작테스트 병합 파일 재생 (PlayMergedFileUseCase에 위임) =====

    fun playEnglishWritingTestMergedFile() {
        playMergedFileUseCase.play()
    }

    fun stopEnglishWritingTestMergedFile() {
        playMergedFileUseCase.stop()
    }

    fun checkEnglishWritingTestMergedFile() {
        playMergedFileUseCase.checkFile()
    }

    // ===== QA 데이터 탐색 (QaDataManager에 위임) =====

    fun selectCategory(category: String) {
        viewModelScope.launch { qaDataManager.selectCategory(category) }
    }

    fun nextQaItem() {
        viewModelScope.launch { qaDataManager.nextQaItem() }
    }

    fun previousQaItem() {
        viewModelScope.launch { qaDataManager.previousQaItem() }
    }

    fun clearError() {
        qaDataManager.clearError()
    }

    fun getItemsInCategory(category: String): List<QaItem> {
        return qaDataManager.getItemsInCategory(category)
    }

    // ===== TTS 재생 제어 =====

    fun playQuestion(question: String) {
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopAllTts()
            ttsPlaybackController.playQuestion(question)
        }
    }

    fun playAnswer(answer: String) {
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopAllTts()
            ttsPlaybackController.playAnswer(answer)
        }
    }

    fun stopCurrentTts() {
        viewModelScope.launch { ttsPlaybackController.stopAllTts() }
    }

    fun stopAllTts() {
        viewModelScope.launch {
            ttsPlaybackController.stopAllTts()
            playMergedFileUseCase.stop()
            ttsPlaybackController.clearHighlight()
        }
    }

    fun cleanupAllTts() {
        viewModelScope.launch {
            ttsPlaybackController.cleanupTts()
        }
    }

    fun cleanupAllTtsSync() {
        try {
            ttsPlaybackController.forceStopTts()
            ttsPlaybackController.clearHighlight()
            playMergedFileUseCase.stop()
        } catch (_: Exception) {
        }
    }

    // ===== 앱 생명주기 =====

    override fun onCleared() {
        super.onCleared()
        ttsPlaybackController.cleanupTts()
        playMergedFileUseCase.stop()
        playMergedFileUseCase.release()
        qaDataManager.release()
    }

    suspend fun cleanupOnAppExit() {
        try {
            val selectedMemorizeLevel = _uiState.value.selectedMemorizeLevel

            val currentItem = qaDataManager.getCurrentQaItem()
            if (currentItem != null) {
                val answerText = getCurrentAnswer(currentItem)
                val totalSentences = answerText.split(".").size

                val currentProgress = progressTracker.getScriptProgress(currentItem.category, qaDataManager.getCurrentIndex(), selectedMemorizeLevel)
                val currentSentenceIndex = currentProgress?.currentSentenceIndex ?: 0

                progressTracker.updateProgress(
                    category = currentItem.category,
                    scriptIndex = qaDataManager.getCurrentIndex(),
                    memorizeLevel = selectedMemorizeLevel,
                    currentSentenceIndex = currentSentenceIndex,
                    totalSentences = totalSentences,
                    isMemorizeTestRunning = false
                )
            }

            qaDataManager.saveCurrentIndex(qaDataManager.getCurrentIndex())

            setAnswerCardFlipped(false)
            setMergedAudioPlaying(false)
        } catch (e: Exception) {
            Log.e("MainViewModel", "앱 종료 시 리소스 정리 중 오류", e)
        }

        try {
            progressTracker.persistChangedProgress()
        } catch (e: Exception) {
            Log.e("MainViewModel", "진행상황 저장 실패", e)
        }
    }

    fun onBackgroundMove() {
        viewModelScope.launch {
            try {
                if (_uiState.value.isPlaying) {
                    ttsPlaybackController.pauseTts()
                }
                ttsPlaybackController.clearHighlight()
            } catch (e: Exception) {
                Log.e("MainViewModel", "백그라운드 이동 처리 실패", e)
            }
        }
    }

    fun onForegroundReturn() {
        viewModelScope.launch {
            try {
                if (_uiState.value.isPlaying) {
                    ttsPlaybackController.resumeTts()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "포그라운드 복귀 처리 실패", e)
            }
        }
    }

    // ===== 헬퍼 =====

    private fun loadMemorizeLevel() {
        val savedLevel = userPreferencesRepository.getMemorizeLevel()
        if (savedLevel.isNotEmpty()) {
            setSelectedMemorizeLevel(savedLevel)
        } else {
            setSelectedMemorizeLevel(MemorizeLevel.REPEAT_LISTENING.displayName)
        }
    }

    fun getCurrentAnswer(qaItem: QaItem?): String {
        return qaDataManager.getCurrentAnswer(qaItem)
    }

    fun getCurrentAnswerKo(qaItem: QaItem?): String {
        return qaDataManager.getCurrentAnswerKo(qaItem)
    }

    fun setUserLevel(level: UserLevel) {
        viewModelScope.launch {
            userPreferencesRepository.setUserLevel(level)
        }
    }
}
