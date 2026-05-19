package com.na982.opichelper.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors - Calm Teal
val PrimaryTeal = Color(0xFF009688)
val PrimaryTealLight = Color(0xFF4DB6AC)
val PrimaryTealDark = Color(0xFF00796B)

// Secondary Colors - Blue-Green
val SecondaryCyan = Color(0xFF00897B)
val SecondaryCyanLight = Color(0xFF4DB6AC)
val SecondaryCyanDark = Color(0xFF00695C)

// Tertiary Colors - Soft Green
val TertiaryGreen = Color(0xFF43A047)
val TertiaryGreenLight = Color(0xFF66BB6A)
val TertiaryGreenDark = Color(0xFF2E7D32)

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
val BackgroundLight = Color(0xFFF5FAFA)
val BackgroundDark = Color(0xFF0D1B1E)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1A2E31)

// Card Colors
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF243B3E)
val CardElevatedLight = Color(0xFFEFF5F4)
val CardElevatedDark = Color(0xFF2E4A4E)

// Text Colors
val TextPrimaryLight = Color(0xFF1B2E2D)
val TextPrimaryDark = Color(0xFFE0F0EE)
val TextSecondaryLight = Color(0xFF5A7A78)
val TextSecondaryDark = Color(0xFFA0C4C0)

// Gradient Colors - Teal to Green
val GradientStart = Color(0xFF009688)
val GradientEnd = Color(0xFF43A047)
val GradientAccent = Color(0xFF26A69A)

// 암기레벨별 색상 — 모두 청록색 톤 통일, 미세한 색조 차이만
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
    // 반복 듣기 - 파란빛 청록 (Cyan-Teal)
    val RepeatListening = MemorizeLevelColorSet(
        backgroundLight = Color(0xFFEFF7F7),
        surfaceLight = Color(0xFFE0EFEF),
        primaryLight = Color(0xFF00897B),
        primaryContainerLight = Color(0xFFC8E6E3),
        onPrimaryContainerLight = Color(0xFF003D36),
        secondaryLight = Color(0xFF26A69A),
        onSecondaryLight = Color(0xFFFFFFFF),
        tertiaryLight = Color(0xFF4DB6AC),
        onTertiaryLight = Color(0xFFFFFFFF),
        cardLight = Color(0xFFE8F3F2),
        backgroundDark = Color(0xFF0D1B1E),
        surfaceDark = Color(0xFF1A2E31),
        primaryDark = Color(0xFF4DB6AC),
        primaryContainerDark = Color(0xFF1A3A38),
        onPrimaryContainerDark = Color(0xFFB0D4D0),
        secondaryDark = Color(0xFF26A69A),
        onSecondaryDark = Color(0xFF00251A),
        tertiaryDark = Color(0xFF80CBC4),
        onTertiaryDark = Color(0xFF003C33),
        cardDark = Color(0xFF243B3E)
    )

    // 영작 테스트 - 순수 청록 (Teal)
    val WritingTest = MemorizeLevelColorSet(
        backgroundLight = Color(0xFFECF5F3),
        surfaceLight = Color(0xFFDCEEEB),
        primaryLight = Color(0xFF009688),
        primaryContainerLight = Color(0xFFB8DFDB),
        onPrimaryContainerLight = Color(0xFF003B33),
        secondaryLight = Color(0xFF00897B),
        onSecondaryLight = Color(0xFFFFFFFF),
        tertiaryLight = Color(0xFF43A047),
        onTertiaryLight = Color(0xFFFFFFFF),
        cardLight = Color(0xFFE4F0ED),
        backgroundDark = Color(0xFF0E1C1A),
        surfaceDark = Color(0xFF1B302D),
        primaryDark = Color(0xFF4DB6AC),
        primaryContainerDark = Color(0xFF1B3833),
        onPrimaryContainerDark = Color(0xFFB2DFDB),
        secondaryDark = Color(0xFF00897B),
        onSecondaryDark = Color(0xFF00251A),
        tertiaryDark = Color(0xFF66BB6A),
        onTertiaryDark = Color(0xFF003D1A),
        cardDark = Color(0xFF253D39)
    )

    // 통암기 - 초록빛 청록 (Green-Teal)
    val FullMemorization = MemorizeLevelColorSet(
        backgroundLight = Color(0xFFEAF5F0),
        surfaceLight = Color(0xFFD8EDE4),
        primaryLight = Color(0xFF2E7D32),
        primaryContainerLight = Color(0xFFC8E6C9),
        onPrimaryContainerLight = Color(0xFF003D00),
        secondaryLight = Color(0xFF43A047),
        onSecondaryLight = Color(0xFFFFFFFF),
        tertiaryLight = Color(0xFF66BB6A),
        onTertiaryLight = Color(0xFFFFFFFF),
        cardLight = Color(0xFFE2F0E8),
        backgroundDark = Color(0xFF0E1C16),
        surfaceDark = Color(0xFF1B3025),
        primaryDark = Color(0xFF66BB6A),
        primaryContainerDark = Color(0xFF1B3820),
        onPrimaryContainerDark = Color(0xFFC8E6C9),
        secondaryDark = Color(0xFF43A047),
        onSecondaryDark = Color(0xFF00251A),
        tertiaryDark = Color(0xFF81C784),
        onTertiaryDark = Color(0xFF003D1A),
        cardDark = Color(0xFF253D2E)
    )
}
