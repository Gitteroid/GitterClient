package com.ne1c.developerstalk;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;

import com.ne1c.developerstalk.Activities.MainActivity;
import com.ne1c.developerstalk.Database.ClientDatabase;
import com.ne1c.developerstalk.Models.RoomModel;
import com.ne1c.developerstalk.RetrofitServices.IApiMethods;
import com.ne1c.developerstalk.Util.Utils;

import java.util.ArrayList;
import java.util.Collections;

import retrofit.RestAdapter;

public class RoomAsyncLoader extends AsyncTaskLoader<ArrayList<RoomModel>> {
    public static final int FROM_SERVER = 0;
    public static final int FROM_DATABASE = 1;

    private RestAdapter mAdapter;
    private final int mFlag;

    private ClientDatabase mClientDatabase;

    public RoomAsyncLoader(Context context, int flag) {
        super(context);

        mFlag = flag;

        if (mFlag == FROM_SERVER) {
            mAdapter = new RestAdapter.Builder()
                    .setEndpoint(Utils.GITTER_API_URL)
                    .build();
        } else {
            mClientDatabase = new ClientDatabase(context);
        }
    }

    @Override
    public ArrayList<RoomModel> loadInBackground() {
        ArrayList<RoomModel> rooms=null;

        if (mFlag == FROM_SERVER) {
            IApiMethods methods = mAdapter.create(IApiMethods.class);
            rooms = methods.getCurrentUserRooms(Utils.getInstance().getBearer());
        } else if (mFlag == FROM_DATABASE) {
            rooms = mClientDatabase.getRooms();
        }
        if (rooms==null) return null;

        ArrayList<RoomModel> multi = new ArrayList<>();
        ArrayList<RoomModel> one = new ArrayList<>();
        for (RoomModel room : rooms) {
            if (room.oneToOne) one.add(room);
            else multi.add(room);
        }

        Collections.sort(multi, new RoomModel.TypeComparator());
        Collections.sort(one, new RoomModel.TypeComparator());
        rooms.clear();
        rooms.addAll(multi);
        rooms.addAll(one);
        return rooms;
    }
}
