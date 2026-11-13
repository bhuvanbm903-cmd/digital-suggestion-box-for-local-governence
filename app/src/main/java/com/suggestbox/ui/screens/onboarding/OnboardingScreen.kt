package com.suggestbox.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.GroupedDivider
import com.suggestbox.core.designsystem.components.GroupedSection
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.PrimaryButton
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.components.SuggestChip
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.motion.SuggestMotion
import com.suggestbox.core.designsystem.motion.rememberSuggestHaptics
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.domain.model.LanguageOption
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
)

/**
 * 3-screen animated iOS-style onboarding flow with inline language switcher (EN / HI / KN).
 */
@Composable
fun OnboardingScreen(
    selectedLanguage: LanguageOption,
    onContinueToConsent: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    val scope = rememberCoroutineScope()

    val pages = remember {
        listOf(
            OnboardingPageData(SuggestIcons.Government, R.string.onb_title_1, R.string.onb_desc_1),
            OnboardingPageData(SuggestIcons.Location, R.string.onb_title_2, R.string.onb_desc_2),
            OnboardingPageData(SuggestIcons.Privacy, R.string.onb_title_3, R.string.onb_desc_3),
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.primary,
            )
            if (pagerState.currentPage < pages.lastIndex) {
                TextButton(onClick = onContinueToConsent) {
                    Text(
                        text = stringResource(R.string.action_skip),
                        color = colors.textSecondary,
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(52.dp),
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(page.titleRes),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(page.descRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Animated page indicator pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                    ),
                    label = "dotWidth",
                )
                val dotColor by animateColorAsState(
                    targetValue = if (isSelected) colors.primary else colors.separator,
                    label = "dotColor",
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Language picker card
        SuggestCard(contentPadding = PaddingValues(14.dp)) {
            Text(
                text = stringResource(R.string.onb_choose_lang),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LanguageOption.entries.forEach { option ->
                    SuggestChip(
                        label = stringResource(option.labelRes),
                        selected = option == selectedLanguage,
                        onClick = { viewModel.selectLanguage(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            text = if (pagerState.currentPage < pages.lastIndex) {
                stringResource(R.string.action_next)
            } else {
                stringResource(R.string.onb_get_started)
            },
            onClick = {
                if (pagerState.currentPage < pages.lastIndex) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onContinueToConsent()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * India DPDP Act 2023 compliant privacy notice & explicit consent gate.
 */
@Composable
fun DpdpConsentScreen(
    onConsentAccepted: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    val haptics = rememberSuggestHaptics()
    var consentChecked by remember { mutableStateOf(true) }

    LargeTitleScaffold(
        title = stringResource(R.string.dpdp_title),
        subtitle = stringResource(R.string.dpdp_subtitle),
        onBackClick = onBack,
        backContentDescription = stringResource(R.string.action_back),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.md,
                end = spacing.md,
                top = innerPadding.calculateTopPadding() + spacing.sm,
                bottom = innerPadding.calculateBottomPadding() + spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item {
                GroupedSection {
                    ConsentRow(
                        icon = SuggestIcons.Lock,
                        title = stringResource(R.string.dpdp_point_1_title),
                        description = stringResource(R.string.dpdp_point_1_desc),
                    )
                    GroupedDivider()
                    ConsentRow(
                        icon = SuggestIcons.Anonymous,
                        title = stringResource(R.string.dpdp_point_2_title),
                        description = stringResource(R.string.dpdp_point_2_desc),
                    )
                    GroupedDivider()
                    ConsentRow(
                        icon = SuggestIcons.Delete,
                        title = stringResource(R.string.dpdp_point_3_title),
                        description = stringResource(R.string.dpdp_point_3_desc),
                    )
                }
            }

            item {
                SuggestCard(
                    onClick = {
                        haptics.select()
                        consentChecked = !consentChecked
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = consentChecked,
                            onCheckedChange = {
                                haptics.select()
                                consentChecked = it
                            },
                            colors = CheckboxDefaults.colors(checkedColor = colors.primary),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dpdp_checkbox_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            item {
                PrimaryButton(
                    text = stringResource(R.string.dpdp_accept_continue),
                    enabled = consentChecked,
                    onClick = {
                        viewModel.grantDpdpConsentAndFinish()
                        onConsentAccepted()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ConsentRow(
    icon: ImageVector,
    title: String,
    description: String,
) {
    val colors = SuggestTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}
