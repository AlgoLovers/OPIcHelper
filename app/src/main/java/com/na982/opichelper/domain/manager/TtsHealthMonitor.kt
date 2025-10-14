package com.na982.opichelper.domain.manager

import android.util.Log
import com.na982.opichelper.domain.state.AppStateManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS 상태를 모니터링하고 슬립 상태일 때 자동으로 복구하는 클래스
 * LCD off나 Doze Mode로 인한 TTS 슬립 상태를 감지하고 복구
 */
@Singleton
class TtsHealthMonitor @Inject constructor(
    private val appStateManager: AppStateManager
) {
    private var monitoringJob: Job? = null
    private var lastTtsActivityTime = 0L
    private var isMonitoring = false
    
    // TTS 복구가 필요할 때 호출될 콜백
    var onTtsRecoveryNeeded: (() -> Unit)? = null
    
    /**
     * TTS 상태 모니터링 시작
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.d("TtsHealthMonitor", "모니터링이 이미 실행 중입니다")
            return
        }
        
        isMonitoring = true
        monitoringJob = CoroutineScope(Dispatchers.Main).launch {
            Log.d("TtsHealthMonitor", "TTS 상태 모니터링 시작")
            
            while (isActive) {
                delay(3000) // 3초마다 체크
                
                if (isTtsActive() && isTtsStuck()) {
                    Log.w("TtsHealthMonitor", "TTS 슬립 상태 감지, 복구 시도")
                    // 복구는 외부에서 처리하도록 콜백 사용
                    onTtsRecoveryNeeded?.invoke()
                }
            }
        }
        
        Log.d("TtsHealthMonitor", "TTS 상태 모니터링 시작됨")
    }
    
    /**
     * TTS가 현재 활성 상태인지 확인
     */
    private fun isTtsActive(): Boolean {
        val currentState = appStateManager.state.value
        return currentState.isPlaying || currentState.isQuestionPlaying || currentState.isAnswerPlaying
    }
    
    /**
     * TTS가 슬립 상태인지 확인
     * TTS가 10초 이상 응답하지 않으면 슬립 상태로 판단
     */
    private fun isTtsStuck(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastActivity = currentTime - lastTtsActivityTime
        
        // TTS가 10초 이상 응답하지 않으면 슬립 상태로 판단
        val isStuck = timeSinceLastActivity > 10000
        
        if (isStuck) {
            Log.d("TtsHealthMonitor", "TTS 응답 시간: ${timeSinceLastActivity}ms (슬립 상태 의심)")
        }
        
        return isStuck
    }
    

    
    /**
     * TTS 활동 시간 업데이트
     * TTS 재생 요청이나 완료 시 호출
     */
    fun updateTtsActivity() {
        lastTtsActivityTime = System.currentTimeMillis()
        Log.d("TtsHealthMonitor", "TTS 활동 시간 업데이트: $lastTtsActivityTime")
    }
    
    /**
     * TTS 상태 모니터링 중지
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            Log.d("TtsHealthMonitor", "모니터링이 이미 중지되었습니다")
            return
        }
        
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        
        Log.d("TtsHealthMonitor", "TTS 상태 모니터링 중지됨")
    }
    
    /**
     * 모니터링 상태 확인
     */
    fun isMonitoringActive(): Boolean = isMonitoring
}
