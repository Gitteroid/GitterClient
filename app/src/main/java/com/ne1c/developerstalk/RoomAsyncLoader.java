package com.ne1c.developerstalk;

import android.content.AsyncTaskLoader;
import android.content.Context;

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

    // If use this flag, than loader will return null
    public static final int WRITE_TO_DATABASE = 2;

    private RestAdapter mAdapter;
    private final int mFlag;

    private ClientDatabase mClientDatabase;
    private ArrayList<RoomModel> mWriteRooms;

    public RoomAsyncLoader(Context context, int flag) {
        super(context);

        mFlag = flag;
        mClientDatabase = new ClientDatabase(context);

        if (mFlag == FROM_SERVER) {
            mAdapter = new RestAdapter.Builder()
                    .setEndpoint(Utils.GITTER_API_URL)
                    .build();
        }
    }

    public RoomAsyncLoader(Context context, int flag, ArrayList<RoomModel> rooms) {
        super(context);

        mFlag = flag;

        if (mFlag == WRITE_TO_DATABASE) {
            mWriteRooms = rooms;
            mClientDatabase = new ClientDatabase(context);
        }
    }

    @Override
    public ArrayList<RoomModel> loadInBackground() {
        ArrayList<RoomModel> rooms = null;

        if (mFlag == WRITE_TO_DATABASE) {
            mClientDatabase.insertRooms(mWriteRooms);

            // Return fresh data
            return mClientDatabase.getRooms();
        }

        ArrayList<RoomModel> dbRooms = mClientDatabase.getRooms();

        if (mFlag == FROM_SERVER) {
            IApiMethods methods = mAdapter.create(IApiMethods.class);
            rooms = methods.getCurrentUserRooms(Utils.getInstance().getBearer());

            if (rooms == null) {
                return null;
            }

            // Restore position and hideRoom status from db
            // And set correct position in list
            for (int i = 0; i < rooms.size(); i++) {
                RoomModel serverRoom = rooms.get(i);
                for (RoomModel dbRoom : dbRooms) {
                    if (serverRoom.id.equals(dbRoom.id)) {
                        serverRoom.listPosition = dbRoom.listPosition;
                        serverRoom.hide = dbRoom.hide;
                        Collections.swap(rooms, i, serverRoom.listPosition);
                    }
                }
            }

            return rooms;
        }

        if (mFlag == FROM_DATABASE) {
            return dbRooms;
        }

        // If it's first start of app
        if (dbRooms.size() == 0) {
            ArrayList<RoomModel> multi = new ArrayList<>();
            ArrayList<RoomModel> one = new ArrayList<>();
            for (RoomModel room : rooms) {
                if (room.oneToOne) one.add(room);
                else multi.add(room);
            }

            Collections.sort(multi, new RoomModel.SortedByName());
            Collections.sort(one, new RoomModel.SortedByName());
            rooms.clear();
            rooms.addAll(multi);
            rooms.addAll(one);

            mClientDatabase.insertRooms(rooms);

            return rooms;
        }

        return new ArrayList<>();
    }
}
