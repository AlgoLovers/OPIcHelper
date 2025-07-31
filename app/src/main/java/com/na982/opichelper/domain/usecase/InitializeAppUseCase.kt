package com.na982.opichelper.domain.usecase

import android.app.Application
import android.util.Log
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import com.na982.opichelper.domain.state.AppStateManager
import javax.inject.Inject

/**
 * 앱 초기화를 담당하는 UseCase
 * 클린 아키텍처의 UseCase 계층에서 앱 시작 시 필요한 모든 초기화를 처리
 */
class InitializeAppUseCase @Inject constructor(
    private val qaDataManager: QaDataManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appStateManager: AppStateManager
) {
    
    /**
     * 앱 초기화 실행
     */
    suspend fun execute(application: Application) {
        try {
            Log.d("InitializeAppUseCase", "앱 초기화 시작")
            
            // 1. QA 데이터 매니저 초기화
            qaDataManager.init(application)
            Log.d("InitializeAppUseCase", "QA 데이터 매니저 초기화 완료")
            
            // 2. 사용자 기본 암기레벨 설정
            val defaultMemorizeLevel = "반복듣기"
            appStateManager.updateSelectedMemorizeLevel(defaultMemorizeLevel)
            Log.d("InitializeAppUseCase", "기본 암기레벨 설정: $defaultMemorizeLevel")
            
            // 3. 현재 QA 아이템 상태 업데이트
            val currentQaItem = qaDataManager.getCurrentQaItem()
            val currentCategory = qaDataManager.getCurrentCategory()
            val currentIndex = qaDataManager.getCurrentIndex()
            val totalCount = qaDataManager.getItemsInCategory(currentCategory ?: "").size
            
            appStateManager.updateCurrentQaItem(
                qaItem = currentQaItem,
                category = currentCategory,
                index = currentIndex,
                totalCount = totalCount
            )
            Log.d("InitializeAppUseCase", "현재 QA 아이템 상태 업데이트 완료")
            
            // 4. 로딩 상태 해제
            appStateManager.updateLoadingState(false)
            
            Log.d("InitializeAppUseCase", "앱 초기화 완료")
            
        } catch (e: Exception) {
            Log.e("InitializeAppUseCase", "앱 초기화 실패", e)
            appStateManager.updateErrorState("앱 초기화 중 오류가 발생했습니다: ${e.message}")
            appStateManager.updateLoadingState(false)
        }
    }
} 