package com.suggestbox.ui.screens.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.ActionTileCard
import com.suggestbox.core.designsystem.components.GroupedDivider
import com.suggestbox.core.designsystem.components.GroupedNavigationRow
import com.suggestbox.core.designsystem.components.GroupedSection
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.SkeletonCardPlaceholder
import com.suggestbox.core.designsystem.components.SuggestBottomSheet
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.domain.model.Announcement
import com.suggestbox.domain.model.UserRole
import com.suggestbox.ui.components.SuggestionFeedCard
import com.suggestbox.ui.components.formatRelativeTime

/**
 * Citizen Home tab featuring Ward header, official announcements banner, quick-action tiles,
 * and trending suggestions with shared-element navigation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateSubmit: () -> Unit,
    onNavigateTrack: () -> Unit,
    onNavigateCommunity: () -> Unit,
    onNavigateDetail: (String) -> Unit,
    onNavigateOfficial: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    var showWardSheet by remember { mutableStateOf(false) }

    val wardTitle = state.activeWard?.name ?: stringResource(R.string.ward_default_eyebrow)
    val municipalitySubtitle = state.activeWard?.municipalityName ?: stringResource(R.string.app_subtitle)

    LargeTitleScaffold(
        title = stringResource(R.string.app_name),
        subtitle = wardTitle,
        actions = {
            IconButton(
                onClick = { showWardSheet = true },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = SuggestIcons.Location,
                    contentDescription = stringResource(R.string.home_select_ward),
                    tint = colors.primary,
                )
            }
        },
        bottomBar = bottomBar,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.md,
                end = spacing.md,
                top = innerPadding.calculateTopPadding() + spacing.xs,
                bottom = innerPadding.calculateBottomPadding() + spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            // Ward Context Pill
            item {
                SuggestCard(
                    onClick = { showWardSheet = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = SuggestIcons.Government,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = wardTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = municipalitySubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Icon(
                            imageVector = SuggestIcons.ChevronRight,
                            contentDescription = stringResource(R.string.home_select_ward),
                            tint = colors.textTertiary,
                        )
                    }
                }
            }

            // Announcements Banner Carousel
            if (state.announcements.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.home_announcements),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.announcements, key = { it.id }) { announcement ->
                                AnnouncementBannerCard(
                                    announcement = announcement,
                                    modifier = Modifier.width(300.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = stringResource(R.string.section_quick_actions),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        ActionTileCard(
                            title = stringResource(R.string.quick_new_suggestion),
                            subtitle = stringResource(R.string.quick_new_suggestion_sub),
                            icon = SuggestIcons.NewSuggestion,
                            accentColor = colors.primary,
                            onClick = onNavigateSubmit,
                            modifier = Modifier.weight(1f),
                        )
                        ActionTileCard(
                            title = stringResource(R.string.quick_track_issue),
                            subtitle = stringResource(R.string.quick_track_issue_sub),
                            icon = SuggestIcons.Track,
                            accentColor = colors.statusInProgress,
                            onClick = onNavigateTrack,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        ActionTileCard(
                            title = stringResource(R.string.quick_nearby_issues),
                            subtitle = stringResource(R.string.quick_nearby_issues_sub),
                            icon = SuggestIcons.Nearby,
                            accentColor = colors.statusResolved,
                            onClick = onNavigateCommunity,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.activeRole != UserRole.CITIZEN) {
                            ActionTileCard(
                                title = stringResource(R.string.quick_official_portal),
                                subtitle = stringResource(R.string.quick_official_portal_sub),
                                icon = SuggestIcons.Dashboard,
                                accentColor = colors.statusUnderReview,
                                onClick = onNavigateOfficial,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            ActionTileCard(
                                title = stringResource(R.string.kind_idea_short),
                                subtitle = stringResource(R.string.home_trending),
                                icon = SuggestIcons.Idea,
                                accentColor = colors.statusUnderReview,
                                onClick = onNavigateSubmit,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Trending Suggestions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_trending),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    TextButton(onClick = onNavigateCommunity) {
                        Text(
                            text = stringResource(R.string.home_view_all),
                            color = colors.primary,
                        )
                    }
                }
            }

            if (state.isLoading) {
                items(2) { SkeletonCardPlaceholder() }
            } else {
                items(state.trendingSuggestions, key = { it.id }) { suggestion ->
                    with(sharedTransitionScope) {
                        SuggestionFeedCard(
                            suggestion = suggestion,
                            onCardClick = { onNavigateDetail(suggestion.id) },
                            onUpvoteClick = { viewModel.toggleUpvote(suggestion.id) },
                            modifier = Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "card-${suggestion.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                            ),
                        )
                    }
                }
            }
        }
    }

    if (showWardSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        SuggestBottomSheet(
            onDismissRequest = { showWardSheet = false },
            title = stringResource(R.string.home_select_ward),
        ) {
            GroupedSection {
                state.availableWards.forEachIndexed { index, ward ->
                    if (index > 0) GroupedDivider(hasLeadingIcon = false)
                    GroupedNavigationRow(
                        title = ward.name,
                        subtitle = ward.municipalityName,
                        trailingValue = if (ward.id == state.activeWard?.id) "✓" else null,
                        onClick = {
                            viewModel.selectWard(ward.id)
                            showWardSheet = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnouncementBannerCard(
    announcement: Announcement,
    modifier: Modifier = Modifier,
) {
    val colors = SuggestTheme.colors
    SuggestCard(
        modifier = modifier,
        containerColor = colors.primaryContainer,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = SuggestIcons.Announcement,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = announcement.department,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatRelativeTime(announcement.publishedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = announcement.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = announcement.body,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
