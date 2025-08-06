package com.na982.opichelper.domain.util

import android.util.Log

/**
 * 문장 파싱 유틸리티
 */
object SentenceParser {
    
    /**
     * 영어 텍스트를 문장 단위로 분리
     */
    fun parseEnglishSentences(text: String): List<String> {
        return try {
            // 영어 문장 종료 패턴
            val englishSentencePattern = Regex("(?<=[.!?])\\s+")
            
            val sentences = text.split(englishSentencePattern)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            Log.d("SentenceParser", "영어 문장 파싱 완료: ${sentences.size}개 문장")
            sentences
            
        } catch (e: Exception) {
            Log.e("SentenceParser", "영어 문장 파싱 실패", e)
            listOf(text)
        }
    }
    
    /**
     * 한국어 텍스트를 문장 단위로 분리
     */
    fun parseKoreanSentences(text: String): List<String> {
        return try {
            // 한국어 문장 종료 패턴
            val koreanSentencePattern = Regex("(?<=[.!?])\\s+")
            
            val sentences = text.split(koreanSentencePattern)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            Log.d("SentenceParser", "한국어 문장 파싱 완료: ${sentences.size}개 문장")
            sentences
            
        } catch (e: Exception) {
            Log.e("SentenceParser", "한국어 문장 파싱 실패", e)
            listOf(text)
        }
    }
} 