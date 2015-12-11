package com.ne1c.developerstalk.services;

import android.content.Context;

import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.database.ClientDatabase;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit.RestAdapter;
import rx.Observable;

public class DataManger {
    private GitterApi mApi;
    private ClientDatabase mClientDatabase;
    private Context mContext;

    public DataManger(Context context) {
        mContext = context;

        mApi = new RestAdapter.Builder()
                .setEndpoint(Utils.GITTER_API_URL)
                .build()
                .create(GitterApi.class);
        mClientDatabase = new ClientDatabase(context);
    }

    public Observable<ArrayList<RoomModel>> getRooms() {
        Observable<ArrayList<RoomModel>> serverRooms = mApi.getCurrentUserRooms(Utils.getInstance().getBearer());
        Observable<ArrayList<RoomModel>> dbRooms = mClientDatabase.getRooms();

        return Observable.zip(serverRooms, dbRooms, (server, db) -> {
            ArrayList<RoomModel> result = new ArrayList<>();

            // Data exist in db ang get from server
            if (db.size() > 0 && server.size() > 0) {
                for (RoomModel r1 : db) {
                    for (RoomModel r2 : server) {
                        if (r1.id.equals(r2.id)) {
                            r2.hide = r1.hide;
                            r2.listPosition = r1.listPosition;
                        }
                    }
                }

                Collections.sort(server, new RoomModel.SortedByPosition());
                mClientDatabase.insertRooms(server);

                result = server;
            } else if (db.size() > 0 && server.size() == 0) { // If data exist only in db
                Collections.sort(db, new RoomModel.SortedByPosition());

                result = db;
            } else if (db.size() == 0 && server.size() > 0) { // If data  not exist in db, only server
                sortByName(server);
                mClientDatabase.insertRooms(server);

                result = db;
            }

            return result;
        });

//        return Observable.merge((serverRooms, dbRooms) -> {
//
//                }
//
//        );
    }

    public Observable<ArrayList<RoomModel>> getDbRooms() {
        return mClientDatabase.getRooms();
    }

    public void writeRoomsToDb(List<RoomModel> rooms) {
        mClientDatabase.insertRooms(rooms);
    }

    private void sortByName(List<RoomModel> rooms) {
        List<RoomModel> multi = new ArrayList<>();
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
    }
}
