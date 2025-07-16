package com.na982.opichelper.presentation.ui.component

import android.content.Context
import android.speech.tts.TextToSpeech
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import com.na982.opichelper.presentation.viewmodel.MainViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class TtsPlayerTest {
    private lateinit var context: Context
    private lateinit var tts: TextToSpeech
    private lateinit var ttsPlayer: TtsPlayer
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        tts = mockk(relaxed = true)
        mockkConstructor(TextToSpeech::class)
        every { anyConstructed<TextToSpeech>().setOnUtteranceProgressListener(any()) } just Awaits
        every { anyConstructed<TextToSpeech>().setSpeechRate(any()) } just Awaits
        every { anyConstructed<TextToSpeech>().speak(any(), any(), any(), any()) } just Awaits
        every { anyConstructed<TextToSpeech>().stop() } just Awaits
        every { anyConstructed<TextToSpeech>().shutdown() } just Awaits
        ttsPlayer = TtsPlayer(context)
        // tts mock을 직접 할당
        val ttsField = TtsPlayer::class.java.getDeclaredField("tts")
        ttsField.isAccessible = true
        ttsField.set(ttsPlayer, tts)
        // isReady를 true로 강제 세팅
        val isReadyField = TtsPlayer::class.java.getDeclaredField("isReady")
        isReadyField.isAccessible = true
        isReadyField.set(ttsPlayer, true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `speakQuestion should call tts speak and update question highlight only`() = runTest {
        val text = "Hello. How are you?"
        ttsPlayer.speakQuestion(text, rate = 1.0f)
        testDispatcher.scheduler.advanceUntilIdle()
        assert(ttsPlayer.currentQuestionSentenceIndex.value == 0)
        assert(ttsPlayer.currentAnswerSentenceIndex.value == null)
        ttsPlayer.stop()
        assert(ttsPlayer.currentQuestionSentenceIndex.value == null)
        assert(ttsPlayer.currentAnswerSentenceIndex.value == null)
    }

    @Test
    fun `speakAnswer should call tts speak and update answer highlight only`() = runTest {
        val text = "Hello. How are you?"
        ttsPlayer.speakAnswer(text, rate = 1.0f)
        testDispatcher.scheduler.advanceUntilIdle()
        assert(ttsPlayer.currentAnswerSentenceIndex.value == 0)
        assert(ttsPlayer.currentQuestionSentenceIndex.value == null)
        ttsPlayer.stop()
        assert(ttsPlayer.currentAnswerSentenceIndex.value == null)
        assert(ttsPlayer.currentQuestionSentenceIndex.value == null)
    }

    @Test
    fun `speakBySentence should update only answer highlight and not question highlight`() = runTest {
        val text = "Hello. How are you?"
        ttsPlayer.speakBySentence(text, repeatCount = 1, pauseRatio = 0.1f)
        testDispatcher.scheduler.advanceUntilIdle()
        assert(ttsPlayer.currentAnswerSentenceIndex.value == 0)
        assert(ttsPlayer.currentQuestionSentenceIndex.value == null)
        ttsPlayer.stop()
        assert(ttsPlayer.currentAnswerSentenceIndex.value == null)
        assert(ttsPlayer.currentQuestionSentenceIndex.value == null)
    }

    @Test
    fun `calcRestDuration returns correct value`() {
        val sentence = "Hello world."
        val duration = ttsPlayer.calcRestDuration(sentence, 2.0f)
        val expected = ((sentence.length * 50L).coerceAtLeast(800L) * 2.0f).toLong()
        assert(duration == expected)
    }

    @Test
    fun `categories should be set from injected map keys`() {
        val testMap = mapOf(
            "personal" to listOf(
                com.na982.opichelper.domain.entity.QaItem(
                    id = "1",
                    category = "personal",
                    questionEn = "Q1",
                    questionKo = "Q1K",
                    answerEn = "A1",
                    answerKo = "A1K"
                )
            ),
            "travel" to emptyList(),
            "custom" to emptyList()
        )
        val vm = MainViewModel(testMap)
        val cats = vm.uiState.value.categories
        assert(cats.contains("personal"))
        assert(cats.contains("travel"))
        assert(cats.contains("custom"))
        assert(cats.size == 3)
    }
} 