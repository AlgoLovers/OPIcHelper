package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsSpeakResult
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class RepeatListeningRepositoryImpl(
    private val ttsOrchestrator: TtsOrchestrator,
    progressPersistenceService: ProgressPersistenceService,
    private val recordingTimeManager: RecordingTimeManager
) : BaseMemorizeTestRepository(progressPersistenceService), RepeatListeningRepository {

    companion object {
        private const val CARD_FLIP_DELAY_MS = 100L
        private const val WORD_DELAY_MS = 500
        private const val REST_TIME_MULTIPLIER = 1.2
    }

    override val memorizeLevel = MemorizeLevel.REPEAT_LISTENING

    override suspend fun executeRepeatListening(
        data: RepeatListeningData,
        repeatCount: Int
    ) {
        val koSentences = splitSentences(data.koreanAnswer)
        val enSentences = splitSentences(data.englishAnswer)
        val count = minOf(koSentences.size, enSentences.size)

        val startIndex = resolveStartIndex(data.category, data.scriptIndex, count)

        sentenceLoop@ for (i in startIndex until count) {
            if (!currentCoroutineContext().isActive) break@sentenceLoop

            progressPersistenceService.saveNavigationState(
                ProgressPersistenceService.NavigationState(data.category, data.scriptIndex, i)
            )

            // 1. 한글 문장 TTS
            emit(MemorizeTestEvent.CardFlip(true))
            delay(CARD_FLIP_DELAY_MS)
            emit(MemorizeTestEvent.KoreanHighlight(i))

            val koResult = ttsOrchestrator.speakAndWaitForCompletion(koSentences[i])
            if (koResult is TtsSpeakResult.Unavailable) break@sentenceLoop
            if (koResult !is TtsSpeakResult.Success) {
                continue@sentenceLoop
            }

            val enSentence = enSentences[i]
            val enWordCount = enSentence.split("\\s+".toRegex()).size
            val baseDelay = enWordCount * WORD_DELAY_MS
            val lengthMultiplier = when {
                enWordCount <= 5 -> 1.5f
                enWordCount <= 10 -> 1.2f
                enWordCount <= 15 -> 1.0f
                else -> 0.8f
            }
            val adaptiveDelay = (baseDelay * lengthMultiplier).toLong()
            delay(adaptiveDelay)

            if (!currentCoroutineContext().isActive) break@sentenceLoop

            // 2. 영문 문장 N회 TTS
            for (j in 1..repeatCount) {
                if (!currentCoroutineContext().isActive) break@sentenceLoop

                emit(MemorizeTestEvent.CardFlip(false))
                delay(CARD_FLIP_DELAY_MS)
                emit(MemorizeTestEvent.Highlight(i))

                val enResult = ttsOrchestrator.speakAndWaitForCompletion(enSentences[i])
                if (enResult is TtsSpeakResult.Unavailable) break@sentenceLoop
                if (enResult !is TtsSpeakResult.Success) continue@sentenceLoop
                val enDuration = enResult.durationMs

                if (j == 1) {
                    recordingTimeManager.saveRecordingTime(data.category, data.scriptIndex, i, enDuration)
                }

                if (!currentCoroutineContext().isActive) break@sentenceLoop

                val restTime = (enDuration * REST_TIME_MULTIPLIER).toLong()
                delay(restTime)
            }
            emit(MemorizeTestEvent.Highlight(null))
        }

        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(data.category, data.scriptIndex, 0)
        )

        emit(MemorizeTestEvent.CardFlip(false))
        emit(MemorizeTestEvent.Highlight(null))
        emit(MemorizeTestEvent.Completed)
    }
}
