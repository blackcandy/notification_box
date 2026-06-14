package com.notifbox

import android.app.Application
import com.notifbox.data.AppDatabase
import com.notifbox.data.NotifRepository
import com.notifbox.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Owns the singletons the app and listener service share. */
class NotifBoxApp : Application() {
    val repository: NotifRepository by lazy {
        val db = AppDatabase.get(this)
        NotifRepository(db.notificationDao(), db.ruleDao())
    }

    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Enforce the retention window on launch too, not only on listener (re)connect —
        // a long-lived connection would otherwise let history grow unbounded.
        appScope.launch {
            val days = settings.retentionDays.first()
            val cutoff = System.currentTimeMillis() - days * MILLIS_PER_DAY
            repository.pruneOlderThan(cutoff)
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
