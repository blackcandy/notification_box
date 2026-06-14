package com.notifbox.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.notifbox.service.NotifListenerService

/** Helpers for the special "notification access" permission this app depends on. */
object NotificationAccess {

    fun isGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val component = ComponentName(context, NotifListenerService::class.java)
        return enabled.split(":").any {
            ComponentName.unflattenFromString(it) == component
        }
    }

    /** Opens the system screen where the user toggles notification access. */
    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Forces the system to re-bind the listener after a process restart left it in a
     * "ghost" state (granted + shown as bound, but no callbacks). Toggling the
     * component off/on is stronger than [NotificationListenerService.requestRebind]
     * alone on aggressive ROMs.
     */
    fun forceRebind(context: Context) {
        val component = ComponentName(context, NotifListenerService::class.java)
        runCatching {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        runCatching { NotificationListenerService.requestRebind(component) }
    }
}
