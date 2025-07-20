package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.runtime.*
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.na982.opichelper.presentation.ui.component.TtsService
import com.na982.opichelper.domain.audio.TtsPlayer
import android.util.Log

@Composable
fun TtsServiceManager(
    context: Context,
    onTtsPlayerReady: (TtsPlayer?) -> Unit,
    onHighlightChange: (questionIndex: Int?, answerIndex: Int?) -> Unit,
    onQuestionPlayStateChange: (Boolean) -> Unit,
    onAnswerPlayStateChange: (Boolean) -> Unit
) {
    Log.d("TtsServiceManager", "Initializing TTS service manager")
    
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, service: IBinder?) {
                Log.d("TtsServiceManager", "TTS service connected")
                val binder = service as? TtsService.TtsBinder
                val ttsService = binder?.getService()
                val ttsPlayer = ttsService // TtsService는 TtsPlayer를 구현함
                
                onTtsPlayerReady(ttsPlayer)
                
                ttsService?.setHighlightCallback(object : TtsService.HighlightCallback {
                    override fun onQuestionHighlight(index: Int?) {
                        Log.d("TtsServiceManager", "Question highlight changed to: $index")
                        onHighlightChange(index, null)
                        if (index == null) {
                            // 질문 재생이 완료되면 상태 초기화
                            onQuestionPlayStateChange(false)
                        }
                    }
                    override fun onAnswerHighlight(index: Int?) {
                        Log.d("TtsServiceManager", "Answer highlight changed to: $index")
                        onHighlightChange(null, index)
                        if (index == null) {
                            // 답변 재생이 완료되면 상태 초기화
                            onAnswerPlayStateChange(false)
                        }
                    }
                })
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                Log.d("TtsServiceManager", "TTS service disconnected")
                onTtsPlayerReady(null)
            }
        }
    }
    
    DisposableEffect(Unit) {
        Log.d("TtsServiceManager", "Binding TTS service")
        val intent = android.content.Intent(context, TtsService::class.java)
        context.bindService(intent, serviceConnection, android.content.Context.BIND_AUTO_CREATE)
        
        onDispose {
            Log.d("TtsServiceManager", "Unbinding TTS service")
            context.unbindService(serviceConnection)
        }
    }
} 