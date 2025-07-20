package com.na982.opichelper.presentation.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Log

/**
 * MainScreen의 상태를 관리하는 클래스
 */
class MainScreenState {
    var isQuestionPlaying by mutableStateOf(false)
    var isAnswerPlaying by mutableStateOf(false)
    var isAnswerRepeatPlaying by mutableStateOf(false)
    
    /**
     * 모든 재생 상태를 초기화합니다.
     */
    fun resetAllPlayStates() {
        Log.d("MainScreenState", "Resetting all play states")
        isQuestionPlaying = false
        isAnswerPlaying = false
        isAnswerRepeatPlaying = false
    }
    
    /**
     * 현재 재생 중인 상태가 있는지 확인합니다.
     */
    fun isAnyPlaying(): Boolean {
        return isQuestionPlaying || isAnswerPlaying || isAnswerRepeatPlaying
    }
    
    /**
     * 특정 재생 상태를 설정합니다.
     */
    fun setPlayingState(type: PlayType, isPlaying: Boolean) {
        when (type) {
            PlayType.QUESTION -> isQuestionPlaying = isPlaying
            PlayType.ANSWER -> isAnswerPlaying = isPlaying
            PlayType.ANSWER_REPEAT -> isAnswerRepeatPlaying = isPlaying
        }
        Log.d("MainScreenState", "Set $type playing state to: $isPlaying")
    }
}

enum class PlayType {
    QUESTION,
    ANSWER,
    ANSWER_REPEAT
} 