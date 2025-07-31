package com.na982.opichelper.data.audio

import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.state.AppStateManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * TtsController 구현체
 * Infrastructure Layer에서 Domain Layer 인터페이스 구현
 * 클린 아키텍처 원칙: Infrastructure Layer가 Domain Layer에 의존
 * 상태 관리는 AppStateManager에 위임
 */
@Singleton
class TtsControllerImpl @Inject constructor(
    private val ttsOrchestrator: TtsOrchestrator,
    private val appStateManager: AppStateManager
) : TtsController {
    
    override suspend fun playQuestion(question: String) {
        Log.d("TtsControllerImpl", "질문 TTS 재생 시작: $question")
        
        // 1. AppState 업데이트 (단일 진실 소스)
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = true,
            isAnswerPlaying = false,
            isPlaying = true
        )
        
        // 2. 하이라이트와 함께 TTS 재생
        ttsOrchestrator.speakWithHighlight(question) { highlightIndex ->
            Log.d("TtsControllerImpl", "질문 하이라이트 업데이트: $highlightIndex")
            appStateManager.updateHighlightState(
                questionHighlightIndex = highlightIndex
            )
        }
        
        // 3. 재생 완료 시 상태 업데이트
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = false,
            isPlaying = false
        )
        appStateManager.updateHighlightState(questionHighlightIndex = null)
        
        Log.d("TtsControllerImpl", "질문 TTS 재생 완료")
    }
    
    override suspend fun playAnswer(answer: String) {
        Log.d("TtsControllerImpl", "답변 TTS 재생 시작: $answer")
        
        // 1. AppState 업데이트 (단일 진실 소스)
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = true,
            isPlaying = true
        )
        
        // 2. 하이라이트와 함께 TTS 재생
        ttsOrchestrator.speakWithHighlight(answer) { highlightIndex ->
            Log.d("TtsControllerImpl", "답변 하이라이트 업데이트: $highlightIndex")
            appStateManager.updateHighlightState(
                answerHighlightIndex = highlightIndex
            )
        }
        
        // 3. 재생 완료 시 상태 업데이트
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = false,
            isPlaying = false
        )
        appStateManager.updateHighlightState(answerHighlightIndex = null)
        
        Log.d("TtsControllerImpl", "답변 TTS 재생 완료")
    }
    
    override suspend fun stopTts() {
        Log.d("TtsControllerImpl", "TTS 중지 시작")
        
        // 1. TTS 중지
        ttsOrchestrator.stop()
        
        // 2. AppState TTS 상태 초기화
        appStateManager.resetTtsState()
        
        Log.d("TtsControllerImpl", "TTS 중지 완료")
    }
    
    override suspend fun stopAllTts() {
        Log.d("TtsControllerImpl", "모든 TTS 중지 시작")
        
        // 1. 모든 TTS 중지
        ttsOrchestrator.stop()
        
        // 2. AppState TTS 상태 초기화
        appStateManager.resetTtsState()
        
        Log.d("TtsControllerImpl", "모든 TTS 중지 완료")
    }
    
    // AppStateManager에서 상태를 가져오므로 내부 상태 제거
    override fun isPlaying(): kotlinx.coroutines.flow.StateFlow<Boolean> {
        return appStateManager.state.map { it.isPlaying }
            .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.WhileSubscribed(), false)
    }
    
    override fun isQuestionPlaying(): kotlinx.coroutines.flow.StateFlow<Boolean> {
        return appStateManager.state.map { it.isQuestionPlaying }
            .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.WhileSubscribed(), false)
    }
    
    override fun isAnswerPlaying(): kotlinx.coroutines.flow.StateFlow<Boolean> {
        return appStateManager.state.map { it.isAnswerPlaying }
            .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.WhileSubscribed(), false)
    }
} 