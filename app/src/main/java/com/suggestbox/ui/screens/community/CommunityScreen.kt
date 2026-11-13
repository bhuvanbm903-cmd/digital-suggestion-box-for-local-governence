package com.suggestbox.ui.screens.community

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.EmptyState
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.PrimaryButton
import com.suggestbox.core.designsystem.components.SegmentedControl
import com.suggestbox.core.designsystem.components.SkeletonCardPlaceholder
import com.suggestbox.core.designsystem.components.SuggestBottomSheet
import com.suggestbox.core.designsystem.components.SuggestChip
import com.suggestbox.core.designsystem.components.SuggestSearchBar
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.domain.model.SubmissionKind
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.domain.model.SuggestionStatus
import com.suggestbox.ui.components.SuggestionFeedCard
import com.suggestbox.ui.components.icon

/**
 * Public Community Feed with search, Trending/Recent/Nearby sort, category chips,
 * and a bottom sheet for Status, Distance, and Fix-It/Build-It filters.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CommunityScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onSuggestionClick: (String) -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    var showFiltersSheet by remember { mutableStateOf(false) }

    LargeTitleScaffold(
        title = stringResource(R.string.tab_community),
        subtitle = stringResource(R.string.ward_default_eyebrow),
        actions = {
            IconButton(
                onClick = { showFiltersSheet = true },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = SuggestIcons.Filter,
                    contentDescription = "Filters",
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
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                SuggestSearchBar(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.community_search_hint),
                    clearContentDescription = stringResource(R.string.action_clear_search),
                )
            }

            item {
                SegmentedControl(
                    options = listOf(
                        stringResource(R.string.community_seg_trending),
                        stringResource(R.string.community_seg_recent),
                        stringResource(R.string.community_seg_nearby),
                    ),
                    selectedIndex = state.segment.ordinal,
                    onOptionSelected = viewModel::onSegmentChange,
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SuggestionCategory.entries) { category ->
                        SuggestChip(
                            label = stringResource(category.labelRes),
                            selected = state.selectedCategory == category,
                            leadingIcon = category.icon(),
                            onClick = { viewModel.toggleCategory(category) },
                        )
                    }
                }
            }

            if (state.isLoading) {
                items(3) { SkeletonCardPlaceholder() }
            } else if (state.items.isEmpty()) {
                item {
                    EmptyState(
                        icon = SuggestIcons.Community,
                        title = stringResource(R.string.community_empty_title),
                        description = stringResource(R.string.community_empty_desc),
                        actionLabel = stringResource(R.string.action_clear_search),
                        onActionClick = viewModel::clearAllFilters,
                    )
                }
            } else {
                items(state.items, key = { it.id }) { suggestion ->
                    with(sharedTransitionScope) {
                        SuggestionFeedCard(
                            suggestion = suggestion,
                            onCardClick = { onSuggestionClick(suggestion.id) },
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

    if (showFiltersSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        SuggestBottomSheet(
            onDismissRequest = { showFiltersSheet = false },
            title = "Filter Suggestions",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Submission Type",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestChip(
                        label = stringResource(R.string.community_all),
                        selected = state.selectedKind == null,
                        onClick = { viewModel.setKindFilter(null) },
                    )
                    SubmissionKind.entries.forEach { kind ->
                        SuggestChip(
                            label = stringResource(kind.labelRes),
                            selected = state.selectedKind == kind,
                            onClick = { viewModel.setKindFilter(kind) },
                        )
                    }
                }

                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        SuggestChip(
                            label = stringResource(R.string.community_all),
                            selected = state.selectedStatus == null,
                            onClick = { viewModel.setStatusFilter(null) },
                        )
                    }
                    items(SuggestionStatus.entries) { status ->
                        SuggestChip(
                            label = stringResource(status.labelRes),
                            selected = state.selectedStatus == status,
                            onClick = { viewModel.setStatusFilter(status) },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.community_filter_distance),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestChip(
                        label = stringResource(R.string.community_dist_all),
                        selected = state.distanceFilter == DistanceFilter.ALL,
                        onClick = { viewModel.setDistanceFilter(DistanceFilter.ALL) },
                    )
                    SuggestChip(
                        label = stringResource(R.string.community_dist_500m),
                        selected = state.distanceFilter == DistanceFilter.WITHIN_500M,
                        onClick = { viewModel.setDistanceFilter(DistanceFilter.WITHIN_500M) },
                    )
                    SuggestChip(
                        label = stringResource(R.string.community_dist_2km),
                        selected = state.distanceFilter == DistanceFilter.WITHIN_2KM,
                        onClick = { viewModel.setDistanceFilter(DistanceFilter.WITHIN_2KM) },
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                PrimaryButton(
                    text = stringResource(R.string.action_done),
                    onClick = { showFiltersSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
