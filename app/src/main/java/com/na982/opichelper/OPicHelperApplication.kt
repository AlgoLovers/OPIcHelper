package com.na982.opichelper

import android.app.Application
import com.na982.opichelper.domain.audio.TtsOrchestrator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OPicHelperApplication : Application() {
    @Inject
    lateinit var ttsOrchestrator: TtsOrchestrator
} 