package com.na982.opichelper.data.repository

import android.util.Log
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.audio.RepeatListeningUiCallback
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import com.na982.opichelper.domain.repository.ProgressData
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.usecase.RepeatListeningService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 반복듣기 Repository 구현체
 * 
 * 클린 아키텍처 원칙:
 * - Data Layer에서 Repository 인터페이스 구현
 * - Infrastructure Layer에 의존
 * - 실제 비즈니스 로직 처리
 */
@Singleton
class RepeatListeningRepositoryImpl @Inject constructor(
    private val repeatListeningService: RepeatListeningService,
    private val progressTracker: MemorizeTestProgressTracker
) : RepeatListeningRepository {
    
    override suspend fun executeRepeatListening(
        data: RepeatListeningData,
        uiCallback: RepeatListeningUiCallback,
        repeatCount: Int
    ) {
        Log.d("RepeatListeningRepositoryImpl", "반복듣기 Repository 실행 시작")
        
        try {
            repeatListeningService.startRepeatListening(
                data = data,
                uiCallback = uiCallback,
                repeatCount = repeatCount
            )
        } catch (e: Exception) {
            Log.e("RepeatListeningRepositoryImpl", "반복듣기 실행 중 오류", e)
            throw e
        }
    }
    
    override suspend fun getCurrentProgress(category: String, scriptIndex: Int): ProgressData? {
        val progress = progressTracker.getScriptProgress(category, scriptIndex, "반복 듣기")
        return progress?.let {
            ProgressData(
                category = it.category,
                scriptIndex = it.scriptIndex,
                memorizeLevel = it.memorizeLevel,
                currentSentenceIndex = it.currentSentenceIndex,
                totalSentences = it.totalSentences,
                isMemorizeTestRunning = it.isMemorizeTestRunning
            )
        }
    }
    
    override suspend fun updateProgress(progressData: ProgressData) {
        progressTracker.updateProgress(
            category = progressData.category,
            scriptIndex = progressData.scriptIndex,
            memorizeLevel = progressData.memorizeLevel,
            currentSentenceIndex = progressData.currentSentenceIndex,
            totalSentences = progressData.totalSentences,
            isMemorizeTestRunning = progressData.isMemorizeTestRunning
        )
    }
    
    override suspend fun clearProgress(category: String, scriptIndex: Int) {
        progressTracker.clearScriptProgress(category, scriptIndex, "반복 듣기")
    }
} 