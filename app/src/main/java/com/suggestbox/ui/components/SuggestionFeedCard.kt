package com.suggestbox.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.PriorityBadge
import com.suggestbox.core.designsystem.components.SlaBadge
import com.suggestbox.core.designsystem.components.StatusBadge
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.motion.pressScaleEffect
import com.suggestbox.core.designsystem.motion.rememberSuggestHaptics
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.core.designsystem.theme.TabularNumeralStyle
import com.suggestbox.domain.model.SubmissionKind
import com.suggestbox.domain.model.Suggestion

/**
 * Reusable civic suggestion card displaying category, Fix-It/Build-It badge, status,
 * location metadata, tracking ID, and interactive upvote pill.
 */
@Composable
fun SuggestionFeedCard(
    suggestion: Suggestion,
    onCardClick: () -> Unit,
    onUpvoteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSlaBadge: Boolean = false,
) {
    val colors = SuggestTheme.colors
    val haptics = rememberSuggestHaptics()

    val upvoteBg by animateColorAsState(
        targetValue = if (suggestion.upvotedByCurrentUser) colors.primaryContainer else colors.surfaceSecondary.copy(alpha = 0.6f),
        label = "upvoteBg",
    )
    val upvoteFg by animateColorAsState(
        targetValue = if (suggestion.upvotedByCurrentUser) colors.primary else colors.textSecondary,
        label = "upvoteFg",
    )

    SuggestCard(
        modifier = modifier,
        onClick = onCardClick,
        contentDescription = "${suggestion.title}, ${suggestion.trackingId}",
    ) {
        // Top metadata row: Category + Kind + Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = suggestion.category.icon(),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(suggestion.category.labelRes),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (suggestion.kind == SubmissionKind.IDEA) {
                                colors.statusUnderReviewBg
                            } else {
                                colors.surfaceSecondary.copy(alpha = 0.7f)
                            },
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (suggestion.kind == SubmissionKind.IDEA) {
                                R.string.kind_idea_short
                            } else {
                                R.string.kind_issue_short
                            },
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (suggestion.kind == SubmissionKind.IDEA) {
                            colors.statusUnderReview
                        } else {
                            colors.textSecondary
                        },
                    )
                }
            }

            StatusBadge(
                label = stringResource(suggestion.status.labelRes),
                tone = suggestion.status.tone,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = suggestion.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = suggestion.description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Location & timestamp line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = SuggestIcons.Location,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "${suggestion.location.addressLabel} • ${formatRelativeTime(suggestion.createdAtMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom bar: Tracking ID / Author + Upvote Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = suggestion.trackingId,
                    style = MaterialTheme.typography.labelMedium.merge(TabularNumeralStyle),
                    color = colors.textSecondary,
                )
                if (showSlaBadge) {
                    if (suggestion.isOverdue) {
                        SlaBadge(
                            label = stringResource(R.string.detail_sla_overdue),
                            isOverdue = true,
                        )
                    } else {
                        PriorityBadge(
                            label = stringResource(suggestion.priority.labelRes),
                            tone = suggestion.priority.tone,
                        )
                    }
                }
                if (suggestion.isAnonymous) {
                    Icon(
                        imageVector = SuggestIcons.Anonymous,
                        contentDescription = "Anonymous submission",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .pressScaleEffect()
                    .semantics {
                        contentDescription = "Upvote ${suggestion.upvoteCount}"
                    }
                    .clickable(role = Role.Button) {
                        haptics.select()
                        onUpvoteClick()
                    },
                shape = RoundedCornerShape(999.dp),
                color = upvoteBg,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = SuggestIcons.Upvote,
                        contentDescription = null,
                        tint = upvoteFg,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = suggestion.upvoteCount.toString(),
                        style = MaterialTheme.typography.labelMedium.merge(TabularNumeralStyle).copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = upvoteFg,
                    )
                }
            }
        }
    }
}
