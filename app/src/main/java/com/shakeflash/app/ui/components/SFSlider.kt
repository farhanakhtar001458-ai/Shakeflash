package com.shakeflash.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.shakeflash.app.ui.theme.SFColors

/**
 * Custom slider (no Material Slider under the hood) so the track, thumb and fill can be
 * styled like shadcn — and so the thumb "slides" with a spring while dragging.
 *
 * @param value current position, 0f..1f
 */
@Composable
fun SFSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = SFColors.Accent,
    enabled: Boolean = true,
) {
    val fraction = value.coerceIn(0f, 1f)
    val haptics = LocalHapticFeedback.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 900f),
        label = "sliderFraction",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    currentOnValueChange((down.position.x / size.width).coerceIn(0f, 1f))
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    drag(down.id) { change ->
                        currentOnValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumbSize = 22.dp
        val travel: androidx.compose.ui.unit.Dp = (maxWidth - thumbSize).coerceAtLeast(0.dp)

        // Inactive track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(SFColors.SurfaceHighlight)
                .border(1.dp, SFColors.Border, RoundedCornerShape(percent = 50)),
        )
        // Active track
        Box(
            modifier = Modifier
                .width(maxWidth * animatedFraction)
                .height(8.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.45f), accent),
                    ),
                ),
        )
        // Thumb
        Box(
            modifier = Modifier
                .offset(x = travel * animatedFraction)
                .size(thumbSize)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(accent)
                .border(width = 3.dp, color = SFColors.Background, shape = CircleShape),
        )
    }
}
