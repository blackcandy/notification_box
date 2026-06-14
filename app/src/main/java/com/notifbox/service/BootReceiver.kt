package com.notifbox.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-launches the keep-alive service after reboot (a foreground-start exemption). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            KeepAliveService.start(context)
        }
    }
}
