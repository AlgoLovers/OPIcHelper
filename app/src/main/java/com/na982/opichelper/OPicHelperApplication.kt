package com.na982.opichelper

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.na982.opichelper.domain.audio.TtsOrchestrator
import javax.inject.Inject

@HiltAndroidApp
class OPicHelperApplication : Application() {
    @Inject
    lateinit var ttsOrchestrator: TtsOrchestrator
} 