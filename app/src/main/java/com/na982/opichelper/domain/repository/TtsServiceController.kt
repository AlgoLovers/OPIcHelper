package com.na982.opichelper.domain.repository

interface TtsServiceController {
    fun startForegroundService()
    fun stopForegroundService()
    fun updateNotificationSentence(sentenceEn: String?, sentenceKo: String?)
}
