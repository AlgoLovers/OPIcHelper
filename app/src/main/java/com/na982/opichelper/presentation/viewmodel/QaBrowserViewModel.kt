package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.UserLevelPreferences
import com.na982.opichelper.domain.repository.PlaybackPreferences
import com.na982.opichelper.domain.repository.OnboardingPreferences
import com.na982.opichelper.domain.repository.MemorizeLevelPreferences
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.usecase.SearchQaItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.manager.AppLogger
import javax.inject.Inject

data class QaBrowserState(
    val currentQaItem: QaItem? = null,
    val currentCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList(),
    val selectedMemorizeLevel: String = MemorizeLevel.REPEAT_LISTENING.displayName,
    val currentUserLevel: String = "",
    val answerPlayCount: Int = 1,
    val completedCount: Int = 0
)

@HiltViewModel
class QaBrowserViewModel @Inject constructor(
    private val qaDataManager: QaDataManager,
    private val userLevelPreferences: UserLevelPreferences,
    private val playbackPreferences: PlaybackPreferences,
    private val onboardingPreferences: OnboardingPreferences,
    private val memorizeLevelPreferences: MemorizeLevelPreferences,
    private val progressTracker: MemorizeTestProgressTracker,
    private val searchQaItemsUseCase: SearchQaItemsUseCase,
    private val appLogger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(QaBrowserState())
    val uiState: StateFlow<QaBrowserState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private suspend fun emitEvent(message: String) {
        _events.emit(message)
    }

    init {
        initQaData()
        setupStateCombination()
    }

    private fun initQaData() {
        viewModelScope.launch {
            try {
                qaDataManager.init()
                progressTracker.restoreAllProgress()
            } catch (e: Exception) {
                appLogger.e("QaBrowserViewModel", "데이터 초기화 실패", e)
                emitEvent("데이터를 불러올 수 없습니다")
            }
        }
        loadMemorizeLevel()
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
                _uiState.update { it.copy(
                    currentQaItem = currentQaItem,
                    currentCategory = currentCategory,
                    categories = categories,
                    isLoading = isLoading,
                    error = error
                ) }
            }.collect { }
        }

        viewModelScope.launch {
            userLevelPreferences.userLevel.collect { userLevel ->
                _uiState.update { it.copy(currentUserLevel = userLevel.name) }
            }
        }

        viewModelScope.launch {
            playbackPreferences.answerPlayCount.collect { count ->
                _uiState.update { it.copy(answerPlayCount = count) }
            }
        }

        viewModelScope.launch {
            combine(
                progressTracker.progressMap,
                qaDataManager.currentCategory
            ) { progressMap, category ->
                if (category != null) {
                    val level = _uiState.value.selectedMemorizeLevel
                    val items = qaDataManager.getItemsInCategory(category)
                    val completed = items.indices.count { scriptIndex ->
                        val key = "${category}_${scriptIndex}_${level}"
                        val progress = progressMap[key]
                        progress != null && !progress.isMemorizeTestRunning && progress.currentSentenceIndex >= progress.totalSentences - 1
                    }
                    _uiState.update { it.copy(completedCount = completed) }
                }
            }.collect { }
        }
    }

    fun selectCategory(category: String) {
        viewModelScope.launch { qaDataManager.selectCategory(category) }
    }

    fun nextQaItem() {
        viewModelScope.launch { qaDataManager.nextQaItem() }
    }

    fun hasNextQaItem(): Boolean = qaDataManager.hasNextQaItem()

    suspend fun nextQaItemSync(): QaItem? {
        if (!qaDataManager.hasNextQaItem()) return null
        qaDataManager.nextQaItem()
        return qaDataManager.currentQaItem.value
    }

    fun previousQaItem() {
        viewModelScope.launch { qaDataManager.previousQaItem() }
    }

    fun clearError() {
        qaDataManager.clearError()
    }

    fun isOnboardingCompleted(): Boolean {
        return onboardingPreferences.isOnboardingCompleted()
    }

    fun setOnboardingCompleted() {
        onboardingPreferences.setOnboardingCompleted()
    }

    fun isPipGuideCompleted(): Boolean {
        return onboardingPreferences.isPipGuideCompleted()
    }

    fun setPipGuideCompleted() {
        onboardingPreferences.setPipGuideCompleted()
    }

    fun search(query: String): List<QaItem> {
        return searchQaItemsUseCase.search(query)
    }

    suspend fun navigateToItem(item: QaItem) {
        qaDataManager.selectCategory(item.category)
        val items = qaDataManager.getItemsInCategory(item.category)
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            qaDataManager.navigateToIndex(index)
        }
    }

    fun getItemsInCategory(category: String): List<QaItem> {
        return qaDataManager.getItemsInCategory(category)
    }

    fun setSelectedMemorizeLevel(level: String) {
        _uiState.update { it.copy(selectedMemorizeLevel = level) }
        memorizeLevelPreferences.setMemorizeLevel(level)
        refreshCompletedCount()
    }

    private fun refreshCompletedCount() {
        val category = _uiState.value.currentCategory ?: return
        val level = _uiState.value.selectedMemorizeLevel
        val items = qaDataManager.getItemsInCategory(category)
        val progressMap = progressTracker.progressMap.value
        val completed = items.indices.count { scriptIndex ->
            val key = "${category}_${scriptIndex}_${level}"
            val progress = progressMap[key]
            progress != null && !progress.isMemorizeTestRunning && progress.currentSentenceIndex >= progress.totalSentences - 1
        }
        _uiState.update { it.copy(completedCount = completed) }
    }

    fun getCurrentAnswer(qaItem: QaItem?): String {
        return qaDataManager.getCurrentAnswer(qaItem)
    }

    fun getCurrentAnswerKo(qaItem: QaItem?): String {
        return qaDataManager.getCurrentAnswerKo(qaItem)
    }

    fun getCurrentIndex(): Int = qaDataManager.getCurrentIndex()

    fun getCurrentUserLevel(): UserLevel = userLevelPreferences.getUserLevel()

    suspend fun cleanupOnAppExit() {
        try {
            val selectedMemorizeLevel = _uiState.value.selectedMemorizeLevel
            val currentItem = qaDataManager.getCurrentQaItem()
            if (currentItem != null) {
                val answerText = getCurrentAnswer(currentItem)
                val totalSentences = SentenceSplitter.split(answerText).size
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

        } catch (e: Exception) {
            appLogger.e("QaBrowserViewModel", "앱 종료 시 리소스 정리 중 오류", e)
        }

        try {
            progressTracker.persistChangedProgress()
        } catch (e: Exception) {
            appLogger.e("QaBrowserViewModel", "진행상황 저장 실패", e)
        }
    }

    private fun loadMemorizeLevel() {
        val savedLevel = memorizeLevelPreferences.getMemorizeLevel()
        if (savedLevel.isNotEmpty()) {
            setSelectedMemorizeLevel(savedLevel)
        } else {
            setSelectedMemorizeLevel(MemorizeLevel.REPEAT_LISTENING.displayName)
        }
    }

    override fun onCleared() {
        super.onCleared()
        qaDataManager.release()
    }
}
