package com.na982.opichelper.domain.entity

data class CategoryProgress(
    val category: String,
    val completedScripts: Int,
    val totalScripts: Int
) {
    val rate: Float get() = if (totalScripts > 0) completedScripts.toFloat() / totalScripts else 0f
}

data class StudyStatistics(
    val totalStudyDurationMs: Long = 0L,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletedScripts: Int = 0,
    val totalScripts: Int = 0,
    val completionRate: Float = 0f,
    val modeBreakdown: Map<String, Int> = emptyMap(),
    val dailyRecords: List<StudyDailyRecord> = emptyList(),
    val categoryProgress: List<CategoryProgress> = emptyList()
)
