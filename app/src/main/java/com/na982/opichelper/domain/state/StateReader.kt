package com.na982.opichelper.domain.state

import com.na982.opichelper.domain.entity.QaItem

/**
 * 현재 상태를 읽기 위한 Domain Layer 인터페이스
 * Clean Architecture의 Dependency Inversion Principle 준수
 */
interface StateReader {
    /**
     * 현재 QA 아이템 가져오기
     */
    val currentQaItem: QaItem?
    
    /**
     * 현재 카테고리 가져오기
     */
    val currentCategory: String?
    
    /**
     * 현재 인덱스 가져오기
     */
    val currentIndex: Int
    
    /**
     * 현재 문장 인덱스 가져오기
     */
    val currentSentenceIndex: Int
} 