package com.na982.opichelper.domain.audio.strategy

import android.util.Log
import com.na982.opichelper.domain.audio.HighlightStrategy
import com.na982.opichelper.domain.audio.HighlightType

/**
 * 녹음 재생용 하이라이트 전략
 * 실제 녹음 시간을 기반으로 정확한 하이라이트를 제공합니다.
 */
class RecordingHighlightStrategy(
    private val recordingTimes: List<Long>,
    private val highlightType: HighlightType
) : HighlightStrategy {
    
    override fun calculateHighlightIndex(currentPositionMs: Int): Int {
        if (recordingTimes.isEmpty()) {
            Log.w("RecordingHighlightStrategy", "녹음 시간 데이터가 없음")
            return -1
        }
        
        // 누적 시간 계산
        var cumulativeTime = 0L
        for (i in recordingTimes.indices) {
            val sentenceDuration = recordingTimes[i]
            if (currentPositionMs < cumulativeTime + sentenceDuration) {
                Log.d("RecordingHighlightStrategy", "하이라이트 계산: 현재시간=${currentPositionMs}ms, 문장인덱스=$i, 누적시간=${cumulativeTime}ms, 문장시간=${sentenceDuration}ms")
                return i
            }
            cumulativeTime += sentenceDuration
        }
        
        // 마지막 문장
        val lastIndex = recordingTimes.size - 1
        Log.d("RecordingHighlightStrategy", "하이라이트 계산: 현재시간=${currentPositionMs}ms, 마지막문장인덱스=$lastIndex")
        return lastIndex
    }
    
    override fun getHighlightType(): HighlightType = highlightType
    
    override fun isValid(): Boolean = recordingTimes.isNotEmpty()
} 