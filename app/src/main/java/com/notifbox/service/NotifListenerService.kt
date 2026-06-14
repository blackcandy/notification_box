package com.notifbox.service

import android.app.Notification
import android.content.ComponentName
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notifbox.NotifBoxApp
import com.notifbox.data.NotificationEntity
import com.notifbox.filter.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Bound by the system once the user grants notification access. Every posted
 * notification arrives in [onNotificationPosted]; we snapshot it, run the rule
 * engine, and persist the result.
 */
class NotifListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val app get() = application as NotifBoxApp
    private val repository get() = app.repository

    /** On (re)connect, drop history older than the user's retention window. */
    override fun onListenerConnected() {
        super.onListenerConnected()
        scope.launch {
            val days = app.settings.retentionDays.first()
            val cutoff = System.currentTimeMillis() - days * MILLIS_PER_DAY
            repository.pruneOlderThan(cutoff)
        }
    }

    /** If the system tears us down, ask it to bind us again. */
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        requestRebind(ComponentName(this, NotifListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Skip ongoing/foreground-service notifications and our own.
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val pkg = sbn.packageName
        val label = appLabel(pkg)
        val posted = sbn.postTime

        scope.launch {
            val verdict = RuleEngine.evaluate(
                rules = repository.enabledRules(),
                packageName = pkg,
                title = title,
                text = text,
            )
            repository.record(
                NotificationEntity(
                    packageName = pkg,
                    appLabel = label,
                    title = title,
                    text = text,
                    subText = subText,
                    postedAt = posted,
                    filtered = verdict.filtered,
                    matchedRuleId = verdict.matchedRuleId,
                )
            )
            // When a rule matches, optionally pull the notification out of the shade.
            // It stays in our history; the user just isn't bothered by it live.
            if (verdict.filtered && app.settings.removeFromShade.first()) {
                cancelNotification(sbn.key)
            }
        }
    }

    private fun appLabel(pkg: String): String = runCatching {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
