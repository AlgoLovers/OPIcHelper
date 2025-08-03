package com.na982.opichelper.domain.strategy

import android.util.Log
import com.na982.opichelper.domain.audio.EnglishWritingUiCallback
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 영작테스트 전략 구현
 */
@Singleton
class EnglishWritingStrategy @Inject constructor(
    private val englishWritingUseCase: StartEnglishWritingTestUseCase
) : MemorizationStrategy {
    
    override suspend fun execute(
        category: String,
        scriptIndex: Int,
        answerKo: String,
        answerEn: String,
        uiCallback: MemorizationUiCallback
    ) {
        Log.d("EnglishWritingStrategy", "영작테스트 전략 실행 - 카테고리: $category, 스크립트: $scriptIndex")
        
        englishWritingUseCase.execute(
            answerKo = answerKo,
            answerEn = answerEn,
            category = category,
            scriptIndex = scriptIndex,
            uiCallback = object : EnglishWritingUiCallback {
                override fun onCardFlip(isKorean: Boolean) {
                    Log.d("EnglishWritingStrategy", "카드 뒤집기: ${if (isKorean) "한글" else "영문"}")
                    uiCallback.onCardFlip(isKorean)
                }
                
                override fun onKoreanHighlight(index: Int) {
                    Log.d("EnglishWritingStrategy", "한글 하이라이트: $index")
                    uiCallback.onKoreanHighlight(index)
                }
                
                override fun onRecordingHighlight(index: Int) {
                    Log.d("EnglishWritingStrategy", "녹음 하이라이트: $index")
                    uiCallback.onRecordingHighlight(index)
                }
                
                override fun onRecordingStateChange(isRecording: Boolean) {
                    Log.d("EnglishWritingStrategy", "녹음 상태 변경: $isRecording")
                    uiCallback.onRecordingStateChange(isRecording)
                }
                
                override fun onMergedFileCreated() {
                    Log.d("EnglishWritingStrategy", "병합 파일 생성 완료")
                    uiCallback.onMergedFileCreated()
                }
                
                override fun onComplete() {
                    Log.d("EnglishWritingStrategy", "영작테스트 완료")
                    uiCallback.onComplete()
                }
            }
        )
    }
    
    override fun getMemorizeLevel(): MemorizeLevel = MemorizeLevel.ENGLISH_WRITING
} 