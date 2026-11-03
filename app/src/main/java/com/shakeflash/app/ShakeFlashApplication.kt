package com.shakeflash.app

import android.app.Application
import android.content.Context
import com.shakeflash.app.data.SettingsRepository
import com.shakeflash.app.flashlight.FlashlightController
import com.shakeflash.app.permissions.PermissionManager
import com.shakeflash.app.service.FlashlightNotifications

/**
 * Hand-rolled dependency container: three singletons, no DI framework needed.
 * Application scope is what keeps [FlashlightController]'s torch callback registered for
 * the whole process lifetime, so the UI and the service can never disagree.
 */
class AppContainer(context: Context) {
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val flashlightController: FlashlightController = FlashlightController(context)
    val permissionManager: PermissionManager = PermissionManager(context)
}

class ShakeFlashApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        FlashlightNotifications.createChannel(this)
    }
}
