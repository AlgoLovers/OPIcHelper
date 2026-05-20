package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import java.io.File
import com.na982.opichelper.domain.audio.AudioPlayer
import android.util.Log

class AudioPlayerImpl : AudioPlayer {
    @Volatile private var player: MediaPlayer? = null
    private val lock = Any()

    override val isPlaying: Boolean
        get() = synchronized(lock) { player?.isPlaying == true }

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
                    Log.e("AudioPlayerImpl", "재생 오류: what=$what, extra=$extra")
                    stop()
                    onCompletion()
                    true
                }

            } catch (e: Exception) {
                Log.e("AudioPlayerImpl", "재생 중 오류 발생", e)
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
            Log.e("AudioPlayerImpl", "stop 중 오류 발생", e)
        }
        player = null
    }

    override fun release() {
        synchronized(lock) {
            try {
                stop()
            } catch (e: Exception) {
                Log.e("AudioPlayerImpl", "완전한 리소스 해제 중 오류", e)
            }
        }
    }

    override fun stopAudio() {
        synchronized(lock) { stop() }
    }
    
    override fun getDuration(filePath: String): Int {
        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            mediaPlayer.duration
        } catch (e: Exception) {
            Log.e("AudioPlayerImpl", "getDuration 실패: $filePath", e)
            0
        } finally {
            mediaPlayer.release()
        }
    }
    
    override fun playAudio(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            play(file) { }
        } else {
            Log.e("AudioPlayerImpl", "파일이 존재하지 않음: $filePath")
        }
    }
} 