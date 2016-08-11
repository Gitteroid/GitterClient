package com.ne1c.gitteroid.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class OnBootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.startService(Intent(context, NotificationService::class.java))
    }
}
