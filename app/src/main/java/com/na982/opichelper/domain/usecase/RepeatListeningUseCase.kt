package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepeatListeningUseCase @Inject constructor(
    private val ttsOrchestrator: TtsOrchestrator,
    private val progressPersistenceService: ProgressPersistenceService,
    private val recordingTimeManager: RecordingTimeManager
) {
    private val _events = MutableSharedFlow<MemorizeTestEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MemorizeTestEvent> = _events.asSharedFlow()

    private suspend fun emit(event: MemorizeTestEvent) {
        _events.emit(event)
    }

    suspend fun startRepeatListening(
        data: RepeatListeningData,
        repeatCount: Int = 5
    ) {
        val koSentences = data.koreanAnswer.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val enSentences = data.englishAnswer.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val count = minOf(koSentences.size, enSentences.size)

        val navState = progressPersistenceService.loadNavigationState()
        val startIndex = if (navState.category == data.category
            && navState.scriptIndex == data.scriptIndex
            && navState.index in 0 until count
        ) {
            navState.index
        } else {
            0
        }

        for (i in startIndex until count) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) break

            progressPersistenceService.saveNavigationState(
                ProgressPersistenceService.NavigationState(data.category, i, data.scriptIndex)
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
}
