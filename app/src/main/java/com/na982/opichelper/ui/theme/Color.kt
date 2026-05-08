package com.na982.opichelper.ui.theme

import androidx.compose.ui.graphics.Color
import com.na982.opichelper.domain.entity.MemorizeLevel

// Modern Color Palette for OPic Helper
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Primary Colors - Modern Blue Gradient
val PrimaryBlue = Color(0xFF2196F3)
val PrimaryBlueLight = Color(0xFF64B5F6)
val PrimaryBlueDark = Color(0xFF1976D2)

// Secondary Colors - Teal Accent
val SecondaryTeal = Color(0xFF009688)
val SecondaryTealLight = Color(0xFF4DB6AC)
val SecondaryTealDark = Color(0xFF00796B)

// Tertiary Colors - Orange for Highlights
val TertiaryOrange = Color(0xFFFF9800)
val TertiaryOrangeLight = Color(0xFFFFB74D)
val TertiaryOrangeDark = Color(0xFFF57C00)

// Success Colors - Green
val SuccessGreen = Color(0xFF4CAF50)
val SuccessGreenLight = Color(0xFF81C784)
val SuccessGreenDark = Color(0xFF388E3C)

// Error Colors - Red
val ErrorRed = Color(0xFFF44336)
val ErrorRedLight = Color(0xFFE57373)
val ErrorRedDark = Color(0xFFD32F2F)

// Neutral Colors
val NeutralGray = Color(0xFF9E9E9E)
val NeutralGrayLight = Color(0xFFE0E0E0)
val NeutralGrayDark = Color(0xFF616161)

// Background Colors
val BackgroundLight = Color(0xFFFAFAFA)
val BackgroundDark = Color(0xFF121212)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1E1E1E)

// Card Colors
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF2D2D2D)
val CardElevatedLight = Color(0xFFF5F5F5)
val CardElevatedDark = Color(0xFF3D3D3D)

// Text Colors
val TextPrimaryLight = Color(0xFF212121)
val TextPrimaryDark = Color(0xFFE0E0E0)
val TextSecondaryLight = Color(0xFF757575)
val TextSecondaryDark = Color(0xFFB0B0B0)

// Gradient Colors
val GradientStart = Color(0xFF2196F3)
val GradientEnd = Color(0xFF009688)
val GradientAccent = Color(0xFFFF9800)

// 암기레벨별 색상 (라이트/다크 테마 구분)
object MemorizeLevelColors {
    // 반복 듣기 - 차분한 블루 톤
    object RepeatListening {
        // 라이트 테마
        val backgroundLight = Color(0xFFF8FBFF)    // 매우 연한 블루 배경
        val surfaceLight = Color(0xFFE3F2FD)       // 연한 블루 서피스
        val primaryLight = Color(0xFF4A90E2)       // 차분한 블루
        val cardLight = Color(0xFFF0F8FF)          // 연한 블루 카드
        
        // 다크 테마
        val backgroundDark = Color(0xFF0A1A2E)     // 어두운 블루 배경
        val surfaceDark = Color(0xFF1A2B3E)        // 어두운 블루 서피스
        val primaryDark = Color(0xFF64B5F6)        // 밝은 블루 (다크에서)
        val cardDark = Color(0xFF2A3B4E)           // 어두운 블루 카드
    }
    
    // 영작 테스트 - 따뜻한 오렌지 톤
    object WritingTest {
        // 라이트 테마
        val backgroundLight = Color(0xFFFFF8F5)    // 매우 연한 오렌지 배경
        val surfaceLight = Color(0xFFFFF3E0)       // 연한 오렌지 서피스
        val primaryLight = Color(0xFFFF7043)       // 따뜻한 오렌지
        val cardLight = Color(0xFFFFF0E6)          // 연한 오렌지 카드
        
        // 다크 테마
        val backgroundDark = Color(0xFF2E1A0A)     // 어두운 오렌지 배경
        val surfaceDark = Color(0xFF3E2A1A)        // 어두운 오렌지 서피스
        val primaryDark = Color(0xFFFF8A65)        // 밝은 오렌지 (다크에서)
        val cardDark = Color(0xFF4E3A2A)           // 어두운 오렌지 카드
    }
    
    // 통암기 - 우아한 퍼플 톤
    object FullMemorization {
        // 라이트 테마
        val backgroundLight = Color(0xFFFDF8FF)    // 매우 연한 퍼플 배경
        val surfaceLight = Color(0xFFF3E5F5)       // 연한 퍼플 서피스
        val primaryLight = Color(0xFF9C27B0)       // 우아한 퍼플
        val cardLight = Color(0xFFF8F0FF)          // 연한 퍼플 카드
        
        // 다크 테마
        val backgroundDark = Color(0xFF2A0A2E)     // 어두운 퍼플 배경
        val surfaceDark = Color(0xFF3A1A3E)        // 어두운 퍼플 서피스
        val primaryDark = Color(0xFFBA68C8)        // 밝은 퍼플 (다크에서)
        val cardDark = Color(0xFF4A2A4E)           // 어두운 퍼플 카드
    }
}

/**
 * 암기레벨과 테마에 따라 배경색을 반환하는 함수
 */
fun getMemorizeLevelBackground(level: String, isDarkTheme: Boolean): Color {
    return when (MemorizeLevel.fromDisplayName(level)) {
        MemorizeLevel.REPEAT_LISTENING -> if (isDarkTheme) MemorizeLevelColors.RepeatListening.backgroundDark else MemorizeLevelColors.RepeatListening.backgroundLight
        MemorizeLevel.ENGLISH_WRITING -> if (isDarkTheme) MemorizeLevelColors.WritingTest.backgroundDark else MemorizeLevelColors.WritingTest.backgroundLight
        MemorizeLevel.FULL_MEMORIZATION -> if (isDarkTheme) MemorizeLevelColors.FullMemorization.backgroundDark else MemorizeLevelColors.FullMemorization.backgroundLight
    }
}

fun getMemorizeLevelSurface(level: String, isDarkTheme: Boolean): Color {
    return when (MemorizeLevel.fromDisplayName(level)) {
        MemorizeLevel.REPEAT_LISTENING -> if (isDarkTheme) MemorizeLevelColors.RepeatListening.surfaceDark else MemorizeLevelColors.RepeatListening.surfaceLight
        MemorizeLevel.ENGLISH_WRITING -> if (isDarkTheme) MemorizeLevelColors.WritingTest.surfaceDark else MemorizeLevelColors.WritingTest.surfaceLight
        MemorizeLevel.FULL_MEMORIZATION -> if (isDarkTheme) MemorizeLevelColors.FullMemorization.surfaceDark else MemorizeLevelColors.FullMemorization.surfaceLight
    }
}

fun getMemorizeLevelCard(level: String, isDarkTheme: Boolean): Color {
    return when (MemorizeLevel.fromDisplayName(level)) {
        MemorizeLevel.REPEAT_LISTENING -> if (isDarkTheme) MemorizeLevelColors.RepeatListening.cardDark else MemorizeLevelColors.RepeatListening.cardLight
        MemorizeLevel.ENGLISH_WRITING -> if (isDarkTheme) MemorizeLevelColors.WritingTest.cardDark else MemorizeLevelColors.WritingTest.cardLight
        MemorizeLevel.FULL_MEMORIZATION -> if (isDarkTheme) MemorizeLevelColors.FullMemorization.cardDark else MemorizeLevelColors.FullMemorization.cardLight
    }
}