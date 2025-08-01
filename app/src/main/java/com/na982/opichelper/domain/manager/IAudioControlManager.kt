package com.na982.opichelper.domain.manager

import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.StateFlow

/**
 * 오디오 제어 인터페이스
 * 책임: TTS 재생, 오디오 중지, 버튼 상태 관리
 */
interface IAudioControlManager {
    
    // 상태 노출
    val isQuestionPlaying: StateFlow<Boolean>
    val isAnswerPlaying: StateFlow<Boolean>
    val isPlaying: StateFlow<Boolean>
    val error: StateFlow<String?>
    
    // 오디오 제어 기능
    fun playQuestion(qaItem: QaItem)
    fun playAnswer(qaItem: QaItem)
    fun stopAllAudio()
    fun stopSpecificAudio(buttonFunction: String)
    
    // 버튼 클릭 이벤트 처리
    fun handleButtonClick(buttonFunction: String, qaItem: QaItem?)
    
    // 상태 초기화
    fun clearError()
    fun resetState()
} 