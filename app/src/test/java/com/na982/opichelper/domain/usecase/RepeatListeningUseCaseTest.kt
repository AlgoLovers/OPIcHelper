package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.repository.ScriptProgress
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.junit.Assert.*

class RepeatListeningServiceTest {

    @Mock
    private lateinit var mockTtsPlayer: TtsPlayer

    @Mock
    private lateinit var mockProgressTracker: MemorizeTestProgressTracker

    private lateinit var repeatListeningService: RepeatListeningService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repeatListeningService = RepeatListeningService(
            ttsPlayer = mockTtsPlayer,
            progressTracker = mockProgressTracker
        )
    }

    @Test
    fun `test RepeatListeningService creation`() {
        assertNotNull(repeatListeningService)
    }

    @Test
    fun `test executeRepeatListeningTest with no progress`() = runTest {
        // Given
        val answerKo = "안녕하세요."
        val answerEn = "Hello."
        val category = "test_category"
        val scriptIndex = 0
        
        whenever(mockProgressTracker.getScriptProgress(category, scriptIndex)).thenReturn(null)
        whenever(mockTtsPlayer.speakAndGetDuration(any(), any(), any())).thenReturn(1000L)
        
        var highlightIndex: Int? = null
        var cardFlipped: Boolean? = null
        
        // When
        repeatListeningService.executeRepeatListeningTest(
            answerKo = answerKo,
            answerEn = answerEn,
            onHighlight = { index -> highlightIndex = index },
            onCardFlip = { isKorean -> cardFlipped = isKorean },
            category = category,
            scriptIndex = scriptIndex
        )
        
        // Then
        verify(mockProgressTracker).getScriptProgress(category, scriptIndex)
        verify(mockProgressTracker).updateCurrentSentenceIndex(category, scriptIndex, 0)
        verify(mockProgressTracker).clearScriptProgress(category, scriptIndex)
    }

    @Test
    fun `test executeRepeatListeningTest with existing progress`() = runTest {
        // Given
        val answerKo = "안녕하세요. 오늘 날씨가 좋네요."
        val answerEn = "Hello. The weather is nice today."
        val category = "test_category"
        val scriptIndex = 0
        
        val existingProgress = ScriptProgress(
            category = category,
            scriptIndex = scriptIndex,
            memorizeLevel = "반복 듣기",
            currentSentenceIndex = 1,
            totalSentences = 2,
            isMemorizeTestRunning = true
        )
        
        whenever(mockProgressTracker.getScriptProgress(category, scriptIndex)).thenReturn(existingProgress)
        whenever(mockTtsPlayer.speakAndGetDuration(any(), any(), any())).thenReturn(1000L)
        
        var highlightIndex: Int? = null
        var cardFlipped: Boolean? = null
        
        // When
        repeatListeningService.executeRepeatListeningTest(
            answerKo = answerKo,
            answerEn = answerEn,
            onHighlight = { index -> highlightIndex = index },
            onCardFlip = { isKorean -> cardFlipped = isKorean },
            category = category,
            scriptIndex = scriptIndex
        )
        
        // Then
        verify(mockProgressTracker).getScriptProgress(category, scriptIndex)
        verify(mockProgressTracker).updateCurrentSentenceIndex(category, scriptIndex, 1)
        verify(mockProgressTracker).clearScriptProgress(category, scriptIndex)
    }

    @Test
    fun `test executeRepeatListeningTest with different memorize level`() = runTest {
        // Given
        val answerKo = "안녕하세요. 오늘 날씨가 좋네요."
        val answerEn = "Hello. The weather is nice today."
        val category = "test_category"
        val scriptIndex = 0
        
        val existingProgress = ScriptProgress(
            category = category,
            scriptIndex = scriptIndex,
            memorizeLevel = "영작 테스트", // 다른 레벨
            currentSentenceIndex = 1,
            totalSentences = 2,
            isMemorizeTestRunning = true
        )
        
        whenever(mockProgressTracker.getScriptProgress(category, scriptIndex)).thenReturn(existingProgress)
        whenever(mockTtsPlayer.speakAndGetDuration(any(), any(), any())).thenReturn(1000L)
        
        var highlightIndex: Int? = null
        var cardFlipped: Boolean? = null
        
        // When
        repeatListeningService.executeRepeatListeningTest(
            answerKo = answerKo,
            answerEn = answerEn,
            onHighlight = { index -> highlightIndex = index },
            onCardFlip = { isKorean -> cardFlipped = isKorean },
            category = category,
            scriptIndex = scriptIndex
        )
        
        // Then - 다른 레벨이므로 0부터 시작해야 함
        verify(mockProgressTracker).getScriptProgress(category, scriptIndex)
        verify(mockProgressTracker).updateCurrentSentenceIndex(category, scriptIndex, 0)
        verify(mockProgressTracker).clearScriptProgress(category, scriptIndex)
    }

    @Test
    fun `test executeRepeatListeningTest with cancellation`() = runTest {
        // Given
        val answerKo = "안녕하세요. 오늘 날씨가 좋네요."
        val answerEn = "Hello. The weather is nice today."
        val category = "test_category"
        val scriptIndex = 0
        
        whenever(mockProgressTracker.getScriptProgress(category, scriptIndex)).thenReturn(null)
        whenever(mockTtsPlayer.speakAndGetDuration(any(), any(), any())).thenReturn(1000L)
        
        var highlightIndex: Int? = null
        var cardFlipped: Boolean? = null
        
        // When - 코루틴이 취소되는 상황 시뮬레이션
        val job = launch {
            repeatListeningService.executeRepeatListeningTest(
                answerKo = answerKo,
                answerEn = answerEn,
                onHighlight = { index -> highlightIndex = index },
                onCardFlip = { isKorean -> cardFlipped = isKorean },
                category = category,
                scriptIndex = scriptIndex
            )
        }
        
        // 코루틴 취소
        job.cancel()
        
        // Then
        verify(mockProgressTracker).getScriptProgress(category, scriptIndex)
        // 취소되었으므로 clearScriptProgress는 호출되지 않아야 함
        verify(mockProgressTracker, never()).clearScriptProgress(category, scriptIndex)
    }

    @Test
    fun `test executeRepeatListeningTest with different memorize level should start from index 0`() = runTest {
        // Given
        val answerKo = "안녕하세요. 오늘 날씨가 좋네요."
        val answerEn = "Hello. The weather is nice today."
        val category = "test_category"
        val scriptIndex = 0
        
        val existingProgress = ScriptProgress(
            category = category,
            scriptIndex = scriptIndex,
            memorizeLevel = "영작 테스트", // 다른 레벨
            currentSentenceIndex = 1,
            totalSentences = 2,
            isMemorizeTestRunning = true
        )
        
        whenever(mockProgressTracker.getScriptProgress(category, scriptIndex)).thenReturn(existingProgress)
        whenever(mockTtsPlayer.speakAndGetDuration(any(), any(), any())).thenReturn(1000L)
        
        var highlightIndex: Int? = null
        var cardFlipped: Boolean? = null
        
        // When
        repeatListeningService.executeRepeatListeningTest(
            answerKo = answerKo,
            answerEn = answerEn,
            onHighlight = { index -> highlightIndex = index },
            onCardFlip = { isKorean -> cardFlipped = isKorean },
            category = category,
            scriptIndex = scriptIndex
        )
        
        // Then - 다른 레벨이므로 0부터 시작해야 함
        verify(mockProgressTracker).getScriptProgress(category, scriptIndex)
        verify(mockProgressTracker).updateCurrentSentenceIndex(category, scriptIndex, 0)
        verify(mockProgressTracker).clearScriptProgress(category, scriptIndex)
    }
} 