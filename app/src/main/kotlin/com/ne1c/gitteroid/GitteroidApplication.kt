package com.ne1c.gitteroid

import android.app.Application
import android.content.Intent
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.ne1c.gitteroid.di.DependencyManager
import com.ne1c.gitteroid.di.PresenterStorage
import com.ne1c.gitteroid.services.NotificationService
import com.ne1c.rainbowmvp.PresenterFactory
import io.fabric.sdk.android.Fabric

class GitteroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val core = CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build()

        val crashlytics = Crashlytics.Builder()
                .core(core)
                .build()

        Fabric.with(this, crashlytics)

        DependencyManager.INSTANCE.init(this)
        PresenterFactory.init(PresenterStorage())
    }

    fun startNotificationService() {
        startService(getServiceIntentWithPrefs())
    }

    fun getServiceIntentWithPrefs(): Intent {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val intent = Intent(this, NotificationService::class.java)
        intent.putExtra("enable_notif", prefs.getBoolean("enable_notif", true))
        intent.putExtra("notif_sound", prefs.getBoolean("notif_sound", true))
        intent.putExtra("notif_vibro", prefs.getBoolean("notif_vibro", true))
        intent.putExtra("notif_username", prefs.getBoolean("notif_username", false))

        return intent
    }
}
