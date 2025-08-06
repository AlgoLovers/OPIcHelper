package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import javax.inject.Inject

/**
 * 현재 사용자 레벨에 맞는 답변을 가져오는 UseCase
 */
class GetLeveledAnswerUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    
    /**
     * 현재 사용자 레벨에 맞는 영어 답변을 가져오기
     */
    fun getCurrentAnswer(qaItem: QaItem?): String {
        if (qaItem == null) return ""
        
        // QaItem에서 직접 영어 답변 문장들을 가져옴
        return qaItem.answerEnSentences.joinToString(" ")
    }
    
    /**
     * 현재 사용자 레벨에 맞는 한국어 답변을 가져오기
     */
    fun getCurrentAnswerKo(qaItem: QaItem?): String {
        if (qaItem == null) return ""
        
        // QaItem에서 직접 한국어 답변 문장들을 가져옴
        return qaItem.answerKoSentences.joinToString(" ")
    }
} 