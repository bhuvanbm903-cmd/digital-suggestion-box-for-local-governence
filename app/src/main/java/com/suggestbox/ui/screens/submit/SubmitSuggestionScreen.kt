package com.suggestbox.ui.screens.submit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.AnimatedSuccessCheckmark
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.PrimaryButton
import com.suggestbox.core.designsystem.components.SecondaryButton
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.motion.SuggestMotion
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.core.designsystem.theme.TabularNumeralStyle

/**
 * 5-step iOS-inspired citizen submission wizard with offline Room draft support,
 * voice-to-text, media compression, 150m duplicate alert, GPS pinning, and animated receipt.
 */
@Composable
fun SubmitSuggestionScreen(
    onBack: () -> Unit,
    onTrackSubmitted: (String) -> Unit,
    viewModel: SubmitSuggestionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }
    val draftSavedMsg = stringResource(R.string.submit_draft_saved)

    LaunchedEffect(state.draftSavedToast) {
        if (state.draftSavedToast) {
            snackbarHostState.showSnackbar(draftSavedMsg)
            viewModel.consumeDraftSavedFlag()
        }
    }

    val submitted = state.submittedSuggestion
    if (submitted != null) {
        SubmissionSuccessView(
            trackingId = submitted.trackingId,
            onTrackClick = { onTrackSubmitted(submitted.id) },
            onDoneClick = onBack,
        )
        return
    }

    val progress by animateFloatAsState(
        targetValue = state.step / state.totalSteps.toFloat(),
        animationSpec = SuggestMotion.SnappySpring,
        label = "submitProgress",
    )

    LargeTitleScaffold(
        title = stringResource(R.string.submit_title),
        subtitle = stringResource(R.string.submit_step_counter, state.step, state.totalSteps),
        onBackClick = {
            if (state.step > 1) viewModel.previousStep() else onBack()
        },
        backContentDescription = stringResource(R.string.action_back),
        actions = {
            TextButton(onClick = { viewModel.saveOfflineDraft() }) {
                Text(
                    text = stringResource(R.string.action_save_draft),
                    color = colors.primary,
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                ),
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = colors.primary,
                trackColor = colors.surfaceSecondary,
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                item {
                    AnimatedContent(
                        targetState = state.step,
                        label = "submitStepContent",
                    ) { step ->
                        when (step) {
                            1 -> SubmitStepCategoryView(
                                state = state,
                                onKindChange = viewModel::setKind,
                                onCategoryChange = viewModel::setCategory,
                            )
                            2 -> SubmitStepDetailsView(
                                state = state,
                                onTitleChange = viewModel::onTitleChange,
                                onDescriptionChange = viewModel::onDescriptionChange,
                                onVoiceResult = viewModel::appendVoiceTranscript,
                                onUpvoteDuplicate = viewModel::upvoteExistingDuplicate,
                            )
                            3 -> SubmitStepMediaView(
                                state = state,
                                onAddUris = viewModel::addMediaUris,
                                onRemoveUri = viewModel::removeMedia,
                            )
                            4 -> SubmitStepLocationView(
                                state = state,
                                onCoordinatesChange = viewModel::updateCoordinates,
                                onAddressChange = viewModel::updateAddressLabel,
                            )
                            else -> SubmitStepReviewView(
                                state = state,
                                onAnonymousChange = viewModel::setAnonymous,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.step > 1) {
                    SecondaryButton(
                        text = stringResource(R.string.action_back),
                        onClick = viewModel::previousStep,
                        modifier = Modifier.weight(0.4f),
                    )
                }
                if (state.step < state.totalSteps) {
                    PrimaryButton(
                        text = stringResource(R.string.action_next),
                        onClick = viewModel::nextStep,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.action_submit),
                        isLoading = state.isSubmitting,
                        onClick = viewModel::submit,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun SubmissionSuccessView(
    trackingId: String,
    onTrackClick: () -> Unit,
    onDoneClick: () -> Unit,
) {
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    val clipboard = LocalClipboardManager.current

    LargeTitleScaffold(
        title = stringResource(R.string.submit_success_title),
        subtitle = stringResource(R.string.app_name),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedSuccessCheckmark()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.submit_success_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.submit_success_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            SuggestCard(
                containerColor = colors.primaryContainer,
                onClick = { clipboard.setText(AnnotatedString(trackingId)) },
            ) {
                Text(
                    text = stringResource(R.string.submit_tracking_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trackingId,
                    style = MaterialTheme.typography.headlineSmall.merge(TabularNumeralStyle).copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.primary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = stringResource(R.string.submit_view_timeline),
                leadingIcon = SuggestIcons.Track,
                onClick = onTrackClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            SecondaryButton(
                text = stringResource(R.string.action_done),
                onClick = onDoneClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
