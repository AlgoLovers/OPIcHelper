package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.entity.RepeatListeningData
import kotlinx.coroutines.flow.SharedFlow

interface RepeatListeningRepository {
    val events: SharedFlow<MemorizeTestEvent>

    suspend fun executeRepeatListening(
        data: RepeatListeningData,
        repeatCount: Int = 5
    )

    suspend fun getResumeIndex(category: String, scriptIndex: Int, totalCount: Int): Int
}
