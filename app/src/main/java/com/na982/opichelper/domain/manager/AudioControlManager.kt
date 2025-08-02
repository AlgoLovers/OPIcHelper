package com.na982.opichelper.domain.manager

import android.util.Log
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.audio.TtsController
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
    private val ttsController: TtsController
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
                ttsController.playAnswer(qaItem.answers.values.first().answerEn)
                
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
     * 모든 오디오 중지
     */
    override fun stopAllAudio() {
        Log.d("AudioControlManager", "모든 오디오 중지")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                ttsController.stopAllTts()
                Log.d("AudioControlManager", "모든 오디오 중지 완료")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "오디오 중지 실패", e)
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
} 