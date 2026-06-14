package com.notifbox.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.notifbox.R
import com.notifbox.ui.MainActivity
import com.notifbox.util.NotificationAccess

/**
 * A foreground service whose only job is to keep the app process resident so the
 * bound [NotifListenerService] survives aggressive OEM background killers.
 * START_STICKY lets the system restart it (and re-promote to foreground) if killed.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        // The process can be restarted (boot / keep-alive) without the listener
        // reconnecting — ColorOS shows it as "bound" but no callbacks fire. Force a
        // rebind so delivery resumes.
        NotificationAccess.forceRebind(this)
        return START_STICKY
    }

    /**
     * The user swiped the app off the recents list. On lenient ROMs we can schedule
     * a restart so recording resumes; on aggressive ROMs (ColorOS/MIUI) this only
     * works if the user has also granted the OEM "auto-start" permission.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        runCatching {
            val restart = PendingIntent.getService(
                this, 1, Intent(applicationContext, KeepAliveService::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
            )
            getSystemService(AlarmManager::class.java)
                .set(AlarmManager.RTC, System.currentTimeMillis() + 1500, restart)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "运行状态", NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "保持通知监听在后台持续运行"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("通知滤盒正在运行")
            .setContentText("正在后台记录并过滤通知")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "keep_alive"
        private const val NOTIF_ID = 1

        /** Safe to call from any foreground entry point; ignores background-start refusals. */
        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }
}
