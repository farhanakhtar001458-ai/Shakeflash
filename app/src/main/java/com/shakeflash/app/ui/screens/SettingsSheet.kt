package com.shakeflash.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shakeflash.app.BuildConfig
import com.shakeflash.app.data.ShakeFlashSettings
import com.shakeflash.app.ui.components.SFButton
import com.shakeflash.app.ui.components.SFButtonVariant
import com.shakeflash.app.ui.components.SFDivider
import com.shakeflash.app.ui.components.SFSettingRow
import com.shakeflash.app.ui.components.SFSwitch
import com.shakeflash.app.ui.icons.SFIcons
import com.shakeflash.app.ui.theme.SFColors
import com.shakeflash.app.util.BatterySnapshot

/**
 * Secondary screen (as a bottom sheet, so the main toggle is always one tap away).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: ShakeFlashSettings,
    battery: BatterySnapshot,
    onDismiss: () -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onLowPowerChange: (Boolean) -> Unit,
    onTorchOffOnStopChange: (Boolean) -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAutostart: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SFColors.Surface,
        contentColor = SFColors.Foreground,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = SFColors.BorderStrong) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = SFColors.Foreground,
            )
            Text(
                text = "How ShakeFlash behaves in the background",
                style = MaterialTheme.typography.bodySmall,
                color = SFColors.Subtle,
            )
            Spacer(Modifier.height(18.dp))

            SFSettingRow(
                icon = SFIcons.Wave,
                title = "Haptic feedback",
                subtitle = "Buzz when a shake toggles the torch",
                trailing = {
                    SFSwitch(
                        checked = settings.hapticFeedback,
                        onCheckedChange = onHapticChange,
                    )
                },
            )
            SFDivider()

            SFSettingRow(
                icon = SFIcons.Battery,
                title = "Low-power sensor",
                subtitle = "Sample at 16 Hz instead of 50 Hz to save battery",
                trailing = {
                    SFSwitch(
                        checked = settings.lowPowerSensor,
                        onCheckedChange = onLowPowerChange,
                    )
                },
            )
            SFDivider()

            SFSettingRow(
                icon = SFIcons.Flashlight,
                title = "Torch off when service stops",
                subtitle = "Never leave the LED burning after disabling detection",
                trailing = {
                    SFSwitch(
                        checked = settings.torchOffWhenServiceStops,
                        onCheckedChange = onTorchOffOnStopChange,
                    )
                },
            )
            SFDivider()

            SFSettingRow(
                icon = SFIcons.Bolt,
                title = "Battery optimisation",
                subtitle = if (battery.optimized) {
                    "Android may pause the sensor — exempt ShakeFlash"
                } else {
                    "ShakeFlash is exempt"
                },
                onClick = onOpenBatterySettings,
            )
            SFDivider()

            SFSettingRow(
                icon = Icons.Rounded.Refresh,
                title = "Autostart / background permissions",
                subtitle = "Extra OEM settings on Xiaomi, Oppo, Vivo, Huawei",
                onClick = onOpenAutostart,
            )
            SFDivider()

            SFSettingRow(
                icon = Icons.Rounded.Settings,
                title = "App info & permissions",
                subtitle = "System settings for ShakeFlash",
                onClick = onOpenAppSettings,
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = SFColors.Subtle,
                    modifier = Modifier.size(17.dp),
                )
                Column {
                    Text(
                        text = "ShakeFlash ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SFColors.Muted,
                    )
                    Text(
                        text = "No internet permission · no analytics · nothing stored off-device",
                        style = MaterialTheme.typography.bodySmall,
                        color = SFColors.Subtle,
                    )
                }
            }

            SFButton(
                text = "Done",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                variant = SFButtonVariant.Secondary,
            )
        }
    }
}
