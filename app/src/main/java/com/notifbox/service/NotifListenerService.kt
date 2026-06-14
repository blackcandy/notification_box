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
import java.util.concurrent.ConcurrentHashMap

/**
 * Bound by the system once the user grants notification access. Every posted
 * notification arrives in [onNotificationPosted]; we snapshot it, run the rule
 * engine, and persist the result.
 */
class NotifListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Cache of package -> app label so we don't hit PackageManager on every notification. */
    private val labelCache = ConcurrentHashMap<String, String>()

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
        // Skip our own notifications, ongoing/persistent ones (media, navigation,
        // downloads — they re-post constantly and aren't "arrivals"), and group
        // summaries (the children carry the real content).
        if (sbn.packageName == packageName) return
        if (sbn.isOngoing) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        // Prefer the richest body available: big text and the inbox/messaging line
        // list often hold content that EXTRA_TEXT only summarizes.
        val text = bestText(extras)
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val pkg = sbn.packageName
        val label = appLabel(pkg)
        val posted = sbn.postTime
        val key = sbn.key

        scope.launch {
            // Dedupe: many apps re-post the same key with unchanged content (progress
            // updates, re-ranking). Skip if the latest record for this key is identical.
            repository.latestByKey(key)?.let { prev ->
                if (prev.title == title && prev.text == text) return@launch
            }
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
                    sbnKey = key,
                )
            )
            // When a rule matches, optionally pull the notification out of the shade.
            // It stays in our history; the user just isn't bothered by it live.
            if (verdict.filtered && app.settings.removeFromShade.first()) {
                cancelNotification(sbn.key)
            }
        }
    }

    private fun appLabel(pkg: String): String = labelCache.getOrPut(pkg) {
        runCatching {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
    }

    /** Pick the longest of EXTRA_TEXT / EXTRA_BIG_TEXT / EXTRA_TEXT_LINES. */
    private fun bestText(extras: android.os.Bundle): String? {
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString("\n") { it.toString() }
            ?.takeIf { it.isNotBlank() }
        return listOfNotNull(text, bigText, lines).maxByOrNull { it.length }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
