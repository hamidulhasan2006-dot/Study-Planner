package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = OxfordNavy,
    onPrimary = Color.White,
    primaryContainer = PeriwinkleInk,
    onPrimaryContainer = OxfordNavyDark,
    secondary = OchreAmber,
    onSecondary = Color.White,
    secondaryContainer = OchreContainer,
    onSecondaryContainer = OchreDark,
    tertiary = SageEmerald,
    onTertiary = Color.White,
    tertiaryContainer = SageContainer,
    onTertiaryContainer = SageDark,
    background = IvoryPaper,
    onBackground = InkBlack,
    surface = CreamSurface,
    onSurface = InkBlack,
    surfaceVariant = ParchmentVariant,
    onSurfaceVariant = SlateMuted,
    outline = WarmBorder,
    outlineVariant = ParchmentVariant,
    error = CrimsonBrick,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonDark
)

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = OxfordNavyDark,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = PeriwinkleInk,
    secondary = NightSecondary,
    onSecondary = OchreDark,
    secondaryContainer = NightSecondaryContainer,
    onSecondaryContainer = OchreContainer,
    tertiary = NightTertiary,
    onTertiary = SageDark,
    tertiaryContainer = NightTertiaryContainer,
    onTertiaryContainer = SageContainer,
    background = NightSlateBg,
    onBackground = NightTextPrimary,
    surface = NightCardSurface,
    onSurface = NightTextPrimary,
    surfaceVariant = NightVariantSurface,
    onSurfaceVariant = NightTextMuted,
    outline = NightBorder,
    outlineVariant = NightVariantSurface,
    error = NightError,
    onError = CrimsonDark,
    errorContainer = NightErrorContainer,
    onErrorContainer = CrimsonContainer
)

val StudyPlannerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudentStudyPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudyPlannerTypography,
        shapes = StudyPlannerShapes,
        content = content
    )
}
