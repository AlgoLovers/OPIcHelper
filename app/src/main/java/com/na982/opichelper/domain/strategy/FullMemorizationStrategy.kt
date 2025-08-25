package com.na982.opichelper.domain.strategy

import android.util.Log
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase
import javax.inject.Inject
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * 통암기 전략 구현
 */
@ViewModelScoped
class FullMemorizationStrategy @Inject constructor(
    private val fullMemorizationUseCase: StartFullMemorizationUseCase
) : MemorizationStrategy {
    
    override suspend fun execute(
        category: String,
        scriptIndex: Int,
        answerKo: String,
        answerEn: String,
        uiCallback: MemorizationUiCallback
    ) {
        Log.d("FullMemorizationStrategy", "통암기 전략 실행 - 카테고리: $category, 스크립트: $scriptIndex")
        
        fullMemorizationUseCase.execute(
            category = category,
            scriptIndex = scriptIndex,
            onRecordingStateChange = { isRecording ->
                Log.d("FullMemorizationStrategy", "녹음 상태 변경: $isRecording")
                uiCallback.onRecordingStateChange(isRecording)
            },
            onPlayingStateChange = { isPlaying ->
                Log.d("FullMemorizationStrategy", "재생 상태 변경: $isPlaying")
                uiCallback.onPlayingStateChange(isPlaying)
            }
        )
    }
    
    override fun getMemorizeLevel(): MemorizeLevel = MemorizeLevel.FULL_MEMORIZATION
} 