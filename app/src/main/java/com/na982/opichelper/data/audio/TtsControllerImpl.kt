package com.na982.opichelper.data.audio

import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.state.AppStateManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TtsController 구현체
 * Infrastructure Layer에서 Domain Layer 인터페이스 구현
 * 클린 아키텍처 원칙: Infrastructure Layer가 Domain Layer에 의존
 */
@Singleton
class TtsControllerImpl @Inject constructor(
    private val ttsPlaybackController: TtsPlaybackController,
    private val appStateManager: AppStateManager
) : TtsController {
    
    private val _isPlaying = MutableStateFlow(false)
    private val _isQuestionPlaying = MutableStateFlow(false)
    private val _isAnswerPlaying = MutableStateFlow(false)
    
    override suspend fun playQuestion(question: String) {
        Log.d("TtsControllerImpl", "질문 TTS 재생 시작: $question")
        
        // 1. TTS 재생
        ttsPlaybackController.playQuestion(question)
        
        // 2. AppState 업데이트 (단일 진실 소스)
        appStateManager.updateTtsPlayingState(isQuestionPlaying = true, isAnswerPlaying = false)
        
        // 3. 내부 상태 업데이트
        _isPlaying.value = true
        _isQuestionPlaying.value = true
        _isAnswerPlaying.value = false
        
        Log.d("TtsControllerImpl", "질문 TTS 재생 완료")
    }
    
    override suspend fun playAnswer(answer: String) {
        Log.d("TtsControllerImpl", "답변 TTS 재생 시작: $answer")
        
        // 1. TTS 재생
        ttsPlaybackController.playAnswer(answer)
        
        // 2. AppState 업데이트 (단일 진실 소스)
        appStateManager.updateTtsPlayingState(isQuestionPlaying = false, isAnswerPlaying = true)
        
        // 3. 내부 상태 업데이트
        _isPlaying.value = true
        _isQuestionPlaying.value = false
        _isAnswerPlaying.value = true
        
        Log.d("TtsControllerImpl", "답변 TTS 재생 완료")
    }
    
    override suspend fun stopTts() {
        Log.d("TtsControllerImpl", "TTS 중지 시작")
        
        // 1. TTS 중지
        ttsPlaybackController.stopTts()
        
        // 2. AppState TTS 상태 초기화 (하이라이트는 AppState에서 자체 관리)
        appStateManager.updateTtsPlayingState(isQuestionPlaying = false, isAnswerPlaying = false)
        
        // 3. 내부 상태 초기화
        _isPlaying.value = false
        _isQuestionPlaying.value = false
        _isAnswerPlaying.value = false
        
        Log.d("TtsControllerImpl", "TTS 중지 완료")
    }
    
    override suspend fun stopAllTts() {
        Log.d("TtsControllerImpl", "모든 TTS 중지 시작")
        
        // 1. 모든 TTS 중지
        ttsPlaybackController.stopAllTts()
        
        // 2. AppState TTS 상태 초기화 (하이라이트는 AppState에서 자체 관리)
        appStateManager.updateTtsPlayingState(isQuestionPlaying = false, isAnswerPlaying = false)
        
        // 3. 내부 상태 초기화
        _isPlaying.value = false
        _isQuestionPlaying.value = false
        _isAnswerPlaying.value = false
        
        Log.d("TtsControllerImpl", "모든 TTS 중지 완료")
    }
    
    override fun isPlaying(): StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    override fun isQuestionPlaying(): StateFlow<Boolean> = _isQuestionPlaying.asStateFlow()
    
    override fun isAnswerPlaying(): StateFlow<Boolean> = _isAnswerPlaying.asStateFlow()
} 