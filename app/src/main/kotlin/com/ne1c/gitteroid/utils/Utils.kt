package com.ne1c.gitteroid.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import android.widget.Toast

import com.ne1c.gitteroid.models.data.AuthResponseModel
import com.ne1c.gitteroid.models.data.UserModel
import com.ne1c.gitteroid.services.NotificationService

/**
 * Helper class
 */
class Utils private constructor(val context: Context) {
    val USERINFO_PREF = "userinfo"

    fun writeUserToPref(model: UserModel) {
        context.getSharedPreferences(USERINFO_PREF, Context.MODE_PRIVATE).edit().putString(ID_PREF_KEY, model.id).putString(USERNAME_PREF_KEY, model.username).putString(DISPLAY_NAME_PREF_KEY, model.displayName).putString(URL_NAME_PREF_KEY, model.url).putString(AVATAR_SMALL_PREF_KEY, model.avatarUrlSmall).putString(AVATAR_MEDIUM_PREF_KEY, model.avatarUrlMedium).apply()
    }

    val userPref: UserModel
        get() {
            val model = UserModel()
            val pref = context.getSharedPreferences(USERINFO_PREF, Context.MODE_PRIVATE)

            if (!pref.all.isEmpty()) {
                model.id = pref.getString(ID_PREF_KEY, "")
                model.username = pref.getString(USERNAME_PREF_KEY, "")
                model.displayName = pref.getString(DISPLAY_NAME_PREF_KEY, "")
                model.url = pref.getString(URL_NAME_PREF_KEY, "")
                model.avatarUrlSmall = pref.getString(AVATAR_SMALL_PREF_KEY, "")
                model.avatarUrlMedium = pref.getString(AVATAR_MEDIUM_PREF_KEY, "")

                return model
            }

            return model
        }

    val isNetworkConnected: Boolean
        get() {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null
        }

    val bearer: String
        get() = "Bearer " + context.getSharedPreferences(USERINFO_PREF, Context.MODE_PRIVATE).getString(ACCESS_TOKEN_PREF_KEY, "")!!

    fun writeAuthResponsePref(model: AuthResponseModel) {
        context.getSharedPreferences(USERINFO_PREF, Context.MODE_PRIVATE).edit().putString(ACCESS_TOKEN_PREF_KEY, model.access_token).putString(EXPIRIES_IN_PREF_KEY, model.expires_in).putString(TOKEN_TYPE_PREF_KEY, model.token_type).apply()
    }

    val accessToken: String
        get() = context.getSharedPreferences(USERINFO_PREF, Context.MODE_PRIVATE).getString(ACCESS_TOKEN_PREF_KEY, "")

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", text)
        clipboard.primaryClip = clip

        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }

    fun startNotificationService() {
        context.startService(serviceIntentWithPrefs)
    }

    val serviceIntentWithPrefs: Intent
        get() {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)

            val intent = Intent(context, NotificationService::class.java)
            intent.putExtra("enable_notif", prefs.getBoolean("enable_notif", true))
            intent.putExtra("notif_sound", prefs.getBoolean("notif_sound", true))
            intent.putExtra("notif_vibro", prefs.getBoolean("notif_vibro", true))
            intent.putExtra("notif_username", prefs.getBoolean("notif_username", false))

            return intent
        }

    companion object {
        val GITTER_FAYE_URL = "https://ws.gitter.im/faye"
        val GITTER_URL = "https://gitter.im"
        val GITHUB_URL = "http://github.com"
        val GITTER_API_URL = "https://api.gitter.im"
        val ID_PREF_KEY = "id"
        val USERNAME_PREF_KEY = "username"
        val DISPLAY_NAME_PREF_KEY = "displayName"
        val URL_NAME_PREF_KEY = "url"
        val AVATAR_SMALL_PREF_KEY = "avatarUrlSmall"
        val AVATAR_MEDIUM_PREF_KEY = "avatarUrlMedium"
        val ACCESS_TOKEN_PREF_KEY = "access_token"
        val EXPIRIES_IN_PREF_KEY = "EXPIRIES_IN"
        val TOKEN_TYPE_PREF_KEY = "TOKEN_TYPE"

        var instance: Utils? = null
            private set

        fun init(context: Context) {
            instance = Utils(context)
        }
    }
}
