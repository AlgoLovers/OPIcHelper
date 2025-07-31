package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.RepeatListeningUiCallback
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.repository.ScriptProgress
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.MemorizeTestProgressTracker
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.junit.Assert.*

class RepeatListeningUseCaseTest {

    @Mock
    private lateinit var mockTtsPlayer: TtsPlayer

    @Mock
    private lateinit var mockTtsOrchestrator: TtsOrchestrator

    @Mock
    private lateinit var mockProgressTracker: MemorizeTestProgressTracker

    @Mock
    private lateinit var mockRecordingTimeManager: RecordingTimeManager

    private lateinit var repeatListeningUseCase: RepeatListeningUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repeatListeningUseCase = RepeatListeningUseCase(
            ttsOrchestrator = mockTtsOrchestrator,
            progressTracker = mockProgressTracker,
            recordingTimeManager = mockRecordingTimeManager
        )
    }

    @Test
    fun `test RepeatListeningUseCase creation`() {
        assertNotNull(repeatListeningUseCase)
    }

    @Test
    fun `test startRepeatListening with no progress`() = runTest {
        // Given
        val data = RepeatListeningData(
            category = "test_category",
            scriptIndex = 0,
            koreanAnswer = "안녕하세요.",
            englishAnswer = "Hello."
        )
        
        val mockUiCallback = mock<RepeatListeningUiCallback>()
        whenever(mockProgressTracker.getScriptProgress(data.category, data.scriptIndex, "반복 듣기")).thenReturn(null)
        
        // When
        repeatListeningUseCase.startRepeatListening(data, mockUiCallback)
        
        // Then
        verify(mockProgressTracker).getScriptProgress(data.category, data.scriptIndex, "반복 듣기")
    }

    @Test
    fun `test startRepeatListening with existing progress`() = runTest {
        // Given
        val data = RepeatListeningData(
            category = "test_category",
            scriptIndex = 0,
            koreanAnswer = "안녕하세요. 오늘 날씨가 좋네요.",
            englishAnswer = "Hello. The weather is nice today."
        )
        
        val existingProgress = ScriptProgress(
            category = data.category,
            scriptIndex = data.scriptIndex,
            memorizeLevel = "반복 듣기",
            currentSentenceIndex = 1,
            totalSentences = 2,
            isMemorizeTestRunning = true
        )
        
        val mockUiCallback = mock<RepeatListeningUiCallback>()
        whenever(mockProgressTracker.getScriptProgress(data.category, data.scriptIndex, "반복 듣기")).thenReturn(existingProgress)
        
        // When
        repeatListeningUseCase.startRepeatListening(data, mockUiCallback)
        
        // Then
        verify(mockProgressTracker).getScriptProgress(data.category, data.scriptIndex, "반복 듣기")
    }

    @Test
    fun `test startRepeatListening with different memorize level`() = runTest {
        // Given
        val data = RepeatListeningData(
            category = "test_category",
            scriptIndex = 0,
            koreanAnswer = "안녕하세요. 오늘 날씨가 좋네요.",
            englishAnswer = "Hello. The weather is nice today."
        )
        
        val existingProgress = ScriptProgress(
            category = data.category,
            scriptIndex = data.scriptIndex,
            memorizeLevel = "영작 테스트", // 다른 레벨
            currentSentenceIndex = 1,
            totalSentences = 2,
            isMemorizeTestRunning = true
        )
        
        val mockUiCallback = mock<RepeatListeningUiCallback>()
        whenever(mockProgressTracker.getScriptProgress(data.category, data.scriptIndex, "반복 듣기")).thenReturn(existingProgress)
        
        // When
        repeatListeningUseCase.startRepeatListening(data, mockUiCallback)
        
        // Then
        verify(mockProgressTracker).getScriptProgress(data.category, data.scriptIndex, "반복 듣기")
    }

    @Test
    fun `test executeRepeatListeningTest with cancellation`() = runTest {
        // Given
        val answerKo = "안녕하세요. 오늘 날씨가 좋네요."
        val answerEn = "Hello. The weather is nice today."
        val category = "test_category"
        val scriptIndex = 0
        
        whenever(mockProgressTracker.getScriptProgress(category, scriptIndex, "반복 듣기")).thenReturn(null)
        whenever(mockTtsPlayer.speakAndGetDuration(any(), any(), any())).thenReturn(1000L)
        
        var highlightIndex: Int? = null
        var cardFlipped: Boolean? = null
        
        // When - 코루틴이 취소되는 상황 시뮬레이션
        val job = launch {
            repeatListeningUseCase.executeRepeatListeningTest(
                answerKo = answerKo,
                answerEn = answerEn,
                onHighlight = { index -> highlightIndex = index },
                onKoreanHighlight = { index -> highlightIndex = index },
                onCardFlip = { isKorean -> cardFlipped = isKorean },
                onComplete = { },
                category = category,
                scriptIndex = scriptIndex
            )
        }
        
        // 코루틴 취소
        job.cancel()
        
        // Then
        verify(mockProgressTracker).getScriptProgress(category, scriptIndex, "반복 듣기")
        // 취소되었으므로 clearScriptProgress는 호출되지 않아야 함
        verify(mockProgressTracker, never()).clearScriptProgress(category, scriptIndex, "반복 듣기")
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
        
        whenever(mockProgressTracker.getScriptProgress(category, scriptIndex, "반복 듣기")).thenReturn(existingProgress)
        whenever(mockTtsPlayer.speakAndGetDuration(any(), any(), any())).thenReturn(1000L)
        
        var highlightIndex: Int? = null
        var cardFlipped: Boolean? = null
        
        // When
        repeatListeningUseCase.executeRepeatListeningTest(
            answerKo = answerKo,
            answerEn = answerEn,
            onHighlight = { index -> highlightIndex = index },
            onKoreanHighlight = { index -> highlightIndex = index },
            onCardFlip = { isKorean -> cardFlipped = isKorean },
            onComplete = { },
            category = category,
            scriptIndex = scriptIndex
        )
        
        // Then - 다른 레벨이므로 0부터 시작해야 함
        verify(mockProgressTracker).getScriptProgress(category, scriptIndex, "반복 듣기")
        verify(mockProgressTracker).updateCurrentSentenceIndex(category, scriptIndex, "반복 듣기", 0)
        verify(mockProgressTracker).clearScriptProgress(category, scriptIndex, "반복 듣기")
    }
} 