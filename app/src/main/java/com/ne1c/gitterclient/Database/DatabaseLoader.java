package com.ne1c.gitterclient.Database;

import android.content.AsyncTaskLoader;
import android.content.Context;

public class DatabaseLoader extends AsyncTaskLoader<Object> {
    public static final int GET_ROOMS = 0;
    public static final int GET_MESSAGES = 0;
    public static final int GET_USERS = 0;

    public DatabaseLoader(Context context) {
        super(context);
    }

    @Override
    public Object loadInBackground() {
        return null;
    }
}
