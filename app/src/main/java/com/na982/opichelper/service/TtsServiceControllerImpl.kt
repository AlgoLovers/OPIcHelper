package com.na982.opichelper.service

import android.app.Application
import android.content.Context
import android.os.Build
import com.na982.opichelper.domain.repository.TtsServiceController

class TtsServiceControllerImpl(
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

    // stopIntent는 Intent를 만들 뿐이라 O 미만에서도 안전하다. lint는 클래스의
    // @RequiresApi(O)를 보고 경고하지만, startForegroundService()가 O 미만에서
    // startService로 폴백하므로 서비스는 실제로 시작될 수 있고 그럼 정지도 필요하다.
    @Suppress("NewApi")
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
