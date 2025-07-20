package com.na982.opichelper.presentation.ui.component

import android.content.Context
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.*

class TtsServiceTest {
    private lateinit var ttsService: TtsService
    private val mockTts: TextToSpeech = mock()
    private val mockCallback: TtsService.HighlightCallback = mock()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        ttsService = TtsService()
        ttsService.tts = mockTts
        ttsService.isReady = true
        ttsService.setHighlightCallback(mockCallback)
    }

    @Test
    fun `speakQuestion calls speak with question mode`() {
        val spyService = spy(ttsService)
        spyService.speakQuestion("Hello", 1.0f)
        verify(spyService).speak("Hello", 1.0f, "question")
    }

    @Test
    fun `speakAnswer calls speak with answer mode`() {
        val spyService = spy(ttsService)
        spyService.speakAnswer("World", 0.8f)
        verify(spyService).speak("World", 0.8f, "answer")
    }

    @Test
    fun `setHighlightCallback stores and triggers callback`() {
        val callback = mock<TtsService.HighlightCallback>()
        ttsService.setHighlightCallback(callback)
        ttsService.highlightCallback?.onQuestionHighlight(2)
        verify(callback).onQuestionHighlight(2)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `speakBySentence triggers speak for each sentence`() = runTest {
        val spyService = spy(ttsService)
        val text = "Hello. World!"
        spyService.speakBySentence(text, repeatCount = 2, pauseRatio = 1.0f, rate = 1.0f)
        verify(spyService, atLeastOnce()).speak(any(), any(), eq("answer"))
    }

    @Test
    fun `stopTts cancels speakJob`() {
        ttsService.speakJob = mock()
        ttsService.stopTts()
        verify(ttsService.speakJob)?.cancel()
    }
} 