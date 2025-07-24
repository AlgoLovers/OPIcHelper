package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem

/**
 * QA 데이터 로딩을 담당하는 인터페이스 (Loader 패턴)
 * 책임: QA 데이터의 로딩만 담당
 */
interface QaDataLoader {
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