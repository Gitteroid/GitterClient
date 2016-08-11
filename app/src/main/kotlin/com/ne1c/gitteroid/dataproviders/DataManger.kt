package com.ne1c.gitteroid.dataproviders

import com.ne1c.gitteroid.api.GitterApi
import com.ne1c.gitteroid.api.responses.JoinRoomResponse
import com.ne1c.gitteroid.models.data.AuthResponseModel
import com.ne1c.gitteroid.models.data.MessageModel
import com.ne1c.gitteroid.models.data.RoomModel
import com.ne1c.gitteroid.models.data.SearchRoomsResponse
import com.ne1c.gitteroid.models.data.UserModel
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.utils.Utils

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

import javax.inject.Inject

import rx.Observable

open class DataManger
@Inject
constructor(private val mApi: GitterApi, private val mClientDatabase: ClientDatabase) {

    // Primary cache in memory
    private val mCachedRooms = ArrayList<RoomModel>()
    private val mCachedMessages = HashMap<String, ArrayList<MessageModel>>()
    private var mCurrentUser: UserModel? = null

    fun getRooms(fresh: Boolean): Observable<ArrayList<RoomModel>> {
        val serverRooms = mApi.getCurrentUserRooms(Utils.instance.bearer).onErrorResumeNext(Observable.just(ArrayList<RoomModel>()))

        val dbRooms = mClientDatabase.rooms

        if (fresh) {
            return Observable.zip(serverRooms, dbRooms) { server, db ->
                val synchronizedRooms = getSynchronizedRooms(server, db)

                mClientDatabase.updateRooms(synchronizedRooms)
                synchronizedRooms
            }
        } else {
            if (mCachedRooms.size == 0) {
                return@Observable.zip mClientDatabase . rooms . map < ArrayList < RoomModel > > { roomModels ->
                    mCachedRooms.clear()
                    mCachedRooms.addAll(roomModels)

                    roomModels
                }
            } else {
                return@mClientDatabase.getRooms()
                        .map Observable . just < ArrayList < RoomModel > > mCachedRooms
            }
        }
    }

    // Return new ArrayList that will synchronized with database
    private fun getSynchronizedRooms(fromNetworkRooms: ArrayList<RoomModel>, dbRooms: ArrayList<RoomModel>): ArrayList<RoomModel> {
        val synchronizedRooms = fromNetworkRooms.clone() as ArrayList<RoomModel>

        // Data exist in db ang get from server
        if (dbRooms.size > 0 && fromNetworkRooms.size > 0) {
            for (r1 in dbRooms) {
                for (r2 in synchronizedRooms) {
                    if (r1.id == r2.id) {
                        r2.hide = r1.hide
                        r2.listPosition = r1.listPosition
                    }
                }
            }

            Collections.sort(synchronizedRooms, RoomModel.SortedByPosition())

        } else if (dbRooms.size > 0 && fromNetworkRooms.size == 0) { // If data exist only in db
            Collections.sort(dbRooms, RoomModel.SortedByPosition())

            return dbRooms
        } else if (dbRooms.size == 0 && fromNetworkRooms.size > 0) { // If data  not exist in db, only server
            sortByTypes(fromNetworkRooms)

            return fromNetworkRooms
        }

        return synchronizedRooms
    }

    fun leaveFromRoom(roomId: String): Observable<Boolean> {
        return mApi.leaveRoom(Utils.instance.bearer, roomId, Utils.instance.userPref.id).map<StatusResponse>({ statusResponse ->
            mCachedMessages.remove(roomId)
            mClientDatabase.removeRoom(roomId)
            statusResponse
        }).map<Boolean>({ statusResponse -> statusResponse.success })
    }

    val profile: Observable<UserModel>
        get() {
            if (mCurrentUser == null) {
                mCurrentUser = Utils.instance.userPref

                val currentUserFromNetwork = mApi.getCurrentUser(Utils.instance.bearer).map<UserModel>({ userModels ->
                    val user = userModels.get(0)
                    Utils.instance.writeUserToPref(user)
                    mCurrentUser = user

                    user
                })

                return Observable.concat(Observable.just<UserModel>(mCurrentUser), currentUserFromNetwork)
            }

            return Observable.just<UserModel>(mCurrentUser)
        }

    fun getMessages(roomId: String, limit: Int, fresh: Boolean): Observable<ArrayList<MessageModel>> {
        if (fresh) {
            return Observable.concat(mClientDatabase.getMessages(roomId),
                    mApi.getMessagesRoom(Utils.instance.bearer, roomId, limit)).map<ArrayList<MessageModel>>({ messageModels ->
                mCachedMessages.put(roomId, messageModels)
                updateLastMessagesInDb(roomId, messageModels)

                messageModels
            })
        } else {
            if (mCachedMessages[roomId] == null) {
                return@Observable.concat(mClientDatabase.getMessages(roomId),
                        mApi.getMessagesRoom(Utils.getInstance().bearer, roomId, limit))
                        .map mClientDatabase . getMessages roomId.map<ArrayList<MessageModel>> { messageModels ->
                    mCachedMessages.put(roomId, messageModels)
                    messageModels
                }
            } else {
                return@mClientDatabase.getMessages(roomId)
                        .map Observable . just < ArrayList < MessageModel > > mCachedMessages[roomId]
            }
        }
    }

    fun getMessagesBeforeId(roomId: String, limit: Int, beforeId: String): Observable<ArrayList<MessageModel>> {
        return mApi.getMessagesBeforeId(Utils.instance.bearer,
                roomId, limit, beforeId)
    }

    fun addSingleMessage(roomId: String, model: MessageModel) {
        mClientDatabase.addSingleMessage(roomId, model)
    }

    fun updateLastMessagesInDb(roomId: String, messages: ArrayList<MessageModel>) {
        // Save last 10 messages
        if (messages.size > 10) {
            mClientDatabase.updateMessages(roomId,
                    messages.subList(messages.size - 11, messages.size) as ArrayList<MessageModel>)
        } else {
            mClientDatabase.updateMessages(roomId, messages)
        }
    }

    private fun sortByTypes(rooms: ArrayList<RoomModel>): ArrayList<RoomModel> {
        val multi = ArrayList<RoomModel>()
        val one = ArrayList<RoomModel>()

        for (room in rooms) {
            if (room.oneToOne) {
                one.add(room)
            } else {
                multi.add(room)
            }
        }

        Collections.sort(multi, RoomModel.SortedByName())
        Collections.sort(one, RoomModel.SortedByName())

        val roomModels = ArrayList<RoomModel>(multi.size + one.size)
        roomModels.addAll(multi)
        roomModels.addAll(one)

        return roomModels
    }

    fun updateMessage(roomId: String, messageId: String, text: String): Observable<MessageModel> {
        return mApi.updateMessage(Utils.instance.bearer, roomId, messageId, text).map<MessageModel>({ model ->
            mClientDatabase.updateSpecificMessage(roomId, model)
            model
        })
    }

    fun updateRooms(rooms: ArrayList<RoomModel>) {
        mClientDatabase.updateRooms(getSynchronizedRooms(rooms, mCachedRooms))
    }

    fun updateRooms(rooms: ArrayList<RoomViewModel>, wasEdit: Boolean) {
        for (cachedRoom in mCachedRooms) {
            for (viewRoom in rooms) {
                if (cachedRoom.id == viewRoom.id) {
                    cachedRoom.hide = viewRoom.hide
                    cachedRoom.listPosition = viewRoom.listPosition
                }
            }
        }

        mClientDatabase.updateRooms(mCachedRooms)
    }

    fun readMessages(roomId: String, ids: Array<String>): Observable<Boolean> {
        return mApi.readMessages(Utils.instance.bearer,
                Utils.instance.userPref.id, roomId, ids).map<Boolean>({ statusResponse -> statusResponse.success }).map<Boolean>({ response ->
            if (response!!) {
                for (room in mCachedRooms) {
                    if (room == roomId) {
                        room.unreadItems -= ids.size
                    }
                }

                updateRooms(mCachedRooms)
            }

            response
        })
    }

    fun sendMessage(roomId: String, text: String): Observable<MessageModel> {
        return mApi.sendMessage(Utils.instance.bearer, roomId, text)
    }

    fun searchRooms(query: String): Observable<SearchRoomsResponse> {
        return mApi.searchRooms(Utils.instance.bearer, query, 30, 0)
    }

    fun searchRoomsWithOffset(query: String, offset: Int): Observable<SearchRoomsResponse> {
        return mApi.searchRooms(Utils.instance.bearer, query, 10, offset)
    }

    fun authorization(client_id: String, client_secret: String, code: String, grant_type: String, redirect_url: String): Observable<AuthResponseModel> {
        return mApi.authorization("https://gitter.im/login/oauth/token",
                client_id, client_secret, code, grant_type, redirect_url)
    }

    fun joinToRoom(roomUri: String): Observable<JoinRoomResponse> {
        return mApi.joinRoom(Utils.instance.bearer, roomUri).map<JoinRoomResponse>({ joinRoomResponse ->
            mClientDatabase.addSingleRoom(joinRoomResponse)
            joinRoomResponse
        })
    }
}