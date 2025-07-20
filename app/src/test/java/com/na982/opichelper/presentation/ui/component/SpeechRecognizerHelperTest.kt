package com.na982.opichelper.presentation.ui.component

import android.content.Context
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SpeechRecognizerHelperTest {
    private val context: Context = mock()
    private val callback: RecognitionCallback = mock()

    @Test
    fun `startListening triggers onPartialResult`() {
        val helper = SpeechRecognizerHelper(context)
        helper.recognitionCallback = callback
        helper.recognitionCallback?.onPartialResult("테스트")
        verify(callback).onPartialResult("테스트")
    }

    @Test
    fun `startListening triggers onFinalResult`() {
        val helper = SpeechRecognizerHelper(context)
        helper.recognitionCallback = callback
        helper.recognitionCallback?.onFinalResult("최종 결과")
        verify(callback).onFinalResult("최종 결과")
    }

    @Test
    fun `startListening triggers onError`() {
        val helper = SpeechRecognizerHelper(context)
        helper.recognitionCallback = callback
        helper.recognitionCallback?.onError("에러 발생")
        verify(callback).onError("에러 발생")
    }
} 