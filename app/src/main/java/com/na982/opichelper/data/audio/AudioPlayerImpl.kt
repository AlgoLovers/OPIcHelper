package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import java.io.File
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.manager.AppLogger

class AudioPlayerImpl(private val appLogger: AppLogger) : AudioPlayer {
    @Volatile private var player: MediaPlayer? = null
    private val lock = Any()

    override fun play(file: File, onCompletion: () -> Unit) = synchronized(lock) {
        stop()
        player = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                start()

                setOnCompletionListener {
                    stop()
                    onCompletion()
                }

                setOnErrorListener { _, what, extra ->
                    appLogger.e("AudioPlayerImpl", "재생 오류: what=$what, extra=$extra")
                    stop()
                    onCompletion()
                    true
                }

            } catch (e: Exception) {
                appLogger.e("AudioPlayerImpl", "재생 중 오류 발생", e)
                stop()
                onCompletion()
            }
        }
    }

    override fun stop() = synchronized(lock) {
        try {
            player?.let { mediaPlayer ->
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            }
        } catch (e: Exception) {
            appLogger.e("AudioPlayerImpl", "stop 중 오류 발생", e)
        }
        player = null
    }

    override fun playAudio(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            play(file) { }
        } else {
            appLogger.e("AudioPlayerImpl", "파일이 존재하지 않음: $filePath")
        }
    }
}
