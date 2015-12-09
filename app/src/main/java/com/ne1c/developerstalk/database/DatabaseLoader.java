package com.ne1c.developerstalk.database;

import android.content.AsyncTaskLoader;
import android.content.Context;

public class DatabaseLoader extends AsyncTaskLoader<Object> {
    public static final int GET_ROOMS = 0;
    public static final int GET_MESSAGES = 0;
    public static final int GET_USERS = 0;

    private final int mFlag;
    private String mArg;
    private ClientDatabase mClientDatabase;

    public DatabaseLoader(Context context, int flag) {
        super(context);

        mFlag = flag;
        mClientDatabase = new ClientDatabase(context);
    }

    // Using arg, if flag will equals GET_MESSAGES
    public DatabaseLoader(Context context, int flag, String arg) {
        super(context);

        mFlag = flag;
        mArg = arg;
        mClientDatabase = new ClientDatabase(context);
    }

    @Override
    public Object loadInBackground() {
        switch (mFlag) {
            case GET_ROOMS:
                return mClientDatabase.getRooms();
            default: return null;
        }
    }
}
