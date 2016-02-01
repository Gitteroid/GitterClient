package com.ne1c.developerstalk.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.ne1c.developerstalk.models.AuthResponseModel;
import com.ne1c.developerstalk.models.UserModel;
import com.ne1c.developerstalk.services.NotificationService;

/**
 * Helper class
 */
public class Utils {
    public static final String GITTER_FAYE_URL = "https://ws.gitter.im/faye";
    public static final String GITTER_URL = "https://gitter.im";
    public static final String GITHUB_URL = "http://github.com";
    public static final String GITTER_API_URL = "https://api.gitter.im";
    public static final String ID_PREF_KEY = "id";
    public static final String USERNAME_PREF_KEY = "username";
    public static final String DISPLAY_NAME_PREF_KEY = "displayName";
    public static final String URL_NAME_PREF_KEY = "url";
    public static final String AVATAR_SMALL_PREF_KEY = "avatarUrlSmall";
    public static final String AVATAR_MEDIUM_PREF_KEY = "avatarUrlMedium";
    public static final String ACCESS_TOKEN_PREF_KEY = "access_token";
    public static final String EXPIRIES_IN_PREF_KEY = "EXPIRIES_IN";
    public static final String TOKEN_TYPE_PREF_KEY = "TOKEN_TYPE";
    public final String USERINFO_PREF = "userinfo";
    
    private static Utils mInstance;
    
    private Context mContext;

    private Utils(Context context) {
        mContext = context;
    }

    public static void init(Context context) {
        mInstance = new Utils(context);
    }

    public static Utils getInstance() {
        return mInstance;
    }

    public void writeUserToPref(UserModel model) {
        mContext.getSharedPreferences(USERINFO_PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(ID_PREF_KEY, model.id)
                .putString(USERNAME_PREF_KEY, model.username)
                .putString(DISPLAY_NAME_PREF_KEY, model.displayName)
                .putString(URL_NAME_PREF_KEY, model.url)
                .putString(AVATAR_SMALL_PREF_KEY, model.avatarUrlSmall)
                .putString(AVATAR_MEDIUM_PREF_KEY, model.avatarUrlMedium)
                .apply();
    }

    public UserModel getUserPref() {
        UserModel model = new UserModel();
        SharedPreferences pref = mContext.getSharedPreferences(USERINFO_PREF, Context.MODE_PRIVATE);

        if (!pref.getAll().isEmpty()) {
            model.id = pref.getString(ID_PREF_KEY, "");
            model.username = pref.getString(USERNAME_PREF_KEY, "");
            model.displayName = pref.getString(DISPLAY_NAME_PREF_KEY, "");
            model.url = pref.getString(URL_NAME_PREF_KEY, "");
            model.avatarUrlSmall = pref.getString(AVATAR_SMALL_PREF_KEY, "");
            model.avatarUrlMedium = pref.getString(AVATAR_MEDIUM_PREF_KEY, "");

            return model;
        }

        return model;
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    public String getBearer() {
        return "Bearer " + mContext.getSharedPreferences(USERINFO_PREF, Context.MODE_PRIVATE)
                .getString(ACCESS_TOKEN_PREF_KEY, "");
    }

    public void writeAuthResponsePref(AuthResponseModel model) {
        mContext.getSharedPreferences(USERINFO_PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(ACCESS_TOKEN_PREF_KEY, model.access_token)
                .putString(EXPIRIES_IN_PREF_KEY, model.expires_in)
                .putString(TOKEN_TYPE_PREF_KEY, model.token_type)
                .apply();
    }

    public String getAccessToken() {
        return mContext.getSharedPreferences(USERINFO_PREF, Context.MODE_PRIVATE)
                .getString(ACCESS_TOKEN_PREF_KEY, "");
    }

    public void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(mContext, "Copied", Toast.LENGTH_SHORT).show();
    }

    public void startNotificationService() {
        mContext.startService(getServiceIntentWithPrefs());
    }

    public Intent getServiceIntentWithPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        Intent intent = new Intent(mContext, NotificationService.class);
        intent.putExtra("enable_notif", prefs.getBoolean("enable_notif", true));
        intent.putExtra("notif_sound", prefs.getBoolean("notif_sound", true));
        intent.putExtra("notif_vibro", prefs.getBoolean("notif_vibro", true));
        intent.putExtra("notif_username", prefs.getBoolean("notif_username", false));

        return intent;
    }

    public ServiceMode getServiceMode() {
        return PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean("background_service", true) ? ServiceMode.LEAVE_WORK_WITHOUT_APP : ServiceMode.DEAD_WITH_APP;
    }
}
