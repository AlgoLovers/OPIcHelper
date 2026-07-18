package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.ScriptProgress
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.UserLevelPreferences
import com.na982.opichelper.domain.repository.PlaybackPreferences
import com.na982.opichelper.domain.repository.MemorizeLevelPreferences
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.usecase.ProgressCleanupUseCase
import com.na982.opichelper.domain.repository.QaSearch
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
    private val memorizeLevelPreferences: MemorizeLevelPreferences,
    private val progressTracker: MemorizeTestProgressTracker,
    private val qaSearch: QaSearch,
    private val progressCleanupUseCase: ProgressCleanupUseCase,
    private val appLogger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(QaBrowserState())
    val uiState: StateFlow<QaBrowserState> = _uiState.asStateFlow()

    private val _selectedMemorizeLevel = MutableStateFlow(MemorizeLevel.REPEAT_LISTENING.displayName)
    val selectedMemorizeLevel: StateFlow<String> = _selectedMemorizeLevel.asStateFlow()

    // 사용자가 암기 레벨을 "실제로 바꿨을 때만" 1회 발화하는 이벤트.
    // 값(selectedMemorizeLevel)을 LaunchedEffect(key=값)로 관찰하면 화면 재진입
    // (PiP 복귀, 설정/통계 왕복)마다 같은 값으로도 재실행되어 진행 중이던 재생이
    // 중단된다. 이벤트로 노출해 실제 변경 시에만 모드 리셋을 트리거한다.
    private val _levelChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val levelChanged: SharedFlow<Unit> = _levelChanged.asSharedFlow()

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
                progressCleanupUseCase.restoreProgress()
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
                qaDataManager.currentCategory,
                _selectedMemorizeLevel
            ) { progressMap, category, level ->
                if (category != null) {
                    val items = qaDataManager.getItemsInCategory(category)
                    val completed = items.indices.count { scriptIndex ->
                        val key = ScriptProgress.progressKey(category, scriptIndex, level)
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

    fun search(query: String): List<QaItem> {
        return qaSearch.searchItems(query)
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

    // 사용자 액션에 의한 레벨 변경 — 실제로 값이 바뀌면 levelChanged 이벤트를 발화한다.
    fun setSelectedMemorizeLevel(level: String) = applyMemorizeLevel(level, notify = true)

    private fun applyMemorizeLevel(level: String, notify: Boolean) {
        if (_selectedMemorizeLevel.value == level) return
        _selectedMemorizeLevel.update { level }
        _uiState.update { it.copy(selectedMemorizeLevel = level) }
        memorizeLevelPreferences.setMemorizeLevel(level)
        if (notify) _levelChanged.tryEmit(Unit)
    }

    fun getCurrentAnswer(qaItem: QaItem?): String {
        return qaDataManager.getCurrentAnswer(qaItem)
    }

    fun getCurrentAnswerKo(qaItem: QaItem?): String {
        return qaDataManager.getCurrentAnswerKo(qaItem)
    }

    fun getCurrentIndex(): Int = qaDataManager.getCurrentIndex()

    fun getCurrentUserLevel(): UserLevel = userLevelPreferences.getUserLevel()

    private fun loadMemorizeLevel() {
        // 앱 시작 시 저장된 레벨 복원은 "사용자 변경"이 아니므로 이벤트를 발화하지 않는다.
        val savedLevel = memorizeLevelPreferences.getMemorizeLevel()
        if (savedLevel.isNotEmpty()) {
            applyMemorizeLevel(savedLevel, notify = false)
        } else {
            applyMemorizeLevel(MemorizeLevel.REPEAT_LISTENING.displayName, notify = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        qaDataManager.reset()
    }
}
