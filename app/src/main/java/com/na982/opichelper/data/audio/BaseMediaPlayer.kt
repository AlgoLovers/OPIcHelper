package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import com.na982.opichelper.domain.manager.AppLogger
import java.io.File

abstract class BaseMediaPlayer(protected val appLogger: AppLogger) {
    @Volatile
    protected var player: MediaPlayer? = null
    protected val lock = Any()

    protected fun releasePlayer() {
        try {
            player?.let { mediaPlayer ->
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            }
        } catch (e: Exception) {
            appLogger.e(tag(), "미디어 플레이어 해제 중 오류", e)
        }
        player = null
    }

    protected fun MediaPlayer.prepareAndStart(
        file: File,
        onCompletion: () -> Unit
    ) {
        try {
            setDataSource(file.absolutePath)
            prepare()
            start()

            setOnCompletionListener {
                releasePlayer()
                onCompletion()
            }

            setOnErrorListener { _, what, extra ->
                appLogger.e(tag(), "재생 오류: what=$what, extra=$extra")
                releasePlayer()
                onCompletion()
                true
            }
        } catch (e: Exception) {
            appLogger.e(tag(), "재생 중 오류 발생", e)
            releasePlayer()
            onCompletion()
        }
    }

    protected abstract fun tag(): String
}
