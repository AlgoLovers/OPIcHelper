package com.na982.opichelper.data.audio

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.na982.opichelper.domain.audio.HighlightManager
import com.na982.opichelper.domain.audio.HighlightStrategy
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.event.HighlightEventHandler
import com.na982.opichelper.domain.event.HighlightEvent
import com.na982.opichelper.domain.repository.RecordingTimeManager
import java.io.File
import javax.inject.Inject

/**
 * 녹음 재생 전용 AudioPlayer 구현체
 * TTS와 완전히 분리된 독립적인 MediaPlayer 인스턴스 사용
 */
class RecordingAudioPlayerImpl @Inject constructor(
    private val recordingTimeManager: RecordingTimeManager,
    private val highlightManager: HighlightManager,
    private val highlightEventHandler: HighlightEventHandler
) : RecordingAudioPlayer {
    private var player: MediaPlayer? = null
    private var positionUpdateHandler: Handler? = null
    private var positionUpdateRunnable: Runnable? = null
    
    override val isPlaying: Boolean
        get() = player?.isPlaying == true

    override fun playRecording(filePath: String, onHighlight: (Int) -> Unit, onCompletion: () -> Unit) {
        Log.d("RecordingAudioPlayerImpl", "녹음 재생 시작 (하이라이트 포함): $filePath")
        
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
                Log.d("RecordingAudioPlayerImpl", "setDataSource 완료")
                
                prepare()
                Log.d("RecordingAudioPlayerImpl", "prepare 완료")

                start()
                Log.d("RecordingAudioPlayerImpl", "start 완료")
                
                // 위치 업데이트 핸들러 초기화
                startPositionUpdates()
                
                // 하이라이트 매니저 시작 (이벤트 기반)
                highlightManager.startHighlightUpdates(highlightEventHandler)
                
                setOnCompletionListener {
                    Log.d("RecordingAudioPlayerImpl", "녹음 재생 완료")
                    stopPositionUpdates()
                    highlightManager.stopHighlightUpdates()
                    highlightEventHandler.handle(HighlightEvent.ClearHighlight)
                    stopRecording()
                    onCompletion()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("RecordingAudioPlayerImpl", "녹음 재생 오류: what=$what, extra=$extra")
                    stopPositionUpdates()
                    highlightManager.stopHighlightUpdates()
                    highlightEventHandler.handle(HighlightEvent.ClearHighlight)
                    stopRecording()
                    onCompletion()
                    true
                }
                
            } catch (e: Exception) {
                Log.e("RecordingAudioPlayerImpl", "녹음 재생 중 오류 발생", e)
                stopPositionUpdates()
                highlightManager.stopHighlightUpdates()
                highlightEventHandler.handle(HighlightEvent.ClearHighlight)
                stopRecording()
                onCompletion()
            }
        }
    }

    override fun playRecordingWithTimes(filePath: String, recordingTimes: List<Long>, onHighlight: (Int) -> Unit, onCompletion: () -> Unit) {
        Log.d("RecordingAudioPlayerImpl", "녹음 재생 시작 (시간 정보 포함): $filePath, 녹음시간: $recordingTimes")
        
        // 기존 재생 중지
        stopRecording()
        
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("RecordingAudioPlayerImpl", "녹음 파일이 존재하지 않음: $filePath")
            onCompletion()
            return
        }
        
        // 하이라이트 전략 설정
        val strategy = com.na982.opichelper.domain.audio.strategy.RecordingHighlightStrategy(
            recordingTimes, 
            com.na982.opichelper.domain.audio.HighlightType.ENGLISH_WRITING_RECORDING
        )
        highlightManager.setStrategy(strategy)
        
        player = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                Log.d("RecordingAudioPlayerImpl", "setDataSource 완료")
                
                prepare()
                Log.d("RecordingAudioPlayerImpl", "prepare 완료")

                start()
                Log.d("RecordingAudioPlayerImpl", "start 완료")
                
                // 위치 업데이트 핸들러 초기화
                startPositionUpdates()
                
                // 하이라이트 매니저 시작 (이벤트 기반)
                highlightManager.startHighlightUpdates(highlightEventHandler)
                
                setOnCompletionListener {
                    Log.d("RecordingAudioPlayerImpl", "녹음 재생 완료")
                    stopPositionUpdates()
                    highlightManager.stopHighlightUpdates()
                    highlightEventHandler.handle(HighlightEvent.ClearHighlight)
                    stopRecording()
                    onCompletion()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e("RecordingAudioPlayerImpl", "녹음 재생 오류: what=$what, extra=$extra")
                    stopPositionUpdates()
                    highlightManager.stopHighlightUpdates()
                    highlightEventHandler.handle(HighlightEvent.ClearHighlight)
                    stopRecording()
                    onCompletion()
                    true
                }
                
            } catch (e: Exception) {
                Log.e("RecordingAudioPlayerImpl", "녹음 재생 중 오류 발생", e)
                stopPositionUpdates()
                highlightManager.stopHighlightUpdates()
                highlightEventHandler.handle(HighlightEvent.ClearHighlight)
                stopRecording()
                onCompletion()
            }
        }
    }

    override fun playRecording(filePath: String, onCompletion: () -> Unit) {
        Log.d("RecordingAudioPlayerImpl", "녹음 재생 시작: $filePath")
        
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
                Log.d("RecordingAudioPlayerImpl", "setDataSource 완료")
                
                prepare()
                Log.d("RecordingAudioPlayerImpl", "prepare 완료")
                
                start()
                Log.d("RecordingAudioPlayerImpl", "start 완료")
                
                setOnCompletionListener {
                    Log.d("RecordingAudioPlayerImpl", "녹음 재생 완료")
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
        Log.d("RecordingAudioPlayerImpl", "녹음 재생 시작 (동기): $filePath")
        
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
                Log.d("RecordingAudioPlayerImpl", "setDataSource 완료")
                
                prepare()
                Log.d("RecordingAudioPlayerImpl", "prepare 완료")
                
                start()
                Log.d("RecordingAudioPlayerImpl", "start 완료")
                
            } catch (e: Exception) {
                Log.e("RecordingAudioPlayerImpl", "녹음 재생 중 오류 발생", e)
                stopRecording()
            }
        }
    }

    override fun stopRecording() {
        try {
            stopPositionUpdates()
            highlightManager.stopHighlightUpdates()
            player?.let { mediaPlayer ->
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
                Log.d("RecordingAudioPlayerImpl", "녹음 MediaPlayer 해제 완료")
            }
        } catch (e: Exception) {
            Log.e("RecordingAudioPlayerImpl", "녹음 중지 중 오류 발생", e)
        }
        player = null
    }
    
    override fun getDuration(filePath: String): Int {
        return try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration
            mediaPlayer.release()
            duration
        } catch (e: Exception) {
            Log.e("RecordingAudioPlayerImpl", "getDuration 실패: $filePath", e)
            0
        }
    }
    
    /**
     * 위치 업데이트 시작
     */
    private fun startPositionUpdates() {
        positionUpdateHandler = Handler(Looper.getMainLooper())
        
        positionUpdateRunnable = object : Runnable {
            override fun run() {
                player?.let { mediaPlayer ->
                    if (mediaPlayer.isPlaying) {
                        val currentPosition = mediaPlayer.currentPosition
                        highlightManager.updateCurrentPosition(currentPosition)
                        
                        // 100ms마다 업데이트
                        positionUpdateHandler?.postDelayed(this, 100)
                    }
                }
            }
        }
        
        positionUpdateHandler?.post(positionUpdateRunnable!!)
    }
    
    /**
     * 위치 업데이트 중지
     */
    private fun stopPositionUpdates() {
        positionUpdateRunnable?.let { runnable ->
            positionUpdateHandler?.removeCallbacks(runnable)
        }
        positionUpdateRunnable = null
        positionUpdateHandler = null
    }
} 