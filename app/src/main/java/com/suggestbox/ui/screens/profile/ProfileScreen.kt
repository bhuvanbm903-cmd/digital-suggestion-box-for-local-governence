package com.suggestbox.ui.screens.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.DestructiveButton
import com.suggestbox.core.designsystem.components.GroupedDivider
import com.suggestbox.core.designsystem.components.GroupedNavigationRow
import com.suggestbox.core.designsystem.components.GroupedSection
import com.suggestbox.core.designsystem.components.GroupedToggleRow
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.SecondaryButton
import com.suggestbox.core.designsystem.components.SegmentedControl
import com.suggestbox.core.designsystem.components.SuggestBottomSheet
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.domain.model.AppPreferences
import com.suggestbox.domain.model.LanguageOption
import com.suggestbox.domain.model.UserRole

private enum class ProfileSheetType { LANGUAGE, PRIVACY, HELP, ABOUT }

/**
 * iOS Settings-inspired Profile & Preferences screen supporting live localization,
 * dark mode, push notification toggle, DPDP privacy/erasure, FAQ, and role previews.
 */
@Composable
fun ProfileScreen(
    preferences: AppPreferences,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSelectLanguage: (LanguageOption) -> Unit,
    onSelectRole: (UserRole) -> Unit,
    onOpenOfficialConsole: () -> Unit,
    onOpenSuperAdmin: () -> Unit,
    onLoggedOut: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    var activeSheet by remember { mutableStateOf<ProfileSheetType?>(null) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showLinkEmailDialog by remember { mutableStateOf(false) }
    var linkEmailInput by remember { mutableStateOf("") }

    if (showLinkEmailDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLinkEmailDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.email_migration_link_action),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.email_migration_banner_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    com.suggestbox.core.designsystem.components.SuggestTextField(
                        value = linkEmailInput,
                        onValueChange = { linkEmailInput = it },
                        label = stringResource(R.string.login_email_label),
                        placeholder = stringResource(R.string.login_email_placeholder),
                        leadingIcon = SuggestIcons.Email,
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (linkEmailInput.isNotBlank()) {
                            viewModel.linkVerifiedEmail(linkEmailInput.trim()) {
                                showLinkEmailDialog = false
                            }
                        }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.email_migration_link_action),
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showLinkEmailDialog = false }) {
                    Text(
                        text = stringResource(R.string.logout_dialog_cancel),
                        color = colors.textSecondary,
                    )
                }
            },
            containerColor = colors.surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        )
    }

    if (showLogoutConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.logout_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.logout_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.signOut(onLoggedOut)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.logout_dialog_confirm),
                        color = colors.destructive,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text(
                        text = stringResource(R.string.logout_dialog_cancel),
                        color = colors.textPrimary,
                    )
                }
            },
            containerColor = colors.surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        )
    }

    LargeTitleScaffold(
        title = stringResource(R.string.tab_profile),
        subtitle = stringResource(preferences.previewRole.labelRes),
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
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            // Identity Header Card
            item {
                SuggestCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (user?.isAnonymous == true) SuggestIcons.Anonymous else SuggestIcons.Profile,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = user?.displayName ?: "Citizen Participant",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                            )
                            Text(
                                text = user?.wardName ?: stringResource(R.string.ward_default_eyebrow),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                            if (!user?.email.isNullOrBlank()) {
                                Text(
                                    text = user?.email.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.primary,
                                )
                            } else if (!user?.phoneNumber.isNullOrBlank()) {
                                Text(
                                    text = user?.phoneNumber.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.primary,
                                )
                            }
                        }
                    }
                }
            }

            // Legacy Phone-Only Account Migration Banner
            if (preferences.needsEmailMigration) {
                item {
                    SuggestCard(
                        containerColor = colors.warningBg,
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = SuggestIcons.Email,
                                    contentDescription = null,
                                    tint = colors.warning,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = stringResource(R.string.email_migration_banner_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text(
                                text = stringResource(R.string.email_migration_banner_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                androidx.compose.material3.TextButton(
                                    onClick = viewModel::dismissEmailMigration,
                                ) {
                                    Text(
                                        text = stringResource(R.string.email_migration_dismiss),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textSecondary,
                                    )
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                                com.suggestbox.core.designsystem.components.PrimaryButton(
                                    text = stringResource(R.string.email_migration_link_action),
                                    onClick = { showLinkEmailDialog = true },
                                )
                            }
                        }
                    }
                }
            }

            // Role Switcher for Governance Demo
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.setting_role_mode).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SegmentedControl(
                        options = listOf("Citizen", "Official", "Super Admin"),
                        selectedIndex = preferences.previewRole.ordinal,
                        onOptionSelected = { index -> onSelectRole(UserRole.entries[index]) },
                    )
                    if (preferences.previewRole == UserRole.OFFICIAL) {
                        SecondaryButton(
                            text = stringResource(R.string.route_official),
                            leadingIcon = SuggestIcons.Dashboard,
                            onClick = onOpenOfficialConsole,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else if (preferences.previewRole == UserRole.SUPER_ADMIN) {
                        SecondaryButton(
                            text = stringResource(R.string.role_super_admin),
                            leadingIcon = SuggestIcons.Admin,
                            onClick = onOpenSuperAdmin,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Appearance & Notifications
            item {
                GroupedSection(title = stringResource(R.string.section_settings_appearance)) {
                    GroupedToggleRow(
                        title = stringResource(R.string.setting_dark_mode),
                        subtitle = stringResource(R.string.setting_dark_mode_sub),
                        checked = preferences.darkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        icon = SuggestIcons.DarkMode,
                        iconTint = colors.statusUnderReview,
                    )
                    GroupedDivider()
                    GroupedToggleRow(
                        title = stringResource(R.string.setting_notifications),
                        subtitle = stringResource(R.string.setting_notifications_sub),
                        checked = preferences.notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled,
                        icon = SuggestIcons.Notifications,
                        iconTint = colors.statusInProgress,
                    )
                    GroupedDivider()
                    GroupedNavigationRow(
                        title = stringResource(R.string.setting_language),
                        trailingValue = stringResource(preferences.language.labelRes),
                        icon = SuggestIcons.Language,
                        iconTint = colors.primary,
                        onClick = { activeSheet = ProfileSheetType.LANGUAGE },
                    )
                }
            }

            // Governance, DPDP Privacy & Help
            item {
                GroupedSection(title = stringResource(R.string.section_governance_tools)) {
                    GroupedNavigationRow(
                        title = stringResource(R.string.setting_privacy),
                        icon = SuggestIcons.Privacy,
                        iconTint = colors.statusResolved,
                        onClick = { activeSheet = ProfileSheetType.PRIVACY },
                    )
                    GroupedDivider()
                    GroupedNavigationRow(
                        title = stringResource(R.string.setting_help_faq),
                        icon = SuggestIcons.Help,
                        onClick = { activeSheet = ProfileSheetType.HELP },
                    )
                    GroupedDivider()
                    GroupedNavigationRow(
                        title = stringResource(R.string.setting_about),
                        icon = SuggestIcons.Info,
                        onClick = { activeSheet = ProfileSheetType.ABOUT },
                    )
                    GroupedDivider()
                    GroupedNavigationRow(
                        title = stringResource(R.string.setting_logout),
                        icon = SuggestIcons.Logout,
                        isDestructive = true,
                        onClick = { showLogoutConfirmDialog = true },
                    )
                }
            }
        }
    }

    when (activeSheet) {
        ProfileSheetType.LANGUAGE -> {
            @OptIn(ExperimentalMaterial3Api::class)
            SuggestBottomSheet(
                onDismissRequest = { activeSheet = null },
                title = stringResource(R.string.setting_language),
            ) {
                GroupedSection {
                    LanguageOption.entries.forEachIndexed { index, option ->
                        if (index > 0) GroupedDivider(hasLeadingIcon = false)
                        GroupedNavigationRow(
                            title = stringResource(option.labelRes),
                            trailingValue = if (option == preferences.language) "✓" else null,
                            onClick = {
                                onSelectLanguage(option)
                                activeSheet = null
                            },
                        )
                    }
                }
            }
        }

        ProfileSheetType.PRIVACY -> {
            @OptIn(ExperimentalMaterial3Api::class)
            SuggestBottomSheet(
                onDismissRequest = { activeSheet = null },
                title = stringResource(R.string.setting_privacy),
            ) {
                Text(
                    text = stringResource(R.string.delete_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                DestructiveButton(
                    text = stringResource(R.string.setting_delete_account),
                    leadingIcon = SuggestIcons.Delete,
                    onClick = {
                        activeSheet = null
                        viewModel.deleteAccountAndData(onLoggedOut)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        ProfileSheetType.HELP -> {
            @OptIn(ExperimentalMaterial3Api::class)
            SuggestBottomSheet(
                onDismissRequest = { activeSheet = null },
                title = stringResource(R.string.setting_help_faq),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FaqItem(
                        q = "How long do officials have to respond?",
                        a = "Urgent safety tickets carry a 24-hour SLA. Standard civic improvements are reviewed in weekly Ward Committee sittings.",
                    )
                    FaqItem(
                        q = "Does Anonymous Mode hide my identity?",
                        a = "Yes. Anonymous submissions omit your name and phone number from all public feeds and fellow citizen views.",
                    )
                    FaqItem(
                        q = "What happens if I submit without internet?",
                        a = "Your draft is securely stored in the local Room database and can be resumed or synced from the Track tab.",
                    )
                }
            }
        }

        ProfileSheetType.ABOUT -> {
            @OptIn(ExperimentalMaterial3Api::class)
            SuggestBottomSheet(
                onDismissRequest = { activeSheet = null },
                title = stringResource(R.string.setting_about),
            ) {
                Text(
                    text = "${stringResource(R.string.app_name)} v1.0.0\n${stringResource(R.string.app_subtitle)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Built with Clean Architecture, Jetpack Compose, Room offline storage, and DPDP Act 2023 privacy-by-design safeguards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }

        null -> Unit
    }
}

@Composable
private fun FaqItem(q: String, a: String) {
    val colors = SuggestTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = q,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
        Text(
            text = a,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}
