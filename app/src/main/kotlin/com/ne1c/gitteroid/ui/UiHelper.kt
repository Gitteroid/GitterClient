package com.ne1c.gitteroid.ui

import android.app.Activity
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import com.ne1c.gitteroid.R

fun loadUrlWithChromeTabs(activity: Activity, url: String) {
    val builder = CustomTabsIntent.Builder()
            .setToolbarColor(activity.resources.getColor(R.color.colorPrimary))
    val customTabsIntent = builder.build()

    customTabsIntent.launchUrl(activity, Uri.parse(url))
}