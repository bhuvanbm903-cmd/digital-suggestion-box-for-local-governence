package com.suggestbox.ui.screens.official

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.SecondaryButton
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.core.designsystem.theme.TabularNumeralStyle
import com.suggestbox.domain.model.SubmissionKind
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.WardInfo
import com.suggestbox.domain.repository.SuggestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SuperAdminUiState(
    val wardScores: List<Pair<WardInfo, Int>> = emptyList(),
    val topIdeas: List<Suggestion> = emptyList(),
)

@HiltViewModel
class SuperAdminViewModel @Inject constructor(
    suggestionRepository: SuggestionRepository,
) : ViewModel() {

    val uiState: StateFlow<SuperAdminUiState> = combine(
        suggestionRepository.observeWards(),
        suggestionRepository.observeSuggestions(),
    ) { wards, suggestions ->
        val scores = wards.mapIndexed { idx, ward ->
            ward to (94 - idx * 7).coerceAtLeast(68)
        }
        val ideas = suggestions
            .filter { it.kind == SubmissionKind.IDEA }
            .sortedByDescending { it.upvoteCount }
        SuperAdminUiState(wardScores = scores, topIdeas = ideas)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SuperAdminUiState(),
    )
}

/**
 * Super Admin / Municipal Commissioner executive overview featuring ward SLA leaderboards,
 * top citizen-upvoted participatory budget ideas, and web admin portal stub.
 */
@Composable
fun SuperAdminScreen(
    onBack: () -> Unit,
    onOpenOfficialDashboard: () -> Unit,
    viewModel: SuperAdminViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    val anim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    LargeTitleScaffold(
        title = stringResource(R.string.super_admin_title),
        subtitle = stringResource(R.string.super_admin_subtitle),
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
                        text = stringResource(R.string.super_admin_ward_perf),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.wardScores.forEach { (ward, score) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = ward.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        text = "$score%",
                                        style = MaterialTheme.typography.labelLarge.merge(TabularNumeralStyle),
                                        color = colors.statusResolved,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.surfaceSecondary),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((score / 100f) * anim.value)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(colors.statusResolved),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.topIdeas.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.super_admin_top_ideas),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                }
                items(state.topIdeas, key = { it.id }) { idea ->
                    SuggestCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = idea.trackingId,
                                style = MaterialTheme.typography.labelMedium.merge(TabularNumeralStyle),
                                color = colors.primary,
                            )
                            Text(
                                text = "${idea.upvoteCount} upvotes",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.statusUnderReview,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = idea.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            item {
                SuggestCard(containerColor = colors.primaryContainer) {
                    Text(
                        text = stringResource(R.string.super_admin_web_stub),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.super_admin_web_stub_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(
                        text = stringResource(R.string.official_dashboard_title),
                        leadingIcon = SuggestIcons.Dashboard,
                        onClick = onOpenOfficialDashboard,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
