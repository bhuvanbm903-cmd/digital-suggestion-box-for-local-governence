package com.suggestbox.ui.screens.detail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.PrimaryButton
import com.suggestbox.core.designsystem.components.PriorityBadge
import com.suggestbox.core.designsystem.components.SecondaryButton
import com.suggestbox.core.designsystem.components.SlaBadge
import com.suggestbox.core.designsystem.components.StatusBadge
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.components.SuggestTextField
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.motion.rememberSuggestHaptics
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.core.designsystem.theme.TabularNumeralStyle
import com.suggestbox.domain.model.StatusUpdateEvent
import com.suggestbox.domain.model.SuggestionStatus
import com.suggestbox.ui.components.formatExactDateTime
import com.suggestbox.ui.components.formatRelativeTime
import com.suggestbox.ui.components.icon

/**
 * Detailed view of a suggestion with shared-element header, official status timeline,
 * Before/After resolution proof verification, community comments, and 1-5 star audit rating.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SuggestionDetailScreen(
    suggestionId: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    viewModel: SuggestionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    val haptics = rememberSuggestHaptics()
    val suggestion = state.suggestion

    LargeTitleScaffold(
        title = suggestion?.trackingId ?: stringResource(R.string.detail_title),
        subtitle = suggestion?.wardName ?: stringResource(R.string.ward_default_eyebrow),
        onBackClick = onBack,
        backContentDescription = stringResource(R.string.action_back),
    ) { innerPadding ->
        if (suggestion == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.track_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
            }
            return@LargeTitleScaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.md,
                end = spacing.md,
                top = innerPadding.calculateTopPadding() + spacing.xs,
                bottom = innerPadding.calculateBottomPadding() + spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            // Hero Shared-Element Card
            item {
                with(sharedTransitionScope) {
                    SuggestCard(
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "card-${suggestion.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusBadge(
                                label = stringResource(suggestion.status.labelRes),
                                tone = suggestion.status.tone,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PriorityBadge(
                                    label = stringResource(suggestion.priority.labelRes),
                                    tone = suggestion.priority.tone,
                                )
                                SlaBadge(
                                    label = stringResource(
                                        if (suggestion.isOverdue) R.string.detail_sla_overdue else R.string.detail_sla_ontrack,
                                    ),
                                    isOverdue = suggestion.isOverdue,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = suggestion.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = suggestion.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                                text = "${stringResource(suggestion.category.labelRes)} • ${suggestion.location.addressLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "By ${suggestion.authorDisplayName} • ${formatExactDateTime(suggestion.createdAtMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textTertiary,
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SecondaryButton(
                            text = "${stringResource(R.string.action_upvote)} (${suggestion.upvoteCount})",
                            leadingIcon = SuggestIcons.Upvote,
                            onClick = viewModel::toggleUpvote,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Attached Citizen Media Gallery
            if (suggestion.mediaUrls.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(suggestion.mediaUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Citizen attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                        }
                    }
                }
            }

            // Official Response Card
            if (!suggestion.officialResponse.isNullOrBlank()) {
                item {
                    SuggestCard(containerColor = colors.primaryContainer) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = SuggestIcons.Government,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.detail_official_response),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = suggestion.officialResponse,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                        if (!suggestion.assignedOfficerName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${suggestion.assignedOfficerName} • ${suggestion.assignedDepartment.orEmpty()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }

            // Synopsis Feature: Before & After Verification Proof
            if (suggestion.afterPhotoUrl != null || suggestion.status == SuggestionStatus.RESOLVED) {
                item {
                    SuggestCard(containerColor = colors.statusResolvedBg) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = SuggestIcons.CheckCircle,
                                contentDescription = null,
                                tint = colors.statusResolved,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(R.string.detail_proof_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.statusResolved,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.detail_proof_verified),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            // Animated Status Timeline
            item {
                SuggestCard {
                    Text(
                        text = stringResource(R.string.detail_timeline),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    suggestion.timeline.forEachIndexed { index, event ->
                        TimelineRow(
                            event = event,
                            isLast = index == suggestion.timeline.lastIndex,
                        )
                    }
                }
            }

            // 1-5 Star Rating once Resolved
            if (suggestion.status == SuggestionStatus.RESOLVED) {
                item {
                    SuggestCard {
                        Text(
                            text = stringResource(R.string.detail_rate_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val existingRating = suggestion.rating
                        if (existingRating != null) {
                            Text(
                                text = stringResource(R.string.detail_rated_thanks, existingRating.stars),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.statusResolved,
                            )
                            if (existingRating.feedback.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = existingRating.feedback,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.detail_rate_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = {
                                            haptics.select()
                                            viewModel.selectStars(star)
                                        },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(
                                            imageVector = SuggestIcons.Star,
                                            contentDescription = "$star stars",
                                            tint = if (star <= state.selectedStars) {
                                                colors.statusInProgress
                                            } else {
                                                colors.surfaceSecondary
                                            },
                                            modifier = Modifier.size(30.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            SuggestTextField(
                                value = state.ratingFeedback,
                                onValueChange = viewModel::onRatingFeedbackChange,
                                placeholder = stringResource(R.string.detail_rate_feedback_hint),
                                singleLine = false,
                                minLines = 2,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PrimaryButton(
                                text = stringResource(R.string.detail_rate_submit),
                                isLoading = state.isSubmittingRating,
                                onClick = viewModel::submitRating,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // Community Comments Section
            item {
                SuggestCard {
                    Text(
                        text = stringResource(R.string.detail_comments, suggestion.comments.size),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (suggestion.comments.isEmpty()) {
                        Text(
                            text = "No comments yet. Share helpful observations below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textTertiary,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            suggestion.comments.forEach { comment ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (comment.isOfficial) {
                                                colors.primaryContainer
                                            } else {
                                                colors.surfaceSecondary.copy(alpha = 0.45f)
                                            },
                                        )
                                        .padding(10.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = comment.authorLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (comment.isOfficial) colors.primary else colors.textPrimary,
                                        )
                                        Text(
                                            text = formatRelativeTime(comment.createdAtMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textTertiary,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textPrimary,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SuggestTextField(
                            value = state.commentDraft,
                            onValueChange = viewModel::onCommentDraftChange,
                            placeholder = stringResource(R.string.detail_comment_hint),
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryButton(
                            text = stringResource(R.string.action_send),
                            isLoading = state.isSubmittingComment,
                            enabled = state.commentDraft.isNotBlank(),
                            onClick = viewModel::postComment,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(
    event: StatusUpdateEvent,
    isLast: Boolean,
) {
    val colors = SuggestTheme.colors
    val dotColor = when (event.status) {
        SuggestionStatus.SUBMITTED -> colors.statusSubmitted
        SuggestionStatus.UNDER_REVIEW -> colors.statusUnderReview
        SuggestionStatus.IN_PROGRESS -> colors.statusInProgress
        SuggestionStatus.RESOLVED -> colors.statusResolved
        SuggestionStatus.REJECTED -> colors.statusRejected
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .background(colors.separator),
                )
            }
        }
        Column(
            modifier = Modifier.padding(bottom = if (isLast) 0.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(event.status.labelRes),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = dotColor,
                )
                Text(
                    text = "• ${formatExactDateTime(event.timestampMillis)}",
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumeralStyle),
                    color = colors.textTertiary,
                )
            }
            Text(
                text = event.note,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textPrimary,
            )
            Text(
                text = event.actorName,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
    }
}
