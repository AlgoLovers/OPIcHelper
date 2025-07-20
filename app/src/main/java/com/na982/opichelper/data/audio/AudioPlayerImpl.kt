package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import java.io.File
import com.na982.opichelper.domain.audio.AudioPlayer

class AudioPlayerImpl : AudioPlayer {
    private var player: MediaPlayer? = null
    override val isPlaying: Boolean
        get() = player?.isPlaying == true

    override fun play(file: File, onCompletion: () -> Unit) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
            setOnCompletionListener {
                stop()
                onCompletion()
            }
        }
    }

    override fun stop() {
        player?.release()
        player = null
    }
} 