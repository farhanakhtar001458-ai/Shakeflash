package com.shakeflash.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shakeflash.app.ui.icons.SFIcons
import com.shakeflash.app.ui.theme.SFColors

/** shadcn Card: hairline border, 22dp radius, subtle top-lit gradient. */
@Composable
fun SFCard(
    modifier: Modifier = Modifier,
    borderColor: Color = SFColors.Border,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF141417), Color(0xFF0E0E10)),
                ),
            )
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .padding(contentPadding),
        content = content,
    )
}

/** Small rounded-square icon chip used at the start of card headers. */
@Composable
fun SFIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = SFColors.Foreground,
    background: Color = SFColors.SurfaceHighlight,
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .border(1.dp, SFColors.Border, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SFCardHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = SFColors.Foreground,
    iconBackground: Color = SFColors.SurfaceHighlight,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SFIconBadge(icon = icon, tint = iconTint, background = iconBackground)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = SFColors.Foreground,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SFColors.Subtle,
                )
            }
        }
        trailing?.invoke()
    }
}

enum class ChipTone { Accent, Success, Danger, Neutral }

/** Tiny status pill: "ON", "OFF", "Granted", "Action needed". */
@Composable
fun SFStatusChip(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
) {
    val contentColor = when (tone) {
        ChipTone.Accent -> SFColors.Accent
        ChipTone.Success -> SFColors.Success
        ChipTone.Danger -> SFColors.Danger
        ChipTone.Neutral -> SFColors.Muted
    }
    val backgroundColor = when (tone) {
        ChipTone.Accent -> SFColors.AccentSoft
        ChipTone.Success -> SFColors.SuccessSoft
        ChipTone.Danger -> SFColors.DangerSoft
        ChipTone.Neutral -> SFColors.SurfaceHighlight
    }
    val animatedContent by animateColorAsState(contentColor, label = "chipContent")
    val animatedBackground by animateColorAsState(backgroundColor, label = "chipBackground")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(animatedBackground)
            .border(1.dp, animatedContent.copy(alpha = 0.28f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(animatedContent),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = animatedContent,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SFDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SFColors.Border),
    )
}

/** Settings-style row: icon, title/subtitle, optional trailing content. */
@Composable
fun SFSettingRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SFIconBadge(
            icon = icon,
            tint = SFColors.Muted,
            background = SFColors.SurfaceHighlight,
            modifier = Modifier.size(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = SFColors.Foreground,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SFColors.Subtle,
                )
            }
        }
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(4.dp))
                trailing()
            }
        } else if (onClick != null) {
            Icon(
                imageVector = SFIcons.ChevronRight,
                contentDescription = null,
                tint = SFColors.Subtle,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
