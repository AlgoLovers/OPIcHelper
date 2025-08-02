package com.na982.opichelper.data.audio

import android.util.Log
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.state.AppStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

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
        
        // 2. 하이라이트 초기화
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        // 3. 하이라이트와 함께 TTS 재생
        ttsOrchestrator.speakWithHighlight(question) { highlightIndex ->
            Log.d("TtsControllerImpl", "질문 하이라이트 콜백: $highlightIndex")
            // 하이라이트 인덱스 업데이트
            appStateManager.updateHighlightState(
                questionHighlightIndex = highlightIndex,
                answerHighlightIndex = -1,
                answerKoHighlightIndex = -1,
                recordingHighlightIndex = -1
            )
        }
        
        // 4. 재생 완료 시 상태 업데이트
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = false,
            isPlaying = false
        )
        // 5. 하이라이트 완전 해제
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
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
        
        // 2. 하이라이트 초기화
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        // 3. 하이라이트와 함께 TTS 재생
        ttsOrchestrator.speakWithHighlight(answer) { highlightIndex ->
            Log.d("TtsControllerImpl", "답변 하이라이트 콜백: $highlightIndex")
            // 하이라이트 인덱스 업데이트
            appStateManager.updateHighlightState(
                questionHighlightIndex = -1,
                answerHighlightIndex = highlightIndex,
                answerKoHighlightIndex = -1,
                recordingHighlightIndex = -1
            )
        }
        
        // 4. 재생 완료 시 상태 업데이트
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = false,
            isPlaying = false
        )
        // 5. 하이라이트 완전 해제
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        Log.d("TtsControllerImpl", "답변 TTS 재생 완료")
    }
    
    override suspend fun playSentenceWithHighlight(
        text: String,
        isKorean: Boolean,
        onHighlight: (Int) -> Unit
    ): Long {
        Log.d("TtsControllerImpl", "문장 하이라이트 TTS 재생 시작: '${text.take(30)}...', isKorean=$isKorean")
        
        // 1. AppState 업데이트
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = true,
            isPlaying = true
        )
        
        // 2. 하이라이트 초기화
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        // 3. 영문/한글 모두 동일하게 하이라이트 처리
        val duration = ttsOrchestrator.speakWithHighlight(text) { highlightIndex ->
            Log.d("TtsControllerImpl", "문장 하이라이트 콜백: $highlightIndex, isKorean=$isKorean")
            
            // TtsOrchestrator에서 받은 highlightIndex를 그대로 사용
            if (isKorean) {
                appStateManager.updateHighlightState(
                    questionHighlightIndex = -1,
                    answerHighlightIndex = -1,
                    answerKoHighlightIndex = highlightIndex,
                    recordingHighlightIndex = -1
                )
            } else {
                appStateManager.updateHighlightState(
                    questionHighlightIndex = -1,
                    answerHighlightIndex = highlightIndex,
                    answerKoHighlightIndex = -1,
                    recordingHighlightIndex = -1
                )
            }
            
            // onHighlight 콜백 호출
            onHighlight(highlightIndex)
        }
        
        // 4. 재생 완료 시 TTS 상태만 업데이트 (하이라이트는 그대로 유지)
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = false,
            isPlaying = false
        )
        
        // 5. 하이라이트 완전 해제
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        Log.d("TtsControllerImpl", "문장 하이라이트 TTS 재생 완료: ${duration}ms")
        return duration
    }
    
    override suspend fun playUnified(
        text: String,
        isKorean: Boolean,
        rate: Float,
        waitForCompletion: Boolean
    ): Long {
        Log.d("TtsControllerImpl", "통합 TTS 재생 시작: '${text.take(30)}...', isKorean=$isKorean, rate=$rate")
        
        // 1. AppState 업데이트
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = true,
            isPlaying = true
        )
        
        // 2. TtsOrchestrator를 통해 하이라이트 없이 재생
        val duration = ttsOrchestrator.speakUnified(
            text = text,
            isKorean = isKorean,
            rate = rate,
            waitForCompletion = waitForCompletion
        )
        
        // 3. 재생 완료 시 상태 업데이트
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = false,
            isPlaying = false
        )
        
        Log.d("TtsControllerImpl", "통합 TTS 재생 완료: ${duration}ms")
        return duration
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