package com.na982.opichelper.data.audio

import android.content.Context
import android.os.Build
import com.na982.opichelper.domain.manager.AppLogger
import java.util.*

class GoogleTtsPlayer(context: Context, appLogger: AppLogger) : BaseTtsPlayer(
    context = context,
    locale = Locale.US,
    serviceName = "Google TTS",
    logTag = "GoogleTtsPlayer",
    appLogger = appLogger
) {
    companion object {
        private const val DEFAULT_SPEECH_RATE_TIRAMISU = 0.8f
        private const val DEFAULT_SPEECH_RATE_LEGACY = 0.7f
    }

    override fun getSpeechRate(): Float = userSpeechRate ?: when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> DEFAULT_SPEECH_RATE_TIRAMISU
        else -> DEFAULT_SPEECH_RATE_LEGACY
    }
}
