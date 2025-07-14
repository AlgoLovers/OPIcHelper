package com.na982.opichelper.presentation.ui.component

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import java.util.*

/**
 * TtsPlayer: 모듈형 TTS 유틸리티. Context와 lifecycle을 안전하게 관리하며,
 * 재생/정지/속도 등 확장 가능. 질문/답변 등 다양한 곳에서 재사용 가능.
 */
class TtsPlayer(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US // 영어 기본
            isReady = true
        }
    }

    fun speak(text: String, rate: Float = 0.8f) {
        if (isReady) {
            tts?.setSpeechRate(rate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}

/**
 * rememberTtsPlayer: Compose에서 안전하게 TtsPlayer를 관리하는 헬퍼
 */
@Composable
fun rememberTtsPlayer(context: Context): TtsPlayer {
    val ttsPlayer = remember { TtsPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsPlayer.shutdown()
        }
    }
    return ttsPlayer
} 