package com.na982.opichelper.domain.usecase

import android.util.Log
import com.na982.opichelper.domain.audio.HighlightManager
import com.na982.opichelper.domain.audio.HighlightType
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.audio.strategy.RecordingHighlightStrategy
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.event.HighlightEventHandler
import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.repository.RecordingTimeManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 녹음 재생 전용 UseCase
 * 
 * 암기 레벨별로 다른 녹음 파일을 재생합니다:
 * - 영작테스트: 병합된 녹음 파일 재생
 * - 통암기: 통암기 녹음 파일 재생
 * - 반복듣기: 녹음 재생 없음
 */
@Singleton
class PlayRecordingUseCase @Inject constructor(
    private val recordingAudioPlayer: RecordingAudioPlayer,
    private val recordingFileRepository: RecordingFileRepository,
    private val recordingTimeManager: RecordingTimeManager,
    private val highlightManager: HighlightManager,
    private val highlightEventHandler: HighlightEventHandler
) {
    
    /**
     * 녹음 재생 실행
     * 
     * @param memorizeLevel 암기 레벨
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @param onCompletion 완료 콜백
     */
    suspend fun execute(
        memorizeLevel: MemorizeLevel,
        category: String,
        scriptIndex: Int,
        onCompletion: () -> Unit
    ) {
        Log.d("PlayRecordingUseCase", "녹음 재생 시작 - 레벨: $memorizeLevel, 카테고리: $category, 스크립트: $scriptIndex")
        
        try {
            when (memorizeLevel) {
                MemorizeLevel.ENGLISH_WRITING -> {
                    playEnglishWritingRecording(category, scriptIndex, onCompletion)
                }
                MemorizeLevel.FULL_MEMORIZATION -> {
                    playFullMemorizationRecording(category, scriptIndex, onCompletion)
                }
                MemorizeLevel.REPEAT_LISTENING -> {
                    Log.d("PlayRecordingUseCase", "반복듣기 모드 - 녹음 재생 없음")
                    onCompletion()
                }
            }
        } catch (e: Exception) {
            Log.e("PlayRecordingUseCase", "녹음 재생 실패", e)
            onCompletion()
        }
    }
    
    /**
     * 영작테스트 녹음 파일 재생
     */
    private suspend fun playEnglishWritingRecording(
        category: String,
        scriptIndex: Int,
        onCompletion: () -> Unit
    ) {
        Log.d("PlayRecordingUseCase", "영작테스트 녹음 재생 시작")
        
        // 영작테스트 병합 녹음 파일 경로 가져오기
        val recordingPath = recordingFileRepository.getEnglishWritingRecordingPath(category, scriptIndex)
        
        if (recordingPath != null) {
            Log.d("PlayRecordingUseCase", "영작테스트 녹음 파일 발견: $recordingPath")
            
            // 녹음 시간 데이터 가져오기
            val recordingTimes = recordingTimeManager.getAllRecordingTimes(category, scriptIndex)
            Log.d("PlayRecordingUseCase", "영작테스트 녹음 시간 데이터: $recordingTimes")
            
            // 하이라이트 전략 설정
            val strategy = RecordingHighlightStrategy(recordingTimes, HighlightType.ENGLISH_WRITING_RECORDING)
            highlightManager.setStrategy(strategy)
            
            // 녹음 파일 재생 (이벤트 기반)
            recordingAudioPlayer.playRecordingWithTimes(recordingPath, recordingTimes, {}, onCompletion)
        } else {
            Log.d("PlayRecordingUseCase", "영작테스트 녹음 파일 없음 - 영작테스트를 먼저 완료해주세요")
            // TODO: 사용자에게 "영작테스트를 먼저 완료해주세요" 메시지 표시
            onCompletion()
        }
    }
    
    /**
     * 통암기 녹음 파일 재생
     */
    private suspend fun playFullMemorizationRecording(
        category: String,
        scriptIndex: Int,
        onCompletion: () -> Unit
    ) {
        Log.d("PlayRecordingUseCase", "통암기 녹음 재생 시작")
        
        // 통암기 녹음 파일 경로 가져오기
        val recordingPath = recordingFileRepository.getFullMemorizationRecordingPath(category, scriptIndex)
        
        if (recordingPath != null) {
            Log.d("PlayRecordingUseCase", "통암기 녹음 파일 발견: $recordingPath")
            
            // 녹음 시간 데이터 가져오기
            val recordingTimes = recordingTimeManager.getAllRecordingTimes(category, scriptIndex)
            Log.d("PlayRecordingUseCase", "통암기 녹음 시간 데이터: $recordingTimes")
            
            // 하이라이트 전략 설정
            val strategy = RecordingHighlightStrategy(recordingTimes, HighlightType.FULL_MEMORIZATION_RECORDING)
            highlightManager.setStrategy(strategy)
            
            // 녹음 파일 재생 (이벤트 기반)
            recordingAudioPlayer.playRecordingWithTimes(recordingPath, recordingTimes, {}, onCompletion)
        } else {
            Log.d("PlayRecordingUseCase", "통암기 녹음 파일 없음 - 통암기 녹음을 먼저 완료해주세요")
            // TODO: 사용자에게 "통암기 녹음을 먼저 완료해주세요" 메시지 표시
            onCompletion()
        }
    }
    

} 