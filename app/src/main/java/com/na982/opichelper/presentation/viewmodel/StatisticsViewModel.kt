package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.na982.opichelper.domain.usecase.StudyStatisticsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

data class DailyRecordUiModel(
    val date: String,
    val studyDurationMs: Long,
    val completedScripts: Int
)

data class CategoryProgressUiModel(
    val category: String,
    val completedScripts: Int,
    val totalScripts: Int,
    val rate: Float
)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val totalStudyDurationMs: Long = 0L,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletedScripts: Int = 0,
    val totalScripts: Int = 0,
    val completionRate: Float = 0f,
    val modeBreakdown: Map<String, Int> = emptyMap(),
    val dailyRecords: List<DailyRecordUiModel> = emptyList(),
    val categoryProgress: List<CategoryProgressUiModel> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val studyStatisticsCalculator: StudyStatisticsCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val statistics = studyStatisticsCalculator.calculate()
            _uiState.update {
                StatisticsUiState(
                    isLoading = false,
                    totalStudyDurationMs = statistics.totalStudyDurationMs,
                    streak = statistics.streak,
                    longestStreak = statistics.longestStreak,
                    totalCompletedScripts = statistics.totalCompletedScripts,
                    totalScripts = statistics.totalScripts,
                    completionRate = statistics.completionRate,
                    modeBreakdown = statistics.modeBreakdown,
                    dailyRecords = statistics.dailyRecords.map { record ->
                        DailyRecordUiModel(
                            date = record.date,
                            studyDurationMs = record.studyDurationMs,
                            completedScripts = record.completedScripts
                        )
                    },
                    categoryProgress = statistics.categoryProgress.map { progress ->
                        CategoryProgressUiModel(
                            category = progress.category,
                            completedScripts = progress.completedScripts,
                            totalScripts = progress.totalScripts,
                            rate = progress.rate
                        )
                    }
                )
            }
        }
    }
}
