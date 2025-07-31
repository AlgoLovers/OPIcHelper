package com.na982.opichelper.domain.audio

import com.na982.opichelper.domain.entity.InterruptHandler
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인터럽트 처리를 중앙에서 관리하는 클래스
 * 단일 책임 원칙에 따라 인터럽트 처리만을 담당
 */
@Singleton
class InterruptManager @Inject constructor(
    private val buttonStateManager: ButtonStateManager,
    private val ttsOrchestrator: TtsOrchestrator
) : InterruptHandler {
    
    private val coroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    
    /**
     * 카테고리 변경 시 인터럽트 처리
     */
    override fun handleCategoryChange() {
        Log.d("InterruptManager", "카테고리 변경 인터럽트 처리 시작")
        
        coroutineScope.launch {
            try {
                // 1. 모든 TTS 중지
                ttsOrchestrator.stop()
                
                // 2. 모든 버튼 상태 초기화
                buttonStateManager.updateButtonState(ButtonFunction.Stop, ButtonState.Idle)
                
                // 3. 진행 중인 작업 중단 (MemorizationViewModel에 이벤트 발생)
                // 이는 MainViewModel에서 처리
                
                Log.d("InterruptManager", "카테고리 변경 인터럽트 처리 완료")
            } catch (e: Exception) {
                Log.e("InterruptManager", "카테고리 변경 인터럽트 처리 실패", e)
            }
        }
    }
    
    /**
     * 암기 레벨 변경 시 인터럽트 처리
     */
    override fun handleMemorizeLevelChange() {
        Log.d("InterruptManager", "암기 레벨 변경 인터럽트 처리 시작")
        
        coroutineScope.launch {
            try {
                // 1. 모든 TTS 중지
                ttsOrchestrator.stop()
                
                // 2. 모든 버튼 상태 초기화
                buttonStateManager.updateButtonState(ButtonFunction.Stop, ButtonState.Idle)
                
                // 3. 진행 중인 작업 중단 (MemorizationViewModel에 이벤트 발생)
                // 이는 MainViewModel에서 처리
                
                Log.d("InterruptManager", "암기 레벨 변경 인터럽트 처리 완료")
            } catch (e: Exception) {
                Log.e("InterruptManager", "암기 레벨 변경 인터럽트 처리 실패", e)
            }
        }
    }
    
    /**
     * 스크립트 변경 시 인터럽트 처리
     */
    override fun handleScriptChange() {
        Log.d("InterruptManager", "스크립트 변경 인터럽트 처리 시작")
        
        coroutineScope.launch {
            try {
                // 1. 모든 TTS 중지
                ttsOrchestrator.stop()
                
                // 2. 모든 버튼 상태 초기화
                buttonStateManager.updateButtonState(ButtonFunction.Stop, ButtonState.Idle)
                
                // 3. 진행 중인 작업 중단 (MemorizationViewModel에 이벤트 발생)
                // 이는 MainViewModel에서 처리
                
                Log.d("InterruptManager", "스크립트 변경 인터럽트 처리 완료")
            } catch (e: Exception) {
                Log.e("InterruptManager", "스크립트 변경 인터럽트 처리 실패", e)
            }
        }
    }
    
    /**
     * 설정 진입 시 인터럽트 처리
     */
    override fun handleSettingsEnter() {
        Log.d("InterruptManager", "설정 진입 인터럽트 처리 시작")
        
        coroutineScope.launch {
            try {
                // 1. TTS 일시 중지 (설정에서 복귀 시 재개 가능)
                ttsOrchestrator.pauseTts()
                
                // 2. 버튼 상태는 유지 (설정에서 복귀 시 동일한 상태로 복원)
                
                Log.d("InterruptManager", "설정 진입 인터럽트 처리 완료")
            } catch (e: Exception) {
                Log.e("InterruptManager", "설정 진입 인터럽트 처리 실패", e)
            }
        }
    }
    
    /**
     * 앱 종료 시 인터럽트 처리
     */
    override fun handleAppExit() {
        Log.d("InterruptManager", "앱 종료 인터럽트 처리 시작")
        
        coroutineScope.launch {
            try {
                // 1. 모든 TTS 중지
                ttsOrchestrator.stopAllTts()
                
                // 2. 모든 버튼 상태 초기화
                buttonStateManager.updateButtonState(ButtonFunction.Stop, ButtonState.Idle)
                
                // 3. 진행 중인 작업 중단 (MemorizationViewModel에 이벤트 발생)
                // 이는 MainViewModel에서 처리
                
                Log.d("InterruptManager", "앱 종료 인터럽트 처리 완료")
            } catch (e: Exception) {
                Log.e("InterruptManager", "앱 종료 인터럽트 처리 실패", e)
            }
        }
    }
    
    /**
     * 백키 처리
     */
    override fun handleBackPress() {
        Log.d("InterruptManager", "백키 인터럽트 처리 시작")
        
        coroutineScope.launch {
            try {
                // 1. 모든 TTS 중지
                ttsOrchestrator.stopAllTts()
                
                // 2. 모든 버튼 상태 초기화
                buttonStateManager.updateButtonState(ButtonFunction.Stop, ButtonState.Idle)
                
                // 3. 진행 중인 작업 중단 (MemorizationViewModel에 이벤트 발생)
                // 이는 MainViewModel에서 처리
                
                Log.d("InterruptManager", "백키 인터럽트 처리 완료")
            } catch (e: Exception) {
                Log.e("InterruptManager", "백키 인터럽트 처리 실패", e)
            }
        }
    }
    
    /**
     * 모든 인터럽트 처리 (긴급 상황)
     */
    fun handleEmergencyStop() {
        Log.d("InterruptManager", "긴급 중지 처리 시작")
        
        coroutineScope.launch {
            try {
                // 1. 모든 TTS 즉시 중지
                ttsOrchestrator.stopAllTts()
                
                // 2. 모든 버튼 상태 즉시 초기화
                buttonStateManager.updateButtonState(ButtonFunction.Stop, ButtonState.Idle)
                
                Log.d("InterruptManager", "긴급 중지 처리 완료")
            } catch (e: Exception) {
                Log.e("InterruptManager", "긴급 중지 처리 실패", e)
            }
        }
    }
} 