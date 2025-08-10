package com.na982.opichelper.domain.manager

import android.util.Log
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.audio.TtsOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 오디오 재생을 관리하는 매니저
 * 단일 책임: 오디오 재생만 담당
 */
@Singleton
class AudioControlManager @Inject constructor(
    private val ttsController: TtsController,
    private val recordingAudioPlayer: RecordingAudioPlayer,
    private val ttsOrchestrator: TtsOrchestrator
) : IAudioControlManager {
    
    private val _error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    override val error: kotlinx.coroutines.flow.StateFlow<String?> = _error
    
    /**
     * 질문 재생
     */
    override fun playQuestion(qaItem: QaItem, onCompletion: () -> Unit) {
        Log.d("AudioControlManager", "질문 재생 시작")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                _error.value = null
                
                // TTS 재생 (하이라이트 포함) - suspend 함수이므로 await
                ttsController.playQuestion(qaItem.questionEn)
                
                // 재생 완료 시 콜백 호출
                onCompletion()
                
                Log.d("AudioControlManager", "질문 재생 완료")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "질문 재생 실패", e)
                _error.value = e.message
                onCompletion() // 에러 시에도 콜백 호출
            }
        }
    }
    
    /**
     * 답변 재생
     */
    override fun playAnswer(qaItem: QaItem, onCompletion: () -> Unit) {
        Log.d("AudioControlManager", "답변 재생 시작")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                _error.value = null
                
                // TTS 재생 (하이라이트 포함) - suspend 함수이므로 await
                ttsController.playAnswer(qaItem.answerEnSentences.joinToString(" "))
                
                // 재생 완료 시 콜백 호출
                onCompletion()
                
                Log.d("AudioControlManager", "답변 재생 완료")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "답변 재생 실패", e)
                _error.value = e.message
                onCompletion() // 에러 시에도 콜백 호출
            }
        }
    }
    
    /**
     * 모든 오디오 중지 (일시 중지 - 재생 중단용)
     * - 재생 중 같은 버튼 클릭
     * - 재생 중 다른 버튼 클릭  
     * - 카테고리/암기레벨 변경 시
     */
    override fun stopAllAudio() {
        Log.d("AudioControlManager", "모든 오디오 중지 (일시 중지)")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // TTS 중지 (Infrastructure Layer 책임)
                ttsController.stopAllTts()
                Log.d("AudioControlManager", "TTS 중지 완료")
                
                // 녹음 재생 중지 (Infrastructure Layer 책임)
                recordingAudioPlayer.stopRecording()
                Log.d("AudioControlManager", "녹음 재생 중지 완료")
                
                Log.d("AudioControlManager", "모든 오디오 중지 완료")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "오디오 중지 실패", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * 모든 오디오 중지 및 TTS 플레이어 해제 (완전 종료용)
     * - 앱 백키로 종료 시
     * - 앱 완전 종료 시
     */
    override fun releaseAllAudio() {
        Log.d("AudioControlManager", "모든 오디오 중지 및 TTS 플레이어 해제 (완전 종료)")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. TTS 중지
                ttsController.stopAllTts()
                Log.d("AudioControlManager", "TTS 중지 완료")
                
                // 2. TTS 플레이어 완전 해제 (중요: 앱 종료 후 TTS 재생 방지)
                ttsOrchestrator.releaseAllPlayers()
                Log.d("AudioControlManager", "TTS 플레이어 해제 완료")
                
                // 3. 녹음 재생 중지
                recordingAudioPlayer.stopRecording()
                Log.d("AudioControlManager", "녹음 재생 중지 완료")
                
                Log.d("AudioControlManager", "모든 오디오 중지 및 TTS 플레이어 해제 완료")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "오디오 중지 및 해제 실패", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * 특정 버튼의 오디오 중지
     */
    override fun stopSpecificAudio(buttonFunction: String) {
        Log.d("AudioControlManager", "특정 오디오 중지: $buttonFunction")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                when (buttonFunction) {
                    com.na982.opichelper.domain.entity.ButtonFunction.QuestionPlay.toString() -> {
                        ttsController.stopAllTts()
                    }
                    com.na982.opichelper.domain.entity.ButtonFunction.AnswerPlay.toString() -> {
                        ttsController.stopAllTts()
                    }
                    else -> {
                        Log.w("AudioControlManager", "알 수 없는 버튼 함수: $buttonFunction")
                    }
                }
                
                Log.d("AudioControlManager", "특정 오디오 중지 완료")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "특정 오디오 중지 실패", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * TTS 플레이어 재초기화 (release 후 재사용 시)
     * - 앱 재시작 시
     * - TTS 오류 복구 시
     */
    override fun reinitializeTtsPlayers() {
        Log.d("AudioControlManager", "TTS 플레이어 재초기화")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // TTS 플레이어 재초기화
                ttsOrchestrator.reinitializeAllPlayers()
                Log.d("AudioControlManager", "TTS 플레이어 재초기화 완료")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "TTS 플레이어 재초기화 실패", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * 동기적 TTS 정리 (앱 종료 시 사용)
     * - 백키로 앱 종료 시
     * - 앱 완전 종료 시
     */
    override suspend fun cleanupTtsSync() {
        Log.d("AudioControlManager", "동기적 TTS 정리 시작")
        
        try {
            // 1. TTS 중지 (동기적)
            ttsController.stopAllTts()
            Log.d("AudioControlManager", "TTS 중지 완료")
            
            // 2. TTS 플레이어 완전 해제 (동기적)
            ttsOrchestrator.releaseAllPlayers()
            Log.d("AudioControlManager", "TTS 플레이어 해제 완료")
            
            Log.d("AudioControlManager", "동기적 TTS 정리 완료")
            
        } catch (e: Exception) {
            Log.e("AudioControlManager", "동기적 TTS 정리 실패", e)
            _error.value = e.message
        }
    }
} 