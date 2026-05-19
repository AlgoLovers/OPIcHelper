package com.na982.opichelper.ui.theme

import com.na982.opichelper.domain.entity.MemorizeLevel
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.White,
    primaryContainer = PrimaryTealDark,
    onPrimaryContainer = Color.White,

    secondary = SecondaryCyan,
    onSecondary = Color.White,
    secondaryContainer = SecondaryCyanDark,
    onSecondaryContainer = Color.White,

    tertiary = TertiaryGreen,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryGreenDark,
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
    inversePrimary = PrimaryTealLight
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.White,
    primaryContainer = PrimaryTealLight,
    onPrimaryContainer = PrimaryTealDark,

    secondary = SecondaryCyan,
    onSecondary = Color.White,
    secondaryContainer = SecondaryCyanLight,
    onSecondaryContainer = SecondaryCyanDark,

    tertiary = TertiaryGreen,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryGreenLight,
    onTertiaryContainer = TertiaryGreenDark,

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
    inversePrimary = PrimaryTeal
)

@Composable
fun OPicHelperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

@Composable
fun OPicHelperThemeWithMemorizeLevel(
    darkTheme: Boolean = isSystemInDarkTheme(),
    memorizeLevel: String = MemorizeLevel.REPEAT_LISTENING.displayName,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val level = MemorizeLevel.fromDisplayName(memorizeLevel)
    val levelColors = when (level) {
        MemorizeLevel.REPEAT_LISTENING -> MemorizeLevelColors.RepeatListening
        MemorizeLevel.ENGLISH_WRITING -> MemorizeLevelColors.WritingTest
        MemorizeLevel.FULL_MEMORIZATION -> MemorizeLevelColors.FullMemorization
    }

    val dynamicColorScheme = baseColorScheme.copy(
        background = if (darkTheme) levelColors.backgroundDark else levelColors.backgroundLight,
        surface = if (darkTheme) levelColors.surfaceDark else levelColors.surfaceLight,
        surfaceVariant = if (darkTheme) levelColors.cardDark else levelColors.cardLight,
        primary = if (darkTheme) levelColors.primaryDark else levelColors.primaryLight,
        onBackground = if (darkTheme) TextPrimaryDark else TextPrimaryLight,
        onSurface = if (darkTheme) TextPrimaryDark else TextPrimaryLight,
        onSurfaceVariant = if (darkTheme) TextSecondaryDark else TextSecondaryLight,
        primaryContainer = if (darkTheme) levelColors.primaryContainerDark else levelColors.primaryContainerLight,
        onPrimaryContainer = if (darkTheme) levelColors.onPrimaryContainerDark else levelColors.onPrimaryContainerLight,
        secondary = if (darkTheme) levelColors.secondaryDark else levelColors.secondaryLight,
        onSecondary = if (darkTheme) levelColors.onSecondaryDark else levelColors.onSecondaryLight,
        tertiary = if (darkTheme) levelColors.tertiaryDark else levelColors.tertiaryLight,
        onTertiary = if (darkTheme) levelColors.onTertiaryDark else levelColors.onTertiaryLight
    )

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        content = content
    )
}
