package com.shakeflash.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shakeflash.app.ui.theme.SFColors

/**
 * Pill switch with a spring-sliding thumb. Bigger than the Material switch on purpose:
 * it is the main "enable background service" control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SFSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackWidth: Dp = 66.dp,
    trackHeight: Dp = 38.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val thumbTravel = trackWidth - trackHeight
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) thumbTravel else 0.dp,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 480f),
        label = "switchThumb",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) SFColors.Accent else SFColors.SurfaceHighlight,
        label = "switchTrack",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) SFColors.Accent.copy(alpha = 0.55f) else SFColors.BorderStrong,
        label = "switchBorder",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) SFColors.Background else SFColors.Muted,
        label = "switchThumbColor",
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(trackColor)
            .border(1.dp, borderColor, RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false, radius = trackHeight, color = Color.White.copy(alpha = 0.25f)),
                enabled = enabled,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .height(trackHeight - 6.dp)
                .width(trackHeight - 6.dp)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}
