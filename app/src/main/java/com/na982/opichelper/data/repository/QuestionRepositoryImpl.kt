package com.na982.opichelper.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.repository.QuestionRepository
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * QuestionRepository 구현체
 */
class QuestionRepositoryImpl @Inject constructor(
    private val context: Context
) : QuestionRepository {
    
    private val gson = Gson()
    private val categoryDisplayNames = listOf(
        "집", "음악", "집에서 보내는 휴가", "영화", "레스토랑", "해변", "인터넷", 
        "산업,커리어", "은행", "교통", "패션", "가족,친구", "가구", "예약", "명절"
    )
    
    private val categoryFileNames = listOf(
        "qa_home", "qa_music", "qa_home_vacation", "qa_movie", "qa_restaurants", 
        "qa_beach", "qa_internet", "qa_industry_career", "qa_bank", "qa_transportation", 
        "qa_fashion", "qa_family_friends", "qa_furniture", "qa_reservation", "qa_holiday"
    )
    
    override suspend fun loadQaItemsFromAssets(): Map<String, List<QaItem>> {
        val itemsByCategory = mutableMapOf<String, List<QaItem>>()
        
        categoryFileNames.forEachIndexed { index, fileName ->
            try {
                val inputStream = context.assets.open("$fileName.json")
                val reader = InputStreamReader(inputStream, "UTF-8")
                val type = object : TypeToken<List<QaItem>>() {}.type
                val qaItems: List<QaItem> = gson.fromJson(reader, type)
                
                val displayName = categoryDisplayNames.getOrNull(index) ?: fileName
                itemsByCategory[displayName] = qaItems
                
                reader.close()
                inputStream.close()
            } catch (e: Exception) {
                // 파일이 없거나 읽기 실패 시 빈 리스트로 처리
                val displayName = categoryDisplayNames.getOrNull(index) ?: fileName
                itemsByCategory[displayName] = emptyList()
            }
        }
        
        return itemsByCategory
    }
    
    override suspend fun getQaItemsByCategory(category: String): List<QaItem> {
        val allItems = loadQaItemsFromAssets()
        return allItems[category] ?: emptyList()
    }
    
    override suspend fun getAllCategories(): List<String> {
        return categoryDisplayNames
    }
} 