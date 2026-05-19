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
data class MemorizeLevelColorSet(
    val backgroundLight: Color,
    val surfaceLight: Color,
    val primaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val secondaryLight: Color,
    val onSecondaryLight: Color,
    val tertiaryLight: Color,
    val onTertiaryLight: Color,
    val cardLight: Color,
    val backgroundDark: Color,
    val surfaceDark: Color,
    val primaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
    val secondaryDark: Color,
    val onSecondaryDark: Color,
    val tertiaryDark: Color,
    val onTertiaryDark: Color,
    val cardDark: Color
)

object MemorizeLevelColors {
    // 반복 듣기 - 차분한 블루 톤
    val RepeatListening = MemorizeLevelColorSet(
        backgroundLight = Color(0xFFF0F4FA),
        surfaceLight = Color(0xFFE1EBF5),
        primaryLight = Color(0xFF4A90E2),
        primaryContainerLight = Color(0xFFD4E6FA),
        onPrimaryContainerLight = Color(0xFF1A3F6E),
        secondaryLight = Color(0xFF5B9BD5),
        onSecondaryLight = Color(0xFFFFFFFF),
        tertiaryLight = Color(0xFF7BAFD4),
        onTertiaryLight = Color(0xFFFFFFFF),
        cardLight = Color(0xFFECF2FA),
        backgroundDark = Color(0xFF0A1A2E),
        surfaceDark = Color(0xFF1A2B3E),
        primaryDark = Color(0xFF64B5F6),
        primaryContainerDark = Color(0xFF1E3A5F),
        onPrimaryContainerDark = Color(0xFFB0D4F1),
        secondaryDark = Color(0xFF4DB6AC),
        onSecondaryDark = Color(0xFF00251A),
        tertiaryDark = Color(0xFF81D4FA),
        onTertiaryDark = Color(0xFF001D31),
        cardDark = Color(0xFF2A3B4E)
    )

    // 영작 테스트 - 따뜻한 오렌지 톤
    val WritingTest = MemorizeLevelColorSet(
        backgroundLight = Color(0xFFFFF3ED),
        surfaceLight = Color(0xFFFFE8DB),
        primaryLight = Color(0xFFFF7043),
        primaryContainerLight = Color(0xFFFFDCC8),
        onPrimaryContainerLight = Color(0xFF6E2C0F),
        secondaryLight = Color(0xFFFFAB91),
        onSecondaryLight = Color(0xFF5C1F00),
        tertiaryLight = Color(0xFFFFB74D),
        onTertiaryLight = Color(0xFF5C3A00),
        cardLight = Color(0xFFFFF0E6),
        backgroundDark = Color(0xFF2E1A0A),
        surfaceDark = Color(0xFF3E2A1A),
        primaryDark = Color(0xFFFF8A65),
        primaryContainerDark = Color(0xFF5C2D10),
        onPrimaryContainerDark = Color(0xFFFFDCC8),
        secondaryDark = Color(0xFFE64A19),
        onSecondaryDark = Color(0xFFFFFFFF),
        tertiaryDark = Color(0xFFFFB74D),
        onTertiaryDark = Color(0xFF5C3A00),
        cardDark = Color(0xFF4E3A2A)
    )

    // 통암기 - 우아한 퍼플 톤
    val FullMemorization = MemorizeLevelColorSet(
        backgroundLight = Color(0xFFF6EEFA),
        surfaceLight = Color(0xFFEDE0F3),
        primaryLight = Color(0xFF9C27B0),
        primaryContainerLight = Color(0xFFEFC8F5),
        onPrimaryContainerLight = Color(0xFF3D005E),
        secondaryLight = Color(0xFFBA68C8),
        onSecondaryLight = Color(0xFFFFFFFF),
        tertiaryLight = Color(0xFFCE93D8),
        onTertiaryLight = Color(0xFF3B002E),
        cardLight = Color(0xFFF5EAF9),
        backgroundDark = Color(0xFF2A0A2E),
        surfaceDark = Color(0xFF3A1A3E),
        primaryDark = Color(0xFFBA68C8),
        primaryContainerDark = Color(0xFF4A1A5E),
        onPrimaryContainerDark = Color(0xFFEFC8F5),
        secondaryDark = Color(0xFFAB47BC),
        onSecondaryDark = Color(0xFFFFFFFF),
        tertiaryDark = Color(0xFFCE93D8),
        onTertiaryDark = Color(0xFF3B002E),
        cardDark = Color(0xFF4A2A4E)
    )
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
