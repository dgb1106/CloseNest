package com.example.closenest.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CloseNestPrimary,
    onPrimary = CloseNestOnPrimary,
    primaryContainer = CloseNestPrimaryContainer,
    onPrimaryContainer = CloseNestOnPrimaryContainer,
    secondary = CloseNestSecondary,
    onSecondary = CloseNestOnSecondary,
    secondaryContainer = CloseNestSecondaryContainer,
    onSecondaryContainer = CloseNestOnSecondaryContainer,
    tertiary = CloseNestTertiary,
    onTertiary = CloseNestOnTertiary,
    tertiaryContainer = CloseNestTertiaryContainer,
    onTertiaryContainer = CloseNestOnTertiaryContainer,
    background = CloseNestBackground,
    onBackground = CloseNestOnBackground,
    surface = CloseNestSurface,
    onSurface = CloseNestOnSurface,
    surfaceVariant = CloseNestSurfaceVariant,
    onSurfaceVariant = CloseNestOnSurfaceVariant,
    outline = CloseNestOutline,
    outlineVariant = CloseNestOutlineVariant,
    error = CloseNestError,
    onError = CloseNestOnError
)

private val DarkColorScheme = darkColorScheme(
    primary = ColorTokens.DarkPrimary,
    onPrimary = ColorTokens.DarkOnPrimary,
    primaryContainer = ColorTokens.DarkPrimaryContainer,
    onPrimaryContainer = ColorTokens.DarkOnPrimaryContainer,
    secondary = ColorTokens.DarkSecondary,
    onSecondary = ColorTokens.DarkOnSecondary,
    secondaryContainer = ColorTokens.DarkSecondaryContainer,
    onSecondaryContainer = ColorTokens.DarkOnSecondaryContainer,
    tertiary = ColorTokens.DarkTertiary,
    onTertiary = ColorTokens.DarkOnTertiary,
    tertiaryContainer = ColorTokens.DarkTertiaryContainer,
    onTertiaryContainer = ColorTokens.DarkOnTertiaryContainer,
    background = ColorTokens.DarkBackground,
    onBackground = ColorTokens.DarkOnBackground,
    surface = ColorTokens.DarkSurface,
    onSurface = ColorTokens.DarkOnSurface,
    surfaceVariant = ColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = ColorTokens.DarkOnSurfaceVariant,
    outline = ColorTokens.DarkOutline,
    outlineVariant = ColorTokens.DarkOutlineVariant,
    error = CloseNestError,
    onError = CloseNestOnError
)

private val CloseNestShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (dynamicColor) {
        LightColorScheme
    } else if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CloseNestTypography,
        shapes = CloseNestShapes,
        content = content
    )
}

private object ColorTokens {
    val DarkPrimary = CloseNestPrimaryContainer
    val DarkOnPrimary = CloseNestOnPrimaryContainer
    val DarkPrimaryContainer = CloseNestPrimary
    val DarkOnPrimaryContainer = CloseNestOnPrimary
    val DarkSecondary = CloseNestSecondaryContainer
    val DarkOnSecondary = CloseNestOnSecondaryContainer
    val DarkSecondaryContainer = CloseNestSecondary
    val DarkOnSecondaryContainer = CloseNestOnSecondary
    val DarkTertiary = CloseNestTertiaryContainer
    val DarkOnTertiary = CloseNestOnTertiaryContainer
    val DarkTertiaryContainer = CloseNestTertiary
    val DarkOnTertiaryContainer = CloseNestOnTertiary
    val DarkBackground = androidx.compose.ui.graphics.Color(0xFF241D19)
    val DarkOnBackground = androidx.compose.ui.graphics.Color(0xFFF7ECE6)
    val DarkSurface = androidx.compose.ui.graphics.Color(0xFF2B231F)
    val DarkOnSurface = androidx.compose.ui.graphics.Color(0xFFF6ECE7)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF534740)
    val DarkOnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFE0D0C7)
    val DarkOutline = androidx.compose.ui.graphics.Color(0xFF9F8E84)
    val DarkOutlineVariant = androidx.compose.ui.graphics.Color(0xFF6A5E57)
}
