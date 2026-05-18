package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import javax.inject.Inject

data class QaBrowserState(
    val currentQaItem: QaItem? = null,
    val currentCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList(),
    val selectedMemorizeLevel: String = MemorizeLevel.REPEAT_LISTENING.displayName,
    val currentUserLevel: String = ""
)

@HiltViewModel
class QaBrowserViewModel @Inject constructor(
    private val qaDataManager: QaDataManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val progressTracker: MemorizeTestProgressTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(QaBrowserState())
    val uiState: StateFlow<QaBrowserState> = _uiState.asStateFlow()

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
                Log.e("QaBrowserViewModel", "데이터 초기화 실패", e)
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
    }

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

    fun setSelectedMemorizeLevel(level: String) {
        _uiState.value = _uiState.value.copy(selectedMemorizeLevel = level)
        userPreferencesRepository.setMemorizeLevel(level)
    }

    fun getCurrentAnswer(qaItem: QaItem?): String {
        return qaDataManager.getCurrentAnswer(qaItem)
    }

    fun getCurrentAnswerKo(qaItem: QaItem?): String {
        return qaDataManager.getCurrentAnswerKo(qaItem)
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

        } catch (e: Exception) {
            Log.e("QaBrowserViewModel", "앱 종료 시 리소스 정리 중 오류", e)
        }

        try {
            progressTracker.persistChangedProgress()
        } catch (e: Exception) {
            Log.e("QaBrowserViewModel", "진행상황 저장 실패", e)
        }
    }

    private fun loadMemorizeLevel() {
        val savedLevel = userPreferencesRepository.getMemorizeLevel()
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
