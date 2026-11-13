package com.suggestbox.ui.screens.track

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.EmptyState
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.PrimaryButton
import com.suggestbox.core.designsystem.components.SecondaryButton
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.components.SuggestTextField
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.ui.components.SuggestionFeedCard
import com.suggestbox.ui.components.formatRelativeTime

/**
 * Citizen Track tab allowing Tracking ID lookup, inspecting submitted tickets,
 * and resuming or syncing offline Room drafts.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TrackScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onOpenSuggestion: (String) -> Unit,
    onResumeDraft: (String) -> Unit,
    onCreateNew: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: TrackViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing

    LaunchedEffect(state.foundSuggestionId) {
        val targetId = state.foundSuggestionId
        if (targetId != null) {
            viewModel.consumeFoundSuggestion()
            onOpenSuggestion(targetId)
        }
    }

    LargeTitleScaffold(
        title = stringResource(R.string.tab_track),
        subtitle = stringResource(R.string.ward_default_eyebrow),
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
            item {
                SuggestCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SuggestTextField(
                            value = state.lookupQuery,
                            onValueChange = viewModel::onLookupQueryChange,
                            label = stringResource(R.string.track_lookup_label),
                            placeholder = stringResource(R.string.track_lookup_hint),
                            leadingIcon = SuggestIcons.Search,
                            errorMessage = if (state.lookupNotFound) stringResource(R.string.track_not_found) else null,
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryButton(
                            text = stringResource(R.string.track_lookup_btn),
                            onClick = viewModel::findByTrackingId,
                        )
                    }
                }
            }

            if (state.offlineDrafts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.track_offline_drafts, state.offlineDrafts.size),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                        if (state.offlineDrafts.any { it.pendingSync }) {
                            TextButton(onClick = viewModel::syncDraftsNow) {
                                Text(text = stringResource(R.string.action_sync_now), color = colors.primary)
                            }
                        }
                    }
                }

                items(state.offlineDrafts, key = { it.localId }) { draft ->
                    SuggestCard {
                        Text(
                            text = draft.title.ifBlank { stringResource(draft.category.labelRes) },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(draft.category.labelRes)} • ${formatRelativeTime(draft.updatedAtMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryButton(
                                text = stringResource(R.string.action_resume_draft),
                                onClick = { onResumeDraft(draft.localId) },
                            )
                            TextButton(onClick = { viewModel.deleteDraft(draft.localId) }) {
                                Text(text = stringResource(R.string.action_cancel), color = colors.destructive)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.track_my_submissions),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                )
            }

            if (state.mySubmissions.isEmpty()) {
                item {
                    EmptyState(
                        icon = SuggestIcons.Track,
                        title = stringResource(R.string.track_empty_title),
                        description = stringResource(R.string.track_empty_desc),
                        actionLabel = stringResource(R.string.quick_new_suggestion),
                        onActionClick = onCreateNew,
                    )
                }
            } else {
                items(state.mySubmissions, key = { it.id }) { suggestion ->
                    with(sharedTransitionScope) {
                        SuggestionFeedCard(
                            suggestion = suggestion,
                            onCardClick = { onOpenSuggestion(suggestion.id) },
                            onUpvoteClick = { viewModel.toggleUpvote(suggestion.id) },
                            showSlaBadge = true,
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
}
