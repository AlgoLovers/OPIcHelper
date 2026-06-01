package com.na982.opichelper.service

import android.app.Application
import android.content.Context
import android.os.Build
import com.na982.opichelper.domain.repository.TtsServiceController
import javax.inject.Inject

class TtsServiceControllerImpl @Inject constructor(
    private val application: Application
) : TtsServiceController {

    @Suppress("NewApi")
    override fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(TtsForegroundService.startIntent(application))
        } else {
            application.startService(TtsForegroundService.startIntent(application))
        }
    }

    override fun stopForegroundService() {
        application.stopService(TtsForegroundService.stopIntent(application))
    }

    @Suppress("NewApi")
    override fun updateNotificationSentence(sentenceEn: String?, sentenceKo: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startService(
                TtsForegroundService.updateSentenceIntent(application, sentenceEn, sentenceKo)
            )
        }
    }
}
