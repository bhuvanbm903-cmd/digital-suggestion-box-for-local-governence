package com.suggestbox.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suggestbox.core.designsystem.motion.pressScaleEffect
import com.suggestbox.core.designsystem.motion.rememberSuggestHaptics
import com.suggestbox.core.designsystem.theme.SuggestTheme

/**
 * Elevated rounded white (or dark elevated) card surface mimicking Apple HIG grouped cards.
 *
 * @param modifier Optional modifier.
 * @param cornerRadius Card radius (defaults to 18dp).
 * @param contentPadding Inner padding.
 * @param onClick Optional tap handler; enables subtle press scale & haptic feedback when set.
 * @param contentDescription Optional TalkBack description when clickable.
 * @param containerColor Override card background color.
 * @param content Card body composable.
 */
@Composable
fun SuggestCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SuggestTheme.spacing.cardCorner,
    contentPadding: PaddingValues = PaddingValues(SuggestTheme.spacing.md),
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    containerColor: Color = SuggestTheme.colors.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SuggestTheme.colors
    val haptics = rememberSuggestHaptics()
    val shape = RoundedCornerShape(cornerRadius)

    val cardModifier = modifier
        .then(
            if (!colors.isDark) {
                Modifier.shadow(
                    elevation = 2.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.06f),
                )
            } else {
                Modifier
            },
        )
        .clip(shape)
        .then(
            if (onClick != null) {
                Modifier
                    .pressScaleEffect()
                    .clickable(role = Role.Button) {
                        haptics.tap()
                        onClick()
                    }
            } else {
                Modifier
            },
        )
        .then(
            if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            },
        )

    Surface(
        modifier = cardModifier,
        shape = shape,
        color = containerColor,
        border = if (colors.isDark) {
            BorderStroke(0.5.dp, colors.separator.copy(alpha = 0.6f))
        } else {
            BorderStroke(0.5.dp, colors.separator.copy(alpha = 0.3f))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}

/**
 * Compact square/rectangular quick-action tile used on Home & Official Dashboards.
 */
@Composable
fun ActionTileCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SuggestTheme.colors

    SuggestCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
        contentDescription = "$title, $subtitle",
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
