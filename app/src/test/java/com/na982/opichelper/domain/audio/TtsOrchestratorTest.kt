package com.na982.opichelper.domain.audio

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class TtsOrchestratorTest {

    @Mock
    private lateinit var mockGoogleTtsPlayer: TtsPlayer

    @Mock
    private lateinit var mockSamsungTtsPlayer: TtsPlayer

    @Mock
    private lateinit var mockContext: android.content.Context

    private lateinit var ttsOrchestrator: TtsOrchestrator

    @Before
    fun setUp() {
        ttsOrchestrator = TtsOrchestrator(
            context = mockContext,
            googleTtsPlayer = mockGoogleTtsPlayer,
            samsungTtsPlayer = mockSamsungTtsPlayer
        )
    }

    @Test
    fun `영문 텍스트는 Google TTS로 재생되어야 함`() = runTest {
        // Given
        val englishText = "What do you like to do in your free time?"
        `when`(mockGoogleTtsPlayer.speak(any(), any())).thenReturn(true)

        // When
        val result = ttsOrchestrator.speak(englishText, null)

        // Then
        assertTrue(result)
        verify(mockGoogleTtsPlayer).speak(englishText, null)
        verify(mockSamsungTtsPlayer, never()).speak(any(), any())
    }

    @Test
    fun `한글 텍스트는 Samsung TTS로 재생되어야 함`() = runTest {
        // Given
        val koreanText = "여가 시간에 무엇을 하시나요?"
        `when`(mockSamsungTtsPlayer.isAvailable()).thenReturn(true)
        `when`(mockSamsungTtsPlayer.speak(any(), any())).thenReturn(true)

        // When
        val result = ttsOrchestrator.speak(koreanText, null)

        // Then
        assertTrue(result)
        verify(mockSamsungTtsPlayer).speak(koreanText, null)
        verify(mockGoogleTtsPlayer, never()).speak(any(), any())
    }

    @Test
    fun `한글 TTS 실패 시 폴백이 작동해야 함`() = runTest {
        // Given
        val koreanText = "여가 시간에 무엇을 하시나요?"
        `when`(mockSamsungTtsPlayer.isAvailable()).thenReturn(false)
        `when`(mockGoogleTtsPlayer.speak(any(), any())).thenReturn(true)

        // When
        val result = ttsOrchestrator.speak(koreanText, null)

        // Then
        assertTrue(result)
        verify(mockSamsungTtsPlayer).isAvailable()
        verify(mockGoogleTtsPlayer).speak(koreanText, null)
    }

    @Test
    fun `모든 TTS 실패 시 false를 반환해야 함`() = runTest {
        // Given
        val koreanText = "여가 시간에 무엇을 하시나요?"
        `when`(mockSamsungTtsPlayer.isAvailable()).thenReturn(false)
        `when`(mockGoogleTtsPlayer.speak(any(), any())).thenReturn(false)

        // When
        val result = ttsOrchestrator.speak(koreanText, null)

        // Then
        assertFalse(result)
    }

    @Test
    fun `콜백이 정상적으로 호출되어야 함`() = runTest {
        // Given
        val englishText = "Test text"
        var callbackCalled = false
        `when`(mockGoogleTtsPlayer.speak(any(), any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<(() -> Unit)?>(1)
            callback?.invoke()
            true
        }

        // When
        val result = ttsOrchestrator.speak(englishText) {
            callbackCalled = true
        }

        // Then
        assertTrue(result)
        assertTrue(callbackCalled)
    }

    @Test
    fun `stop 호출 시 모든 TTS 플레이어가 중지되어야 함`() {
        // When
        ttsOrchestrator.stop()

        // Then
        verify(mockGoogleTtsPlayer).stop()
        verify(mockSamsungTtsPlayer).stop()
    }

    @Test
    fun `isPlaying 호출 시 모든 TTS 플레이어 상태를 확인해야 함`() {
        // Given
        `when`(mockGoogleTtsPlayer.isPlaying()).thenReturn(false)
        `when`(mockSamsungTtsPlayer.isPlaying()).thenReturn(false)

        // When
        val result = ttsOrchestrator.isPlaying()

        // Then
        assertFalse(result)
        verify(mockGoogleTtsPlayer).isPlaying()
        verify(mockSamsungTtsPlayer).isPlaying()
    }

    @Test
    fun `한글 TTS 서비스 이름이 정상적으로 반환되어야 함`() {
        // Given
        `when`(mockSamsungTtsPlayer.getServiceName()).thenReturn("Samsung TTS")

        // When
        val serviceName = ttsOrchestrator.getCurrentKoreanTtsServiceName()

        // Then
        assertEquals("Samsung TTS", serviceName)
    }

    @Test
    fun `사용 가능한 한글 TTS 서비스 목록이 정상적으로 반환되어야 함`() {
        // Given
        `when`(mockSamsungTtsPlayer.isAvailable()).thenReturn(true)
        `when`(mockSamsungTtsPlayer.getServiceName()).thenReturn("Samsung TTS")

        // When
        val availableServices = ttsOrchestrator.getAvailableKoreanTtsServices()

        // Then
        assertEquals(1, availableServices.size)
        assertEquals("Samsung TTS", availableServices[0])
    }

    @Test
    fun `한글 TTS 서비스 상태가 정상적으로 반환되어야 함`() {
        // Given
        `when`(mockSamsungTtsPlayer.isAvailable()).thenReturn(true)
        `when`(mockSamsungTtsPlayer.getServiceName()).thenReturn("Samsung TTS")

        // When
        val serviceStatus = ttsOrchestrator.getKoreanTtsServiceStatus()

        // Then
        assertEquals(1, serviceStatus.size)
        assertEquals("Samsung TTS" to true, serviceStatus[0])
    }
} 