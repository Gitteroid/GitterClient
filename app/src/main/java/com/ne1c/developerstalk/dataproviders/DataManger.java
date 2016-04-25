package com.ne1c.developerstalk.dataproviders;

import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.api.responses.JoinRoomResponse;
import com.ne1c.developerstalk.models.data.AuthResponseModel;
import com.ne1c.developerstalk.models.data.MessageModel;
import com.ne1c.developerstalk.models.data.RoomModel;
import com.ne1c.developerstalk.models.data.SearchRoomsResponse;
import com.ne1c.developerstalk.models.data.UserModel;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import rx.Observable;

public class DataManger {
    private GitterApi mApi;
    private ClientDatabase mClientDatabase;

    // Primary cache in memory
    private ArrayList<RoomModel> mCachedRooms = new ArrayList<>();
    private Map<String, ArrayList<MessageModel>> mCachedMessages = new HashMap<>();
    private UserModel mCurrentUser;

    @Inject
    public DataManger(GitterApi api, ClientDatabase database) {
        mApi = api;
        mClientDatabase = database;
    }

    public Observable<ArrayList<RoomModel>> getRooms(boolean fresh) {
        Observable<ArrayList<RoomModel>> serverRooms = mApi.getCurrentUserRooms(Utils.getInstance().getBearer())
                .onErrorResumeNext(Observable.just(new ArrayList<>()));

        Observable<ArrayList<RoomModel>> dbRooms = mClientDatabase.getRooms();

        if (fresh) {
             return Observable.zip(serverRooms, dbRooms, (server, db) -> {
                ArrayList<RoomModel> synchronizedRooms = getSynchronizedRooms(server, db);

                mClientDatabase.updateRooms(synchronizedRooms);
                return synchronizedRooms;
            });
        } else {
            if (mCachedRooms.size() == 0) {
                return mClientDatabase.getRooms()
                        .map(roomModels -> {
                            mCachedRooms.clear();
                            mCachedRooms.addAll(roomModels);

                            return roomModels;
                        });
            } else {
                return Observable.just(mCachedRooms);
            }
        }
    }

    // Return new ArrayList that will synchronized with database
    private ArrayList<RoomModel> getSynchronizedRooms(ArrayList<RoomModel> fromNetworkRooms, ArrayList<RoomModel> dbRooms) {
        ArrayList<RoomModel> synchronizedRooms = (ArrayList<RoomModel>) fromNetworkRooms.clone();

        // Data exist in db ang get from server
        if (dbRooms.size() > 0 && fromNetworkRooms.size() > 0) {
            for (RoomModel r1 : dbRooms) {
                for (RoomModel r2 : synchronizedRooms) {
                    if (r1.id.equals(r2.id)) {
                        r2.hide = r1.hide;
                        r2.listPosition = r1.listPosition;
                    }
                }
            }

            Collections.sort(synchronizedRooms, new RoomModel.SortedByPosition());

        } else if (dbRooms.size() > 0 && fromNetworkRooms.size() == 0) { // If data exist only in db
            Collections.sort(dbRooms, new RoomModel.SortedByPosition());

            return dbRooms;
        } else if (dbRooms.size() == 0 && fromNetworkRooms.size() > 0) { // If data  not exist in db, only server
            sortByTypes(fromNetworkRooms);

            return fromNetworkRooms;
        }

        return synchronizedRooms;
    }

    public Observable<ArrayList<RoomModel>> getDbRooms() {
        return mClientDatabase.getRooms();
    }

    public void writeRoomsToDb(List<RoomModel> rooms) {
        mClientDatabase.insertRooms(rooms);
    }

    public Observable<Boolean> leaveFromRoom(String roomId) {
        return mApi.leaveRoom(Utils.getInstance().getBearer(), roomId, Utils.getInstance().getUserPref().id)
                .map(statusResponse -> statusResponse.success);
    }

    public Observable<UserModel> getProfile() {
        if (mCurrentUser == null) {
            mCurrentUser = Utils.getInstance().getUserPref();

            Observable<UserModel> currentUserFromNetwork = mApi.getCurrentUser(Utils.getInstance().getBearer())
                    .map(userModels -> userModels.get(0))
                    .map(userModel -> {
                        Utils.getInstance().writeUserToPref(userModel);
                        mCurrentUser = userModel;

                        return userModel;
                    });

            return Observable.concat(Observable.just(mCurrentUser), currentUserFromNetwork);
        }

        return Observable.just(mCurrentUser);
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

    private ArrayList<RoomModel> sortByTypes(ArrayList<RoomModel> rooms) {
        ArrayList<RoomModel> multi = new ArrayList<>();
        ArrayList<RoomModel> one = new ArrayList<>();

        for (RoomModel room : rooms) {
            if (room.oneToOne) {
                one.add(room);
            } else {
                multi.add(room);
            }
        }

        Collections.sort(multi, new RoomModel.SortedByName());
        Collections.sort(one, new RoomModel.SortedByName());

        ArrayList<RoomModel> roomModels = new ArrayList<>(multi.size() + one.size());
        roomModels.addAll(multi);
        roomModels.addAll(one);

        return roomModels;
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

    public Observable<Boolean> readMessages(String roomId, String[] ids) {
        return mApi.readMessages(Utils.getInstance().getBearer(),
                Utils.getInstance().getUserPref().id, roomId, ids)
                .map(statusResponse -> statusResponse.success);
    }

    public Observable<MessageModel> sendMessage(String roomId, String text) {
        return mApi.sendMessage(Utils.getInstance().getBearer(), roomId, text);
    }

    public Observable<ArrayList<MessageModel>> getDbMessages(String roomId) {
        return mClientDatabase.getMessages(roomId);
    }

    public Observable<SearchRoomsResponse> searchRooms(String query) {
        return mApi.searchRooms(Utils.getInstance().getBearer(), query, 30, 0);
    }

    public Observable<SearchRoomsResponse> searchRoomsWithOffset(String query, int offset) {
        return mApi.searchRooms(Utils.getInstance().getBearer(), query, 10, offset);
    }

    public Observable<AuthResponseModel> authorization(String client_id, String client_secret, String code, String grant_type, String redirect_url) {
        return mApi.authorization("https://gitter.im/login/oauth/token",
                client_id, client_secret, code, grant_type, redirect_url);
    }

    public Observable<JoinRoomResponse> joinToRoom(String roomUri) {
        return mApi.joinRoom(Utils.getInstance().getBearer(), roomUri)
                .map(joinRoomResponse -> {
                    writeRoomsToDb(Collections.singletonList((RoomModel) joinRoomResponse));
                    return joinRoomResponse;
                });
    }
}