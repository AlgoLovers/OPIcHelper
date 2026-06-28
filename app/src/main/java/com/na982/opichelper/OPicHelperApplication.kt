package com.na982.opichelper

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.usecase.FullMemorizationUseCase
import com.na982.opichelper.domain.usecase.PlayMergedFileUseCase
import javax.inject.Inject

@HiltAndroidApp
class OPicHelperApplication : Application() {
    @Inject
    lateinit var ttsOrchestrator: TtsOrchestrator

    @Inject
    lateinit var ttsPlaybackController: TtsPlaybackController

    @Inject
    lateinit var fullMemorizationUseCase: FullMemorizationUseCase

    @Inject
    lateinit var playMergedFileUseCase: PlayMergedFileUseCase

    override fun onTerminate() {
        super.onTerminate()
        closeResources()
    }

    fun closeResources() {
        try { ttsPlaybackController.close() } catch (_: Exception) {}
        try { fullMemorizationUseCase.close() } catch (_: Exception) {}
        try { playMergedFileUseCase.close() } catch (_: Exception) {}
    }
} 