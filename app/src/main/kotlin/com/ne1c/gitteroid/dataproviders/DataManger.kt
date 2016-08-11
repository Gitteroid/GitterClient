package com.ne1c.gitteroid.dataproviders

import android.content.SharedPreferences
import com.ne1c.gitteroid.api.GitterApi
import com.ne1c.gitteroid.api.responses.JoinRoomResponse
import com.ne1c.gitteroid.models.data.*
import com.ne1c.gitteroid.models.view.RoomViewModel
import rx.Observable
import java.util.*

open class DataManger(private val mApi: GitterApi,
                      private val mClientDatabase: ClientDatabase,
                      private val mUserPreferences: SharedPreferences) {

    companion object {
        val USERINFO_PREF = "userinfo"

        val GITTER_FAYE_URL = "https://ws.gitter.im/faye"
        val GITTER_URL = "https://gitter.im"
        val GITHUB_URL = "http://github.com"
        val GITTER_API_URL = "https://api.gitter.im"
        val ID_PREF_KEY = "id"
        val USERNAME_PREF_KEY = "username"
        val DISPLAY_NAME_PREF_KEY = "displayName"
        val URL_NAME_PREF_KEY = "url"
        val AVATAR_SMALL_PREF_KEY = "avatarUrlSmall"
        val AVATAR_MEDIUM_PREF_KEY = "avatarUrlMedium"
        val ACCESS_TOKEN_PREF_KEY = "access_token"
        val EXPIRIES_IN_PREF_KEY = "EXPIRIES_IN"
        val TOKEN_TYPE_PREF_KEY = "TOKEN_TYPE"
    }

    // Primary cache in memory
    private val mCachedRooms = ArrayList<RoomModel>()
    private val mCachedMessages = HashMap<String, ArrayList<MessageModel>>()
    private var mCurrentUser: UserModel? = null

    fun getRooms(fresh: Boolean): Observable<ArrayList<RoomModel>> {
        if (fresh) {
            return mApi.getCurrentUserRooms(bearer)
                    .onErrorResumeNext(mClientDatabase.rooms)
        } else {
            return Observable.just(mCachedRooms)
        }
    }

    fun leaveFromRoom(roomId: String): Observable<Boolean> {
        return mApi.leaveRoom(bearer, roomId, getUser().id)
                .map({
                    mCachedMessages.remove(roomId)
                    mClientDatabase.removeRoom(roomId)
                    return@map it
                })
                .map({ it.success })
    }

    fun getProfile(): Observable<UserModel> {
        if (mCurrentUser == null) {
            mCurrentUser = getUser()

            return mApi.getCurrentUser(bearer)
                    .map { userModels ->
                        val user = userModels[0]
                        mCurrentUser = user
                        return@map user
                    }
                    .onErrorResumeNext { Observable.just(mCurrentUser) }
        }

        return Observable.just(mCurrentUser)
    }

    fun getMessages(roomId: String, limit: Int, fresh: Boolean): Observable<ArrayList<MessageModel>> {
        if (fresh) {
            return mApi.getMessagesRoom(bearer, roomId, limit)
                    .map({ messageModels ->
                        mCachedMessages.put(roomId, messageModels)
                        updateLastMessagesInDb(roomId, messageModels)

                        return@map messageModels
                    })
        } else {
            if (mCachedMessages[roomId] == null) {
                return mClientDatabase.getMessages(roomId)
            } else {
                return Observable.just(mCachedMessages[roomId])
            }
        }
    }

    fun getMessagesBeforeId(roomId: String, limit: Int, beforeId: String): Observable<ArrayList<MessageModel>> {
        return mApi.getMessagesBeforeId(bearer, roomId, limit, beforeId)
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
        return mApi.updateMessage(bearer, roomId, messageId, text)
                .map({ model ->
                    mClientDatabase.updateSpecificMessage(roomId, model)
                    model
                })
    }

    fun updateRooms(rooms: ArrayList<RoomModel>) {
        //mClientDatabase.updateRooms(getSynchronizedRooms(rooms, mCachedRooms))
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
        return mApi.readMessages(bearer, getUser().id, roomId, ids)
                .map({ it.success })
                .map({ response ->
                    if (response) {
                        for (room in mCachedRooms) {
                            if (room.id === roomId) {
                                room.unreadItems -= ids.size
                            }
                        }

                        updateRooms(mCachedRooms)
                    }

                    return@map response
                })
    }

    fun sendMessage(roomId: String, text: String): Observable<MessageModel> {
        return mApi.sendMessage(bearer, roomId, text)
    }

    fun searchRooms(query: String): Observable<SearchRoomsResponse> {
        return mApi.searchRooms(bearer, query, 30, 0)
    }

    fun searchRoomsWithOffset(query: String, offset: Int): Observable<SearchRoomsResponse> {
        return mApi.searchRooms(bearer, query, 10, offset)
    }

    fun authorization(client_id: String, client_secret: String, code: String,
                      grant_type: String, redirect_url: String): Observable<AuthResponseModel> {
        return mApi.authorization("https://gitter.im/login/oauth/token", client_id, client_secret,
                code, grant_type, redirect_url)
    }

    fun joinToRoom(roomUri: String): Observable<JoinRoomResponse> {
        return mApi.joinRoom(bearer, roomUri)
                .map({ joinRoomResponse ->
                    mClientDatabase.addSingleRoom(joinRoomResponse)
                    joinRoomResponse
                })
    }

    fun getUser(): UserModel {
        val model = UserModel()

        if (!mUserPreferences.all.isEmpty()) {
            model.id = mUserPreferences.getString(ID_PREF_KEY, "")
            model.username = mUserPreferences.getString(USERNAME_PREF_KEY, "")
            model.displayName = mUserPreferences.getString(DISPLAY_NAME_PREF_KEY, "")
            model.url = mUserPreferences.getString(URL_NAME_PREF_KEY, "")
            model.avatarUrlSmall = mUserPreferences.getString(AVATAR_SMALL_PREF_KEY, "")
            model.avatarUrlMedium = mUserPreferences.getString(AVATAR_MEDIUM_PREF_KEY, "")

            return model
        }

        return model
    }

    fun saveUser(model: AuthResponseModel) {
        mUserPreferences.edit()
                .putString(ACCESS_TOKEN_PREF_KEY, model.access_token)
                .putString(EXPIRIES_IN_PREF_KEY, model.expires_in)
                .putString(TOKEN_TYPE_PREF_KEY, model.token_type)
                .apply()
    }

    val bearer: String
        get() = "Bearer " + mUserPreferences.getString(ACCESS_TOKEN_PREF_KEY, "")

    fun isAuthorize(): Boolean {
        return !mUserPreferences.getString(ACCESS_TOKEN_PREF_KEY, "").isEmpty()
    }

    fun cleatProfile() {
        mUserPreferences.edit()
                .putString(ACCESS_TOKEN_PREF_KEY, "")
                .putString(EXPIRIES_IN_PREF_KEY, "")
                .putString(TOKEN_TYPE_PREF_KEY, "")
                .apply()
    }
}