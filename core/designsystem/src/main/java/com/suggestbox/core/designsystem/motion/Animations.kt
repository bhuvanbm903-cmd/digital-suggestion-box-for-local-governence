package com.suggestbox.core.designsystem.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.suggestbox.core.designsystem.theme.SuggestTheme

/**
 * Standard iOS-inspired spring specs for 60fps responsive motion without excessive bounce.
 */
object SuggestMotion {
    val SnappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    val GentleSpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow,
    )

    const val TRANSITION_DURATION_MS = 320

    fun slideEnter(): EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS / 2))

    fun slideExit(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -(fullWidth * 0.28f).toInt() },
        animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))

    fun slidePopEnter(): EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> -(fullWidth * 0.28f).toInt() },
        animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))

    fun slidePopExit(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
}

/**
 * Applies a subtle iOS-like tactile press scale animation (0.97f when pressed).
 *
 * @param enabled Whether the press interaction should animate.
 * @param pressedScale Target scale when pressed down.
 */
fun Modifier.pressScaleEffect(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = SuggestMotion.SnappySpring,
        label = "pressScale",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
            )
        }
}

/**
 * Applies an animated skeleton shimmer gradient background for loading placeholders.
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val colors = SuggestTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            colors.shimmerBase,
            colors.shimmerHighlight,
            colors.shimmerBase,
        ),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 300f),
    )

    return this.background(brush = brush)
}
