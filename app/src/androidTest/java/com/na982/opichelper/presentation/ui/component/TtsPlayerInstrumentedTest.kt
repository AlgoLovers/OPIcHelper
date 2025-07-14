package com.na982.opichelper.presentation.ui.component

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TtsPlayerInstrumentedTest {

    @Test
    fun ttsPlayer_speak_doesNotThrow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ttsPlayer = TtsPlayer(context)
        // 실제로 speak를 호출해도 예외가 발생하지 않는지 확인
        ttsPlayer.speak("Hello, this is a test.", rate = 0.8f)
        // 종료
        ttsPlayer.shutdown()
    }
} 