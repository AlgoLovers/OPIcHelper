package com.na982.opichelper.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.repository.QaDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MemorizationViewModel - MemorizationManager를 래핑하는 ViewModel
 * 책임: UI 이벤트 처리 및 MemorizationManager와의 연결
 */
@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val memorizationManager: MemorizationManager,
    private val qaDataRepository: QaDataRepository,
    private val ttsOrchestrator: TtsOrchestrator
) : ViewModel() {
    
    // MemorizationManager의 상태를 그대로 노출
    val uiState: StateFlow<MemorizationUiState> = memorizationManager.uiState
    
    /**
     * 암기 테스트 버튼 클릭 처리
     */
    fun onMemorizeTestButtonClick(selectedLevel: String) {
        viewModelScope.launch {
            try {
                Log.d("MemorizationViewModel", "암기 테스트 버튼 클릭: $selectedLevel")
                
                // 기존 작업 중단
                stopAllOperations()
                
                val category = qaDataRepository.getCurrentCategory() ?: ""
                val scriptIndex = qaDataRepository.getCurrentIndex()
                
                when (selectedLevel) {
                    "반복 듣기" -> {
                        Log.d("MemorizationViewModel", "반복듣기 시작")
                        memorizationManager.startRepeatListening(category, scriptIndex)
                    }
                    "영작 테스트" -> {
                        Log.d("MemorizationViewModel", "영작테스트 시작")
                        memorizationManager.startEnglishWritingTest(category, scriptIndex)
                    }
                    "통암기" -> {
                        Log.d("MemorizationViewModel", "통암기 시작")
                        memorizationManager.startFullMemorization(category, scriptIndex)
                    }
                    else -> {
                        Log.w("MemorizationViewModel", "알 수 없는 암기 레벨: '$selectedLevel'")
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "암기 테스트 시작 실패", e)
            }
        }
    }
    
    /**
     * 모든 작업 중단
     */
    fun stopAllOperations() {
        Log.d("MemorizationViewModel", "모든 작업 중단")
        
        viewModelScope.launch {
            try {
                memorizationManager.stopCurrentMode()
                ttsOrchestrator.stop()
                ttsOrchestrator.clearHighlight()
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "작업 중단 실패", e)
            }
        }
    }
    
    /**
     * 통암기 녹음 중지
     */
    fun stopFullMemorizationRecording() {
        Log.d("MemorizationViewModel", "통암기 녹음 중지")
        memorizationManager.stopFullMemorizationRecording()
    }
    
    /**
     * 통암기 녹음 재생
     */
    fun playFullMemorizationRecording() {
        Log.d("MemorizationViewModel", "통암기 녹음 재생")
        memorizationManager.playFullMemorizationRecording()
    }
    
    /**
     * 통암기 녹음 파일 삭제
     */
    fun deleteFullMemorizationRecording() {
        Log.d("MemorizationViewModel", "통암기 녹음 파일 삭제")
        memorizationManager.deleteFullMemorizationRecording()
    }
    
    /**
     * 통암기 녹음 파일 존재 여부 확인
     */
    suspend fun hasFullMemorizationRecording(): Boolean {
        return memorizationManager.hasFullMemorizationRecording()
    }
    
    /**
     * 통암기 녹음 상태 업데이트
     */
    fun updateFullMemorizationRecordingStatus() {
        Log.d("MemorizationViewModel", "통암기 녹음 상태 업데이트")
        memorizationManager.updateFullMemorizationRecordingStatus()
    }
    
    /**
     * 상태 초기화
     */
    fun resetState() {
        Log.d("MemorizationViewModel", "상태 초기화")
        memorizationManager.resetState()
    }
    
    /**
     * 앱 재시작 시 상태 초기화
     */
    fun resetStateOnAppRestart() {
        Log.d("MemorizationViewModel", "앱 재시작 시 상태 초기화")
        memorizationManager.resetState()
    }
    
    /**
     * 스크립트 변경 시 통암기 녹음 파일 존재 여부 확인
     */
    init {
        viewModelScope.launch {
            qaDataRepository.currentQaItem.collect { currentItem ->
                if (currentItem != null) {
                    Log.d("MemorizationViewModel", "스크립트 변경 감지 - 통암기 녹음 파일 상태 확인")
                    updateFullMemorizationRecordingStatus()
                }
            }
        }
    }
} 