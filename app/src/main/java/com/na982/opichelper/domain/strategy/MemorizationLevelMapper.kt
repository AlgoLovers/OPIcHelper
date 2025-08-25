package com.na982.opichelper.domain.strategy

import android.util.Log
import com.na982.opichelper.domain.entity.MemorizeLevel
import javax.inject.Inject
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * 암기 레벨 매핑 전용 클래스
 * String을 MemorizeLevel으로 변환하는 로직을 중앙화
 * 
 * 클린 아키텍처 원칙:
 * - Domain Layer에서 비즈니스 로직 처리
 * - 단일 책임 원칙 준수
 * - 중복 코드 제거
 */
@ViewModelScoped
class MemorizationLevelMapper @Inject constructor() {
    
    /**
     * String을 MemorizeLevel으로 변환
     * @param levelString UI에서 전달받은 레벨 문자열
     * @return 해당하는 MemorizeLevel, 매칭되지 않으면 null
     */
    fun mapToMemorizeLevel(levelString: String): MemorizeLevel? {
        return when (levelString.trim()) {
            "반복 듣기", "반복듣기" -> {
                Log.d("MemorizationLevelMapper", "반복듣기 레벨 매핑")
                MemorizeLevel.REPEAT_LISTENING
            }
            "영작 테스트", "영작테스트" -> {
                Log.d("MemorizationLevelMapper", "영작테스트 레벨 매핑")
                MemorizeLevel.ENGLISH_WRITING
            }
            "통암기" -> {
                Log.d("MemorizationLevelMapper", "통암기 레벨 매핑")
                MemorizeLevel.FULL_MEMORIZATION
            }
            else -> {
                Log.w("MemorizationLevelMapper", "알 수 없는 암기 레벨: '$levelString'")
                null
            }
        }
    }
    
    /**
     * MemorizeLevel을 String으로 변환 (UI 표시용)
     * @param level MemorizeLevel
     * @return UI에 표시할 문자열
     */
    fun mapToString(level: MemorizeLevel): String {
        return when (level) {
            MemorizeLevel.REPEAT_LISTENING -> "반복 듣기"
            MemorizeLevel.ENGLISH_WRITING -> "영작 테스트"
            MemorizeLevel.FULL_MEMORIZATION -> "통암기"
        }
    }
    
    /**
     * 모든 유효한 레벨 문자열 목록 반환
     * @return UI에서 사용할 수 있는 레벨 문자열 목록
     */
    fun getAllLevelStrings(): List<String> {
        return listOf(
            "반복 듣기",
            "영작 테스트", 
            "통암기"
        )
    }
    
    /**
     * 모든 MemorizeLevel 목록 반환
     * @return 모든 MemorizeLevel 열거형 값들
     */
    fun getAllMemorizeLevels(): List<MemorizeLevel> {
        return MemorizeLevel.values().toList()
    }
} 