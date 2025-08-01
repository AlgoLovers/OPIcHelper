package com.na982.opichelper.domain.strategy

import android.util.Log
import com.na982.opichelper.domain.entity.MemorizeLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 암기 전략 팩토리
 * 암기레벨에 따라 적절한 전략을 반환
 */
@Singleton
class MemorizationStrategyFactory @Inject constructor(
    private val repeatListeningStrategy: RepeatListeningStrategy,
    private val englishWritingStrategy: EnglishWritingStrategy,
    private val fullMemorizationStrategy: FullMemorizationStrategy
) {
    
    /**
     * 암기레벨에 따른 전략 반환
     * @param level 암기레벨
     * @return 해당하는 전략
     */
    fun getStrategy(level: MemorizeLevel): MemorizationStrategy {
        return when (level) {
            MemorizeLevel.REPEAT_LISTENING -> {
                Log.d("MemorizationStrategyFactory", "반복듣기 전략 선택")
                repeatListeningStrategy
            }
            MemorizeLevel.ENGLISH_WRITING -> {
                Log.d("MemorizationStrategyFactory", "영작테스트 전략 선택")
                englishWritingStrategy
            }
            MemorizeLevel.FULL_MEMORIZATION -> {
                Log.d("MemorizationStrategyFactory", "통암기 전략 선택")
                fullMemorizationStrategy
            }
        }
    }
    
    /**
     * 모든 전략 목록 반환
     */
    fun getAllStrategies(): List<MemorizationStrategy> {
        return listOf(
            repeatListeningStrategy,
            englishWritingStrategy,
            fullMemorizationStrategy
        )
    }
} 