package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import java.io.File
import com.na982.opichelper.domain.audio.AudioPlayer
import android.util.Log

class AudioPlayerImpl : AudioPlayer {
    private var player: MediaPlayer? = null
    override val isPlaying: Boolean
        get() = player?.isPlaying == true

    override fun play(file: File, onCompletion: () -> Unit) {
        Log.d("AudioPlayerImpl", "재생 시작: ${file.absolutePath}, 파일 크기: ${file.length()} bytes, 존재: ${file.exists()}")
        
        stop()
        player = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                Log.d("AudioPlayerImpl", "setDataSource 완료")
                
                prepare()
                Log.d("AudioPlayerImpl", "prepare 완료")
                
                start()
                Log.d("AudioPlayerImpl", "start 완료")
                
                setOnCompletionListener {
                    Log.d("AudioPlayerImpl", "재생 완료 콜백 호출")
                    stop()
                    onCompletion()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerImpl", "재생 오류: what=$what, extra=$extra")
                    stop()
                    onCompletion()
                    true
                }
                
                setOnInfoListener { _, what, extra ->
                    Log.d("AudioPlayerImpl", "재생 정보: what=$what, extra=$extra")
                    false
                }
                
            } catch (e: Exception) {
                Log.e("AudioPlayerImpl", "재생 중 오류 발생", e)
                stop()
                onCompletion()
            }
        }
    }

    override fun stop() {
        try {
            player?.let { mediaPlayer ->
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
                Log.d("AudioPlayerImpl", "MediaPlayer 해제 완료")
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerImpl", "stop 중 오류 발생", e)
        }
        player = null
    }
    
    /**
     * 완전한 리소스 해제 (앱 종료 시 사용)
     */
    override fun release() {
        try {
            Log.d("AudioPlayerImpl", "완전한 리소스 해제 시작")
            
            // 1. 재생 중지
            stop()
            
            // 2. 추가 정리 작업 (필요시)
            Log.d("AudioPlayerImpl", "완전한 리소스 해제 완료")
        } catch (e: Exception) {
            Log.e("AudioPlayerImpl", "완전한 리소스 해제 중 오류", e)
        }
    }
    
    override fun stopAudio() {
        stop()
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
            play(file) {
                Log.d("AudioPlayerImpl", "playAudio 완료: $filePath")
            }
        } else {
            Log.e("AudioPlayerImpl", "파일이 존재하지 않음: $filePath")
        }
    }
} 