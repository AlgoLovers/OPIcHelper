package com.na982.opichelper.domain.strategy

import android.util.Log
import com.na982.opichelper.domain.entity.MemorizeLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 녹음 재생 전략 팩토리
 * 암기레벨에 따라 적절한 녹음 재생 전략을 반환
 * 
 * 개선사항:
 * - Map을 사용한 전략 매핑으로 확장성 향상
 * - 새로운 전략 추가 시 Map만 수정하면 됨
 * - 에러 처리 개선
 */
@Singleton
class RecordingPlayStrategyFactory @Inject constructor(
    private val englishWritingRecordingStrategy: EnglishWritingRecordingStrategy,
    private val fullMemorizationRecordingStrategy: FullMemorizationRecordingStrategy,
    private val repeatListeningRecordingStrategy: RepeatListeningRecordingStrategy
) {
    
    /**
     * 전략 매핑 Map
     * 새로운 전략 추가 시 이 Map만 수정하면 됨
     */
    private val strategyMap: Map<MemorizeLevel, RecordingPlayStrategy> by lazy {
        mapOf(
            MemorizeLevel.ENGLISH_WRITING to englishWritingRecordingStrategy,
            MemorizeLevel.FULL_MEMORIZATION to fullMemorizationRecordingStrategy,
            MemorizeLevel.REPEAT_LISTENING to repeatListeningRecordingStrategy
        )
    }
    
    /**
     * 암기레벨에 따른 녹음 재생 전략 반환
     * @param level 암기레벨
     * @return 해당하는 녹음 재생 전략
     */
    fun getStrategy(level: MemorizeLevel): RecordingPlayStrategy {
        val strategy = strategyMap[level]
        if (strategy != null) {
            Log.d("RecordingPlayStrategyFactory", "${level.name} 녹음 재생 전략 선택")
            return strategy
        } else {
            Log.w("RecordingPlayStrategyFactory", "알 수 없는 암기레벨: $level, 기본 전략 반환")
            return repeatListeningRecordingStrategy // 기본 전략
        }
    }
    
    /**
     * 모든 전략 목록 반환
     */
    fun getAllStrategies(): List<RecordingPlayStrategy> {
        return strategyMap.values.toList()
    }
    
    /**
     * 사용 가능한 모든 암기레벨 반환
     */
    fun getAvailableLevels(): List<MemorizeLevel> {
        return strategyMap.keys.toList()
    }
} 