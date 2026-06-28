package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.manager.AppLogger
import java.io.File

class RecordingAudioPlayerImpl(appLogger: AppLogger) : BaseMediaPlayer(appLogger), RecordingAudioPlayer {

    override fun playRecording(filePath: String, onCompletion: () -> Unit) = synchronized(lock) {
        releasePlayer()

        val file = File(filePath)
        if (!file.exists()) {
            appLogger.e(tag(), "녹음 파일이 존재하지 않음: $filePath")
            onCompletion()
            return
        }

        player = MediaPlayer().apply {
            prepareAndStart(file, onCompletion)
        }
    }

    override fun stopRecording() = synchronized(lock) {
        releasePlayer()
    }

    override fun tag() = "RecordingAudioPlayerImpl"
}
