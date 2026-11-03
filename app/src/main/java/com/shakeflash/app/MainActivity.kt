package com.shakeflash.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shakeflash.app.ui.ShakeFlashViewModel
import com.shakeflash.app.ui.screens.HomeScreen
import com.shakeflash.app.ui.screens.SettingsSheet
import com.shakeflash.app.ui.theme.SFColors
import com.shakeflash.app.ui.theme.ShakeFlashTheme
import com.shakeflash.app.util.SystemIntents

class MainActivity : ComponentActivity() {

    private val viewModel: ShakeFlashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars; the whole app is dark so the bars stay dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            ShakeFlashTheme {
                ShakeFlashRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Permissions may have changed in system settings while we were away.
        viewModel.refreshSystemState()
    }
}

@Composable
private fun ShakeFlashRoot(viewModel: ShakeFlashViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var enableServiceAfterGrant by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        viewModel.onPermissionsResult()
        if (enableServiceAfterGrant && results.values.all { it }) {
            viewModel.setServiceEnabled(true)
        }
        enableServiceAfterGrant = false
    }

    val requestPermissions: () -> Unit = {
        val missing = state.missingPermissions.map { it.permission }
        if (missing.isEmpty()) {
            viewModel.refreshSystemState()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SFColors.Background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(14.dp),
                    containerColor = SFColors.SurfaceHighlight,
                    contentColor = SFColors.Foreground,
                    actionColor = SFColors.Accent,
                )
            }
        },
    ) { innerPadding ->
        HomeScreen(
            state = state,
            contentPadding = innerPadding,
            onToggleTorch = viewModel::toggleTorch,
            onServiceToggle = { enabled ->
                if (enabled && !state.allPermissionsGranted) {
                    // Ask first, then start the service automatically once granted.
                    enableServiceAfterGrant = true
                    requestPermissions()
                } else {
                    viewModel.setServiceEnabled(enabled)
                }
            },
            onSensitivityChange = viewModel::setSensitivity,
            onRequestPermissions = requestPermissions,
            onOpenSettings = { showSettings = true },
            onOpenBatterySettings = {
                SystemIntents.launch(context, SystemIntents.batteryOptimizationSettings())
            },
        )
    }

    if (showSettings) {
        SettingsSheet(
            settings = state.settings,
            battery = state.battery,
            onDismiss = { showSettings = false },
            onHapticChange = viewModel::setHapticFeedback,
            onLowPowerChange = viewModel::setLowPowerSensor,
            onTorchOffOnStopChange = viewModel::setTorchOffWhenServiceStops,
            onOpenBatterySettings = {
                SystemIntents.launch(context, SystemIntents.batteryOptimizationSettings())
            },
            onOpenAutostart = {
                SystemIntents.launch(context, SystemIntents.autostartSettings(context))
            },
            onOpenAppSettings = {
                SystemIntents.launch(context, SystemIntents.appDetails(context))
            },
        )
    }
}
