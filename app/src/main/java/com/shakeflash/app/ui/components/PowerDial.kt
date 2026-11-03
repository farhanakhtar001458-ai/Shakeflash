package com.shakeflash.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shakeflash.app.ui.icons.SFIcons
import com.shakeflash.app.ui.theme.SFColors

/**
 * The big ON/OFF control: a circular dial with expanding "light" rings while the torch is
 * on, a spring press-in effect and a tap ripple.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerDial(
    isOn: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val dialScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "dialScale",
    )
    val glow by animateFloatAsState(
        targetValue = if (isOn) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "dialGlow",
    )
    val pulse = rememberInfiniteTransition(label = "dialPulse")
    val pulsePhase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dialPulsePhase",
    )

    val ringColor = if (isOn) SFColors.Accent else SFColors.Muted

    Box(
        modifier = modifier
            .size(226.dp)
            .scale(dialScale),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val middle = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            if (glow > 0.01f) {
                repeat(2) { index ->
                    val phase = (pulsePhase + index * 0.5f) % 1f
                    drawCircle(
                        color = SFColors.Accent.copy(alpha = 0.20f * glow * (1f - phase)),
                        radius = maxRadius * (0.52f + 0.46f * phase),
                        center = middle,
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SFColors.Accent.copy(alpha = 0.16f * glow), Color.Transparent),
                        center = middle,
                        radius = maxRadius * 0.92f,
                    ),
                    radius = maxRadius * 0.92f,
                    center = middle,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(166.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(SFColors.SurfaceHighlight, Color(0xFF0C0C0E)),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = if (isOn) SFColors.Accent.copy(alpha = 0.6f) else SFColors.Border,
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(
                        bounded = false,
                        radius = 83.dp,
                        color = if (isOn) SFColors.Accent else Color.White,
                    ),
                    enabled = enabled,
                    onClick = onToggle,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = SFIcons.Flashlight,
                    contentDescription = null,
                    tint = if (isOn) SFColors.Accent else SFColors.Subtle,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (isOn) "ON" else "OFF",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isOn) SFColors.Foreground else SFColors.Muted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (enabled) "tap to toggle" else "unavailable",
                    style = MaterialTheme.typography.labelSmall,
                    color = SFColors.Subtle,
                )
            }
            // Amber edge highlight when on.
            if (glow > 0.5f) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .border(1.dp, ringColor.copy(alpha = 0.10f * glow), CircleShape),
                )
            }
        }
    }
}
