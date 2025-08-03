package com.na982.opichelper.domain.strategy

import android.util.Log
import com.na982.opichelper.domain.entity.MemorizeLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 암기 전략 팩토리
 * 암기레벨에 따라 적절한 전략을 반환
 * 
 * 개선사항:
 * - Map을 사용한 전략 매핑으로 확장성 향상
 * - 새로운 전략 추가 시 Map만 수정하면 됨
 * - 에러 처리 개선
 */
@Singleton
class MemorizationStrategyFactory @Inject constructor(
    private val repeatListeningStrategy: RepeatListeningStrategy,
    private val englishWritingStrategy: EnglishWritingStrategy,
    private val fullMemorizationStrategy: FullMemorizationStrategy
) {
    
    /**
     * 전략 매핑 Map
     * 새로운 전략 추가 시 이 Map만 수정하면 됨
     */
    private val strategyMap: Map<MemorizeLevel, MemorizationStrategy> by lazy {
        mapOf(
            MemorizeLevel.REPEAT_LISTENING to repeatListeningStrategy,
            MemorizeLevel.ENGLISH_WRITING to englishWritingStrategy,
            MemorizeLevel.FULL_MEMORIZATION to fullMemorizationStrategy
        )
    }
    
    /**
     * 암기레벨에 따른 전략 반환
     * @param level 암기레벨
     * @return 해당하는 전략
     */
    fun getStrategy(level: MemorizeLevel): MemorizationStrategy {
        val strategy = strategyMap[level]
        if (strategy != null) {
            Log.d("MemorizationStrategyFactory", "${level.name} 전략 선택")
            return strategy
        } else {
            Log.w("MemorizationStrategyFactory", "알 수 없는 암기레벨: $level, 기본 전략 반환")
            return repeatListeningStrategy // 기본 전략
        }
    }
    
    /**
     * 모든 전략 목록 반환
     */
    fun getAllStrategies(): List<MemorizationStrategy> {
        return strategyMap.values.toList()
    }
    
    /**
     * 사용 가능한 모든 암기레벨 반환
     */
    fun getAvailableLevels(): List<MemorizeLevel> {
        return strategyMap.keys.toList()
    }
} 