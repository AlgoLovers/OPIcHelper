package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import android.util.Log
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import java.io.File

/**
 * 녹음 재생 전용 AudioPlayer 구현체
 * TTS와 완전히 분리된 독립적인 MediaPlayer 인스턴스 사용
 */
class RecordingAudioPlayerImpl : RecordingAudioPlayer {
    @Volatile private var player: MediaPlayer? = null
    
    override val isPlaying: Boolean
        get() = player?.isPlaying == true

    override fun playRecording(filePath: String, onCompletion: () -> Unit) {
        // 기존 재생 중지
        stopRecording()

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
    
    /**
     * 녹음 재생 시작 (동기적)
     */
    override fun startRecordingPlayback(filePath: String) {
        // 기존 재생 중지
        stopRecording()

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

    override fun stopRecording() {
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
        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            mediaPlayer.duration
        } catch (e: Exception) {
            Log.e("RecordingAudioPlayerImpl", "getDuration 실패: $filePath", e)
            0
        } finally {
            mediaPlayer.release()
        }
    }
} 