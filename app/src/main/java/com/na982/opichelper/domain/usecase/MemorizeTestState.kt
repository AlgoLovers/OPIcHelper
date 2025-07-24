package com.na982.opichelper.domain.usecase

import android.util.Log
import com.na982.opichelper.domain.repository.ProgressRepository
import com.na982.opichelper.domain.repository.ProgressState
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 암기 테스트 진행 상태 관리 전담 클래스
 * 책임: 암기 테스트 진행 상태 관리, 제어 흐름, Repository와 UseCase 간 중재
 */
@Singleton
class MemorizeTestState @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val qaDataRepository: QaDataRepository
) {
    
    // 현재 진행 상태
    private val _currentProgress = MutableStateFlow<ProgressState?>(null)
    val currentProgress: StateFlow<ProgressState?> = _currentProgress.asStateFlow()
    
    // 진행 상태 존재 여부
    private val _hasProgress = MutableStateFlow(false)
    val hasProgress: StateFlow<Boolean> = _hasProgress.asStateFlow()
    
    /**
     * 앱 시작 시 진행 상태 복원
     */
    suspend fun restoreProgress() {
        try {
            val progress = progressRepository.loadProgress()
            if (progress != null && !progress.isCompleted) {
                // QA 아이템 복원
                val success = qaDataRepository.restoreToQaItem(progress.category, progress.qaItemId)
                if (success) {
                    _currentProgress.value = progress
                    _hasProgress.value = true
                    Log.d("MemorizeTestState", "진행 상태 복원 성공: $progress")
                } else {
                    Log.w("MemorizeTestState", "QA 아이템 복원 실패, 진행 상태 삭제")
                    progressRepository.clearProgress()
                    _currentProgress.value = null
                    _hasProgress.value = false
                }
            } else {
                Log.d("MemorizeTestState", "완료된 진행 상태 또는 저장된 상태 없음")
                _currentProgress.value = null
                _hasProgress.value = false
            }
        } catch (e: Exception) {
            Log.e("MemorizeTestState", "진행 상태 복원 실패", e)
            _currentProgress.value = null
            _hasProgress.value = false
        }
    }
    
    /**
     * 테스트 시작 시 진행 상태 초기화
     */
    suspend fun startProgress(
        category: String,
        qaItemId: String,
        testType: String,
        totalSentences: Int
    ) {
        try {
            val progressState = ProgressState(
                category = category,
                qaItemId = qaItemId,
                testType = testType,
                currentSentenceIndex = 0,
                totalSentences = totalSentences,
                isCompleted = false
            )
            
            progressRepository.saveProgress(
                category = category,
                qaItemId = qaItemId,
                testType = testType,
                currentSentenceIndex = 0,
                totalSentences = totalSentences,
                isCompleted = false
            )
            
            _currentProgress.value = progressState
            _hasProgress.value = true
            
            Log.d("MemorizeTestState", "테스트 시작 - 진행 상태 초기화: $progressState")
        } catch (e: Exception) {
            Log.e("MemorizeTestState", "진행 상태 초기화 실패", e)
        }
    }
    
    /**
     * 문장 진행 시 상태 업데이트
     */
    suspend fun updateProgress(currentSentenceIndex: Int) {
        try {
            val currentProgress = _currentProgress.value
            if (currentProgress != null) {
                val updatedProgress = currentProgress.copy(
                    currentSentenceIndex = currentSentenceIndex
                )
                
                progressRepository.saveProgress(
                    category = updatedProgress.category,
                    qaItemId = updatedProgress.qaItemId,
                    testType = updatedProgress.testType,
                    currentSentenceIndex = updatedProgress.currentSentenceIndex,
                    totalSentences = updatedProgress.totalSentences,
                    isCompleted = updatedProgress.isCompleted
                )
                
                _currentProgress.value = updatedProgress
                
                Log.d("MemorizeTestState", "진행 상태 업데이트: $updatedProgress")
            }
        } catch (e: Exception) {
            Log.e("MemorizeTestState", "진행 상태 업데이트 실패", e)
        }
    }
    
    /**
     * 테스트 완료 시 진행 상태 삭제
     */
    suspend fun completeProgress() {
        try {
            progressRepository.clearProgress()
            _currentProgress.value = null
            _hasProgress.value = false
            
            Log.d("MemorizeTestState", "테스트 완료 - 진행 상태 삭제")
        } catch (e: Exception) {
            Log.e("MemorizeTestState", "진행 상태 삭제 실패", e)
        }
    }
    
    /**
     * 현재 진행 상태 확인
     */
    fun getCurrentProgress(): ProgressState? {
        return _currentProgress.value
    }
    
    /**
     * 진행률 계산 (0.0 ~ 1.0)
     */
    fun getProgressPercentage(): Float {
        val progress = _currentProgress.value
        return if (progress != null && progress.totalSentences > 0) {
            progress.currentSentenceIndex.toFloat() / progress.totalSentences.toFloat()
        } else {
            0.0f
        }
    }
} 