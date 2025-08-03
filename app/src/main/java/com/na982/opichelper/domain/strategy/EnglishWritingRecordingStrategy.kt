package com.na982.opichelper.domain.strategy

import android.util.Log
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.repository.RecordingFileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 영작테스트 녹음 재생 전략 구현
 */
@Singleton
class EnglishWritingRecordingStrategy @Inject constructor(
    private val recordingAudioPlayer: RecordingAudioPlayer,
    private val recordingFileRepository: RecordingFileRepository
) : RecordingPlayStrategy {
    
    override suspend fun playRecording(
        category: String,
        scriptIndex: Int,
        onHighlight: (Int) -> Unit,
        onCompletion: () -> Unit
    ) {
        Log.d("EnglishWritingRecordingStrategy", "영작테스트 녹음 재생 시작")
        
        // 영작테스트 병합 녹음 파일 경로 가져오기
        val recordingPath = recordingFileRepository.getEnglishWritingRecordingPath(category, scriptIndex)
        
        if (recordingPath != null) {
            Log.d("EnglishWritingRecordingStrategy", "영작테스트 녹음 파일 발견: $recordingPath")
            
            // 녹음 파일 재생
            recordingAudioPlayer.playRecording(recordingPath, onHighlight) {
                Log.d("EnglishWritingRecordingStrategy", "영작테스트 녹음 재생 완료")
                onCompletion()
            }
        } else {
            Log.d("EnglishWritingRecordingStrategy", "영작테스트 녹음 파일 없음 - 영작테스트를 먼저 완료해주세요")
            // TODO: 사용자에게 "영작테스트를 먼저 완료해주세요" 메시지 표시
            onCompletion()
        }
    }
    
    override fun getMemorizeLevel(): MemorizeLevel = MemorizeLevel.ENGLISH_WRITING
} 