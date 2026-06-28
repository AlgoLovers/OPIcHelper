package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import java.io.File
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.manager.AppLogger

class AudioPlayerImpl(appLogger: AppLogger) : BaseMediaPlayer(appLogger), AudioPlayer {

    private fun play(file: File, onCompletion: () -> Unit) = synchronized(lock) {
        releasePlayer()
        player = MediaPlayer().apply {
            prepareAndStart(file, onCompletion)
        }
    }

    override fun stop() = synchronized(lock) {
        releasePlayer()
    }

    override fun playAudio(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            play(file) { }
        } else {
            appLogger.e(tag(), "파일이 존재하지 않음: $filePath")
        }
    }

    override fun tag() = "AudioPlayerImpl"
}
