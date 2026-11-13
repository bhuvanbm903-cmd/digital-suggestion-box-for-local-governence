package com.suggestbox.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.motion.rememberSuggestHaptics
import com.suggestbox.core.designsystem.theme.SuggestTheme

/**
 * iOS Settings-style inset grouped container with optional section header and footer caption.
 *
 * @param title Optional uppercase/medium section header above the card.
 * @param footer Optional explanatory caption below the card.
 * @param content Rows placed inside the grouped container, separated by [GroupedDivider].
 */
@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SuggestTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .semantics { heading() },
            )
        }

        SuggestCard(
            cornerRadius = SuggestTheme.spacing.cardCorner,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            content = content,
        )

        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * Inset hairline divider aligned to text start when a leading icon badge is present.
 */
@Composable
fun GroupedDivider(hasLeadingIcon: Boolean = true) {
    HorizontalDivider(
        modifier = Modifier.padding(start = if (hasLeadingIcon) 58.dp else 16.dp),
        thickness = SuggestTheme.spacing.hairline,
        color = SuggestTheme.colors.separator.copy(alpha = 0.65f),
    )
}

/**
 * Interactive row inside a [GroupedSection] with leading icon badge, optional value, and chevron.
 */
@Composable
fun GroupedNavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingValue: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = SuggestTheme.colors.primary,
    isDestructive: Boolean = false,
) {
    val colors = SuggestTheme.colors
    val haptics = rememberSuggestHaptics()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button) {
                haptics.tap()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            LeadingIconBadge(
                icon = icon,
                tint = if (isDestructive) colors.destructive else iconTint,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isDestructive) FontWeight.Medium else FontWeight.Normal,
                ),
                color = if (isDestructive) colors.destructive else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (!trailingValue.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = trailingValue,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }

        if (!isDestructive) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = SuggestIcons.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Toggle switch row inside a [GroupedSection] meeting >=48dp touch height and TalkBack semantics.
 */
@Composable
fun GroupedToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = SuggestTheme.colors.primary,
) {
    val colors = SuggestTheme.colors
    val haptics = rememberSuggestHaptics()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Switch) {
                haptics.select()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            LeadingIconBadge(icon = icon, tint = iconTint)
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                haptics.select()
                onCheckedChange(newValue)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.success,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.surfaceSecondary,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun LeadingIconBadge(
    icon: ImageVector,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}
