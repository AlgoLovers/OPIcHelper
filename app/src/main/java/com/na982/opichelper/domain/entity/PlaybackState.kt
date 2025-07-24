package com.na982.opichelper.domain.entity

/**
 * 오디오 재생 상태를 관리하는 데이터 클래스
 * 순수한 도메인 상태만 포함
 */
data class PlaybackState(
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    val isAnswerRepeatPlaying: Boolean = false,
    val isMergedAudioPlaying: Boolean = false
) {
    /**
     * 현재 재생 중인 상태가 있는지 확인합니다.
     */
    fun isAnyPlaying(): Boolean {
        return isQuestionPlaying || isAnswerPlaying || isAnswerRepeatPlaying || isMergedAudioPlaying
    }
    
    /**
     * 모든 재생 상태를 초기화한 새로운 상태를 반환합니다.
     */
    fun resetAllPlayStates(): PlaybackState {
        return copy(
            isQuestionPlaying = false,
            isAnswerPlaying = false,
            isAnswerRepeatPlaying = false,
            isMergedAudioPlaying = false
        )
    }
    
    /**
     * 특정 재생 상태를 설정한 새로운 상태를 반환합니다.
     */
    fun setPlayingState(type: PlayType, isPlaying: Boolean): PlaybackState {
        return when (type) {
            PlayType.QUESTION -> copy(isQuestionPlaying = isPlaying)
            PlayType.ANSWER -> copy(isAnswerPlaying = isPlaying)
            PlayType.ANSWER_REPEAT -> copy(isAnswerRepeatPlaying = isPlaying)
            PlayType.MERGED_AUDIO -> copy(isMergedAudioPlaying = isPlaying)
        }
    }
}

/**
 * 재생 타입을 정의하는 열거형
 */
enum class PlayType {
    QUESTION,
    ANSWER,
    ANSWER_REPEAT,
    MERGED_AUDIO
} 