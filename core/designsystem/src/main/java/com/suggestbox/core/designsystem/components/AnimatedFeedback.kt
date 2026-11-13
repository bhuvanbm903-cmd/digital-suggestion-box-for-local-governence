package com.suggestbox.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suggestbox.core.designsystem.motion.rememberSuggestHaptics
import com.suggestbox.core.designsystem.motion.shimmerEffect
import com.suggestbox.core.designsystem.theme.SuggestTheme

/**
 * Smooth iOS-style animated circular checkmark rendered on suggestion submission confirmation.
 */
@Composable
fun AnimatedSuccessCheckmark(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    tint: Color = SuggestTheme.colors.success,
) {
    val haptics = rememberSuggestHaptics()
    val circleProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        haptics.confirm()
        circleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        )
        checkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 4.dp.toPx()

            // Soft fill halo
            drawCircle(
                color = tint.copy(alpha = 0.12f * circleProgress.value),
                radius = (this.size.minDimension / 2f) - strokeWidth,
            )

            // Outer ring sweep
            drawArc(
                color = tint,
                startAngle = -90f,
                sweepAngle = 360f * circleProgress.value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Checkmark path
            if (checkProgress.value > 0f) {
                val p1 = Offset(this.size.width * 0.28f, this.size.height * 0.52f)
                val p2 = Offset(this.size.width * 0.44f, this.size.height * 0.67f)
                val p3 = Offset(this.size.width * 0.73f, this.size.height * 0.36f)

                val firstSegProgress = (checkProgress.value * 2f).coerceIn(0f, 1f)
                val secondSegProgress = ((checkProgress.value - 0.5f) * 2f).coerceIn(0f, 1f)

                val midEnd = Offset(
                    x = p1.x + (p2.x - p1.x) * firstSegProgress,
                    y = p1.y + (p2.y - p1.y) * firstSegProgress,
                )
                drawLine(
                    color = tint,
                    start = p1,
                    end = midEnd,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )

                if (secondSegProgress > 0f) {
                    val finalEnd = Offset(
                        x = p2.x + (p3.x - p2.x) * secondSegProgress,
                        y = p2.y + (p3.y - p2.y) * secondSegProgress,
                    )
                    drawLine(
                        color = tint,
                        start = p2,
                        end = finalEnd,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

/**
 * Shimmer skeleton placeholder card displayed while data is loading.
 */
@Composable
fun SkeletonCardPlaceholder(modifier: Modifier = Modifier) {
    SuggestCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .shimmerEffect(),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect(),
            )
        }
    }
}
