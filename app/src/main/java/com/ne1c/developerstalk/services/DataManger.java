package com.ne1c.developerstalk.services;

import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.database.ClientDatabase;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.models.UserModel;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import retrofit.RestAdapter;
import retrofit.client.Response;
import rx.Observable;

public class DataManger {
    private GitterApi mApi;
    private ClientDatabase mClientDatabase;

    @Inject
    public DataManger(ClientDatabase database) {
        mApi = new RestAdapter.Builder()
                .setEndpoint(Utils.GITTER_API_URL)
                .build()
                .create(GitterApi.class);

        mClientDatabase = database;
    }

    public Observable<ArrayList<RoomModel>> getRooms() {
        Observable<ArrayList<RoomModel>> serverRooms = mApi.getCurrentUserRooms(Utils.getInstance().getBearer())
                .onErrorResumeNext(Observable.just(new ArrayList<>()));

        Observable<ArrayList<RoomModel>> dbRooms = mClientDatabase.getRooms();

        return Observable.combineLatest(serverRooms, dbRooms, (server, db) -> {
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

                return server;
            } else if (db.size() > 0 && server.size() == 0) { // If data exist only in db
                Collections.sort(db, new RoomModel.SortedByPosition());

                return db;
            } else if (db.size() == 0 && server.size() > 0) { // If data  not exist in db, only server
                sortByName(server);
                mClientDatabase.insertRooms(server);

                return server;
            }

            return new ArrayList<>();
        });
    }

    public Observable<ArrayList<RoomModel>> getDbRooms() {
        return mClientDatabase.getRooms();
    }

    public void writeRoomsToDb(List<RoomModel> rooms) {
        mClientDatabase.insertRooms(rooms);
    }

    public Observable<Response> leaveFromRoom(String roomId) {
        return mApi.leaveRoom(Utils.getInstance().getAccessToken(), roomId, Utils.getInstance().getUserPref().id);
    }

    public Observable<ArrayList<UserModel>> getProfile() {
        return mApi.getCurrentUser(Utils.getInstance().getBearer());
    }

    public Observable<ArrayList<MessageModel>> getMessagesBeforeId(String roomId, int limit, String beforeId) {
        return mApi.getMessagesBeforeId(Utils.getInstance().getBearer(),
                roomId, limit, beforeId);
    }

    public void insertMessageToDb(MessageModel model, String roomId) {
        mClientDatabase.insertMessage(model, roomId);
    }

    public void insertMessagesToDb(ArrayList<MessageModel> messages, String roomId) {
        mClientDatabase.insertMessages(messages, roomId);
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

    public Observable<MessageModel> updateMessage(String roomId, String messageId, String text) {
        return mApi.updateMessage(Utils.getInstance().getBearer(), roomId, messageId, text);
    }

    public Observable<ArrayList<MessageModel>> getMessages(String roomId, int limit) {
        return mApi.getMessagesRoom(Utils.getInstance().getBearer(), roomId, limit);
    }

    public Observable<Response> readMessages(String roomId, String[] ids) {
        return mApi.readMessages(Utils.getInstance().getBearer(),
                Utils.getInstance().getUserPref().id,
                roomId,
                ids);
    }

    public Observable<MessageModel> sendMessage(String roomId, String text) {
        return mApi.sendMessage(Utils.getInstance().getBearer(), roomId, text);
    }

    public Observable<ArrayList<MessageModel>> getCachedMessages(String roomId) {
        return mClientDatabase.getMessages(roomId);
    }
}