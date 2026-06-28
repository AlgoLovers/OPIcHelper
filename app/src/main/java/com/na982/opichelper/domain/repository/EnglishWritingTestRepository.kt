package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import kotlinx.coroutines.flow.SharedFlow

interface EnglishWritingTestRepository {
    val events: SharedFlow<MemorizeTestEvent>

    suspend fun executeEnglishWritingTest(
        answerKo: String,
        answerEn: String,
        category: String,
        scriptIndex: Int
    )
}
