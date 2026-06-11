package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.entity.CategoryProgress
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.StudyDailyRecord
import com.na982.opichelper.domain.entity.StudyStatistics
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.StudySessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyStatisticsCalculator @Inject constructor(
    private val studySessionRepository: StudySessionRepository,
    private val progressTracker: MemorizeTestProgressTracker,
    private val qaDataManager: QaDataManager
) {
    companion object {
        private const val DAILY_RECORD_DAYS = 7
    }

    fun calculate(): StudyStatistics {
        val progressMap = progressTracker.progressMap.value
        val categories = qaDataManager.categories.value

        val totalCompletedScripts = countCompletedScripts(progressMap)
        val totalScripts = countTotalScripts(categories, qaDataManager)
        val modeBreakdown = buildModeBreakdown(progressMap)
        val categoryProgress = buildCategoryProgress(categories, progressMap, qaDataManager)

        return StudyStatistics(
            totalStudyDurationMs = studySessionRepository.getTotalStudyDurationMs(),
            streak = studySessionRepository.getStreak(),
            longestStreak = studySessionRepository.getLongestStreak(),
            totalCompletedScripts = totalCompletedScripts,
            totalScripts = totalScripts,
            completionRate = if (totalScripts > 0) totalCompletedScripts.toFloat() / totalScripts else 0f,
            modeBreakdown = modeBreakdown,
            dailyRecords = studySessionRepository.getDailyRecords(DAILY_RECORD_DAYS),
            categoryProgress = categoryProgress
        )
    }

    private fun countCompletedScripts(progressMap: Map<String, *>): Int {
        return progressMap.values.count { progress ->
            progress is com.na982.opichelper.domain.entity.ScriptProgress &&
                !progress.isMemorizeTestRunning &&
                progress.currentSentenceIndex >= progress.totalSentences - 1
        }
    }

    private fun countTotalScripts(categories: List<String>, qaDataManager: QaDataManager): Int {
        return categories.sumOf { category ->
            qaDataManager.getItemsInCategory(category).size * MemorizeLevel.entries.size
        }
    }

    private fun buildModeBreakdown(progressMap: Map<String, *>): Map<String, Int> {
        val breakdown = mutableMapOf<String, Int>()
        MemorizeLevel.entries.forEach { level ->
            breakdown[level.displayName] = progressMap.values.count { progress ->
                progress is com.na982.opichelper.domain.entity.ScriptProgress &&
                    progress.memorizeLevel == level.displayName &&
                    !progress.isMemorizeTestRunning &&
                    progress.currentSentenceIndex >= progress.totalSentences - 1
            }
        }
        return breakdown
    }

    private fun buildCategoryProgress(
        categories: List<String>,
        progressMap: Map<String, *>,
        qaDataManager: QaDataManager
    ): List<CategoryProgress> {
        return categories.map { category ->
            val items = qaDataManager.getItemsInCategory(category)
            val completed = items.indices.sumOf { scriptIndex ->
                MemorizeLevel.entries.count { level ->
                    val key = com.na982.opichelper.domain.entity.ScriptProgress.progressKey(
                        category, scriptIndex, level.displayName
                    )
                    val progress = progressMap[key] as? com.na982.opichelper.domain.entity.ScriptProgress
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
