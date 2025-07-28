package com.na982.opichelper.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.entity.LeveledAnswer
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
        UserLevel.IH_RAW to "ih_raw",
        UserLevel.IM to "im"
    )
    
    /**
     * 특정 레벨의 데이터를 로드
     */
    suspend fun loadQaItemsForLevel(level: UserLevel): List<QaItem> = withContext(Dispatchers.IO) {
        try {
            val folderName = levelFolderMapping[level] ?: "ih"
            val allQaItems = mutableListOf<QaItem>()
            
            // 해당 폴더의 모든 JSON 파일을 로드
            context.assets.list(folderName)?.forEach { fileName ->
                if (fileName.endsWith(".json")) {
                    try {
                        val jsonString = context.assets.open("$folderName/$fileName").bufferedReader().use { it.readText() }
                        val type = object : TypeToken<List<QaItemAsset>>() {}.type
                        val assetItems: List<QaItemAsset> = gson.fromJson(jsonString, type) ?: emptyList()
                        
                        // 기존 JSON 구조를 새로운 QaItem 구조로 변환
                        val items = assetItems.map { asset ->
                            val leveledAnswer = LeveledAnswer(
                                answerEn = asset.answer_en,
                                answerKo = asset.answer_ko
                            )
                            
                            QaItem(
                                id = asset.id ?: "",
                                category = getCategoryFromFileName(fileName),
                                questionEn = asset.question_en,
                                questionKo = asset.question_ko,
                                answers = mapOf(level to leveledAnswer)
                            )
                        }
                        
                        allQaItems.addAll(items)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            allQaItems
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 파일명에서 카테고리 추출
     */
    private fun getCategoryFromFileName(fileName: String): String {
        return when (fileName) {
            "qa_home.json" -> "집"
            "qa_music.json" -> "음악"
            "qa_home_vacation.json" -> "집에서 보내는 휴가"
            "qa_movie.json" -> "영화"
            "qa_restaurants.json" -> "레스토랑"
            "qa_beach.json" -> "해변"
            "qa_internet.json" -> "인터넷"
            "qa_industry_career.json" -> "산업,커리어"
            "qa_bank.json" -> "은행"
            "qa_transportation.json" -> "교통"
            "qa_fashion.json" -> "패션"
            "qa_family_friends.json" -> "가족,친구"
            "qa_furniture.json" -> "가구"
            "qa_reservation.json" -> "예약"
            "qa_holiday.json" -> "명절"
            else -> {
                // 파일명에서 qa_ 접두사와 .json 접미사 제거
                val categoryName = fileName.removePrefix("qa_").removeSuffix(".json")
                // 언더스코어를 공백으로 변경하고 첫 글자 대문자로
                categoryName.replace("_", " ").split(" ").joinToString("") { 
                    it.capitalize() 
                }
            }
        }
    }
    
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
     * 모든 레벨의 데이터를 로드 (관리자용)
     */
    suspend fun loadAllLevelData(): Map<UserLevel, List<QaItem>> = withContext(Dispatchers.IO) {
        UserLevel.values().associateWith { level ->
            loadQaItemsForLevel(level)
        }
    }
} 