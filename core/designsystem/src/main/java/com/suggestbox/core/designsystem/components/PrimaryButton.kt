package com.suggestbox.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.suggestbox.core.designsystem.motion.pressScaleEffect
import com.suggestbox.core.designsystem.motion.rememberSuggestHaptics
import com.suggestbox.core.designsystem.theme.SuggestBoxTheme
import com.suggestbox.core.designsystem.theme.SuggestTheme

/**
 * Primary pill/rounded action button following Apple HIG proportions with tactile press scale,
 * minimum 50dp height (exceeding 48dp accessibility target), and built-in loading state.
 *
 * @param text Button label text.
 * @param onClick Callback invoked on tap (with haptic feedback).
 * @param modifier Optional modifier.
 * @param enabled Controls interactive state.
 * @param isLoading Shows an inline progress indicator and prevents duplicate taps.
 * @param leadingIcon Optional leading SF-style rounded icon.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val colors = SuggestTheme.colors
    val haptics = rememberSuggestHaptics()

    Button(
        onClick = {
            if (!isLoading) {
                haptics.tap()
                onClick()
            }
        },
        enabled = enabled && !isLoading,
        modifier = modifier
            .heightIn(min = 50.dp)
            .pressScaleEffect(enabled = enabled && !isLoading),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.primary.copy(alpha = 0.4f),
            disabledContentColor = colors.onPrimary.copy(alpha = 0.8f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
    ) {
        ButtonContent(
            text = text,
            isLoading = isLoading,
            leadingIcon = leadingIcon,
            indicatorColor = colors.onPrimary,
        )
    }
}

/**
 * Calm secondary button styled with subtle translucent government blue container fill.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val colors = SuggestTheme.colors
    val haptics = rememberSuggestHaptics()

    Button(
        onClick = {
            if (!isLoading) {
                haptics.tap()
                onClick()
            }
        },
        enabled = enabled && !isLoading,
        modifier = modifier
            .heightIn(min = 50.dp)
            .pressScaleEffect(enabled = enabled && !isLoading),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.primary,
            disabledContainerColor = colors.surfaceSecondary,
            disabledContentColor = colors.textTertiary,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
    ) {
        ButtonContent(
            text = text,
            isLoading = isLoading,
            leadingIcon = leadingIcon,
            indicatorColor = colors.primary,
        )
    }
}

/**
 * Destructive action button (e.g., Delete Account, Reject) with WCAG AA crimson accent.
 */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = SuggestTheme.colors
    val haptics = rememberSuggestHaptics()

    OutlinedButton(
        onClick = {
            haptics.reject()
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 50.dp)
            .pressScaleEffect(enabled = enabled),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.destructive.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.destructiveBg.copy(alpha = 0.6f),
            contentColor = colors.destructive,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ButtonContent(
            text = text,
            isLoading = false,
            leadingIcon = leadingIcon,
            indicatorColor = colors.destructive,
        )
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: ImageVector?,
    indicatorColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.2.dp,
                color = indicatorColor,
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ButtonsPreview() {
    SuggestBoxTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.width(320.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(text = "Submit Suggestion", onClick = {}, modifier = Modifier.fillMaxWidth())
            SecondaryButton(text = "Save Offline Draft", onClick = {}, modifier = Modifier.fillMaxWidth())
            DestructiveButton(text = "Delete My Data", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
