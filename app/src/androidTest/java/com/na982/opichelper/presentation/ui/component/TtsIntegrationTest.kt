package com.na982.opichelper.presentation.ui.component

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.audio.HighlightStateHolder
import com.na982.opichelper.domain.audio.TtsSpeakResult
import com.na982.opichelper.data.audio.GoogleTtsPlayer
import com.na982.opichelper.data.audio.SamsungTtsPlayer
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

        googleTtsPlayer = GoogleTtsPlayer(context)
        samsungTtsPlayer = SamsungTtsPlayer(context)

        Log.d("TtsIntegrationTest", "TTS 초기화 대기 중...")
        waitForTtsInitialization()
        Log.d("TtsIntegrationTest", "TTS 초기화 완료")

        ttsOrchestrator = TtsOrchestrator(
            googleTtsPlayer = googleTtsPlayer,
            samsungTtsPlayer = samsungTtsPlayer
        )

        val highlightStateHolder = HighlightStateHolder()
        ttsPlaybackController = TtsPlaybackController(ttsOrchestrator, highlightStateHolder)
    }

    private fun waitForTtsInitialization() {
        Log.d("TtsIntegrationTest", "TTS 초기화 대기 중...")

        var attempts = 0
        val maxAttempts = 20

        while (attempts < maxAttempts) {
            try {
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
            Thread.sleep(500)
            attempts++
        }

        Log.w("TtsIntegrationTest", "TTS 초기화 시간 초과, 계속 진행")
    }

    @Test
    fun english_tts_should_play_normally() = runTest {
        val englishText = "What do you like to do in your free time?"
        val result = ttsOrchestrator.speak(englishText)
        assertTrue("TTS 재생이 성공해야 함", result is TtsSpeakResult.Success)
    }

    @Test
    fun korean_tts_should_play_normally() = runTest {
        val koreanText = "여가 시간에 무엇을 하시나요?"
        val result = ttsOrchestrator.speak(koreanText)
        assertTrue("TTS 재생이 성공해야 함", result is TtsSpeakResult.Success)
    }

    @Test
    fun tts_service_name_should_be_correct() {
        assertEquals("Google TTS", googleTtsPlayer.getServiceName())
        assertEquals("삼성 TTS", samsungTtsPlayer.getServiceName())
    }
}
