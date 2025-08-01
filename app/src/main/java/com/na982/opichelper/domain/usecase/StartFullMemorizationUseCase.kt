package com.na982.opichelper.domain.usecase

import android.util.Log
import com.na982.opichelper.domain.repository.FullMemorizationRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 전체 암기 테스트 시작 UseCase
 * 
 * 클린 아키텍처 원칙:
 * - Domain Layer에서 비즈니스 로직 처리
 * - Repository에 의존하여 데이터 처리
 * - 상태 관리와 플로우 조율 담당
 */
@Singleton
class StartFullMemorizationUseCase @Inject constructor(
    private val fullMemorizationRepository: FullMemorizationRepository
) {
    
    /**
     * 전체 암기 테스트 시작
     * 1. 질문 TTS 재생
     * 2. 녹음 시작
     */
    suspend fun execute(
        category: String,
        scriptIndex: Int,
        onRecordingStateChange: (Boolean) -> Unit,
        onPlayingStateChange: (Boolean) -> Unit
    ) {
        try {
            Log.d("StartFullMemorizationUseCase", "전체 암기 테스트 시작: $category, $scriptIndex")
            
            // 1. 질문 TTS 재생
            Log.d("StartFullMemorizationUseCase", "질문 재생 시작")
            Log.d("StartFullMemorizationUseCase", "onPlayingStateChange(true) 호출")
            onPlayingStateChange(true)
            
            fullMemorizationRepository.playQuestionWithHighlight()
            
            Log.d("StartFullMemorizationUseCase", "onPlayingStateChange(false) 호출")
            onPlayingStateChange(false)
            Log.d("StartFullMemorizationUseCase", "질문 재생 완료")
            
            // TTS 완료 후 약간의 지연 (GUI 업데이트 대기)
            delay(500L)
            Log.d("StartFullMemorizationUseCase", "TTS 완료 후 지연 완료")
            
            // 2. 녹음 시작
            Log.d("StartFullMemorizationUseCase", "녹음 시작")
            onRecordingStateChange(true)
            
            fullMemorizationRepository.startRecording(category, scriptIndex)
            
            Log.d("StartFullMemorizationUseCase", "전체 암기 테스트 시작 완료")
            
        } catch (e: Exception) {
            Log.e("StartFullMemorizationUseCase", "전체 암기 테스트 시작 실패", e)
            onPlayingStateChange(false)
            onRecordingStateChange(false)
            throw e
        }
    }
    
    /**
     * 녹음 종료
     */
    suspend fun stopRecording(
        onRecordingStateChange: (Boolean) -> Unit
    ) {
        try {
            Log.d("StartFullMemorizationUseCase", "녹음 종료")
            
            fullMemorizationRepository.stopRecording()
            onRecordingStateChange(false)
            
            Log.d("StartFullMemorizationUseCase", "녹음 종료 완료")
            
        } catch (e: Exception) {
            Log.e("StartFullMemorizationUseCase", "녹음 종료 실패", e)
            onRecordingStateChange(false)
            throw e
        }
    }
    
    /**
     * 녹음 재생
     */
    suspend fun playRecording(
        onPlayingStateChange: (Boolean) -> Unit,
        onHighlight: (Int?) -> Unit
    ) {
        try {
            Log.d("StartFullMemorizationUseCase", "녹음 재생 시작")
            
            onPlayingStateChange(true)
            
            fullMemorizationRepository.playRecording { index ->
                onHighlight(index)
            }
            
            onPlayingStateChange(false)
            
            Log.d("StartFullMemorizationUseCase", "녹음 재생 완료")
            
        } catch (e: Exception) {
            Log.e("StartFullMemorizationUseCase", "녹음 재생 실패", e)
            onPlayingStateChange(false)
            throw e
        }
    }
    
    /**
     * 녹음 파일 존재 여부 확인
     */
    fun hasRecording(): Boolean {
        return fullMemorizationRepository.hasRecording()
    }
    
    /**
     * 녹음 파일 정보 초기화
     */
    fun clearRecording() {
        fullMemorizationRepository.clearRecording()
    }
} 