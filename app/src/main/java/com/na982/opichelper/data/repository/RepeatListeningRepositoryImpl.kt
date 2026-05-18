package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import com.na982.opichelper.domain.repository.ProgressData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepeatListeningRepositoryImpl @Inject constructor(
    private val ttsOrchestrator: TtsOrchestrator,
    private val progressPersistenceService: ProgressPersistenceService,
    private val recordingTimeManager: RecordingTimeManager
) : RepeatListeningRepository {

    private val _events = MutableSharedFlow<MemorizeTestEvent>(
        extraBufferCapacity = 1
    )
    override val events: SharedFlow<MemorizeTestEvent> = _events.asSharedFlow()

    private suspend fun emit(event: MemorizeTestEvent) {
        _events.emit(event)
    }

    override suspend fun executeRepeatListening(
        data: RepeatListeningData,
        repeatCount: Int
    ) {
        val koSentences = data.koreanAnswer.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val enSentences = data.englishAnswer.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val count = minOf(koSentences.size, enSentences.size)

        val navState = progressPersistenceService.loadNavigationState()
        val startIndex = if (navState.category == data.category && navState.index in 0 until count) {
            navState.index
        } else {
            0
        }

        for (i in startIndex until count) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) break

            progressPersistenceService.saveNavigationState(
                ProgressPersistenceService.NavigationState(data.category, i)
            )

            // 1. 한글 문장 TTS
            emit(MemorizeTestEvent.CardFlip(true))
            delay(100)
            emit(MemorizeTestEvent.KoreanHighlight(i))

            ttsOrchestrator.speakAndWaitForCompletion(koSentences[i], isKorean = true, rate = 1.0f)

            val enSentence = enSentences[i]
            val enWordCount = enSentence.split("\\s+".toRegex()).size
            val baseDelay = enWordCount * 500
            val lengthMultiplier = when {
                enWordCount <= 5 -> 1.5f
                enWordCount <= 10 -> 1.2f
                enWordCount <= 15 -> 1.0f
                else -> 0.8f
            }
            val adaptiveDelay = (baseDelay * lengthMultiplier).toLong()
            kotlinx.coroutines.delay(adaptiveDelay)

            if (!kotlinx.coroutines.currentCoroutineContext().isActive) break

            // 2. 영문 문장 N회 TTS
            for (j in 1..repeatCount) {
                if (!kotlinx.coroutines.currentCoroutineContext().isActive) break

                emit(MemorizeTestEvent.CardFlip(false))
                delay(100)
                emit(MemorizeTestEvent.Highlight(i))

                val enDuration = ttsOrchestrator.speakAndWaitForCompletion(enSentences[i], isKorean = false, rate = 1.0f)

                if (j == 1) {
                    recordingTimeManager.saveRecordingTime(data.category, data.scriptIndex, i, enDuration)
                }

                if (!kotlinx.coroutines.currentCoroutineContext().isActive) break

                val restTime = (enDuration * 1.2).toLong()
                delay(restTime)
            }
            emit(MemorizeTestEvent.Highlight(null))
        }

        emit(MemorizeTestEvent.CardFlip(false))
        emit(MemorizeTestEvent.Highlight(null))
        emit(MemorizeTestEvent.Completed)
    }

    override suspend fun getCurrentProgress(category: String, scriptIndex: Int): ProgressData? {
        val navState = progressPersistenceService.loadNavigationState()
        return if (navState.category == category) {
            ProgressData(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = MemorizeLevel.REPEAT_LISTENING.displayName,
                currentSentenceIndex = navState.index,
                totalSentences = 0,
                isMemorizeTestRunning = false
            )
        } else null
    }

    override suspend fun updateProgress(progressData: ProgressData) {
        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(progressData.category, progressData.currentSentenceIndex)
        )
    }

    override suspend fun clearProgress(category: String, scriptIndex: Int) {
        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(category, 0)
        )
    }
}
