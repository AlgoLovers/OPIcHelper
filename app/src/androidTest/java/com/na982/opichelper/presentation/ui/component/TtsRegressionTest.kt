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
class TtsRegressionTest {

    private lateinit var ttsOrchestrator: TtsOrchestrator
    private lateinit var ttsPlaybackController: TtsPlaybackController
    private lateinit var googleTtsPlayer: GoogleTtsPlayer
    private lateinit var samsungTtsPlayer: SamsungTtsPlayer

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        googleTtsPlayer = GoogleTtsPlayer(context)
        samsungTtsPlayer = SamsungTtsPlayer(context)

        Log.d("TtsRegressionTest", "TTS 초기화 대기 중...")
        waitForTtsInitialization()
        Log.d("TtsRegressionTest", "TTS 초기화 완료")

        ttsOrchestrator = TtsOrchestrator(
            context = context,
            googleTtsPlayer = googleTtsPlayer,
            samsungTtsPlayer = samsungTtsPlayer
        )

        ttsPlaybackController = TtsPlaybackController(ttsOrchestrator)
    }

    private fun waitForTtsInitialization() {
        var attempts = 0
        val maxAttempts = 10

        while (attempts < maxAttempts) {
            if (googleTtsPlayer.isAvailable() && samsungTtsPlayer.isAvailable()) {
                Log.d("TtsRegressionTest", "TTS 초기화 완료 확인")
                return
            }

            Log.d("TtsRegressionTest", "TTS 초기화 대기 중... (시도 ${attempts + 1}/$maxAttempts)")
            Thread.sleep(500)
            attempts++
        }

        Log.w("TtsRegressionTest", "TTS 초기화 시간 초과, 계속 진행")
    }

    @Test
    fun should_not_have_complex_initialization_state_management() = runTest {
        val orchestratorClass = ttsOrchestrator::class.java
        val methods = orchestratorClass.declaredMethods.map { it.name }

        assertFalse(methods.contains("waitForInitialization"))
        assertFalse(methods.contains("setInitialized"))
        assertFalse(methods.contains("isInitialized"))

        assertTrue(methods.contains("speak"))
        assertTrue(methods.contains("stop"))
    }

    @Test
    fun should_not_have_duplicate_tts_instances() {
        val controllerClass = ttsPlaybackController::class.java
        val fields = controllerClass.declaredFields.map { it.name }

        assertTrue(fields.contains("ttsOrchestrator"))
    }

    @Test
    fun should_not_have_state_inconsistency() = runTest {
        val googleAvailable = googleTtsPlayer.isAvailable()
        val samsungAvailable = samsungTtsPlayer.isAvailable()

        assertTrue(googleAvailable || samsungAvailable)
    }

    @Test
    fun should_not_violate_dependency_inversion_principle() {
        val orchestratorClass = ttsOrchestrator::class.java
        val publicMethods = orchestratorClass.methods.map { it.name }

        assertFalse(publicMethods.contains("setInitialized"))
        assertFalse(publicMethods.contains("waitForInitialization"))
    }

    @Test
    fun should_have_simplified_state_management() = runTest {
        assertTrue(ttsPlaybackController.isPlaying.first() == false)
        assertTrue(ttsPlaybackController.isQuestionPlaying.first() == false)
        assertTrue(ttsPlaybackController.isAnswerPlaying.first() == false)
    }

    @Test
    fun should_not_have_tts_orchestrator_initialization_error() = runTest {
        val englishText = "Test text"
        val result = ttsOrchestrator.speak(englishText, null)

        assertTrue(result)
    }

    @Test
    fun should_not_have_async_initialization_error() = runTest {
        kotlinx.coroutines.delay(2000)

        val googleAvailable = googleTtsPlayer.isAvailable()
        val samsungAvailable = samsungTtsPlayer.isAvailable()

        assertTrue(googleAvailable || samsungAvailable)
    }

    @Test
    fun should_not_have_excessive_complexity() {
        val orchestratorClass = ttsOrchestrator::class.java
        val publicMethods = orchestratorClass.methods.filter {
            it.modifiers and java.lang.reflect.Modifier.PUBLIC != 0
        }

        val methodNames = publicMethods.map { it.name }
        assertFalse(methodNames.contains("bindTtsService"))
        assertFalse(methodNames.contains("unbindTtsService"))
        assertFalse(methodNames.contains("acquireWakeLock"))
        assertFalse(methodNames.contains("releaseWakeLock"))

        assertTrue(methodNames.contains("speak"))
        assertTrue(methodNames.contains("stop"))
    }

    @Test
    fun should_not_have_state_sync_issue() = runTest {
        ttsPlaybackController.playQuestion("Test question")

        assertTrue(ttsPlaybackController.isPlaying.first())
        assertTrue(ttsPlaybackController.isQuestionPlaying.first())
        assertFalse(ttsPlaybackController.isAnswerPlaying.first())

        ttsPlaybackController.stopTts()
        assertFalse(ttsPlaybackController.isPlaying.first())
        assertFalse(ttsPlaybackController.isQuestionPlaying.first())
        assertFalse(ttsPlaybackController.isAnswerPlaying.first())
    }

    @Test
    fun should_not_have_memory_leak_issue() {
        val orchestratorClass = ttsOrchestrator::class.java
        val fields = orchestratorClass.declaredFields.map { it.name }

        assertFalse(fields.contains("isInitialized"))
        assertFalse(fields.contains("initializationDeferred"))
        assertFalse(fields.contains("wakeLock"))
        assertFalse(fields.contains("audioFocusRequest"))

        assertTrue(fields.contains("googleTtsPlayer"))
        assertTrue(fields.contains("samsungTtsPlayer"))
        assertTrue(fields.contains("koreanTtsPlayers"))
        assertTrue(fields.contains("currentKoreanTtsIndex"))
    }

    @Test
    fun tts_service_name_should_be_correct() {
        assertEquals("Google TTS", googleTtsPlayer.getServiceName())
        assertEquals("삼성 TTS", samsungTtsPlayer.getServiceName())
    }

    @Test
    fun ttsPlaybackController_should_not_have_audioPlayer_dependency() {
        val controllerClass = ttsPlaybackController::class.java
        val fields = controllerClass.declaredFields.map { it.name }

        assertFalse(fields.contains("audioPlayer"))
        assertFalse(fields.contains("serviceConnection"))
        assertFalse(fields.contains("isServiceBound"))
        assertFalse(fields.contains("onStopOtherMemorizationMode"))
    }
}
