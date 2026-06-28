package com.na982.opichelper.data.audio

import android.content.Context
import android.os.Build
import com.na982.opichelper.domain.manager.AppLogger
import java.util.*

class SamsungTtsPlayer(context: Context, appLogger: AppLogger) : BaseTtsPlayer(
    context = context,
    locale = Locale.KOREAN,
    serviceName = "삼성 TTS",
    logTag = "SamsungTtsPlayer",
    appLogger = appLogger
) {
    companion object {
        private const val DEFAULT_SPEECH_RATE_UPSIDE_DOWN_CAKE = 1.1f
        private const val DEFAULT_SPEECH_RATE_TIRAMISU = 0.9f
        private const val DEFAULT_SPEECH_RATE_LEGACY = 0.8f
    }

    override fun getSpeechRate(): Float = userSpeechRate ?: when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> DEFAULT_SPEECH_RATE_UPSIDE_DOWN_CAKE
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> DEFAULT_SPEECH_RATE_TIRAMISU
        else -> DEFAULT_SPEECH_RATE_LEGACY
    }
}
