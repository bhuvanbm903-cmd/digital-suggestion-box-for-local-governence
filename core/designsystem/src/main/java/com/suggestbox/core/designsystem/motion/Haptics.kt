package com.suggestbox.core.designsystem.motion

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * Tactile haptic wrapper tuned for crisp iOS-like confirmation and selection feedback.
 */
class SuggestHaptics(
    private val haptic: HapticFeedback,
    private val view: View,
) {
    fun tap() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun select() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun confirm() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun reject() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberSuggestHaptics(): SuggestHaptics {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(haptic, view) { SuggestHaptics(haptic, view) }
}
