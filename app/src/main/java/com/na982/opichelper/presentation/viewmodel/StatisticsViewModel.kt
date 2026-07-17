package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.na982.opichelper.domain.usecase.StudyStatisticsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            try {
            // calculate()는 SharedPreferences+Gson을 최대 366회 순회하는 무거운 동기 IO다.
            // 메인 스레드에서 돌리면 통계 화면 진입 시 프레임 드랍/ANR이 발생하므로 IO로 옮긴다.
            val statistics = withContext(Dispatchers.IO) { studyStatisticsCalculator.calculate() }
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
            } catch (e: Exception) {
                // 저장된 통계 데이터가 손상된 경우(예: SharedPreferences JSON 파손)
                // 통계 화면 진입만으로 앱이 크래시하지 않도록 방어한다. 로딩 스피너는 해제한다.
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
