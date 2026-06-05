package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.manager.AppLogger
import java.io.File

class RecordingAudioPlayerImpl(private val appLogger: AppLogger) : RecordingAudioPlayer {
    @Volatile private var player: MediaPlayer? = null
    private val lock = Any()

    override fun playRecording(filePath: String, onCompletion: () -> Unit) = synchronized(lock) {
        stopRecordingInternal()

        val file = File(filePath)
        if (!file.exists()) {
            appLogger.e("RecordingAudioPlayerImpl", "녹음 파일이 존재하지 않음: $filePath")
            onCompletion()
            return
        }

        player = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                start()

                setOnCompletionListener {
                    stopRecording()
                    onCompletion()
                }

                setOnErrorListener { _, what, extra ->
                    appLogger.e("RecordingAudioPlayerImpl", "녹음 재생 오류: what=$what, extra=$extra")
                    stopRecording()
                    onCompletion()
                    true
                }

            } catch (e: Exception) {
                appLogger.e("RecordingAudioPlayerImpl", "녹음 재생 중 오류 발생", e)
                stopRecording()
                onCompletion()
            }
        }
    }

    override fun stopRecording() = synchronized(lock) {
        stopRecordingInternal()
    }

    private fun stopRecordingInternal() {
        try {
            player?.let { mediaPlayer ->
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            }
        } catch (e: Exception) {
            appLogger.e("RecordingAudioPlayerImpl", "녹음 중지 중 오류 발생", e)
        }
        player = null
    }
}
