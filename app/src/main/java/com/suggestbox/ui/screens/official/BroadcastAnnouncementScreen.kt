package com.suggestbox.ui.screens.official

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.AnimatedSuccessCheckmark
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.PrimaryButton
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.components.SuggestChip
import com.suggestbox.core.designsystem.components.SuggestTextField
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme

/**
 * Allows ward officers and municipal heads to publish verified civic announcements
 * directly onto citizens' Home banner.
 */
@Composable
fun BroadcastAnnouncementScreen(
    onBack: () -> Unit,
    viewModel: BroadcastAnnouncementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing

    if (state.publishedSuccess) {
        LargeTitleScaffold(
            title = stringResource(R.string.broadcast_title),
            onBackClick = onBack,
            backContentDescription = stringResource(R.string.action_back),
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.broadcast_published),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                PrimaryButton(
                    text = stringResource(R.string.action_done),
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }

    LargeTitleScaffold(
        title = stringResource(R.string.broadcast_title),
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
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item {
                SuggestCard {
                    Text(
                        text = stringResource(R.string.broadcast_ward_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.wards, key = { it.id }) { ward ->
                            SuggestChip(
                                label = ward.name,
                                selected = state.selectedWardId == ward.id,
                                onClick = { viewModel.selectWard(ward.id) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.broadcast_dept_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(viewModel.departments) { dept ->
                            SuggestChip(
                                label = dept,
                                selected = state.department == dept,
                                onClick = { viewModel.selectDepartment(dept) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SuggestTextField(
                        value = state.headline,
                        onValueChange = viewModel::onHeadlineChange,
                        label = stringResource(R.string.broadcast_headline_label),
                        placeholder = stringResource(R.string.broadcast_headline_hint),
                        leadingIcon = SuggestIcons.Announcement,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SuggestTextField(
                        value = state.body,
                        onValueChange = viewModel::onBodyChange,
                        label = stringResource(R.string.broadcast_body_label),
                        placeholder = stringResource(R.string.broadcast_body_hint),
                        singleLine = false,
                        minLines = 4,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = stringResource(R.string.broadcast_publish_btn),
                        leadingIcon = SuggestIcons.Announcement,
                        isLoading = state.isPublishing,
                        enabled = state.headline.isNotBlank() && state.body.isNotBlank(),
                        onClick = viewModel::publish,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
