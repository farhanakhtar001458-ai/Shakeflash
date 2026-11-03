package com.shakeflash.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shakeflash.app.ui.theme.SFColors

enum class SFButtonVariant { Primary, Secondary, Outline, Ghost, Accent }

private data class ButtonSkin(
    val background: Brush,
    val content: Color,
    val border: Color,
    val sheen: Color,
)

private fun skinFor(variant: SFButtonVariant): ButtonSkin = when (variant) {
    SFButtonVariant.Primary -> ButtonSkin(
        background = Brush.verticalGradient(listOf(SFColors.Foreground, Color(0xFFE4E4E7))),
        content = SFColors.OnPrimary,
        border = Color.Transparent,
        sheen = Color.Black.copy(alpha = 0.10f),
    )

    SFButtonVariant.Secondary -> ButtonSkin(
        background = Brush.verticalGradient(listOf(SFColors.SurfaceHighlight, SFColors.SurfaceElevated)),
        content = SFColors.Foreground,
        border = SFColors.Border,
        sheen = Color.White.copy(alpha = 0.12f),
    )

    SFButtonVariant.Outline -> ButtonSkin(
        background = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
        content = SFColors.Foreground,
        border = SFColors.BorderStrong,
        sheen = Color.White.copy(alpha = 0.10f),
    )

    SFButtonVariant.Ghost -> ButtonSkin(
        background = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
        content = SFColors.Muted,
        border = Color.Transparent,
        sheen = Color.White.copy(alpha = 0.08f),
    )

    SFButtonVariant.Accent -> ButtonSkin(
        background = Brush.verticalGradient(listOf(SFColors.AccentLight, SFColors.Accent)),
        content = Color(0xFF1A1305),
        border = Color.Transparent,
        sheen = Color.White.copy(alpha = 0.25f),
    )
}

/**
 * shadcn-style button with the two effects this project leans on:
 *  - a spring "press-in" scale, and
 *  - a light sheen that *slides* across the face while pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SFButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: SFButtonVariant = SFButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "buttonPress",
    )
    val sheenProgress = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            sheenProgress.snapTo(0f)
            sheenProgress.animateTo(1f, animationSpec = tween(460, easing = LinearEasing))
        } else {
            sheenProgress.snapTo(0f)
        }
    }

    val skin = skinFor(variant)
    val shape = RoundedCornerShape(13.dp)
    val alpha = if (enabled) 1f else 0.45f

    Row(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .clip(shape)
            .background(skin.background)
            .border(1.dp, skin.border, shape)
            .drawWithContent {
                drawContent()
                val progress = sheenProgress.value
                if (progress > 0f && progress < 1f) {
                    val travel = size.width * 2.4f
                    val x = -size.width + progress * travel
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, skin.sheen, Color.Transparent),
                            start = Offset(x - size.width * 0.4f, 0f),
                            end = Offset(x + size.width * 0.4f, size.height),
                        ),
                    )
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = skin.content.copy(alpha = 0.18f)),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = skin.content.copy(alpha = alpha),
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = skin.content.copy(alpha = alpha),
        )
    }
}

/** Square, borderless icon button used by the top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SFIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background by androidx.compose.animation.animateColorAsState(
        targetValue = if (pressed) SFColors.SurfaceHighlight else SFColors.Surface,
        label = "iconButtonBackground",
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 42.dp, minHeight = 42.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, SFColors.Border, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false, radius = 21.dp, color = Color.White.copy(alpha = 0.2f)),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = SFColors.Foreground,
            modifier = Modifier.size(19.dp),
        )
    }
}
