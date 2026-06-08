package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.RepeatListeningProgress
import com.na982.opichelper.domain.entity.RepeatListeningData
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface RepeatListeningRepository {
    val events: SharedFlow<MemorizeTestEvent>

    val repeatProgress: StateFlow<RepeatListeningProgress?>

    suspend fun executeRepeatListening(
        data: RepeatListeningData,
        repeatCount: Int = 5
    )

    suspend fun getResumeIndex(category: String, scriptIndex: Int, totalCount: Int): Int

    fun requestExtraRepetitions(count: Int)
}
