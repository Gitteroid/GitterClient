package com.ne1c.gitteroid

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.ne1c.gitteroid.utils.Utils
import io.fabric.sdk.android.Fabric

class GitteroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())

        Utils.init(applicationContext)
    }
}
