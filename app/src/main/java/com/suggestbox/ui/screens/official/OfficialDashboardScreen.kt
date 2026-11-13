package com.suggestbox.ui.screens.official

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.ActionTileCard
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.SecondaryButton
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.core.designsystem.theme.TabularNumeralStyle
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.domain.model.SuggestionStatus

/**
 * Ward/Municipal Official Dashboard featuring animated status ring & category bar charts,
 * SLA overdue KPIs, triage inbox launch, ward announcement broadcast, and CSV/PDF report export.
 */
@Composable
fun OfficialDashboardScreen(
    onBack: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenBroadcast: () -> Unit,
    viewModel: OfficialDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val chartProgress = remember { Animatable(0f) }
    LaunchedEffect(state.analytics.totalCount) {
        chartProgress.snapTo(0f)
        chartProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        )
    }

    LaunchedEffect(state.exportedUri, state.exportMessage) {
        val uri = state.exportedUri
        val fileName = state.exportMessage
        if (uri != null && fileName != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = state.exportedMimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                context.startActivity(Intent.createChooser(shareIntent, fileName))
            }
            snackbarHostState.showSnackbar(context.getString(R.string.official_export_done, fileName))
            viewModel.consumeExportEvent()
        }
    }

    LargeTitleScaffold(
        title = stringResource(R.string.official_dashboard_title),
        subtitle = stringResource(R.string.role_official),
        onBackClick = onBack,
        backContentDescription = stringResource(R.string.action_back),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
            // Top KPI Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    KpiStatCard(
                        value = state.analytics.totalCount.toString(),
                        label = stringResource(R.string.official_kpi_total),
                        accent = colors.primary,
                        modifier = Modifier.weight(1f),
                    )
                    KpiStatCard(
                        value = state.analytics.overdueCount.toString(),
                        label = stringResource(R.string.official_kpi_overdue),
                        accent = colors.destructive,
                        modifier = Modifier.weight(1f),
                    )
                    KpiStatCard(
                        value = "${state.analytics.averageResolutionHours.toInt()}h",
                        label = stringResource(R.string.official_kpi_avg_time),
                        accent = colors.statusResolved,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Quick Official Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    ActionTileCard(
                        title = stringResource(R.string.official_open_inbox),
                        subtitle = "${state.analytics.totalCount} active items",
                        icon = SuggestIcons.Inbox,
                        accentColor = colors.primary,
                        onClick = onOpenInbox,
                        modifier = Modifier.weight(1f),
                    )
                    ActionTileCard(
                        title = stringResource(R.string.official_broadcast_btn),
                        subtitle = stringResource(R.string.ward_default_eyebrow),
                        icon = SuggestIcons.Announcement,
                        accentColor = colors.statusInProgress,
                        onClick = onOpenBroadcast,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Animated Status Breakdown Ring Chart
            item {
                val statusColors = mapOf(
                    SuggestionStatus.SUBMITTED to colors.statusSubmitted,
                    SuggestionStatus.UNDER_REVIEW to colors.statusUnderReview,
                    SuggestionStatus.IN_PROGRESS to colors.statusInProgress,
                    SuggestionStatus.RESOLVED to colors.statusResolved,
                    SuggestionStatus.REJECTED to colors.statusRejected,
                )

                SuggestCard {
                    Text(
                        text = stringResource(R.string.official_status_chart),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        val total = state.analytics.totalCount.coerceAtLeast(1)
                        Canvas(modifier = Modifier.size(116.dp)) {
                            var startAngle = -90f
                            SuggestionStatus.entries.forEach { status ->
                                val count = state.analytics.countByStatus[status] ?: 0
                                val sweep = (count.toFloat() / total) * 360f * chartProgress.value
                                if (sweep > 0f) {
                                    drawArc(
                                        color = statusColors[status] ?: Color.Gray,
                                        startAngle = startAngle,
                                        sweepAngle = (sweep - 3f).coerceAtLeast(1f),
                                        useCenter = false,
                                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                                    )
                                    startAngle += sweep
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SuggestionStatus.entries.forEach { status ->
                                val count = state.analytics.countByStatus[status] ?: 0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(statusColors[status] ?: colors.primary),
                                        )
                                        Text(
                                            text = stringResource(status.labelRes),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                        )
                                    }
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelLarge.merge(TabularNumeralStyle),
                                        color = colors.textPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Animated Category Breakdown Bar Chart
            item {
                SuggestCard {
                    Text(
                        text = stringResource(R.string.official_category_chart),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val maxCatCount = (state.analytics.countByCategory.values.maxOrNull() ?: 1).coerceAtLeast(1)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SuggestionCategory.entries
                            .filter { (state.analytics.countByCategory[it] ?: 0) > 0 }
                            .forEach { category ->
                                val count = state.analytics.countByCategory[category] ?: 0
                                val fraction = (count.toFloat() / maxCatCount) * chartProgress.value
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = stringResource(category.labelRes),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textPrimary,
                                        )
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.labelMedium.merge(TabularNumeralStyle),
                                            color = colors.primary,
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
                                                .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(colors.primary),
                                        )
                                    }
                                }
                            }
                    }
                }
            }

            // Export Reports Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    SecondaryButton(
                        text = stringResource(R.string.official_export_csv),
                        leadingIcon = SuggestIcons.Export,
                        onClick = viewModel::exportCsv,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.official_export_pdf),
                        leadingIcon = SuggestIcons.Export,
                        onClick = viewModel::exportPdf,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val colors = SuggestTheme.colors
    SuggestCard(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.merge(TabularNumeralStyle).copy(
                fontWeight = FontWeight.Bold,
            ),
            color = accent,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
    }
}
