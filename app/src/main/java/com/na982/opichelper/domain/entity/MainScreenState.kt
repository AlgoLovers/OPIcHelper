package com.na982.opichelper.domain.entity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Log

/**
 * MainScreen의 도메인 상태를 관리하는 클래스
 * 비즈니스 로직과 UI 상태를 함께 관리
 */
class MainScreenState {
    var isQuestionPlaying by mutableStateOf(false)
    var isAnswerPlaying by mutableStateOf(false)
    var isAnswerRepeatPlaying by mutableStateOf(false)
    var isMergedAudioPlaying by mutableStateOf(false)
    
    /**
     * 모든 재생 상태를 초기화합니다.
     */
    fun resetAllPlayStates() {
        Log.d("MainScreenState", "Resetting all play states")
        isQuestionPlaying = false
        isAnswerPlaying = false
        isAnswerRepeatPlaying = false
        isMergedAudioPlaying = false
    }
    
    /**
     * 현재 재생 중인 상태가 있는지 확인합니다.
     */
    fun isAnyPlaying(): Boolean {
        return isQuestionPlaying || isAnswerPlaying || isAnswerRepeatPlaying || isMergedAudioPlaying
    }
    
    /**
     * 특정 재생 상태를 설정합니다.
     */
    fun setPlayingState(type: PlayType, isPlaying: Boolean) {
        when (type) {
            PlayType.QUESTION -> isQuestionPlaying = isPlaying
            PlayType.ANSWER -> isAnswerPlaying = isPlaying
            PlayType.ANSWER_REPEAT -> isAnswerRepeatPlaying = isPlaying
            PlayType.MERGED_AUDIO -> isMergedAudioPlaying = isPlaying
        }
        Log.d("MainScreenState", "Set $type playing state to: $isPlaying")
    }
}

/**
 * 재생 타입을 정의하는 열거형
 * 도메인 로직과 관련된 타입들을 포함
 */
enum class PlayType {
    QUESTION,
    ANSWER,
    ANSWER_REPEAT,
    MERGED_AUDIO
} 