package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.na982.opichelper.domain.entity.StudyStatistics
import com.na982.opichelper.domain.usecase.StudyStatisticsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

data class StatisticsUiState(
    val totalStudyDurationMs: Long = 0L,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletedScripts: Int = 0,
    val totalScripts: Int = 0,
    val completionRate: Float = 0f,
    val modeBreakdown: Map<String, Int> = emptyMap(),
    val dailyRecords: List<com.na982.opichelper.domain.entity.StudyDailyRecord> = emptyList(),
    val categoryProgress: List<com.na982.opichelper.domain.entity.CategoryProgress> = emptyList()
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
        viewModelScope.launch {
            val statistics = studyStatisticsCalculator.calculate()
            _uiState.update {
                StatisticsUiState(
                    totalStudyDurationMs = statistics.totalStudyDurationMs,
                    streak = statistics.streak,
                    longestStreak = statistics.longestStreak,
                    totalCompletedScripts = statistics.totalCompletedScripts,
                    totalScripts = statistics.totalScripts,
                    completionRate = statistics.completionRate,
                    modeBreakdown = statistics.modeBreakdown,
                    dailyRecords = statistics.dailyRecords,
                    categoryProgress = statistics.categoryProgress
                )
            }
        }
    }
}
