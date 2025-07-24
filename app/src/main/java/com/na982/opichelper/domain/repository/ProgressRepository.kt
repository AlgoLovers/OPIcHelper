package com.na982.opichelper.domain.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 암기 테스트 진행 상태 저장/복원 전담 Repository
 * 책임: 진행 상태의 영속성 관리
 */
interface ProgressRepository {
    /**
     * 진행 상태 저장
     */
    suspend fun saveProgress(
        category: String,
        qaItemId: String,
        testType: String, // "반복 듣기", "영작 테스트"
        currentSentenceIndex: Int,
        totalSentences: Int,
        isCompleted: Boolean = false
    )
    
    /**
     * 진행 상태 복원
     */
    suspend fun loadProgress(): ProgressState?
    
    /**
     * 진행 상태 삭제 (테스트 완료 시)
     */
    suspend fun clearProgress()
    
    /**
     * 진행 상태 존재 여부 확인
     */
    suspend fun hasProgress(): Boolean
}

/**
 * 진행 상태 데이터 클래스
 */
data class ProgressState(
    val category: String,
    val qaItemId: String,
    val testType: String,
    val currentSentenceIndex: Int,
    val totalSentences: Int,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ProgressRepository 구현체
 */
class ProgressRepositoryImpl @javax.inject.Inject constructor(
    private val context: Context
) : ProgressRepository {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("progress_prefs", Context.MODE_PRIVATE)
    }
    
    private val gson = Gson()
    
    companion object {
        private const val KEY_PROGRESS = "memorize_test_progress"
    }
    
    override suspend fun saveProgress(
        category: String,
        qaItemId: String,
        testType: String,
        currentSentenceIndex: Int,
        totalSentences: Int,
        isCompleted: Boolean
    ) {
        try {
            val progressState = ProgressState(
                category = category,
                qaItemId = qaItemId,
                testType = testType,
                currentSentenceIndex = currentSentenceIndex,
                totalSentences = totalSentences,
                isCompleted = isCompleted
            )
            
            val json = gson.toJson(progressState)
            prefs.edit().putString(KEY_PROGRESS, json).apply()
            
            Log.d("ProgressRepository", "진행 상태 저장: $progressState")
        } catch (e: Exception) {
            Log.e("ProgressRepository", "진행 상태 저장 실패", e)
        }
    }
    
    override suspend fun loadProgress(): ProgressState? {
        return try {
            val json = prefs.getString(KEY_PROGRESS, null)
            if (json != null) {
                val progressState = gson.fromJson(json, ProgressState::class.java)
                Log.d("ProgressRepository", "진행 상태 복원: $progressState")
                progressState
            } else {
                Log.d("ProgressRepository", "저장된 진행 상태 없음")
                null
            }
        } catch (e: Exception) {
            Log.e("ProgressRepository", "진행 상태 복원 실패", e)
            null
        }
    }
    
    override suspend fun clearProgress() {
        try {
            prefs.edit().remove(KEY_PROGRESS).apply()
            Log.d("ProgressRepository", "진행 상태 삭제 완료")
        } catch (e: Exception) {
            Log.e("ProgressRepository", "진행 상태 삭제 실패", e)
        }
    }
    
    override suspend fun hasProgress(): Boolean {
        return prefs.contains(KEY_PROGRESS)
    }
} 