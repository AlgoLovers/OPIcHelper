package com.na982.opichelper.domain.audio

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class TtsPlaybackControllerTest {

    @Mock
    private lateinit var mockTtsPlayer: TtsPlayer

    @Mock
    private lateinit var mockAudioPlayer: AudioPlayer

    @Mock
    private lateinit var mockTtsOrchestrator: TtsOrchestrator

    private lateinit var ttsPlaybackController: TtsPlaybackController

    @Before
    fun setUp() {
        ttsPlaybackController = TtsPlaybackController(
            audioPlayer = mockAudioPlayer
        )
        ttsPlaybackController.setTtsOrchestrator(mockTtsOrchestrator)
    }

    @Test
    fun `초기 상태는 모든 재생 상태가 false여야 함`() = runTest {
        // Then
        assertFalse(ttsPlaybackController.isPlaying.first())
        assertFalse(ttsPlaybackController.isQuestionPlaying.first())
        assertFalse(ttsPlaybackController.isAnswerPlaying.first())
        assertEquals(null, ttsPlaybackController.questionHighlightIndex.first())
        assertEquals(null, ttsPlaybackController.answerHighlightIndex.first())
    }

    @Test
    fun `질문 재생 시 상태가 올바르게 업데이트되어야 함`() = runTest {
        // Given
        val question = "What do you like to do in your free time?"
        `when`(mockTtsOrchestrator.speakWithHighlight(any(), any())).thenAnswer { invocation ->
            val onHighlight = invocation.getArgument<(Int?) -> Unit>(1)
            onHighlight(0)
            onHighlight(1)
            onHighlight(null)
        }

        // When
        ttsPlaybackController.playQuestion(question)

        // Then
        assertTrue(ttsPlaybackController.isPlaying.first())
        assertTrue(ttsPlaybackController.isQuestionPlaying.first())
        assertFalse(ttsPlaybackController.isAnswerPlaying.first())
        verify(mockTtsOrchestrator).speakWithHighlight(question, any())
    }

    @Test
    fun `답변 재생 시 상태가 올바르게 업데이트되어야 함`() = runTest {
        // Given
        val answer = "I like to read books and watch movies."
        `when`(mockTtsOrchestrator.speakWithHighlight(any(), any())).thenAnswer { invocation ->
            val onHighlight = invocation.getArgument<(Int?) -> Unit>(1)
            onHighlight(0)
            onHighlight(1)
            onHighlight(null)
        }

        // When
        ttsPlaybackController.playAnswer(answer)

        // Then
        assertTrue(ttsPlaybackController.isPlaying.first())
        assertFalse(ttsPlaybackController.isQuestionPlaying.first())
        assertTrue(ttsPlaybackController.isAnswerPlaying.first())
        verify(mockTtsOrchestrator).speakWithHighlight(answer, any())
    }

    @Test
    fun `TTS 중지 시 모든 상태가 초기화되어야 함`() = runTest {
        // Given
        val question = "Test question"
        `when`(mockTtsOrchestrator.speakWithHighlight(any(), any())).thenAnswer { invocation ->
            val onHighlight = invocation.getArgument<(Int?) -> Unit>(1)
            onHighlight(0)
        }

        // When
        ttsPlaybackController.playQuestion(question)
        ttsPlaybackController.stopTts()

        // Then
        assertFalse(ttsPlaybackController.isPlaying.first())
        assertFalse(ttsPlaybackController.isQuestionPlaying.first())
        assertFalse(ttsPlaybackController.isAnswerPlaying.first())
        assertEquals(null, ttsPlaybackController.questionHighlightIndex.first())
        assertEquals(null, ttsPlaybackController.answerHighlightIndex.first())
        verify(mockTtsOrchestrator).stop()
    }

    @Test
    fun `질문 재생 중 답변 재생 시 기존 재생이 중지되어야 함`() = runTest {
        // Given
        val question = "Test question"
        val answer = "Test answer"
        `when`(mockTtsOrchestrator.speakWithHighlight(any(), any())).thenAnswer { invocation ->
            val onHighlight = invocation.getArgument<(Int?) -> Unit>(1)
            onHighlight(0)
            onHighlight(null)
        }

        // When
        ttsPlaybackController.playQuestion(question)
        ttsPlaybackController.playAnswer(answer)

        // Then
        verify(mockTtsOrchestrator, times(2)).stop() // 질문 중지 + 답변 재생
        verify(mockTtsOrchestrator).speakWithHighlight(answer, any())
    }

    @Test
    fun `하이라이트 인덱스가 올바르게 업데이트되어야 함`() = runTest {
        // Given
        val question = "Test question."
        `when`(mockTtsOrchestrator.speakWithHighlight(any(), any())).thenAnswer { invocation ->
            val onHighlight = invocation.getArgument<(Int?) -> Unit>(1)
            onHighlight(0)
            onHighlight(null)
        }

        // When
        ttsPlaybackController.playQuestion(question)

        // Then
        assertEquals(0, ttsPlaybackController.questionHighlightIndex.first())
        // 재생 완료 후 null로 초기화
        kotlinx.coroutines.delay(100)
        assertEquals(null, ttsPlaybackController.questionHighlightIndex.first())
    }

    @Test
    fun `오디오 파일 재생 시 상태가 올바르게 업데이트되어야 함`() = runTest {
        // Given
        val mockFile = java.io.File("test.mp3")
        `when`(mockAudioPlayer.play(any(), any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<() -> Unit>(1)
            callback()
        }

        // When
        ttsPlaybackController.playAudioFile(mockFile)

        // Then
        assertTrue(ttsPlaybackController.isPlaying.first())
        verify(mockAudioPlayer).play(mockFile, any())
    }

    @Test
    fun `오디오 중지 시 상태가 초기화되어야 함`() = runTest {
        // When
        ttsPlaybackController.stopAudio()

        // Then
        assertFalse(ttsPlaybackController.isPlaying.first())
        verify(mockAudioPlayer).stop()
    }

    @Test
    fun `하이라이트 인덱스 수동 설정이 올바르게 작동해야 함`() = runTest {
        // When
        ttsPlaybackController.setQuestionHighlightIndex(2)
        ttsPlaybackController.setAnswerHighlightIndex(3)
        ttsPlaybackController.setAnswerKoHighlightIndex(4)
        ttsPlaybackController.setRecordingHighlightIndex(5)

        // Then
        assertEquals(2, ttsPlaybackController.questionHighlightIndex.first())
        assertEquals(3, ttsPlaybackController.answerHighlightIndex.first())
        assertEquals(4, ttsPlaybackController.answerKoHighlightIndex.first())
        assertEquals(5, ttsPlaybackController.recordingHighlightIndex.first())
    }

    @Test
    fun `하이라이트 초기화가 올바르게 작동해야 함`() = runTest {
        // Given
        ttsPlaybackController.setQuestionHighlightIndex(1)
        ttsPlaybackController.setAnswerHighlightIndex(2)

        // When
        ttsPlaybackController.clearHighlight()

        // Then
        assertEquals(null, ttsPlaybackController.questionHighlightIndex.first())
        assertEquals(null, ttsPlaybackController.answerHighlightIndex.first())
        assertEquals(null, ttsPlaybackController.answerKoHighlightIndex.first())
        assertEquals(null, ttsPlaybackController.recordingHighlightIndex.first())
    }

    @Test
    fun `한글 TTS 서비스 이름이 올바르게 반환되어야 함`() {
        // Given
        `when`(mockTtsOrchestrator.getCurrentKoreanTtsServiceName()).thenReturn("Samsung TTS")

        // When
        val serviceName = ttsPlaybackController.getCurrentKoreanTtsServiceName()

        // Then
        assertEquals("Samsung TTS", serviceName)
    }



    @Test
    fun `합쳐진 오디오 재생 시 순서가 올바르게 작동해야 함`() = runTest {
        // Given
        val question = "Test question."
        val answer = "Test answer."
        `when`(mockTtsOrchestrator.speakWithHighlight(any(), any())).thenAnswer { invocation ->
            val onHighlight = invocation.getArgument<(Int?) -> Unit>(1)
            onHighlight(0)
            onHighlight(null)
        }

        // When
        ttsPlaybackController.playMergedAudio(question, answer)

        // Then
        verify(mockTtsOrchestrator, times(2)).speakWithHighlight(any(), any())
        verify(mockTtsOrchestrator).stop()
    }

    @Test
    fun `예외 발생 시 상태가 안전하게 초기화되어야 함`() = runTest {
        // Given
        val question = "Test question"
        `when`(mockTtsOrchestrator.speakWithHighlight(any(), any())).thenThrow(RuntimeException("TTS Error"))

        // When
        ttsPlaybackController.playQuestion(question)

        // Then
        // 예외 발생 후 상태가 안전하게 초기화되어야 함
        kotlinx.coroutines.delay(100)
        assertFalse(ttsPlaybackController.isPlaying.first())
        assertFalse(ttsPlaybackController.isQuestionPlaying.first())
        assertEquals(null, ttsPlaybackController.questionHighlightIndex.first())
    }
} 