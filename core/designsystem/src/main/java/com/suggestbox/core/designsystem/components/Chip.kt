package com.suggestbox.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suggestbox.core.designsystem.motion.pressScaleEffect
import com.suggestbox.core.designsystem.motion.rememberSuggestHaptics
import com.suggestbox.core.designsystem.theme.SuggestTheme

/**
 * Smooth pill-shaped filter/category chip meeting 48dp touch height when interactive.
 *
 * @param label Visible chip text.
 * @param selected Active selection state.
 * @param onClick Tap callback with haptic feedback.
 * @param leadingIcon Optional rounded icon.
 */
@Composable
fun SuggestChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val colors = SuggestTheme.colors
    val haptics = rememberSuggestHaptics()

    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surface,
        label = "chipBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.textPrimary,
        label = "chipFg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.separator.copy(alpha = 0.7f),
        label = "chipBorder",
    )

    val shape = RoundedCornerShape(999.dp)

    Surface(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .pressScaleEffect()
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab) {
                haptics.select()
                onClick()
            },
        shape = shape,
        color = containerColor,
        border = BorderStroke(0.8.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}
