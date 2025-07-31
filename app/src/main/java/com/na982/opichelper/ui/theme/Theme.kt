package com.na982.opichelper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color.White,
    
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = SecondaryTealDark,
    onSecondaryContainer = Color.White,
    
    tertiary = TertiaryOrange,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryOrangeDark,
    onTertiaryContainer = Color.White,
    
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,
    
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedDark,
    onErrorContainer = Color.White,
    
    outline = NeutralGray,
    outlineVariant = NeutralGrayDark,
    
    scrim = Color.Black.copy(alpha = 0.32f),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = PrimaryBlueLight
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = SecondaryTealLight,
    onSecondaryContainer = SecondaryTealDark,
    
    tertiary = TertiaryOrange,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryOrangeLight,
    onTertiaryContainer = TertiaryOrangeDark,
    
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextSecondaryLight,
    
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedLight,
    onErrorContainer = ErrorRedDark,
    
    outline = NeutralGray,
    outlineVariant = NeutralGrayLight,
    
    scrim = Color.Black.copy(alpha = 0.32f),
    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,
    inversePrimary = PrimaryBlue
)

@Composable
fun OPicHelperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // OPic Helper 브랜드 색상 유지
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * 암기레벨별 동적 테마
 */
@Composable
fun OPicHelperThemeWithMemorizeLevel(
    darkTheme: Boolean = isSystemInDarkTheme(),
    memorizeLevel: String = "반복 듣기",
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // 암기레벨별 색상 적용 (다크 테마 고려)
    val backgroundColor = getMemorizeLevelBackground(memorizeLevel, darkTheme)
    val surfaceColor = getMemorizeLevelSurface(memorizeLevel, darkTheme)
    val cardColor = getMemorizeLevelCard(memorizeLevel, darkTheme)
    val primaryColor = when (memorizeLevel) {
        "반복 듣기" -> if (darkTheme) MemorizeLevelColors.RepeatListening.primaryDark else MemorizeLevelColors.RepeatListening.primaryLight
        "영작 테스트" -> if (darkTheme) MemorizeLevelColors.WritingTest.primaryDark else MemorizeLevelColors.WritingTest.primaryLight
        "통암기" -> if (darkTheme) MemorizeLevelColors.FullMemorization.primaryDark else MemorizeLevelColors.FullMemorization.primaryLight
        else -> if (darkTheme) MemorizeLevelColors.RepeatListening.primaryDark else MemorizeLevelColors.RepeatListening.primaryLight
    }
    
    // 디버깅 로그
    android.util.Log.d("Theme", "암기레벨: $memorizeLevel, 다크테마: $darkTheme")
    android.util.Log.d("Theme", "배경색: $backgroundColor, 서피스색: $surfaceColor, 카드색: $cardColor, 주색: $primaryColor")
    
    val dynamicColorScheme = baseColorScheme.copy(
        background = backgroundColor,
        surface = surfaceColor,
        surfaceVariant = cardColor,
        primary = primaryColor,
        onBackground = if (darkTheme) Color.White else Color.Black,
        onSurface = if (darkTheme) Color.White else Color.Black
    )

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        content = content
    )
}