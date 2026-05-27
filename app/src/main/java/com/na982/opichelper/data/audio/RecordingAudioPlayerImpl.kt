package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import android.util.Log
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import java.io.File

class RecordingAudioPlayerImpl : RecordingAudioPlayer {
    @Volatile private var player: MediaPlayer? = null
    private var cachedDuration: Int = 0
    private var cachedDurationPath: String? = null
    private val lock = Any()

    override val isPlaying: Boolean
        get() = synchronized(lock) { player?.isPlaying == true }

    override fun playRecording(filePath: String, onCompletion: () -> Unit) = synchronized(lock) {
        stopRecordingInternal()

        val file = File(filePath)
        if (!file.exists()) {
            Log.e("RecordingAudioPlayerImpl", "녹음 파일이 존재하지 않음: $filePath")
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
                    Log.e("RecordingAudioPlayerImpl", "녹음 재생 오류: what=$what, extra=$extra")
                    stopRecording()
                    onCompletion()
                    true
                }

            } catch (e: Exception) {
                Log.e("RecordingAudioPlayerImpl", "녹음 재생 중 오류 발생", e)
                stopRecording()
                onCompletion()
            }
        }
    }

    override fun startRecordingPlayback(filePath: String) = synchronized(lock) {
        stopRecordingInternal()

        val file = File(filePath)
        if (!file.exists()) {
            Log.e("RecordingAudioPlayerImpl", "녹음 파일이 존재하지 않음: $filePath")
            return
        }

        player = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                start()

                setOnCompletionListener {
                    stopRecording()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("RecordingAudioPlayerImpl", "녹음 재생 오류 (동기): what=$what, extra=$extra")
                    stopRecording()
                    true
                }

            } catch (e: Exception) {
                Log.e("RecordingAudioPlayerImpl", "녹음 재생 중 오류 발생", e)
                stopRecording()
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
            Log.e("RecordingAudioPlayerImpl", "녹음 중지 중 오류 발생", e)
        }
        player = null
    }

    override fun getDuration(filePath: String): Int {
        if (cachedDurationPath == filePath && cachedDuration > 0) return cachedDuration
        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration
            cachedDuration = duration
            cachedDurationPath = filePath
            duration
        } catch (e: Exception) {
            Log.e("RecordingAudioPlayerImpl", "getDuration 실패: $filePath", e)
            0
        } finally {
            mediaPlayer.release()
        }
    }
}
