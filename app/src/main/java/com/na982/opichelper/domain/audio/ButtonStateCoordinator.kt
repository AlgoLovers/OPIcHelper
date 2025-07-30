package com.na982.opichelper.domain.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState

/**
 * 버튼 상태 조정을 담당하는 클래스
 * 단일 책임 원칙: 상태 조정만을 담당
 */
@Singleton
class ButtonStateCoordinator @Inject constructor(
    private val buttonStateManager: ButtonStateManager,
    private val buttonStateObserver: ButtonStateObserver
) {
    private val coroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    
    init {
        setupStateObservers()
    }
    
    private fun setupStateObservers() {
        // 질문 재생 완료 감지
        coroutineScope.launch {
            buttonStateObserver.isQuestionPlaying.collect { isPlaying ->
                if (!isPlaying) {
                    Log.d("ButtonStateCoordinator", "질문 재생 완료 - 버튼 상태를 Idle로 변경")
                    buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
                }
            }
        }
        
        // 답변 재생 완료 감지
        coroutineScope.launch {
            buttonStateObserver.isAnswerPlaying.collect { isPlaying ->
                if (!isPlaying) {
                    Log.d("ButtonStateCoordinator", "답변 재생 완료 - 버튼 상태를 Idle로 변경")
                    buttonStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
                }
            }
        }
    }
} 