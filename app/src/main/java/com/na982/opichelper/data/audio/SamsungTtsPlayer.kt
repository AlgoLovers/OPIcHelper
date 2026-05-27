package com.na982.opichelper.data.audio

import android.content.Context
import android.os.Build
import java.util.*

class SamsungTtsPlayer(context: Context) : BaseTtsPlayer(
    context = context,
    locale = Locale.KOREAN,
    serviceName = "삼성 TTS",
    logTag = "SamsungTtsPlayer"
) {
    override fun getSpeechRate(): Float = userSpeechRate ?: when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> 1.1f
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> 0.9f
        else -> 0.8f
    }

    override fun getPitch(): Float = 1.0f
}
