package com.suggestbox.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme

/**
 * Semantic status variants mapped cleanly from domain statuses.
 */
enum class StatusTone {
    SUBMITTED,
    UNDER_REVIEW,
    IN_PROGRESS,
    RESOLVED,
    REJECTED,
}

/**
 * Priority urgency variants for official SLA handling.
 */
enum class PriorityTone {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
}

/**
 * Calm, official status pill badge with leading indicator dot and WCAG AA foreground/background pairing.
 */
@Composable
fun StatusBadge(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val colors = SuggestTheme.colors
    val (fg, bg) = when (tone) {
        StatusTone.SUBMITTED -> colors.statusSubmitted to colors.statusSubmittedBg
        StatusTone.UNDER_REVIEW -> colors.statusUnderReview to colors.statusUnderReviewBg
        StatusTone.IN_PROGRESS -> colors.statusInProgress to colors.statusInProgressBg
        StatusTone.RESOLVED -> colors.statusResolved to colors.statusResolvedBg
        StatusTone.REJECTED -> colors.statusRejected to colors.statusRejectedBg
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(fg),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
        )
    }
}

/**
 * Compact priority tag badge used in the official inbox and detail views.
 */
@Composable
fun PriorityBadge(
    label: String,
    tone: PriorityTone,
    modifier: Modifier = Modifier,
) {
    val colors = SuggestTheme.colors
    val (fg, bg) = when (tone) {
        PriorityTone.LOW -> colors.textSecondary to colors.surfaceSecondary
        PriorityTone.MEDIUM -> colors.statusSubmitted to colors.statusSubmittedBg
        PriorityTone.HIGH -> colors.warning to colors.warningBg
        PriorityTone.URGENT -> colors.destructive to colors.destructiveBg
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (tone == PriorityTone.URGENT || tone == PriorityTone.HIGH) {
            Icon(
                imageVector = SuggestIcons.Priority,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
        )
    }
}

/**
 * SLA countdown or overdue indicator badge for municipal officers.
 */
@Composable
fun SlaBadge(
    label: String,
    isOverdue: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = SuggestTheme.colors
    val fg: Color = if (isOverdue) colors.destructive else colors.textSecondary
    val bg: Color = if (isOverdue) colors.destructiveBg else colors.surfaceSecondary.copy(alpha = 0.7f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = SuggestIcons.Schedule,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
        )
    }
}
