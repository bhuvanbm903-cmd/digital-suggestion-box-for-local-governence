package com.suggestbox.ui.screens.official

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.DestructiveButton
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.PrimaryButton
import com.suggestbox.core.designsystem.components.SecondaryButton
import com.suggestbox.core.designsystem.components.SlaBadge
import com.suggestbox.core.designsystem.components.StatusBadge
import com.suggestbox.core.designsystem.components.SuggestBottomSheet
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.components.SuggestChip
import com.suggestbox.core.designsystem.components.SuggestTextField
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.domain.model.SuggestionPriority
import com.suggestbox.domain.model.SuggestionStatus

/**
 * Official ticket management screen allowing department assignment, SLA priority tags,
 * status transitions with public response, resolution proof verification, and duplicate merging.
 */
@Composable
fun OfficialDetailScreen(
    suggestionId: String,
    onBack: () -> Unit,
    viewModel: OfficialDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMsg = stringResource(R.string.official_action_saved)
    var showMergeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.actionSavedToast) {
        if (state.actionSavedToast) {
            snackbarHostState.showSnackbar(savedMsg)
            viewModel.consumeToast()
        }
    }

    val suggestion = state.suggestion

    LargeTitleScaffold(
        title = suggestion?.trackingId ?: stringResource(R.string.official_action_title),
        subtitle = stringResource(R.string.role_official),
        onBackClick = onBack,
        backContentDescription = stringResource(R.string.action_back),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (suggestion == null) return@LargeTitleScaffold

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
            // Citizen Summary
            item {
                SuggestCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusBadge(
                            label = stringResource(suggestion.status.labelRes),
                            tone = suggestion.status.tone,
                        )
                        SlaBadge(
                            label = stringResource(
                                if (suggestion.isOverdue) R.string.detail_sla_overdue else R.string.detail_sla_ontrack,
                            ),
                            isOverdue = suggestion.isOverdue,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = suggestion.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📍 ${suggestion.location.addressLabel} (${suggestion.upvoteCount} upvotes)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primary,
                    )
                }
            }

            // Assignment & Priority Section
            item {
                SuggestCard {
                    Text(
                        text = stringResource(R.string.official_assign_section),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.official_dept_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(viewModel.departments) { dept ->
                            SuggestChip(
                                label = dept,
                                selected = state.department == dept,
                                onClick = { viewModel.setDepartment(dept) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SuggestTextField(
                        value = state.officerName,
                        onValueChange = viewModel::setOfficerName,
                        label = stringResource(R.string.official_officer_label),
                        placeholder = "Officer Name",
                        leadingIcon = SuggestIcons.Profile,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.official_priority_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SuggestionPriority.entries) { prio ->
                            SuggestChip(
                                label = "${stringResource(prio.labelRes)} (${prio.slaHours}h)",
                                selected = state.priority == prio,
                                onClick = { viewModel.setPriority(prio) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    SecondaryButton(
                        text = stringResource(R.string.official_save_assignment),
                        onClick = viewModel::saveAssignment,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Status Update & Public Response Section
            item {
                SuggestCard {
                    Text(
                        text = stringResource(R.string.official_status_section),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SuggestionStatus.entries) { st ->
                            SuggestChip(
                                label = stringResource(st.labelRes),
                                selected = state.status == st,
                                onClick = { viewModel.setStatus(st) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SuggestTextField(
                        value = state.officialResponse,
                        onValueChange = viewModel::setOfficialResponse,
                        label = stringResource(R.string.official_response_label),
                        placeholder = stringResource(R.string.official_response_hint),
                        singleLine = false,
                        minLines = 3,
                    )
                    if (state.status == SuggestionStatus.RESOLVED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = state.attachProofChecked,
                                onCheckedChange = viewModel::setAttachProof,
                                colors = CheckboxDefaults.colors(checkedColor = colors.primary),
                            )
                            Text(
                                text = stringResource(R.string.official_proof_check),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PrimaryButton(
                        text = stringResource(R.string.official_update_status_btn),
                        leadingIcon = SuggestIcons.CheckCircle,
                        onClick = viewModel::updateStatusAndResponse,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Duplicate Merge Action
            item {
                DestructiveButton(
                    text = stringResource(R.string.official_merge_btn),
                    onClick = { showMergeSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showMergeSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        SuggestBottomSheet(
            onDismissRequest = { showMergeSheet = false },
            title = stringResource(R.string.official_merge_title),
        ) {
            Text(
                text = stringResource(R.string.official_merge_desc),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (state.otherActiveTrackingIds.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.otherActiveTrackingIds) { code ->
                        SuggestChip(
                            label = code,
                            selected = state.mergeTrackingId == code,
                            onClick = { viewModel.setMergeTrackingId(code) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            SuggestTextField(
                value = state.mergeTrackingId,
                onValueChange = viewModel::setMergeTrackingId,
                placeholder = stringResource(R.string.official_merge_hint),
            )
            Spacer(modifier = Modifier.height(14.dp))
            PrimaryButton(
                text = stringResource(R.string.official_merge_btn),
                enabled = state.mergeTrackingId.isNotBlank(),
                onClick = {
                    viewModel.mergeAsDuplicate()
                    showMergeSheet = false
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
