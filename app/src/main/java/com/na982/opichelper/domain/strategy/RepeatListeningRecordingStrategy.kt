package com.na982.opichelper.domain.strategy

import android.util.Log
import com.na982.opichelper.domain.entity.MemorizeLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 반복듣기 녹음 재생 전략 구현
 * 반복듣기 모드에서는 녹음 재생이 없음
 */
@Singleton
class RepeatListeningRecordingStrategy @Inject constructor() : RecordingPlayStrategy {
    
    override suspend fun playRecording(
        category: String,
        scriptIndex: Int,
        onHighlight: (Int) -> Unit,
        onCompletion: () -> Unit
    ) {
        Log.d("RepeatListeningRecordingStrategy", "반복듣기 모드 - 녹음 재생 없음")
        onCompletion()
    }
    
    override fun getMemorizeLevel(): MemorizeLevel = MemorizeLevel.REPEAT_LISTENING
} 