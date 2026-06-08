package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsSpeakResult
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class BaseMemorizeTestRepository(
    protected val progressPersistenceService: ProgressPersistenceService
) {
    private val _events = MutableSharedFlow<MemorizeTestEvent>(
        extraBufferCapacity = 8
    )
    val events: SharedFlow<MemorizeTestEvent> = _events.asSharedFlow()

    protected suspend fun emit(event: MemorizeTestEvent) {
        _events.emit(event)
    }

    protected abstract val memorizeLevel: MemorizeLevel

    protected fun splitSentences(text: String): List<String> {
        return SentenceSplitter.split(text)
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

    open suspend fun getResumeIndex(category: String, scriptIndex: Int, totalCount: Int): Int {
        return resolveStartIndex(category, scriptIndex, totalCount)
    }

    protected suspend fun playKoreanWithHighlight(
        ttsOrchestrator: TtsOrchestrator,
        koreanSentence: String,
        sentenceIndex: Int,
        cardFlipDelayMs: Long = 100L
    ): TtsSpeakResult {
        emit(MemorizeTestEvent.CardFlip(true))
        delay(cardFlipDelayMs)
        emit(MemorizeTestEvent.KoreanHighlight(sentenceIndex))
        return ttsOrchestrator.speakAndWaitForCompletion(koreanSentence)
    }
}
