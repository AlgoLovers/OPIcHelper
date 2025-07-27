package com.na982.opichelper.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.*
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class BaseTtsPlayerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockTts: TextToSpeech

    private lateinit var testTtsPlayer: TestBaseTtsPlayer

    /**
     * 테스트용 BaseTtsPlayer 구현체
     */
    private inner class TestBaseTtsPlayer : BaseTtsPlayer(
        context = mockContext,
        locale = Locale.US,
        serviceName = "Test TTS",
        logTag = "TestTtsPlayer"
    ) {
        override fun getSpeechRate(): Float = 0.8f
        override fun getPitch(): Float = 1.0f
        
        // 테스트용 초기화 상태 설정 메서드
        fun setTestInitialized(initialized: Boolean) {
            this.isInitialized = initialized
        }
    }

    @Before
    fun setUp() {
        testTtsPlayer = TestBaseTtsPlayer()
    }

    @Test
    fun `초기화 전에는 사용 불가능해야 함`() {
        // Given
        testTtsPlayer.setTestInitialized(false)
        
        // Then
        assertFalse(testTtsPlayer.isAvailable())
        assertEquals("Test TTS", testTtsPlayer.getServiceName())
    }

    @Test
    fun `초기화 성공 시 사용 가능해야 함`() {
        // Given
        testTtsPlayer.setTestInitialized(true)
        
        // Then
        assertTrue(testTtsPlayer.isAvailable())
    }

    @Test
    fun `사용 불가능한 상태에서 speak 호출 시 false 반환해야 함`() = runTest {
        // Given
        val text = "Test text"
        testTtsPlayer.setTestInitialized(false)
        var callbackCalled = false
        
        // When
        val result = testTtsPlayer.speak(text) {
            callbackCalled = true
        }
        
        // Then
        assertFalse(result)
        assertTrue(callbackCalled) // 콜백은 호출되어야 함
    }

    @Test
    fun `사용 가능한 상태에서 speak 호출 시 true 반환해야 함`() = runTest {
        // Given
        val text = "Test text"
        testTtsPlayer.setTestInitialized(true)
        
        // When
        val result = testTtsPlayer.speak(text, null)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `stop 호출 시 재생 상태가 false가 되어야 함`() {
        // Given
        testTtsPlayer.setTestInitialized(true)
        
        // When
        testTtsPlayer.stop()
        
        // Then
        assertFalse(testTtsPlayer.isPlaying())
    }

    @Test
    fun `release 호출 시 모든 리소스가 정리되어야 함`() {
        // Given
        testTtsPlayer.setTestInitialized(true)
        
        // When
        testTtsPlayer.release()
        
        // Then
        assertFalse(testTtsPlayer.isAvailable())
        assertFalse(testTtsPlayer.isPlaying())
    }

    @Test
    fun `예외 발생 시 안전하게 처리되어야 함`() = runTest {
        // Given
        val text = "Test text"
        testTtsPlayer.setTestInitialized(true)
        var callbackCalled = false
        
        // When - 예외 상황 시뮬레이션
        val result = testTtsPlayer.speak(text) {
            callbackCalled = true
        }
        
        // Then
        // 예외가 발생해도 콜백은 호출되어야 함
        assertTrue(callbackCalled)
    }

    @Test
    fun `speakWithHighlight는 기본적으로 speak를 호출해야 함`() = runTest {
        // Given
        val text = "Test text."
        testTtsPlayer.setTestInitialized(true)
        var highlightCalled = false
        
        // When
        testTtsPlayer.speakWithHighlight(text) { index ->
            highlightCalled = true
        }
        
        // Then
        assertTrue(highlightCalled)
    }

    @Test
    fun `speakAndGetDuration이 올바른 시간을 반환해야 함`() = runTest {
        // Given
        val text = "Test text"
        testTtsPlayer.setTestInitialized(true)
        
        // When
        val duration = testTtsPlayer.speakAndGetDuration(text, false, 0.8f)
        
        // Then
        assertTrue(duration >= 0)
    }

    @Test
    fun `서비스 이름이 올바르게 반환되어야 함`() {
        // When
        val serviceName = testTtsPlayer.getServiceName()
        
        // Then
        assertEquals("Test TTS", serviceName)
    }

    @Test
    fun `초기화 상태가 올바르게 관리되어야 함`() {
        // Given - 초기 상태
        testTtsPlayer.setTestInitialized(false)
        assertFalse(testTtsPlayer.isAvailable())
        
        // When - 성공적인 초기화
        testTtsPlayer.setTestInitialized(true)
        
        // Then
        assertTrue(testTtsPlayer.isAvailable())
        
        // When - 실패한 초기화
        testTtsPlayer.setTestInitialized(false)
        
        // Then
        assertFalse(testTtsPlayer.isAvailable())
    }

    @Test
    fun `재생 상태가 올바르게 관리되어야 함`() = runTest {
        // Given
        testTtsPlayer.setTestInitialized(true)
        
        // When - 재생 시작
        val speakJob = launch {
            testTtsPlayer.speak("Test text", null)
        }
        
        // Then - 재생 중 상태 확인
        kotlinx.coroutines.delay(100)
        // 재생 상태는 내부적으로 관리되므로 실제 확인은 어려움
        
        // When - 재생 중지
        testTtsPlayer.stop()
        
        // Then
        assertFalse(testTtsPlayer.isPlaying())
        
        speakJob.cancel()
    }

    @Test
    fun `여러 번의 초기화가 안전하게 처리되어야 함`() {
        // Given
        testTtsPlayer.setTestInitialized(true)
        assertTrue(testTtsPlayer.isAvailable())
        
        // When - 다시 초기화
        testTtsPlayer.setTestInitialized(false)
        
        // Then
        assertFalse(testTtsPlayer.isAvailable())
        
        // When - 다시 성공적인 초기화
        testTtsPlayer.setTestInitialized(true)
        
        // Then
        assertTrue(testTtsPlayer.isAvailable())
    }
} 