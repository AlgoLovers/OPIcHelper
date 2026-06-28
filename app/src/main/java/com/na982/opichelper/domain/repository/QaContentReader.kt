package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem

interface QaContentReader {
    fun getCurrentQaItem(): QaItem?
    fun getCurrentIndex(): Int
    fun getCurrentCategory(): String?
    fun getCurrentAnswer(qaItem: QaItem?): String
    fun getCurrentAnswerKo(qaItem: QaItem?): String
    fun getItemsInCategory(category: String): List<QaItem>
}
