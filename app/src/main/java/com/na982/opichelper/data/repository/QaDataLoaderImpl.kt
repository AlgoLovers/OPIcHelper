package com.na982.opichelper.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.repository.QaDataLoader
import com.na982.opichelper.domain.util.SentenceParser
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * QaDataLoader 구현체
 * QA 데이터를 assets에서 로드하는 역할
 */
class QaDataLoaderImpl @Inject constructor(
    private val context: Context,
    private val userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository
) : QaDataLoader {
    
    private val gson = Gson()
    private val categoryDisplayNames = listOf(
        "자기소개", "롤플레이", "집", "음악", "집에서 보내는 휴가", "영화", "레스토랑", "해변", "인터넷",
        "산업,커리어", "은행", "교통", "패션", "가족,친구", "가구", "예약", "명절"

    )
    
    private val categoryFileNames = listOf(
        "qa_intro", "roleplay", "qa_home", "qa_music", "qa_home_vacation", "qa_movie", "qa_restaurants",
        "qa_beach", "qa_internet", "qa_industry_career", "qa_bank", "qa_transportation", 
        "qa_fashion", "qa_family_friends", "qa_furniture", "qa_reservation", "qa_holiday"
    )
    
    override suspend fun loadQaItemsFromAssets(): Map<String, List<QaItem>> {
        val selectedDataSource = userPreferencesRepository.getDataSource()
        Log.d("QaDataLoaderImpl", "선택된 데이터 소스: ${selectedDataSource.folderName}")
        Log.d("QaDataLoaderImpl", "assets에서 QA 데이터 로드 시작")
        val itemsByCategory = mutableMapOf<String, List<QaItem>>()
        
        categoryFileNames.forEachIndexed { index, fileName ->
            try {
                val filePath = "${selectedDataSource.folderName}/$fileName.json"
                Log.d("QaDataLoaderImpl", "파일 로드 시도: $filePath")
                val inputStream = context.assets.open(filePath)
                val reader = InputStreamReader(inputStream, "UTF-8")
                
                // JSON 구조에 맞는 임시 데이터 클래스
                data class TempQaItem(
                    val id: String,
                    val question_en: String?, // Made nullable
                    val question_ko: String?, // Made nullable
                    val answer_en: String?,   // Made nullable
                    val answer_ko: String?    // Made nullable
                )
                
                val type = object : TypeToken<List<TempQaItem>>() {}.type
                val tempQaItems: List<TempQaItem> = gson.fromJson(reader, type)
                
                Log.d("QaDataLoaderImpl", "Parsed ${tempQaItems.size} temporary items from file $filePath")
                
                // TempQaItem을 QaItem으로 변환
                val qaItems = tempQaItems.map { temp ->
                    Log.d("QaDataLoaderImpl", "Converting item: id=${temp.id}, question_en=${temp.question_en?.take(50)}..., answer_en=${temp.answer_en?.take(50)}...")
                    
                    // 문장 파싱
                    val questionEnSentences = SentenceParser.parseEnglishSentences(temp.question_en ?: "")
                    val questionKoSentences = SentenceParser.parseKoreanSentences(temp.question_ko ?: "")
                    val answerEnSentences = SentenceParser.parseEnglishSentences(temp.answer_en ?: "")
                    val answerKoSentences = SentenceParser.parseKoreanSentences(temp.answer_ko ?: "")
                    
                    QaItem(
                        id = temp.id,
                        category = categoryDisplayNames.getOrNull(index) ?: fileName,
                        questionEn = temp.question_en ?: "",
                        questionKo = temp.question_ko ?: "",
                        questionEnSentences = questionEnSentences,
                        questionKoSentences = questionKoSentences,
                        answerEnSentences = answerEnSentences,
                        answerKoSentences = answerKoSentences
                    )
                }
                
                val displayName = categoryDisplayNames.getOrNull(index) ?: fileName
                itemsByCategory[displayName] = qaItems
                Log.d("QaDataLoaderImpl", "카테고리 '$displayName'에서 ${qaItems.size}개 아이템 로드됨")
                
                reader.close()
                inputStream.close()
            } catch (e: Exception) {
                // 파일이 없거나 읽기 실패 시 빈 리스트로 처리
                val displayName = categoryDisplayNames.getOrNull(index) ?: fileName
                itemsByCategory[displayName] = emptyList()
                Log.e("QaDataLoaderImpl", "파일 로드 실패: ${selectedDataSource.folderName}/$fileName.json", e)
            }
        }
        
        Log.d("QaDataLoaderImpl", "전체 QA 데이터 로드 완료: ${itemsByCategory.size}개 카테고리")
        return itemsByCategory
    }
    
    override suspend fun getQaItemsByCategory(category: String): List<QaItem> {
        Log.d("QaDataLoaderImpl", "카테고리 '$category'의 QA 아이템 요청")
        val allItems = loadQaItemsFromAssets()
        val items = allItems[category] ?: emptyList()
        Log.d("QaDataLoaderImpl", "카테고리 '$category'에서 ${items.size}개의 QA 아이템 로드됨")
        return items
    }
    
    override suspend fun getAllCategories(): List<String> {
        return categoryDisplayNames
    }
} 