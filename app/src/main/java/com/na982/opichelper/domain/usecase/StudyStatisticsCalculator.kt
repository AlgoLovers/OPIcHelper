package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.entity.CategoryProgress
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.ScriptProgress
import com.na982.opichelper.domain.entity.StudyStatistics
import com.na982.opichelper.domain.repository.QaContentReader
import com.na982.opichelper.domain.repository.StudySessionStatisticsReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyStatisticsCalculator @Inject constructor(
    private val studySessionStatisticsReader: StudySessionStatisticsReader,
    private val progressTracker: MemorizeTestProgressTracker,
    private val qaContentReader: QaContentReader
) {
    companion object {
        private const val DAILY_RECORD_DAYS = 7
    }

    fun calculate(): StudyStatistics {
        val progressMap = progressTracker.progressMap.value
        val categories = qaContentReader.getCategories()

        val totalCompletedScripts = countCompletedScripts(progressMap)
        val totalScripts = countTotalScripts(categories)
        val modeBreakdown = buildModeBreakdown(progressMap)
        val categoryProgress = buildCategoryProgress(categories, progressMap)

        return StudyStatistics(
            totalStudyDurationMs = studySessionStatisticsReader.getTotalStudyDurationMs(),
            streak = studySessionStatisticsReader.getStreak(),
            longestStreak = studySessionStatisticsReader.getLongestStreak(),
            totalCompletedScripts = totalCompletedScripts,
            totalScripts = totalScripts,
            modeBreakdown = modeBreakdown,
            dailyRecords = studySessionStatisticsReader.getDailyRecords(DAILY_RECORD_DAYS),
            categoryProgress = categoryProgress
        )
    }

    private fun countCompletedScripts(progressMap: Map<String, ScriptProgress>): Int {
        return progressMap.values.count { progress ->
            !progress.isMemorizeTestRunning &&
                progress.currentSentenceIndex >= progress.totalSentences - 1
        }
    }

    private fun countTotalScripts(categories: List<String>): Int {
        return categories.sumOf { category ->
            qaContentReader.getItemsInCategory(category).size * MemorizeLevel.entries.size
        }
    }

    private fun buildModeBreakdown(progressMap: Map<String, ScriptProgress>): Map<String, Int> {
        val breakdown = mutableMapOf<String, Int>()
        MemorizeLevel.entries.forEach { level ->
            breakdown[level.displayName] = progressMap.values.count { progress ->
                progress.memorizeLevel == level.displayName &&
                    !progress.isMemorizeTestRunning &&
                    progress.currentSentenceIndex >= progress.totalSentences - 1
            }
        }
        return breakdown
    }

    private fun buildCategoryProgress(
        categories: List<String>,
        progressMap: Map<String, ScriptProgress>
    ): List<CategoryProgress> {
        return categories.map { category ->
            val items = qaContentReader.getItemsInCategory(category)
            val completed = items.indices.sumOf { scriptIndex ->
                MemorizeLevel.entries.count { level ->
                    val key = ScriptProgress.progressKey(category, scriptIndex, level.displayName)
                    val progress = progressMap[key]
                    progress != null && !progress.isMemorizeTestRunning &&
                        progress.currentSentenceIndex >= progress.totalSentences - 1
                }
            }
            CategoryProgress(
                category = category,
                completedScripts = completed,
                totalScripts = items.size * MemorizeLevel.entries.size
            )
        }
    }
}
