package com.na982.opichelper.domain.strategy

import android.util.Log
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 반복듣기 전략 구현
 */
@Singleton
class RepeatListeningStrategy @Inject constructor(
    private val repeatListeningUseCase: StartRepeatListeningUseCase
) : MemorizationStrategy {
    
    override suspend fun execute(
        category: String,
        scriptIndex: Int,
        answerKo: String,
        answerEn: String,
        uiCallback: MemorizationUiCallback
    ) {
        Log.d("RepeatListeningStrategy", "반복듣기 전략 실행 - 카테고리: $category, 스크립트: $scriptIndex")
        
        val repeatListeningData = RepeatListeningData(
            category = category,
            scriptIndex = scriptIndex,
            koreanAnswer = answerKo,
            englishAnswer = answerEn
        )
        
        repeatListeningUseCase.execute(
            data = repeatListeningData,
            uiCallback = object : com.na982.opichelper.domain.audio.RepeatListeningUiCallback {
                override fun onCardFlip(isKorean: Boolean) {
                    Log.d("RepeatListeningStrategy", "카드 뒤집기: ${if (isKorean) "한글" else "영문"}")
                    uiCallback.onCardFlip(isKorean)
                }
                
                override fun onHighlight(index: Int) {
                    Log.d("RepeatListeningStrategy", "영문 하이라이트: $index")
                    uiCallback.onHighlight(index)
                }
                
                override fun onKoreanHighlight(index: Int) {
                    Log.d("RepeatListeningStrategy", "한글 하이라이트: $index")
                    uiCallback.onKoreanHighlight(index)
                }
                
                override fun onComplete() {
                    Log.d("RepeatListeningStrategy", "반복듣기 완료")
                    uiCallback.onComplete()
                }
            }
        )
    }
    
    override fun getMemorizeLevel(): MemorizeLevel = MemorizeLevel.REPEAT_LISTENING
} 