package com.na982.opichelper.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.util.SentenceParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LeveledQaDataLoader(private val context: Context) {
    
    private val gson = Gson()
    
    /**
     * 레벨별 폴더명 매핑
     */
    private val levelFolderMapping = mapOf(
        UserLevel.AL to "al",
        UserLevel.IH to "ih", 
        UserLevel.IH_RAW to "ih_raw",  // hi_raw에서 ih_raw로 다시 변경
        UserLevel.SAMPLE to "sample_script",
        UserLevel.IM to "im"
    )

    /**
     * 기존 JSON 구조를 위한 데이터 클래스
     */
    private data class QaItemAsset(
        val id: String?,
        val question_en: String,
        val question_ko: String,
        val answer_en: String,
        val answer_ko: String
    )
    
    /**
     * 특정 카테고리의 데이터만 로드 (Lazy Loading용)
     */
    suspend fun loadQaItemsForCategory(level: UserLevel, category: String): List<QaItem> = withContext(Dispatchers.IO) {
        try {
            val folderName = levelFolderMapping[level] ?: "sample_script"
            val fileName = getFileNameFromCategory(category)
            val filePath = "$folderName/$fileName"
            
            Log.d("LeveledQaDataLoader", "카테고리별 로드: 레벨=$level, 카테고리=$category, 파일=$filePath")
            
            if (context.assets.list(folderName)?.contains(fileName) == true) {
                val jsonString = context.assets.open(filePath).bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<QaItemAsset>>() {}.type
                val assetItems: List<QaItemAsset> = gson.fromJson(jsonString, type) ?: emptyList()
                
                // 기존 JSON 구조를 새로운 QaItem 구조로 변환
                val items = assetItems.map { asset ->
                    // 4개의 문장 리스트 파싱
                    val questionEnSentences = SentenceParser.parseEnglishSentences(asset.question_en)
                    val questionKoSentences = SentenceParser.parseKoreanSentences(asset.question_ko)
                    val answerEnSentences = SentenceParser.parseEnglishSentences(asset.answer_en)
                    val answerKoSentences = SentenceParser.parseKoreanSentences(asset.answer_ko)
                    
                    QaItem(
                        id = asset.id ?: "",
                        category = category,
                        questionEn = asset.question_en,
                        questionKo = asset.question_ko,
                        questionEnSentences = questionEnSentences,
                        questionKoSentences = questionKoSentences,
                        answerEnSentences = answerEnSentences,
                        answerKoSentences = answerKoSentences
                    )
                }
                
                Log.d("LeveledQaDataLoader", "카테고리 '$category' 로드 완료: ${items.size}개 항목")
                items
            } else {
                Log.w("LeveledQaDataLoader", "파일이 존재하지 않음: $filePath")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("LeveledQaDataLoader", "카테고리 '$category' 로드 실패", e)
            emptyList()
        }
    }
    
    /**
     * 카테고리명에서 파일명 추출
     */
    private fun getFileNameFromCategory(category: String): String {
        return when (category) {
            "자기소개" -> "qa_intro.json"
            "롤플레이" -> "qa_roleplay.json"
            "집" -> "qa_home.json"
            "음악" -> "qa_music.json"
            "집에서 보내는 휴가" -> "qa_home_vacation.json"
            "영화" -> "qa_movie.json"
            "레스토랑" -> "qa_restaurants.json"
            "해변" -> "qa_beach.json"
            "인터넷" -> "qa_internet.json"
            "산업,커리어" -> "qa_industry_career.json"
            "은행" -> "qa_bank.json"
            "교통" -> "qa_transportation.json"
            "패션" -> "qa_fashion.json"
            "가족,친구" -> "qa_family_friends.json"
            "가구" -> "qa_furniture.json"
            "예약" -> "qa_reservation.json"
            "명절" -> "qa_holiday.json"
            else -> "qa_home.json" // 기본값
        }
    }
} 