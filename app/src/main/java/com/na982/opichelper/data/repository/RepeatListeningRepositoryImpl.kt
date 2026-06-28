package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.RepeatListeningProgress
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger

class RepeatListeningRepositoryImpl(
    private val ttsOrchestrator: TtsOrchestrator,
    progressPersistenceService: ProgressPersistenceService,
    private val recordingTimeManager: RecordingTimeManager
) : BaseMemorizeTestRepository(progressPersistenceService), RepeatListeningRepository {

    companion object {
        private val WHITESPACE_REGEX = "\\s+".toRegex()
        private const val WORD_DELAY_MS = 500
        private const val REST_TIME_MULTIPLIER = 1.2
        private const val SHORT_WORD_THRESHOLD = 5
        private const val MEDIUM_WORD_THRESHOLD = 10
        private const val LONG_WORD_THRESHOLD = 15
        private const val SHORT_WORD_MULTIPLIER = 1.5f
        private const val MEDIUM_WORD_MULTIPLIER = 1.2f
        private const val LONG_WORD_MULTIPLIER = 1.0f
        private const val VERY_LONG_WORD_MULTIPLIER = 0.8f
    }

    override val memorizeLevel = MemorizeLevel.REPEAT_LISTENING

    private val _repeatProgress = MutableStateFlow<RepeatListeningProgress?>(null)
    override val repeatProgress: StateFlow<RepeatListeningProgress?> = _repeatProgress.asStateFlow()

    private val _extraRepetitions = AtomicInteger(0)

    override fun requestExtraRepetitions(count: Int) {
        _extraRepetitions.addAndGet(count)
    }

    override suspend fun executeRepeatListening(
        data: RepeatListeningData,
        repeatCount: Int
    ) {
        val koSentences = splitSentences(data.koreanAnswer)
        val enSentences = splitSentences(data.englishAnswer)
        val count = minOf(koSentences.size, enSentences.size)

        _extraRepetitions.set(0)
        _repeatProgress.update { RepeatListeningProgress(totalSentences = count, totalRepetitions = repeatCount) }

        val startIndex = resolveStartIndex(data.category, data.scriptIndex, count)

        sentenceLoop@ for (i in startIndex until count) {
            if (!currentCoroutineContext().isActive) break@sentenceLoop

            _repeatProgress.update { prev -> prev?.copy(sentenceIndex = i, totalSentences = count) ?: prev }

            progressPersistenceService.saveNavigationState(
                ProgressPersistenceService.NavigationState(data.category, data.scriptIndex, i)
            )

            // 1. 한글 문장 TTS
            val koResult = playKoreanWithHighlight(ttsOrchestrator, koSentences[i], i)
            if (koResult is TtsSpeakResult.Unavailable) break@sentenceLoop
            if (koResult !is TtsSpeakResult.Success) {
                continue@sentenceLoop
            }

            val enSentence = enSentences[i]
            val enWordCount = enSentence.split(WHITESPACE_REGEX).size
            val baseDelay = enWordCount * WORD_DELAY_MS
            val lengthMultiplier = when {
                enWordCount <= SHORT_WORD_THRESHOLD -> SHORT_WORD_MULTIPLIER
                enWordCount <= MEDIUM_WORD_THRESHOLD -> MEDIUM_WORD_MULTIPLIER
                enWordCount <= LONG_WORD_THRESHOLD -> LONG_WORD_MULTIPLIER
                else -> VERY_LONG_WORD_MULTIPLIER
            }
            val adaptiveDelay = (baseDelay * lengthMultiplier).toLong()
            delay(adaptiveDelay)

            if (!currentCoroutineContext().isActive) break@sentenceLoop

            // 2. 영문 문장 N회 TTS (동적 연장 지원)
            var effectiveRepeatCount = repeatCount
            var j = 1
            while (j <= effectiveRepeatCount) {
                if (!currentCoroutineContext().isActive) break@sentenceLoop

                val extra = _extraRepetitions.getAndSet(0)
                if (extra > 0) {
                    effectiveRepeatCount += extra
                    _repeatProgress.update { prev -> prev?.copy(totalRepetitions = effectiveRepeatCount) ?: prev }
                }

                _repeatProgress.update { prev -> prev?.copy(currentRepetition = j, totalRepetitions = effectiveRepeatCount) ?: prev }

                emit(MemorizeTestEvent.CardFlip(false))
                delay(BaseMemorizeTestRepository.CARD_FLIP_DELAY_MS)
                emit(MemorizeTestEvent.Highlight(i))

                val enResult = ttsOrchestrator.speakAndWaitForCompletion(enSentences[i])
                if (enResult is TtsSpeakResult.Unavailable) break@sentenceLoop
                if (enResult !is TtsSpeakResult.Success) {
                    continue@sentenceLoop
                }
                val enDuration = enResult.durationMs

                if (j == 1) {
                    recordingTimeManager.saveRecordingTime(data.category, data.scriptIndex, i, enDuration)
                }

                if (!currentCoroutineContext().isActive) break@sentenceLoop

                val restTime = (enDuration * REST_TIME_MULTIPLIER).toLong()
                delay(restTime)
                j++
            }
            emit(MemorizeTestEvent.Highlight(null))
        }

        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(data.category, data.scriptIndex, 0)
        )

        _repeatProgress.update { null }
        emit(MemorizeTestEvent.CardFlip(false))
        emit(MemorizeTestEvent.Highlight(null))
        emit(MemorizeTestEvent.Completed)
    }
}
