package com.ne1c.developerstalk.dataproviders;

import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.api.responses.JoinRoomResponse;
import com.ne1c.developerstalk.api.responses.StatusResponse;
import com.ne1c.developerstalk.models.data.AuthResponseModel;
import com.ne1c.developerstalk.models.data.MessageModel;
import com.ne1c.developerstalk.models.data.RoomModel;
import com.ne1c.developerstalk.models.data.SearchRoomsResponse;
import com.ne1c.developerstalk.models.data.UserModel;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;

public class DataManger {
    private GitterApi mApi;
    private ClientDatabase mClientDatabase;

    @Inject
    public DataManger(GitterApi api, ClientDatabase database) {
        mApi = api;
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

    public Observable<StatusResponse> leaveFromRoom(String roomId) {
        return mApi.leaveRoom(Utils.getInstance().getBearer(), roomId, Utils.getInstance().getUserPref().id);
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
        // Save last 10 messages
        if (messages.size() > 10) {
            ArrayList<MessageModel> newList = new ArrayList<>();

            for (int i = messages.size() - 11; i < messages.size(); i++) {
                newList.add(messages.get(i));
            }

            mClientDatabase.insertMessages(newList, roomId);
        } else {
            mClientDatabase.insertMessages(messages, roomId);
        }
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

    public Observable<ArrayList<MessageModel>> getNetworkMessages(String roomId, int limit) {
        return mApi.getMessagesRoom(Utils.getInstance().getBearer(), roomId, limit)
                .onErrorResumeNext(Observable.just(new ArrayList<>()))
                .map(messageModels -> {
                    insertMessagesToDb(messageModels, roomId);
                    return messageModels;
                });
    }

    public Observable<StatusResponse> readMessages(String roomId, String[] ids) {
        return mApi.readMessages(Utils.getInstance().getBearer(),
                Utils.getInstance().getUserPref().id,
                roomId,
                ids);
    }

    public Observable<MessageModel> sendMessage(String roomId, String text) {
        return mApi.sendMessage(Utils.getInstance().getBearer(), roomId, text);
    }

    public Observable<ArrayList<MessageModel>> getDbMessages(String roomId) {
        return mClientDatabase.getMessages(roomId);
    }

    public void clearCachedMessagesInRoom(String roomId) {
        mClientDatabase.clearCachedMessages(roomId);
    }

    public Observable<ArrayList<MessageModel>> getCachedMessages(String roomId) {
        return mClientDatabase.getCachedMessagesModel(roomId);
    }

    public void insertCachedMessages(ArrayList<MessageModel> list, String roomId) {
        mClientDatabase.insertCachedMessages(list, roomId);
    }

    public void insertCachedMessage(MessageModel message, String roomId) {
        ArrayList<MessageModel> list = new ArrayList<>();
        list.add(message);

        mClientDatabase.insertCachedMessages(list, roomId);
    }

    public Observable<SearchRoomsResponse> searchRooms(String query) {
        return mApi.searchRooms(Utils.getInstance().getBearer(), query, 30, 0);
    }

    public Observable<SearchRoomsResponse> searchRoomsWithOffset(String query, int offset) {
        return mApi.searchRooms(Utils.getInstance().getBearer(), query, 10, offset);
    }

    public Observable<AuthResponseModel> authorization(String client_id, String client_secret, String code, String grant_type, String redirect_url) {
        return mApi.authorization("https://gitter.im/login/oauth/token",
                client_id,
                client_secret,
                code,
                grant_type,
                redirect_url);
    }

    public Observable<JoinRoomResponse> joinToRoom(String roomUri) {
        return mApi.joinRoom(Utils.getInstance().getBearer(), roomUri);
    }
}