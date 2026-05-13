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

    suspend fun getCurrentProgress(category: String, scriptIndex: Int): ProgressData?
    suspend fun updateProgress(progressData: ProgressData)
    suspend fun clearProgress(category: String, scriptIndex: Int)
}

data class ProgressData(
    val category: String,
    val scriptIndex: Int,
    val memorizeLevel: String,
    val currentSentenceIndex: Int,
    val totalSentences: Int,
    val isMemorizeTestRunning: Boolean
)
