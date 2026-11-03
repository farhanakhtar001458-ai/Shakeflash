package com.shakeflash.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shakeflash.app.BuildConfig
import com.shakeflash.app.permissions.PermissionManager
import com.shakeflash.app.ui.ShakeFlashViewModel
import com.shakeflash.app.ui.components.ChipTone
import com.shakeflash.app.ui.components.PowerDial
import com.shakeflash.app.ui.components.SFButton
import com.shakeflash.app.ui.components.SFButtonVariant
import com.shakeflash.app.ui.components.SFCard
import com.shakeflash.app.ui.components.SFCardHeader
import com.shakeflash.app.ui.components.SFDivider
import com.shakeflash.app.ui.components.SFIconBadge
import com.shakeflash.app.ui.components.SFIconButton
import com.shakeflash.app.ui.components.SFSlider
import com.shakeflash.app.ui.components.SFStatusChip
import com.shakeflash.app.ui.components.SFSwitch
import com.shakeflash.app.ui.icons.SFIcons
import com.shakeflash.app.ui.theme.SFColors
import com.shakeflash.app.util.BatteryInfo
import kotlin.math.PI
import kotlin.math.sin
import java.util.Locale

@Composable
fun HomeScreen(
    state: ShakeFlashViewModel.UiState,
    contentPadding: PaddingValues,
    onToggleTorch: () -> Unit,
    onServiceToggle: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SFColors.Background),
    ) {
        NightGlow(isOn = state.torch.isOn)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            AppHeader(onOpenSettings = onOpenSettings)
            Spacer(Modifier.height(18.dp))

            TorchCard(state = state, onToggleTorch = onToggleTorch)
            Spacer(Modifier.height(14.dp))

            ServiceCard(state = state, onServiceToggle = onServiceToggle)
            Spacer(Modifier.height(14.dp))

            SensitivityCard(state = state, onSensitivityChange = onSensitivityChange)
            Spacer(Modifier.height(14.dp))

            PermissionCard(state = state, onRequestPermissions = onRequestPermissions)
            Spacer(Modifier.height(14.dp))

            BatteryCard(state = state, onOpenBatterySettings = onOpenBatterySettings)
            Spacer(Modifier.height(22.dp))

            Footer()
            Spacer(Modifier.height(26.dp))
        }
    }
}

/* ------------------------------------------------------------------------------------ */
/* Header                                                                               */
/* ------------------------------------------------------------------------------------ */

@Composable
private fun AppHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(SFColors.SurfaceElevated)
                .border(1.dp, SFColors.Border, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SFIcons.Flashlight,
                contentDescription = null,
                tint = SFColors.Accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ShakeFlash",
                style = MaterialTheme.typography.headlineSmall,
                color = SFColors.Foreground,
            )
            Text(
                text = "Shake to flashlight",
                style = MaterialTheme.typography.bodySmall,
                color = SFColors.Subtle,
            )
        }
        SFIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings,
        )
    }
}

/* ------------------------------------------------------------------------------------ */
/* Torch                                                                                */
/* ------------------------------------------------------------------------------------ */

@Composable
private fun TorchCard(
    state: ShakeFlashViewModel.UiState,
    onToggleTorch: () -> Unit,
) {
    val torchOn = state.torch.isOn
    SFCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "FLASHLIGHT",
                style = MaterialTheme.typography.labelSmall,
                color = SFColors.Subtle,
            )
            Spacer(Modifier.weight(1f))
            SFStatusChip(
                text = when {
                    !state.torch.available -> "NO FLASH"
                    torchOn -> "ON"
                    else -> "OFF"
                },
                tone = when {
                    !state.torch.available -> ChipTone.Danger
                    torchOn -> ChipTone.Accent
                    else -> ChipTone.Neutral
                },
            )
        }

        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PowerDial(
                isOn = torchOn,
                enabled = state.torch.available,
                onToggle = onToggleTorch,
            )
        }
        Spacer(Modifier.height(4.dp))

        Text(
            text = when {
                !state.torch.available -> "This device has no usable flashlight."
                torchOn -> "Shining — shake again or tap to switch off."
                state.detectionActive -> "Ready. Shake the phone or tap the dial."
                else -> "Ready. Tap the dial, or enable shake detection below."
            },
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (torchOn) SFColors.Foreground else SFColors.Muted,
            textAlign = TextAlign.Center,
        )

        if (torchOn) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SFColors.AccentSoft)
                    .border(1.dp, SFColors.Accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = SFIcons.Bolt,
                    contentDescription = null,
                    tint = SFColors.Accent,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "Torch is draining battery — remember to switch it off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SFColors.Accent,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------------------ */
/* Background service                                                                   */
/* ------------------------------------------------------------------------------------ */

@Composable
private fun ServiceCard(
    state: ShakeFlashViewModel.UiState,
    onServiceToggle: (Boolean) -> Unit,
) {
    SFCard {
        SFCardHeader(
            icon = SFIcons.Wave,
            title = "Shake detection",
            subtitle = if (state.detectionActive) "Active in the background" else "Stopped",
            iconTint = if (state.detectionActive) SFColors.Accent else SFColors.Muted,
            trailing = {
                SFSwitch(
                    checked = state.settings.serviceEnabled,
                    onCheckedChange = onServiceToggle,
                )
            },
        )
        Spacer(Modifier.height(14.dp))
        DetectionStrip(active = state.detectionActive)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Shake detection runs in a foreground service, so it keeps working when " +
                "the screen is locked, the app is minimized, another app is open, or " +
                "ShakeFlash is swiped away from Recent Apps.",
            style = MaterialTheme.typography.bodySmall,
            color = SFColors.Subtle,
        )
    }
}

@Composable
private fun DetectionStrip(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "detectionStrip")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "detectionPhase",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SFColors.SurfaceElevated)
            .border(1.dp, SFColors.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            repeat(4) { index ->
                val barPhase = (phase + index * 0.17f) % 1f
                val barHeight = if (active) {
                    (7f + 15f * (0.5f + 0.5f * sin(barPhase * 2 * PI).toFloat())).dp
                } else {
                    4.dp
                }
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (active) SFColors.Accent else SFColors.Subtle.copy(alpha = 0.5f),
                        ),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (active) "Listening for shakes" else "Not listening",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (active) SFColors.Foreground else SFColors.Muted,
            )
            Text(
                text = if (active) {
                    "Ongoing notification keeps it alive"
                } else {
                    "Turn on to enable background detection"
                },
                style = MaterialTheme.typography.bodySmall,
                color = SFColors.Subtle,
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) SFColors.Success else SFColors.Subtle),
        )
    }
}

/* ------------------------------------------------------------------------------------ */
/* Sensitivity                                                                          */
/* ------------------------------------------------------------------------------------ */

@Composable
private fun SensitivityCard(
    state: ShakeFlashViewModel.UiState,
    onSensitivityChange: (Int) -> Unit,
) {
    SFCard {
        SFCardHeader(
            icon = SFIcons.Bolt,
            title = "Shake sensitivity",
            subtitle = "Reacts to ${state.settings.sensitivityLabel.lowercase()} shakes",
            trailing = {
                SFStatusChip(
                    text = "${state.settings.sensitivity}",
                    tone = ChipTone.Accent,
                )
            },
        )
        Spacer(Modifier.height(10.dp))
        SFSlider(
            value = state.settings.sensitivity / 100f,
            onValueChange = { onSensitivityChange((it * 100f).toInt().coerceIn(0, 100)) },
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Gentle",
                style = MaterialTheme.typography.labelSmall,
                color = SFColors.Subtle,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Vigorous",
                style = MaterialTheme.typography.labelSmall,
                color = SFColors.Subtle,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Higher values respond to lighter shakes. Lower them if the torch ever " +
                "switches on by itself — ShakeFlash needs two sharp movements to fire, " +
                "so casual bumps are ignored.",
            style = MaterialTheme.typography.bodySmall,
            color = SFColors.Subtle,
        )
    }
}

/* ------------------------------------------------------------------------------------ */
/* Permissions                                                                          */
/* ------------------------------------------------------------------------------------ */

@Composable
private fun PermissionCard(
    state: ShakeFlashViewModel.UiState,
    onRequestPermissions: () -> Unit,
) {
    val missing = state.missingPermissions
    SFCard {
        SFCardHeader(
            icon = SFIcons.Shield,
            title = "Permissions",
            subtitle = if (missing.isEmpty()) "Everything needed is granted" else "${missing.size} still needed",
            iconTint = if (missing.isEmpty()) SFColors.Success else SFColors.Danger,
            trailing = {
                SFStatusChip(
                    text = if (missing.isEmpty()) "READY" else "ACTION",
                    tone = if (missing.isEmpty()) ChipTone.Success else ChipTone.Danger,
                )
            },
        )
        Spacer(Modifier.height(6.dp))

        val items = state.permissionItems
        items.forEachIndexed { index, item ->
            PermissionRow(item)
            if (index != items.lastIndex) SFDivider()
        }

        if (missing.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SFButton(
                text = "Grant permissions",
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth(),
                variant = SFButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun PermissionRow(item: PermissionManager.PermissionItem) {
    val icon: ImageVector = when (item.permission) {
        PermissionManager.CAMERA -> SFIcons.Flashlight
        PermissionManager.NOTIFICATIONS -> Icons.Rounded.Notifications
        android.Manifest.permission.RECEIVE_BOOT_COMPLETED -> Icons.Rounded.Refresh
        else -> SFIcons.Shield
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SFIconBadge(
            icon = icon,
            tint = if (item.granted) SFColors.Muted else SFColors.Danger,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = SFColors.Foreground,
            )
            Text(
                text = item.reason,
                style = MaterialTheme.typography.bodySmall,
                color = SFColors.Subtle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        SFStatusChip(
            text = if (item.granted) "GRANTED" else if (item.required) "NEEDED" else "OPTIONAL",
            tone = when {
                item.granted -> ChipTone.Success
                item.required -> ChipTone.Danger
                else -> ChipTone.Neutral
            },
        )
    }
}

/* ------------------------------------------------------------------------------------ */
/* Battery                                                                              */
/* ------------------------------------------------------------------------------------ */

@Composable
private fun BatteryCard(
    state: ShakeFlashViewModel.UiState,
    onOpenBatterySettings: () -> Unit,
) {
    val battery = state.battery
    val drain = BatteryInfo.estimatedExtraDrainPerHour(
        lowPowerSensor = state.settings.lowPowerSensor,
        torchOn = state.torch.isOn,
    )

    SFCard {
        SFCardHeader(
            icon = SFIcons.Battery,
            title = "Battery",
            subtitle = "≈ ${String.format(Locale.US, "%.1f", drain)}% per hour extra",
            trailing = {
                Text(
                    text = if (battery.levelPercent >= 0) "${battery.levelPercent}%" else "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = SFColors.Foreground,
                    fontWeight = FontWeight.SemiBold,
                )
            },
        )
        Spacer(Modifier.height(14.dp))
        BatteryBar(level = battery.levelPercent, charging = battery.isCharging)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MiniStat(
                label = "State",
                value = when {
                    battery.isCharging -> "Charging"
                    battery.powerSaveMode -> "Power save"
                    else -> "On battery"
                },
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "Sensor",
                value = if (state.settings.lowPowerSensor) "16 Hz" else "50 Hz",
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "Temp",
                value = battery.temperatureC?.let { "${it.toInt()}°C" } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }

        if (battery.optimized) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SFColors.DangerSoft)
                    .border(1.dp, SFColors.Danger.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Battery optimization is on",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SFColors.Danger,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Some phones pause background sensors when the screen is off " +
                            "for a long time. Exempting ShakeFlash keeps detection reliable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SFColors.Muted,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SFButton(
                text = "Open battery settings",
                onClick = onOpenBatterySettings,
                modifier = Modifier.fillMaxWidth(),
                variant = SFButtonVariant.Outline,
            )
        }
    }
}

@Composable
private fun BatteryBar(level: Int, charging: Boolean) {
    val fraction = (level.coerceIn(0, 100)) / 100f
    val color = when {
        charging -> SFColors.Success
        level in 0..15 -> SFColors.Danger
        else -> SFColors.Accent
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(SFColors.SurfaceHighlight)
            .border(1.dp, SFColors.Border, RoundedCornerShape(percent = 50)),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color))),
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SFColors.SurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(
            text = label.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            color = SFColors.Subtle,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = SFColors.Foreground,
            fontWeight = FontWeight.Medium,
        )
    }
}

/* ------------------------------------------------------------------------------------ */
/* Footer & background                                                                  */
/* ------------------------------------------------------------------------------------ */

@Composable
private fun Footer() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No internet permission · all processing stays on this device",
            style = MaterialTheme.typography.bodySmall,
            color = SFColors.Subtle,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "ShakeFlash ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = SFColors.Subtle.copy(alpha = 0.7f),
        )
    }
}

/** Slow amber glow that slides across the top of the screen while the torch is on. */
@Composable
private fun NightGlow(isOn: Boolean) {
    val transition = rememberInfiniteTransition(label = "nightGlow")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nightGlowShift",
    )
    val tint = if (isOn) SFColors.Accent else Color(0xFF6366F1)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.width * 0.95f
        val centerX = size.width * (0.18f + 0.64f * shift)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = if (isOn) 0.16f else 0.06f),
                    Color.Transparent,
                ),
                center = Offset(centerX, -size.height * 0.02f),
                radius = radius,
            ),
            radius = radius,
            center = Offset(centerX, 0f),
        )
    }
}
