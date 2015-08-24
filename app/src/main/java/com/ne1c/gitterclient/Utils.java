package com.ne1c.gitterclient;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.ne1c.gitterclient.Models.UserModel;

import org.json.JSONObject;

public class Utils {

    public final String GITHUB_URL = "http://github.com";
    public final String APP_DATA_PREF = "app_data";
    public final String AUTH_KEY_PREF = "auth_state";
    public final String GITTER_API_URL = "https://api.gitter.im";
    public final String GITTER_STREAM_URL = "https://stream.gitter.im";
    public final String USERINFO_PREF = "userinfo";

    public final String ID_PREF_KEY = "id";
    public final String USERNAME_PREF_KEY = "username";
    public final String DISPLAY_NAME_PREF_KEY = "displayName";
    public final String URL_NAME_PREF_KEY = "url";
    public final String AVATAR_SMALL_PREF_KEY = "avatarUrlSmall";
    public final String AVATAR_MEDIUM_PREF_KEY = "avatarUrlMedium";

    private Context mContext;

    private static Utils instance;

    private Utils(Context context) {
        mContext = context;
    }

    public static void init(Context context) {
        instance = new Utils(context);
    }

    public static Utils getInstance() {
        return instance;
    }

    public boolean checkAuth(JSONObject obj, Activity activity) {
        boolean auth = true;

        return auth;
    }

    public void writeStateAuthPref(Context context, boolean state) {
        context.getSharedPreferences(APP_DATA_PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(AUTH_KEY_PREF, state)
                .apply();
    }

    public boolean getStateAuthPref(Context context) {
        return context.getSharedPreferences(APP_DATA_PREF, Context.MODE_PRIVATE)
                .getBoolean(AUTH_KEY_PREF, false);
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

        return null;
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }
}
