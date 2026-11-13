package com.suggestbox.ui.screens.official

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.EmptyState
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.SegmentedControl
import com.suggestbox.core.designsystem.components.SuggestChip
import com.suggestbox.core.designsystem.components.SuggestSearchBar
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.ui.components.SuggestionFeedCard
import com.suggestbox.ui.components.icon

/**
 * Official triage inbox prioritizing overdue SLA and high-urgency citizen submissions.
 */
@Composable
fun OfficialInboxScreen(
    onBack: () -> Unit,
    onOpenOfficialDetail: (String) -> Unit,
    viewModel: OfficialInboxViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = SuggestTheme.spacing

    LargeTitleScaffold(
        title = stringResource(R.string.inbox_title),
        subtitle = stringResource(R.string.role_official),
        onBackClick = onBack,
        backContentDescription = stringResource(R.string.action_back),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.md,
                end = spacing.md,
                top = innerPadding.calculateTopPadding() + spacing.xs,
                bottom = innerPadding.calculateBottomPadding() + spacing.xl,
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
                        stringResource(R.string.inbox_seg_all),
                        stringResource(R.string.inbox_seg_overdue),
                        stringResource(R.string.inbox_seg_unassigned),
                        stringResource(R.string.inbox_seg_resolved),
                    ),
                    selectedIndex = state.segment.ordinal,
                    onOptionSelected = viewModel::onSegmentChange,
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SuggestionCategory.entries) { cat ->
                        SuggestChip(
                            label = stringResource(cat.labelRes),
                            selected = state.selectedCategory == cat,
                            leadingIcon = cat.icon(),
                            onClick = { viewModel.toggleCategory(cat) },
                        )
                    }
                }
            }

            if (state.items.isEmpty()) {
                item {
                    EmptyState(
                        icon = SuggestIcons.Inbox,
                        title = stringResource(R.string.inbox_empty_title),
                        description = stringResource(R.string.inbox_empty_desc),
                    )
                }
            } else {
                items(state.items, key = { it.id }) { suggestion ->
                    SuggestionFeedCard(
                        suggestion = suggestion,
                        onCardClick = { onOpenOfficialDetail(suggestion.id) },
                        onUpvoteClick = { onOpenOfficialDetail(suggestion.id) },
                        showSlaBadge = true,
                    )
                }
            }
        }
    }
}
