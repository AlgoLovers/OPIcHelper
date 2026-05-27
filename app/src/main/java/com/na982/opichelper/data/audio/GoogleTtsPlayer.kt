package com.na982.opichelper.data.audio

import android.content.Context
import android.os.Build
import java.util.*

class GoogleTtsPlayer(context: Context) : BaseTtsPlayer(
    context = context,
    locale = Locale.US,
    serviceName = "Google TTS",
    logTag = "GoogleTtsPlayer"
) {
    override fun getSpeechRate(): Float = userSpeechRate ?: when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> 0.8f
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> 0.8f
        else -> 0.7f
    }

    override fun getPitch(): Float = 1.0f
}
