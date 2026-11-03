package com.shakeflash.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Everything the app persists. Defaults are chosen so that a fresh install is safe:
 * the service is OFF until the user explicitly turns it on.
 */
data class ShakeFlashSettings(
    val serviceEnabled: Boolean = false,
    val sensitivity: Int = DEFAULT_SENSITIVITY,
    val hapticFeedback: Boolean = true,
    val lowPowerSensor: Boolean = false,
    val torchOffWhenServiceStops: Boolean = true,
) {
    /** "Low" / "Balanced" / "High" label used by the UI. */
    val sensitivityLabel: String
        get() = when {
            sensitivity < 34 -> "Low"
            sensitivity < 67 -> "Balanced"
            else -> "High"
        }

    companion object {
        const val DEFAULT_SENSITIVITY = 60
        const val MIN_SENSITIVITY = 0
        const val MAX_SENSITIVITY = 100
    }
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shakeflash_settings",
)

/**
 * DataStore-backed settings store. A single source of truth shared by the UI and the
 * foreground service, so changing the slider immediately changes the live threshold.
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    private object Keys {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val SENSITIVITY = intPreferencesKey("sensitivity")
        val HAPTIC = booleanPreferencesKey("haptic_feedback")
        val LOW_POWER = booleanPreferencesKey("low_power_sensor")
        val TORCH_OFF_ON_STOP = booleanPreferencesKey("torch_off_on_stop")
    }

    val settings: Flow<ShakeFlashSettings> = dataStore.data
        .catch { error ->
            // A corrupted preferences file must never crash the service/UI.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            ShakeFlashSettings(
                serviceEnabled = prefs[Keys.SERVICE_ENABLED] ?: false,
                sensitivity = (prefs[Keys.SENSITIVITY] ?: ShakeFlashSettings.DEFAULT_SENSITIVITY)
                    .coerceIn(ShakeFlashSettings.MIN_SENSITIVITY, ShakeFlashSettings.MAX_SENSITIVITY),
                hapticFeedback = prefs[Keys.HAPTIC] ?: true,
                lowPowerSensor = prefs[Keys.LOW_POWER] ?: false,
                torchOffWhenServiceStops = prefs[Keys.TORCH_OFF_ON_STOP] ?: true,
            )
        }

    suspend fun setServiceEnabled(enabled: Boolean) = dataStore.edit {
        it[Keys.SERVICE_ENABLED] = enabled
    }

    suspend fun setSensitivity(value: Int) = dataStore.edit {
        it[Keys.SENSITIVITY] = value.coerceIn(
            ShakeFlashSettings.MIN_SENSITIVITY,
            ShakeFlashSettings.MAX_SENSITIVITY,
        )
    }

    suspend fun setHapticFeedback(enabled: Boolean) = dataStore.edit {
        it[Keys.HAPTIC] = enabled
    }

    suspend fun setLowPowerSensor(enabled: Boolean) = dataStore.edit {
        it[Keys.LOW_POWER] = enabled
    }

    suspend fun setTorchOffWhenServiceStops(enabled: Boolean) = dataStore.edit {
        it[Keys.TORCH_OFF_ON_STOP] = enabled
    }
}
