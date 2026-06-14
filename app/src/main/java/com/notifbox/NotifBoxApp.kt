package com.notifbox

import android.app.Application
import com.notifbox.data.AppDatabase
import com.notifbox.data.NotifRepository
import com.notifbox.data.SettingsRepository

/** Owns the singletons the app and listener service share. */
class NotifBoxApp : Application() {
    val repository: NotifRepository by lazy {
        val db = AppDatabase.get(this)
        NotifRepository(db.notificationDao(), db.ruleDao())
    }

    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
