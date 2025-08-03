package com.na982.opichelper.domain.strategy

import android.util.Log
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.repository.RecordingFileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 통암기 녹음 재생 전략 구현
 */
@Singleton
class FullMemorizationRecordingStrategy @Inject constructor(
    private val recordingAudioPlayer: RecordingAudioPlayer,
    private val recordingFileRepository: RecordingFileRepository
) : RecordingPlayStrategy {
    
    override suspend fun playRecording(
        category: String,
        scriptIndex: Int,
        onHighlight: (Int) -> Unit,
        onCompletion: () -> Unit
    ) {
        Log.d("FullMemorizationRecordingStrategy", "통암기 녹음 재생 시작")
        
        // 통암기 녹음 파일 경로 가져오기
        val recordingPath = recordingFileRepository.getFullMemorizationRecordingPath(category, scriptIndex)
        
        if (recordingPath != null) {
            Log.d("FullMemorizationRecordingStrategy", "통암기 녹음 파일 발견: $recordingPath")
            
            // 녹음 파일 재생
            recordingAudioPlayer.playRecording(recordingPath, onHighlight) {
                Log.d("FullMemorizationRecordingStrategy", "통암기 녹음 재생 완료")
                onCompletion()
            }
        } else {
            Log.d("FullMemorizationRecordingStrategy", "통암기 녹음 파일 없음 - 통암기 녹음을 먼저 완료해주세요")
            // TODO: 사용자에게 "통암기 녹음을 먼저 완료해주세요" 메시지 표시
            onCompletion()
        }
    }
    
    override fun getMemorizeLevel(): MemorizeLevel = MemorizeLevel.FULL_MEMORIZATION
} 