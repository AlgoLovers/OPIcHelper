package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem

/**
 * 질문 데이터를 관리하는 Repository
 */
interface QuestionRepository {
    /**
     * Assets에서 QA 아이템들을 로드
     * @return 카테고리별 QA 아이템 맵
     */
    suspend fun loadQaItemsFromAssets(): Map<String, List<QaItem>>
    
    /**
     * 특정 카테고리의 QA 아이템들을 가져오기
     * @param category 카테고리명
     * @return 해당 카테고리의 QA 아이템 리스트
     */
    suspend fun getQaItemsByCategory(category: String): List<QaItem>
    
    /**
     * 모든 카테고리 목록 가져오기
     * @return 카테고리명 리스트
     */
    suspend fun getAllCategories(): List<String>
} 