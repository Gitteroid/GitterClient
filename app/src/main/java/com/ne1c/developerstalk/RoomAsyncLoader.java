package com.ne1c.developerstalk;

import android.content.AsyncTaskLoader;

import com.ne1c.developerstalk.Activities.MainActivity;
import com.ne1c.developerstalk.Database.ClientDatabase;
import com.ne1c.developerstalk.Models.RoomModel;
import com.ne1c.developerstalk.RetrofitServices.IApiMethods;

import java.util.ArrayList;

import retrofit.RestAdapter;

public class RoomAsyncLoader extends AsyncTaskLoader<ArrayList<RoomModel>> {
    public static final int FROM_SERVER = 0;
    public static final int FROM_DATABASE = 1;

    private RestAdapter mAdapter;
    private final int mFlag;

    private ClientDatabase mClientDatabase;

    public RoomAsyncLoader(MainActivity activity, int flag) {
        super(activity);

        mFlag = flag;

        if (mFlag == FROM_SERVER) {
            mAdapter = new RestAdapter.Builder()
                    .setEndpoint(Utils.GITTER_API_URL)
                    .build();
        } else {
            mClientDatabase = new ClientDatabase(activity);
        }
    }

    @Override
    public ArrayList<RoomModel> loadInBackground() {
        if (mFlag == FROM_SERVER) {
            IApiMethods methods = mAdapter.create(IApiMethods.class);
            return methods.getCurrentUserRooms(Utils.getInstance().getBearer());
        } else if (mFlag == FROM_DATABASE) {
            return mClientDatabase.getRooms();
        } else {
            return null;
        }
    }
}
