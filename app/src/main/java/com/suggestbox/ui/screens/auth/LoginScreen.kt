package com.suggestbox.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.components.PrimaryButton
import com.suggestbox.core.designsystem.components.SecondaryButton
import com.suggestbox.core.designsystem.components.SuggestCard
import com.suggestbox.core.designsystem.components.SuggestTextField
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestTheme
import com.suggestbox.domain.usecase.ValidationError

/**
 * Citizen authentication screen supporting Mobile Number (+91) & Password Authentication,
 * Password visibility toggle, registration with confirmation, duplicate checks,
 * Real-time Email OTP verification, and Anonymous citizen session fallback.
 */
@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SuggestTheme.colors
    val spacing = SuggestTheme.spacing
    val focusRequester = remember { FocusRequester() }
    var authModeTab by remember { mutableIntStateOf(0) } // 0: Mobile + Password, 1: Email OTP

    LaunchedEffect(state.authenticated) {
        if (state.authenticated) {
            onAuthenticated()
        }
    }

    LaunchedEffect(state.verificationId) {
        if (state.verificationId != null) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val emailError = when (state.error) {
        ValidationError.INVALID_EMAIL -> stringResource(R.string.err_invalid_email)
        else -> null
    }

    val otpError = when (state.error) {
        ValidationError.INVALID_OTP -> stringResource(R.string.err_invalid_otp)
        ValidationError.CODE_EXPIRED -> stringResource(R.string.err_code_expired)
        ValidationError.MAX_ATTEMPTS_EXCEEDED -> stringResource(R.string.err_max_attempts)
        else -> null
    }

    LargeTitleScaffold(
        title = stringResource(R.string.login_title),
        subtitle = stringResource(R.string.app_name),
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
                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
            }

            // Auth Method Tabs: Mobile + Password vs Email OTP
            item {
                TabRow(
                    selectedTabIndex = authModeTab,
                    containerColor = colors.surface,
                    contentColor = colors.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[authModeTab]),
                            color = colors.primary,
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                ) {
                    Tab(
                        selected = authModeTab == 0,
                        onClick = { authModeTab = 0 },
                        text = {
                            Text(
                                text = stringResource(R.string.login_phone_password_tab),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (authModeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                ),
                            )
                        },
                    )
                    Tab(
                        selected = authModeTab == 1,
                        onClick = { authModeTab = 1 },
                        text = {
                            Text(
                                text = stringResource(R.string.login_email_otp_tab),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (authModeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                ),
                            )
                        },
                    )
                }
            }

            // TAB 0: Mobile Number (+91) & Password Authentication (Primary)
            if (authModeTab == 0) {
                item {
                    SuggestCard {
                        if (state.isRegisterMode) {
                            // Register Mode Form
                            Text(
                                text = stringResource(R.string.login_register_btn),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            SuggestTextField(
                                value = state.fullName,
                                onValueChange = viewModel::onFullNameChange,
                                label = stringResource(R.string.login_fullname_label),
                                placeholder = stringResource(R.string.login_fullname_placeholder),
                                leadingIcon = SuggestIcons.Profile,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            SuggestTextField(
                                value = state.mobileNumber,
                                onValueChange = viewModel::onMobileNumberChange,
                                label = "${stringResource(R.string.login_phone_label)} (+91)",
                                placeholder = "10-digit mobile number",
                                leadingIcon = SuggestIcons.Location,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            SuggestTextField(
                                value = state.password,
                                onValueChange = viewModel::onPasswordChange,
                                label = stringResource(R.string.login_password_label),
                                placeholder = stringResource(R.string.login_password_placeholder),
                                leadingIcon = SuggestIcons.Lock,
                                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingContent = {
                                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                                        Icon(
                                            imageVector = if (state.isPasswordVisible) SuggestIcons.Privacy else SuggestIcons.Anonymous,
                                            contentDescription = "Toggle password visibility",
                                            tint = colors.textSecondary,
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            SuggestTextField(
                                value = state.confirmPassword,
                                onValueChange = viewModel::onConfirmPasswordChange,
                                label = stringResource(R.string.login_confirm_password_label),
                                placeholder = stringResource(R.string.login_confirm_password_placeholder),
                                leadingIcon = SuggestIcons.Lock,
                                visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingContent = {
                                    IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                                        Icon(
                                            imageVector = if (state.isConfirmPasswordVisible) SuggestIcons.Privacy else SuggestIcons.Anonymous,
                                            contentDescription = "Toggle confirm password visibility",
                                            tint = colors.textSecondary,
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            )

                            if (!state.serverError.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.serverError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.destructive,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            PrimaryButton(
                                text = stringResource(R.string.login_register_btn),
                                isLoading = state.isLoading,
                                onClick = viewModel::registerWithMobilePassword,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = { viewModel.setRegisterMode(false) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = stringResource(R.string.login_switch_to_login),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.primary,
                                )
                            }
                        } else {
                            // Login Mode Form
                            Text(
                                text = stringResource(R.string.login_signin_btn),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            SuggestTextField(
                                value = state.mobileNumber,
                                onValueChange = viewModel::onMobileNumberChange,
                                label = "${stringResource(R.string.login_phone_label)} (+91)",
                                placeholder = "10-digit mobile number",
                                leadingIcon = SuggestIcons.Location,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            SuggestTextField(
                                value = state.password,
                                onValueChange = viewModel::onPasswordChange,
                                label = stringResource(R.string.login_password_label),
                                placeholder = stringResource(R.string.login_password_placeholder),
                                leadingIcon = SuggestIcons.Lock,
                                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingContent = {
                                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                                        Icon(
                                            imageVector = if (state.isPasswordVisible) SuggestIcons.Privacy else SuggestIcons.Anonymous,
                                            contentDescription = "Toggle password visibility",
                                            tint = colors.textSecondary,
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            )

                            if (!state.serverError.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.serverError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.destructive,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            PrimaryButton(
                                text = stringResource(R.string.login_signin_btn),
                                isLoading = state.isLoading,
                                onClick = viewModel::signInWithMobilePassword,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = { viewModel.setRegisterMode(true) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = stringResource(R.string.login_switch_to_register),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.primary,
                                )
                            }
                        }
                    }
                }
            }

            // TAB 1: Real-Time Delivered Email OTP Authentication (Secondary)
            if (authModeTab == 1) {
                if (state.deliveredOtp != null) {
                    item {
                        val code = state.deliveredOtp?.code.orEmpty()
                        SuggestCard(
                            containerColor = colors.primaryContainer,
                            contentPadding = PaddingValues(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = SuggestIcons.Email,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.login_otp_sms_delivered, state.email),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary,
                                    )
                                    Text(
                                        text = "OTP: $code",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 2.sp,
                                        ),
                                        color = colors.textPrimary,
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.autoFillAndVerify(code) },
                                ) {
                                    Text(
                                        text = stringResource(R.string.login_otp_autofill_btn, code),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = colors.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SuggestCard {
                        SuggestTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            label = stringResource(R.string.login_email_label),
                            placeholder = stringResource(R.string.login_email_placeholder),
                            leadingIcon = SuggestIcons.Email,
                            errorMessage = emailError,
                            enabled = state.verificationId == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        AnimatedVisibility(
                            visible = state.verificationId != null,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.login_otp_label),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        text = stringResource(R.string.login_attempts_remaining, state.attemptsRemaining),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.attemptsRemaining <= 2) colors.destructive else colors.textTertiary,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // 6-Cell Segmented PIN Input
                                OtpSegmentedInput(
                                    otp = state.otpCode,
                                    onOtpChange = viewModel::onOtpChange,
                                    focusRequester = focusRequester,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = stringResource(R.string.login_code_expiry_notice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textTertiary,
                                    )
                                }

                                if (otpError != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = otpError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.destructive,
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TextButton(onClick = viewModel::changeEmail) {
                                        Text(
                                            text = stringResource(R.string.login_change_email),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.primary,
                                        )
                                    }

                                    if (state.resendCountdown > 0) {
                                        Text(
                                            text = stringResource(R.string.login_resend_countdown, state.resendCountdown),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.textTertiary,
                                        )
                                    } else {
                                        TextButton(onClick = viewModel::resendOtp) {
                                            Text(
                                                text = stringResource(R.string.login_resend_button),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colors.primary,
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        if (!state.serverError.isNullOrBlank() && otpError == null) {
                            Text(
                                text = state.serverError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.destructive,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (state.verificationId == null) {
                            PrimaryButton(
                                text = stringResource(R.string.login_send_otp),
                                isLoading = state.isLoading,
                                onClick = viewModel::requestOtp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            PrimaryButton(
                                text = stringResource(R.string.login_verify_otp),
                                isLoading = state.isLoading,
                                enabled = state.otpCode.length == 6,
                                onClick = viewModel::verifyOtp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // Divider & Anonymous session option
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = colors.separator)
                    Text(
                        text = stringResource(R.string.login_or_divider),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textTertiary,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = colors.separator)
                }
            }

            item {
                SuggestCard {
                    Text(
                        text = stringResource(R.string.login_anon_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(
                        text = stringResource(R.string.login_anon_cta),
                        onClick = viewModel::continueAnonymously,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * High-clarity 6-cell segmented PIN input box with automatic cursor positioning.
 */
@Composable
private fun OtpSegmentedInput(
    otp: String,
    onOtpChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = SuggestTheme.colors

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = otp,
            onValueChange = onOtpChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            keyboardActions = KeyboardActions.Default,
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focusRequester),
            decorationBox = { /* Hidden, rendered via visual cells below */ },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (i in 0 until 6) {
                val digit = otp.getOrNull(i)?.toString().orEmpty()
                val isFocused = otp.length == i || (otp.length == 6 && i == 5)

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) colors.primary else colors.separator,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { focusRequester.requestFocus() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = digit,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
