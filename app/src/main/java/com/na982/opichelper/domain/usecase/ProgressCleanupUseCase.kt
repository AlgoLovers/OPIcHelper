package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.manager.AppLogger
import com.na982.opichelper.domain.repository.MemorizeLevelPreferences
import com.na982.opichelper.domain.repository.QaContentReader
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProgressCleanupUseCase @Inject constructor(
    private val qaContentReader: QaContentReader,
    private val memorizeLevelPreferences: MemorizeLevelPreferences,
    private val progressTracker: MemorizeTestProgressTracker,
    private val appLogger: AppLogger
) {
    suspend fun cleanupOnExit() = withContext(NonCancellable) {
        try {
            val selectedMemorizeLevel = memorizeLevelPreferences.getMemorizeLevel()
            val currentItem = qaContentReader.getCurrentQaItem()
            if (currentItem != null) {
                val answerText = qaContentReader.getCurrentAnswer(currentItem)
                val totalSentences = SentenceSplitter.split(answerText).size
                val currentProgress = progressTracker.getScriptProgress(
                    currentItem.category, qaContentReader.getCurrentIndex(), selectedMemorizeLevel
                )
                val currentSentenceIndex = currentProgress?.currentSentenceIndex ?: 0

                progressTracker.updateProgress(
                    category = currentItem.category,
                    scriptIndex = qaContentReader.getCurrentIndex(),
                    memorizeLevel = selectedMemorizeLevel,
                    currentSentenceIndex = currentSentenceIndex,
                    totalSentences = totalSentences,
                    isMemorizeTestRunning = false
                )
            }
        } catch (e: Exception) {
            appLogger.e("ProgressCleanupUseCase", "앱 종료 시 리소스 정리 중 오류", e)
        }

        try {
            progressTracker.persistChangedProgress()
        } catch (e: Exception) {
            appLogger.e("ProgressCleanupUseCase", "진행상황 저장 실패", e)
        }
    }

    suspend fun restoreProgress() {
        try {
            progressTracker.restoreAllProgress()
        } catch (e: Exception) {
            appLogger.e("ProgressCleanupUseCase", "진행상황 복원 실패", e)
        }
    }
}
