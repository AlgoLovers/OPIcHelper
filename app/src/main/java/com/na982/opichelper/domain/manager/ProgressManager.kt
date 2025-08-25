package com.na982.opichelper.domain.manager

import android.util.Log
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.ScriptProgress
import com.na982.opichelper.domain.repository.CategoryProgress
import com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase
import com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase
import com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * 진행상황 저장/복원 + 암기테스트 로직을 통합 관리하는 클래스
 * 책임: 
 * 1. 진행상황 저장/복원
 * 2. 암기테스트 로직 (영작테스트, 반복듣기, 통암기)
 * 3. 현재 진행상황 상태 관리
 */
@ViewModelScoped
class ProgressManager @Inject constructor(
    private val progressPersistenceService: ProgressPersistenceService,
    private val englishWritingUseCase: StartEnglishWritingTestUseCase,
    private val repeatListeningUseCase: StartRepeatListeningUseCase,
    private val fullMemorizationUseCase: StartFullMemorizationUseCase
) {
    
    // ===== 진행상황 저장/복원 =====
    
    // 모든 스크립트의 진행 상황 (메모리에서 관리)
    private val _progressMap = MutableStateFlow<Map<String, ScriptProgress>>(emptyMap())
    val progressMap: StateFlow<Map<String, ScriptProgress>> = _progressMap.asStateFlow()
    
    // 진행 상태 존재 여부
    private val _hasProgress = MutableStateFlow(false)
    val hasProgress: StateFlow<Boolean> = _hasProgress.asStateFlow()
    
    // ===== 현재 진행상황 상태 =====
    
    private val _currentProgress = MutableStateFlow(ProgressState())
    val currentProgress: StateFlow<ProgressState> = _currentProgress.asStateFlow()
    
    init {
        Log.d("ProgressManager", "진행상황 매니저 초기화")
    }
    
    // ===== 진행상황 저장/복원 메서드들 =====
    
    /**
     * 앱 시작 시 모든 진행 상황 복원
     */
    suspend fun restoreAllProgress() {
        try {
            val allProgress = progressPersistenceService.loadAllCategoryProgress()
            _progressMap.value = allProgress.mapValues { (_, categoryProgress) ->
                ScriptProgress(
                    category = categoryProgress.category,
                    scriptIndex = categoryProgress.scriptIndex,
                    memorizeLevel = categoryProgress.memorizeLevel,
                    currentSentenceIndex = categoryProgress.currentSentenceIndex,
                    totalSentences = categoryProgress.totalSentences,
                    isMemorizeTestRunning = categoryProgress.isMemorizeTestRunning
                )
            }
            _hasProgress.value = allProgress.isNotEmpty()
            
            Log.d("ProgressManager", "진행 상황 복원 완료: ${allProgress.size}개")
        } catch (e: Exception) {
            Log.e("ProgressManager", "진행 상황 복원 실패", e)
        }
    }
    
    /**
     * 특정 스크립트의 진행 상황 가져오기
     */
    fun getScriptProgress(category: String, scriptIndex: Int, memorizeLevel: String): ScriptProgress? {
        val key = "${category}_${scriptIndex}_${memorizeLevel}"
        return _progressMap.value[key]
    }
    
    /**
     * 진행 상황 존재 여부 확인
     */
    fun hasScriptProgress(category: String, scriptIndex: Int, memorizeLevel: String): Boolean {
        return getScriptProgress(category, scriptIndex, memorizeLevel) != null
    }
    
    /**
     * 진행 상황 업데이트 (메모리에서만)
     */
    fun updateProgress(
        category: String,
        scriptIndex: Int,
        memorizeLevel: String,
        currentSentenceIndex: Int,
        totalSentences: Int,
        isMemorizeTestRunning: Boolean
    ) {
        val key = "${category}_${scriptIndex}_${memorizeLevel}"
        val currentMap = _progressMap.value.toMutableMap()
        
        currentMap[key] = ScriptProgress(
            category = category,
            scriptIndex = scriptIndex,
            memorizeLevel = memorizeLevel,
            currentSentenceIndex = currentSentenceIndex,
            totalSentences = totalSentences,
            isMemorizeTestRunning = isMemorizeTestRunning,
            needsSave = true // 변경되었으므로 저장 필요
        )
        
        _progressMap.value = currentMap
        _hasProgress.value = true
        
        // 현재 진행상황 상태도 업데이트
        updateCurrentProgress(category, scriptIndex, memorizeLevel, currentSentenceIndex, totalSentences, isMemorizeTestRunning)
        
        Log.d("ProgressManager", "진행 상황 업데이트: $key -> 문장 ${currentSentenceIndex + 1}/${totalSentences}")
    }
    
    /**
     * 특정 스크립트의 진행 상황 삭제 (암기레벨별)
     */
    fun clearScriptProgress(category: String, scriptIndex: Int, memorizeLevel: String) {
        val key = "${category}_${scriptIndex}_${memorizeLevel}"
        val currentMap = _progressMap.value.toMutableMap()
        
        currentMap.remove(key)
        _progressMap.value = currentMap
        _hasProgress.value = currentMap.isNotEmpty()
        
        Log.d("ProgressManager", "진행 상황 삭제: $key")
    }
    
    /**
     * 변경된 진행 상황만 저장
     */
    suspend fun persistChangedProgress() {
        try {
            val changedProgress = _progressMap.value.values.filter { progress -> progress.needsSave }
            if (changedProgress.isNotEmpty()) {
                changedProgress.forEach { progress ->
                    val categoryProgress = CategoryProgress(
                        category = progress.category,
                        scriptIndex = progress.scriptIndex,
                        memorizeLevel = progress.memorizeLevel,
                        currentSentenceIndex = progress.currentSentenceIndex,
                        totalSentences = progress.totalSentences,
                        isMemorizeTestRunning = progress.isMemorizeTestRunning
                    )
                    progressPersistenceService.saveCategoryProgress(categoryProgress)
                }
                Log.d("ProgressManager", "진행 상황 저장 완료: ${changedProgress.size}개")
            }
        } catch (e: Exception) {
            Log.e("ProgressManager", "진행 상황 저장 실패", e)
        }
    }
    
    // ===== 암기테스트 로직 메서드들 =====
    
    /**
     * 영작테스트 시작
     */
    suspend fun startEnglishWritingTest(category: String, scriptIndex: Int) {
        Log.d("ProgressManager", "영작테스트 시작: $category/$scriptIndex")
        
        try {
            // 현재 진행상황 상태 업데이트
            _currentProgress.value = _currentProgress.value.copy(
                isRunning = true,
                currentMode = ProgressMode.ENGLISH_WRITING,
                isEnglishWritingTestRunning = true,
                isEnglishWritingTestMode = true
            )
            
            // UseCase 실행 - UI 콜백은 별도로 구현 필요
            // englishWritingUseCase.execute(category, scriptIndex, uiCallback)
            Log.d("ProgressManager", "영작테스트 UseCase 실행 준비 완료")
            
            Log.d("ProgressManager", "영작테스트 시작 완료")
        } catch (e: Exception) {
            Log.e("ProgressManager", "영작테스트 시작 실패", e)
            _currentProgress.value = _currentProgress.value.copy(
                isRunning = false,
                currentMode = ProgressMode.NONE,
                isEnglishWritingTestRunning = false,
                isEnglishWritingTestMode = false
            )
        }
    }
    
    /**
     * 반복듣기 시작
     */
    suspend fun startRepeatListening(category: String, scriptIndex: Int) {
        Log.d("ProgressManager", "반복듣기 시작: $category/$scriptIndex")
        
        try {
            // 현재 진행상황 상태 업데이트
            _currentProgress.value = _currentProgress.value.copy(
                isRunning = true,
                currentMode = ProgressMode.REPEAT_LISTENING,
                isRepeatListeningRunning = true,
                isRepeatListeningMode = true,
                repeatListeningCurrentRepeat = 0
            )
            
            // UseCase 실행 - UI 콜백은 별도로 구현 필요
            // repeatListeningUseCase.execute(category, scriptIndex, uiCallback)
            Log.d("ProgressManager", "반복듣기 UseCase 실행 준비 완료")
            
            Log.d("ProgressManager", "반복듣기 시작 완료")
        } catch (e: Exception) {
            Log.e("ProgressManager", "반복듣기 시작 실패", e)
            _currentProgress.value = _currentProgress.value.copy(
                isRunning = false,
                currentMode = ProgressMode.NONE,
                isRepeatListeningRunning = false,
                isRepeatListeningMode = false
            )
        }
    }
    
    /**
     * 통암기 시작
     */
    suspend fun startFullMemorization(category: String, scriptIndex: Int) {
        Log.d("ProgressManager", "통암기 시작: $category/$scriptIndex")
        
        try {
            // 현재 진행상황 상태 업데이트
            _currentProgress.value = _currentProgress.value.copy(
                isRunning = true,
                currentMode = ProgressMode.FULL_MEMORIZATION,
                isFullMemorizationMode = true
            )
            
            // UseCase 실행 - UI 콜백은 별도로 구현 필요
            // fullMemorizationUseCase.execute(category, scriptIndex, uiCallback)
            Log.d("ProgressManager", "통암기 UseCase 실행 준비 완료")
            
            Log.d("ProgressManager", "통암기 시작 완료")
        } catch (e: Exception) {
            Log.e("ProgressManager", "통암기 시작 실패", e)
            _currentProgress.value = _currentProgress.value.copy(
                isRunning = false,
                currentMode = ProgressMode.NONE,
                isFullMemorizationMode = false
            )
        }
    }
    
    /**
     * 현재 모드 중지
     */
    fun stopCurrentMode() {
        Log.d("ProgressManager", "현재 모드 중지")
        
        _currentProgress.value = _currentProgress.value.copy(
            isRunning = false,
            currentMode = ProgressMode.NONE,
            isRepeatListeningMode = false,
            isRepeatListeningRunning = false,
            isEnglishWritingTestMode = false,
            isEnglishWritingTestRunning = false,
            isFullMemorizationMode = false
        )
        
        Log.d("ProgressManager", "현재 모드 중지 완료")
    }
    
    /**
     * 영작테스트 완료 처리
     */
    fun onEnglishWritingTestCompleted() {
        Log.d("ProgressManager", "영작테스트 완료 처리")
        
        _currentProgress.value = _currentProgress.value.copy(
            isRunning = false,
            currentMode = ProgressMode.NONE,
            isEnglishWritingTestMode = false,
            isEnglishWritingTestRunning = false,
            englishWritingTestCompleted = true
        )
        
        Log.d("ProgressManager", "영작테스트 완료 상태 업데이트 완료")
    }
    
    // ===== 현재 진행상황 상태 업데이트 =====
    
    private fun updateCurrentProgress(
        category: String,
        scriptIndex: Int,
        memorizeLevel: String,
        currentSentenceIndex: Int,
        totalSentences: Int,
        isMemorizeTestRunning: Boolean
    ) {
        _currentProgress.value = _currentProgress.value.copy(
            currentCategory = category,
            currentScriptIndex = scriptIndex,
            currentMemorizeLevel = memorizeLevel,
            currentSentenceIndex = currentSentenceIndex,
            totalSentences = totalSentences,
            isMemorizeTestRunning = isMemorizeTestRunning
        )
    }
    
    // ===== 상태 초기화 =====
    
    fun resetState() {
        Log.d("ProgressManager", "상태 초기화")
        _currentProgress.value = ProgressState()
    }
    
    fun clearError() {
        Log.d("ProgressManager", "에러 상태 초기화")
        _currentProgress.value = _currentProgress.value.copy(error = null)
    }
}

/**
 * 현재 진행상황 상태를 나타내는 데이터 클래스
 */
data class ProgressState(
    // 실행 상태
    val isRunning: Boolean = false,
    val currentMode: ProgressMode = ProgressMode.NONE,
    
    // 현재 진행상황
    val currentCategory: String = "",
    val currentScriptIndex: Int = 0,
    val currentMemorizeLevel: String = "",
    val currentSentenceIndex: Int = 0,
    val totalSentences: Int = 0,
    val isMemorizeTestRunning: Boolean = false,
    
    // 반복듣기 상태
    val isRepeatListeningCardFlipped: Boolean = false,
    val isRepeatListeningRunning: Boolean = false,
    val isRepeatListeningMode: Boolean = false,
    val repeatListeningCurrentRepeat: Int = 0,
    val repeatListeningTotalRepeats: Int = 5,
    
    // 영작테스트 상태
    val isEnglishWritingTestCardFlipped: Boolean = false,
    val isEnglishWritingTestRunning: Boolean = false,
    val isEnglishWritingTestMode: Boolean = false,
    val isEnglishWritingTestRecording: Boolean = false,
    val isEnglishWritingTestPlaying: Boolean = false,
    val hasEnglishWritingTestRecording: Boolean = false,
    val englishWritingTestCompleted: Boolean = false,
    
    // 통암기 상태
    val isFullMemorizationMode: Boolean = false,
    val isFullMemorizationQuestionPlaying: Boolean = false,
    val isFullMemorizationRecording: Boolean = false,
    val isFullMemorizationPlaying: Boolean = false,
    val hasFullMemorizationRecording: Boolean = false,
    val isFullMemorizationRecordingPlaying: Boolean = false,
    
    // 편의 변수들 (중복 제거)
    // val isMemorizeTestRunning: Boolean = false, // 위에서 이미 정의됨
    
    // 하이라이트 인덱스들
    val answerHighlightIndex: Int = -1,
    val answerKoHighlightIndex: Int = -1,
    val recordingHighlightIndex: Int = -1,
    
    // 에러 상태
    val error: String? = null
)

/**
 * 진행 모드를 나타내는 enum
 */
enum class ProgressMode {
    NONE,
    REPEAT_LISTENING,
    ENGLISH_WRITING,
    FULL_MEMORIZATION
} 