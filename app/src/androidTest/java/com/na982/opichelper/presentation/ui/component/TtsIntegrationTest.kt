package com.na982.opichelper.presentation.ui.component

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.data.audio.GoogleTtsPlayer
import com.na982.opichelper.data.audio.SamsungTtsPlayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import android.util.Log

@RunWith(AndroidJUnit4::class)
class TtsIntegrationTest {

    private lateinit var ttsOrchestrator: TtsOrchestrator
    private lateinit var ttsPlaybackController: TtsPlaybackController
    private lateinit var googleTtsPlayer: GoogleTtsPlayer
    private lateinit var samsungTtsPlayer: SamsungTtsPlayer

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // TTS 플레이어들 초기화
        googleTtsPlayer = GoogleTtsPlayer(context)
        samsungTtsPlayer = SamsungTtsPlayer(context)
        
        // TTS 초기화 완료 대기
        Log.d("TtsIntegrationTest", "TTS 초기화 대기 중...")
        waitForTtsInitialization()
        Log.d("TtsIntegrationTest", "TTS 초기화 완료")
        
        // TTS 오케스트레이터 초기화
        ttsOrchestrator = TtsOrchestrator(
            context = context,
            googleTtsPlayer = googleTtsPlayer,
            samsungTtsPlayer = samsungTtsPlayer
        )
        
        // TTS 재생 컨트롤러 초기화
        ttsPlaybackController = TtsPlaybackController(
            ttsPlayer = googleTtsPlayer,
            audioPlayer = mockAudioPlayer()
        )
        ttsPlaybackController.setTtsOrchestrator(ttsOrchestrator)
    }

    /**
     * TTS 초기화 완료 대기
     */
    private fun waitForTtsInitialization() {
        Log.d("TtsIntegrationTest", "TTS 초기화 대기 중...")
        
        // TTS 플레이어가 실제로 사용 가능할 때까지 대기
        var attempts = 0
        val maxAttempts = 20 // 더 많은 시도
        
        while (attempts < maxAttempts) {
            try {
                // 실제 TTS 서비스 상태 확인
                val googleAvailable = googleTtsPlayer.isAvailable()
                val samsungAvailable = samsungTtsPlayer.isAvailable()
                
                Log.d("TtsIntegrationTest", "TTS 상태 확인: Google=$googleAvailable, Samsung=$samsungAvailable")
                
                if (googleAvailable || samsungAvailable) {
                    Log.d("TtsIntegrationTest", "TTS 초기화 완료 확인")
                    return
                }
            } catch (e: Exception) {
                Log.w("TtsIntegrationTest", "TTS 상태 확인 중 오류: ${e.message}")
            }
            
            Log.d("TtsIntegrationTest", "TTS 초기화 대기 중... (시도 ${attempts + 1}/$maxAttempts)")
            Thread.sleep(500) // 0.5초 대기
            attempts++
        }
        
        Log.w("TtsIntegrationTest", "TTS 초기화 시간 초과, 계속 진행")
    }

    // 영문 TTS가 정상적으로 재생되어야 함
    @Test
    fun english_tts_should_play_normally() = runTest {
        // Given
        val englishText = "What do you like to do in your free time?"
        var callbackCalled = false
        
        // When
        val result = ttsOrchestrator.speak(englishText) {
            callbackCalled = true
        }
        
        // Then
        assertTrue("TTS 재생이 성공해야 함", result)
        
        // TTS 재생 완료 이벤트 대기 (최대 10초)
        var attempts = 0
        val maxAttempts = 20
        
        while (attempts < maxAttempts && !callbackCalled) {
            kotlinx.coroutines.delay(500) // 0.5초 대기
            attempts++
            Log.d("TtsIntegrationTest", "영문 TTS 콜백 대기 중... (시도 $attempts/$maxAttempts)")
        }
        
        assertTrue("콜백이 호출되어야 함", callbackCalled)
    }

    // 한글 TTS가 정상적으로 재생되어야 함
    @Test
    fun korean_tts_should_play_normally() = runTest {
        // Given
        val koreanText = "여가 시간에 무엇을 하시나요?"
        var callbackCalled = false
        
        // When
        val result = ttsOrchestrator.speak(koreanText) {
            callbackCalled = true
        }
        
        // Then
        assertTrue("TTS 재생이 성공해야 함", result)
        
        // TTS 재생 완료 이벤트 대기 (최대 10초)
        var attempts = 0
        val maxAttempts = 20
        
        while (attempts < maxAttempts && !callbackCalled) {
            kotlinx.coroutines.delay(500) // 0.5초 대기
            attempts++
            Log.d("TtsIntegrationTest", "한글 TTS 콜백 대기 중... (시도 $attempts/$maxAttempts)")
        }
        
        assertTrue("콜백이 호출되어야 함", callbackCalled)
    }

    // TTS 서비스 이름이 올바르게 반환되어야 함
    @Test
    fun tts_service_name_should_be_correct() {
        // When & Then
        assertEquals("Google TTS", googleTtsPlayer.getServiceName())
        // 실제 기기에서는 "삼성 TTS"로 표시됨
        assertEquals("삼성 TTS", samsungTtsPlayer.getServiceName())
    }

    private fun mockAudioPlayer(): com.na982.opichelper.domain.audio.AudioPlayer {
        return object : com.na982.opichelper.domain.audio.AudioPlayer {
            override fun play(file: java.io.File, onComplete: () -> Unit) {
                // 테스트용 더미 구현
                onComplete()
            }
            
            override fun stop() {
                // 테스트용 더미 구현
            }
            
            override val isPlaying: Boolean = false
            
            override fun getDuration(filePath: String): Int {
                return 1000 // 테스트용 더미 값
            }
            
            override fun playAudio(filePath: String) {
                // 테스트용 더미 구현
            }

            override fun stopAudio() {
                // 테스트용 더미 구현
            }
        }
    }
} 