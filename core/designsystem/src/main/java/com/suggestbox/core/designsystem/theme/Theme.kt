package com.suggestbox.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalSuggestColors = staticCompositionLocalOf { LightSuggestColors }
val LocalSuggestSpacing = staticCompositionLocalOf { SuggestSpacing() }

private val SuggestShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Accessor object for SuggestBox design tokens.
 */
object SuggestTheme {
    val colors: SuggestColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSuggestColors.current

    val spacing: SuggestSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSuggestSpacing.current
}

/**
 * Top-level design system theme applying iOS-inspired surfaces, calm government blue accent,
 * typography, shapes, and full dark mode support.
 *
 * @param darkTheme Whether dark appearance is active. Defaults to system setting.
 * @param content Composable hierarchy to wrap with theme tokens.
 */
@Composable
fun SuggestBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val suggestColors = if (darkTheme) DarkSuggestColors else LightSuggestColors
    val spacing = SuggestSpacing()

    val materialScheme = if (darkTheme) {
        darkColorScheme(
            primary = suggestColors.primary,
            onPrimary = suggestColors.onPrimary,
            primaryContainer = suggestColors.primaryContainer,
            background = suggestColors.background,
            onBackground = suggestColors.textPrimary,
            surface = suggestColors.surface,
            onSurface = suggestColors.textPrimary,
            surfaceVariant = suggestColors.surfaceElevated,
            onSurfaceVariant = suggestColors.textSecondary,
            outline = suggestColors.separator,
            error = suggestColors.destructive,
        )
    } else {
        lightColorScheme(
            primary = suggestColors.primary,
            onPrimary = suggestColors.onPrimary,
            primaryContainer = suggestColors.primaryContainer,
            background = suggestColors.background,
            onBackground = suggestColors.textPrimary,
            surface = suggestColors.surface,
            onSurface = suggestColors.textPrimary,
            surfaceVariant = suggestColors.surfaceElevated,
            onSurfaceVariant = suggestColors.textSecondary,
            outline = suggestColors.separator,
            error = suggestColors.destructive,
        )
    }

    CompositionLocalProvider(
        LocalSuggestColors provides suggestColors,
        LocalSuggestSpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = SuggestTypography,
            shapes = SuggestShapes,
            content = content,
        )
    }
}
