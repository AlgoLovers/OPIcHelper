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
    override fun getSpeechRate(): Float = userSpeechRate ?: when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> 0.8f
        else -> 0.7f
    }
}
