package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.runtime.*
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.na982.opichelper.domain.audio.TtsPlayer
import android.util.Log

/**
 * TTS 서비스 관리자
 * 도메인 계층의 TtsPlayer 인터페이스에만 의존
 */
@Composable
fun TtsServiceManager(
    context: Context,
    onTtsPlayerReady: (TtsPlayer?) -> Unit,
    onKoreanTtsServiceUpdate: ((String) -> Unit)? = null
) {
    Log.d("TtsServiceManager", "Initializing TTS service manager")
    
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, service: IBinder?) {
                Log.d("TtsServiceManager", "TTS service connected")
                val ttsBinder = service as? com.na982.opichelper.presentation.ui.component.TtsService.TtsBinder
                val ttsPlayer = ttsBinder?.getService()
                
                // 한글 TTS 서비스 이름 업데이트
                val ttsService = ttsBinder?.getService() as? com.na982.opichelper.presentation.ui.component.TtsService
                ttsService?.let { service ->
                    val serviceName = service.getCurrentKoreanTtsServiceName()
                    Log.d("TtsServiceManager", "한글 TTS 서비스: $serviceName")
                    onKoreanTtsServiceUpdate?.invoke(serviceName)
                }
                
                Log.d("TtsServiceManager", "TTS player ready: ${ttsPlayer != null}")
                onTtsPlayerReady(ttsPlayer)
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                Log.d("TtsServiceManager", "TTS service disconnected")
                onTtsPlayerReady(null)
            }
        }
    }
    
    DisposableEffect(Unit) {
        Log.d("TtsServiceManager", "Binding TTS service")
        val intent = android.content.Intent(context, com.na982.opichelper.presentation.ui.component.TtsService::class.java)
        context.bindService(intent, serviceConnection, android.content.Context.BIND_AUTO_CREATE)
        
        onDispose {
            Log.d("TtsServiceManager", "Unbinding TTS service")
            try {
                // 하이라이트 콜백 해제
                val ttsService = context.getSystemService(com.na982.opichelper.presentation.ui.component.TtsService::class.java)
                ttsService?.setHighlightCallback(null)
                
                // 서비스 연결 해제
                context.unbindService(serviceConnection)
                
                Log.d("TtsServiceManager", "TTS service unbound successfully")
            } catch (e: Exception) {
                Log.e("TtsServiceManager", "Error unbinding TTS service", e)
            }
        }
    }
} 