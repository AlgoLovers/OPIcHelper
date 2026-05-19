package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.TestProgressData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class BaseMemorizeTestRepository(
    protected val progressPersistenceService: ProgressPersistenceService
) {
    private val _events = MutableSharedFlow<MemorizeTestEvent>(
        extraBufferCapacity = 1
    )
    val events: SharedFlow<MemorizeTestEvent> = _events.asSharedFlow()

    protected suspend fun emit(event: MemorizeTestEvent) {
        _events.emit(event)
    }

    protected abstract val memorizeLevel: MemorizeLevel

    protected fun splitSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    protected suspend fun resolveStartIndex(category: String, scriptIndex: Int, totalCount: Int): Int {
        val navState = progressPersistenceService.loadNavigationState()
        return if (navState.category == category
            && navState.scriptIndex == scriptIndex
            && navState.sentenceIndex in 0 until totalCount
        ) {
            navState.sentenceIndex
        } else {
            0
        }
    }

    suspend fun getCurrentProgress(category: String, scriptIndex: Int): TestProgressData? {
        val navState = progressPersistenceService.loadNavigationState()
        return if (navState.category == category && navState.scriptIndex == scriptIndex) {
            TestProgressData(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = memorizeLevel.displayName,
                currentSentenceIndex = navState.sentenceIndex,
                totalSentences = 0,
                isMemorizeTestRunning = false
            )
        } else null
    }

    suspend fun updateProgress(progressData: TestProgressData) {
        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(progressData.category, progressData.scriptIndex, progressData.currentSentenceIndex)
        )
    }

    suspend fun clearProgress(category: String, scriptIndex: Int) {
        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(category, scriptIndex, 0)
        )
    }
}
