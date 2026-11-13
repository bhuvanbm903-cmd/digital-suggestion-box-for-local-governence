package com.suggestbox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spatial rhythm, corner radius, and accessibility sizing tokens for SuggestBox.
 */
@Immutable
data class SuggestSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val cardCorner: Dp = 18.dp,
    val cardCornerSmall: Dp = 12.dp,
    val sheetCorner: Dp = 24.dp,
    val minTouchTarget: Dp = 48.dp,
    val hairline: Dp = 0.5.dp,
)
