package com.ne1c.gitteroid.dataproviders

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import com.ne1c.gitteroid.models.data.MessageModel
import com.ne1c.gitteroid.models.data.RoomModel
import com.ne1c.gitteroid.models.data.UserModel

import java.util.ArrayList
import java.util.Collections

import rx.Observable

class ClientDatabase(context: Context) {

    private val mDatabase: SQLiteDatabase

    init {
        val DBWorker = DBWorker(context, DB_NAME, DB_VERSION)
        mDatabase = DBWorker.writableDatabase
    }

    val rooms: Observable<ArrayList<RoomModel>>
        get() = Observable.fromCallable<ArrayList<RoomModel>>(Callable<ArrayList<RoomModel>> { this.getSyncRooms() })

    val syncRooms: ArrayList<RoomModel>
        get() {
            val list = ArrayList<RoomModel>()

            val cursor = mDatabase.query(ROOM_TABLE, null, null, null, null, null, null, null)
            try {
                if (cursor.moveToFirst()) {
                    val columnId = cursor.getColumnIndex(COLUMN_ROOM_ID)
                    val columnName = cursor.getColumnIndex(COLUMN_NAME)
                    val columnTopic = cursor.getColumnIndex(COLUMN_TOPIC)
                    val columnUsersIds = cursor.getColumnIndex(COLUMN_USERS_IDS)
                    val columnOneToOne = cursor.getColumnIndex(COLUMN_ONE_TO_ONE)
                    val columnUsersCount = cursor.getColumnIndex(COLUMN_USERS_COUNT)
                    val columnUnreadItems = cursor.getColumnIndex(COLUMN_UNREAD_ITEMS)
                    val columnUrl = cursor.getColumnIndex(COLUMN_URL)
                    val columnVersion = cursor.getColumnIndex(COLUMN_VERSION)
                    val columnHide = cursor.getColumnIndex(COLUMN_HIDE)
                    val columnListPos = cursor.getColumnIndex(COLUMN_LIST_POSITION)

                    do {
                        val model = RoomModel()
                        model.id = cursor.getString(columnId)
                        model.name = cursor.getString(columnName)
                        model.topic = cursor.getString(columnTopic)
                        model.oneToOne = cursor.getInt(columnOneToOne) == 1
                        model.userCount = cursor.getInt(columnUsersCount)
                        model.unreadItems = cursor.getInt(columnUnreadItems)
                        model.url = cursor.getString(columnUrl)
                        model.v = cursor.getInt(columnVersion)
                        model.hide = cursor.getInt(columnHide) == 1
                        model.listPosition = cursor.getInt(columnListPos)

                        val idsStr = cursor.getString(columnUsersIds)
                        val idsArr = getUsersIds(idsStr)
                        model.users = getUsers(idsArr)

                        list.add(model)
                    } while (cursor.moveToNext())

                }
            } finally {
                cursor.close()
            }

            Collections.sort(list, RoomModel.SortedByPosition())

            return list
        }

    fun getUser(userId: String): UserModel? {
        val cursor = mDatabase.query(USERS_TABLE, null, COLUMN_USER_ID + " = ?",
                arrayOf(userId), null, null, null, null)
        try {
            if (cursor.moveToFirst()) {
                val columnUserId = cursor.getColumnIndex(COLUMN_USER_ID)
                val columnUsername = cursor.getColumnIndex(COLUMN_USERNAME)
                val columnDisplayName = cursor.getColumnIndex(COLUMN_DISPLAY_NAME)
                val columnAvatarSmall = cursor.getColumnIndex(COLUMN_AVATAR_SMALL_URL)
                val columnAvatarMedium = cursor.getColumnIndex(COLUMN_AVATAR_MEDIUM_URL)

                val model = UserModel()
                model.id = cursor.getString(columnUserId)
                model.username = cursor.getString(columnUsername)
                model.displayName = cursor.getString(columnDisplayName)
                model.avatarUrlMedium = cursor.getString(columnAvatarMedium)
                model.avatarUrlSmall = cursor.getString(columnAvatarSmall)

                return model
            }
        } finally {
            cursor.close()
        }
        return null
    }

    fun getUsers(ids: Array<String>): ArrayList<UserModel> {
        val list = ArrayList<UserModel>()
        val cursor = mDatabase.query(USERS_TABLE, null, COLUMN_USER_ID + " = ?",
                ids, null, null, null, null)

        try {
            if (cursor.moveToFirst()) {
                val columnUserId = cursor.getColumnIndex(COLUMN_USER_ID)
                val columnUsername = cursor.getColumnIndex(COLUMN_USERNAME)
                val columnDisplayname = cursor.getColumnIndex(COLUMN_DISPLAY_NAME)
                val columnAvatarSmall = cursor.getColumnIndex(COLUMN_AVATAR_SMALL_URL)
                val columnAvatarMedium = cursor.getColumnIndex(COLUMN_AVATAR_MEDIUM_URL)

                do {
                    val model = UserModel()
                    model.id = cursor.getString(columnUserId)
                    model.username = cursor.getString(columnUsername)
                    model.displayName = cursor.getString(columnDisplayname)
                    model.avatarUrlMedium = cursor.getString(columnAvatarMedium)
                    model.avatarUrlSmall = cursor.getString(columnAvatarSmall)

                    list.add(model)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }

        return list
    }

    fun addSingleMessage(roomId: String, model: MessageModel) {
        mDatabase.beginTransaction()

        val cv = ContentValues()
        cv.put(COLUMN_MESSAGE_ID, model.id)
        cv.put(COLUMN_ROOM_ID, roomId)
        cv.put(COLUMN_TEXT, model.text)
        cv.put(COLUMN_HTML, model.html)
        cv.put(COLUMN_SENT, model.sent)
        cv.put(COLUMN_EDITED_AT, model.editedAt)
        cv.put(COLUMN_FROM_USER_ID, model.fromUser.id)
        cv.put(COLUMN_UNREAD, if (model.unread) 1 else 0)
        cv.put(COLUMN_READ_BY, model.readBy)
        cv.put(COLUMN_VERSION, model.v)

        var urls = ""
        for (url in model.urls) {
            urls += url.url + ";"
        }

        cv.put(COLUMN_URLS, urls)

        mDatabase.insert(MESSAGES_TABLE, null, cv)

        insertUsers(ArrayList(listOf(model.fromUser)))

        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
    }

    fun getMessages(roomId: String): Observable<ArrayList<MessageModel>> {
        return Observable.create<ArrayList<MessageModel>> { subscriber ->
            val list = ArrayList<MessageModel>()
            val cursor = mDatabase.rawQuery(String.format("SELECT * FROM %s WHERE %s = \'%s\' ORDER BY %s DESC LIMIT 10;",
                    MESSAGES_TABLE, COLUMN_ROOM_ID, roomId, COLUMN_ID), null)

            var difference = 1
            if (cursor.moveToPosition(cursor.count - difference)) {
                val columnMessageId = cursor.getColumnIndex(COLUMN_MESSAGE_ID)
                val columnText = cursor.getColumnIndex(COLUMN_TEXT)
                val columnHtml = cursor.getColumnIndex(COLUMN_HTML)
                val columnSent = cursor.getColumnIndex(COLUMN_SENT)
                val columnEditedAt = cursor.getColumnIndex(COLUMN_EDITED_AT)
                val columnFromUserId = cursor.getColumnIndex(COLUMN_FROM_USER_ID)
                val columnUnread = cursor.getColumnIndex(COLUMN_UNREAD)
                val columnReadBy = cursor.getColumnIndex(COLUMN_READ_BY)
                val columnVersion = cursor.getColumnIndex(COLUMN_VERSION)
                val columnUrls = cursor.getColumnIndex(COLUMN_URLS)

                do {
                    val model = MessageModel()
                    model.id = cursor.getString(columnMessageId)
                    model.text = cursor.getString(columnText)
                    model.html = cursor.getString(columnHtml)
                    model.sent = cursor.getString(columnSent)
                    model.editedAt = cursor.getString(columnEditedAt)
                    model.fromUser = getUser(cursor.getString(columnFromUserId))
                    model.unread = cursor.getInt(columnUnread) == 1
                    model.readBy = cursor.getInt(columnReadBy)
                    model.v = cursor.getInt(columnVersion)
                    val url = cursor.getString(columnUrls)
                    model.urls = getUrls(url)

                    list.add(model)
                    difference += 1
                } while (cursor.moveToPosition(cursor.count - difference))
            }

            subscriber.onNext(list)
            subscriber.onCompleted()
        }
    }

    // Get users ids, from string
    private fun getUsersIds(ids: String): Array<String> {
        val list = ArrayList<String>()

        var startIndex = 0
        for (i in 0..ids.length - 1) {
            if (ids.toCharArray()[i] == ';') {
                list.add(ids.substring(startIndex, i))
                startIndex = i + 1
            }
        }

        return list.toTypedArray()
    }

    // Get urls in message, from string
    private fun getUrls(urls: String): List<MessageModel.Urls> {
        val list = ArrayList<MessageModel.Urls>()

        var startIndex = 0
        for (i in 0..urls.length - 1) {
            if (urls.toCharArray()[i] == ';') {
                val url = MessageModel.Urls()
                url.url = urls.substring(startIndex, i)
                list.add(url)
                startIndex = i + 1
            }
        }

        return list
    }

    fun insertUsers(list: ArrayList<UserModel>) {
        mDatabase.beginTransaction()

        for (model in list) {
            val cv = ContentValues()
            cv.put(COLUMN_USER_ID, model.id)
            cv.put(COLUMN_USERNAME, model.username)
            cv.put(COLUMN_DISPLAY_NAME, model.displayName)
            cv.put(COLUMN_AVATAR_SMALL_URL, model.avatarUrlSmall)
            cv.put(COLUMN_AVATAR_MEDIUM_URL, model.avatarUrlMedium)

            mDatabase.insertWithOnConflict(USERS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }

        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
    }

    fun close() {
        mDatabase.close()
    }

    // Send param only with synchronized rooms
    fun updateRooms(synchronizedRooms: ArrayList<RoomModel>) {
        mDatabase.beginTransaction()

        mDatabase.delete(ROOM_TABLE, null, null)

        for (room in synchronizedRooms) {
            val cv = ContentValues()
            cv.put(COLUMN_ROOM_ID, room.id)
            cv.put(COLUMN_NAME, room.name)
            cv.put(COLUMN_TOPIC, room.topic)
            cv.put(COLUMN_ONE_TO_ONE, if (room.oneToOne) 1 else 0)
            cv.put(COLUMN_USERS_COUNT, room.userCount)
            cv.put(COLUMN_UNREAD_ITEMS, room.unreadItems)
            cv.put(COLUMN_URL, room.url)
            cv.put(COLUMN_VERSION, room.v)
            cv.put(COLUMN_HIDE, if (room.hide) 1 else 0)
            cv.put(COLUMN_LIST_POSITION, room.listPosition)

            var userIds = ""
            for (user in room.users) {
                userIds += user.id + ";"
            }
            cv.put(COLUMN_USERS_IDS, userIds)

            mDatabase.insert(ROOM_TABLE, null, cv)
        }

        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
    }

    fun addSingleRoom(room: RoomModel) {
        mDatabase.beginTransaction()

        val cv = ContentValues()
        cv.put(COLUMN_ROOM_ID, room.id)
        cv.put(COLUMN_NAME, room.name)
        cv.put(COLUMN_TOPIC, room.topic)
        cv.put(COLUMN_ONE_TO_ONE, if (room.oneToOne) 1 else 0)
        cv.put(COLUMN_USERS_COUNT, room.userCount)
        cv.put(COLUMN_UNREAD_ITEMS, room.unreadItems)
        cv.put(COLUMN_URL, room.url)
        cv.put(COLUMN_VERSION, room.v)
        cv.put(COLUMN_HIDE, if (room.hide) 1 else 0)
        cv.put(COLUMN_LIST_POSITION, room.listPosition)

        var userIds = ""
        for (user in room.users) {
            userIds += user.id + ";"
        }
        cv.put(COLUMN_USERS_IDS, userIds)

        mDatabase.insert(ROOM_TABLE, null, cv)

        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
    }

    fun updateMessages(roomId: String, messageModels: ArrayList<MessageModel>) {
        mDatabase.beginTransaction()

        mDatabase.delete(MESSAGES_TABLE, COLUMN_ROOM_ID + " = ?", arrayOf(roomId))

        val users = ArrayList<UserModel>()

        for (model in messageModels) {
            val cv = ContentValues()
            cv.put(COLUMN_MESSAGE_ID, model.id)
            cv.put(COLUMN_ROOM_ID, roomId)
            cv.put(COLUMN_TEXT, model.text)
            cv.put(COLUMN_HTML, model.html)
            cv.put(COLUMN_SENT, model.sent)
            cv.put(COLUMN_EDITED_AT, model.editedAt)
            cv.put(COLUMN_FROM_USER_ID, model.fromUser.id)
            cv.put(COLUMN_UNREAD, if (model.unread) 1 else 0)
            cv.put(COLUMN_READ_BY, model.readBy)
            cv.put(COLUMN_VERSION, model.v)

            var urls = ""
            for (url in model.urls) {
                urls += url.url + ";"
            }

            cv.put(COLUMN_URLS, urls)

            users.add(model.fromUser)

            mDatabase.insert(MESSAGES_TABLE, null, cv)
        }

        insertUsers(users)

        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
    }

    fun updateSpecificMessage(roomId: String, model: MessageModel) {
        mDatabase.beginTransaction()

        val cv = ContentValues()
        cv.put(COLUMN_MESSAGE_ID, model.id)
        cv.put(COLUMN_ROOM_ID, roomId)
        cv.put(COLUMN_TEXT, model.text)
        cv.put(COLUMN_HTML, model.html)
        cv.put(COLUMN_SENT, model.sent)
        cv.put(COLUMN_EDITED_AT, model.editedAt)
        cv.put(COLUMN_FROM_USER_ID, model.fromUser.id)
        cv.put(COLUMN_UNREAD, if (model.unread) 1 else 0)
        cv.put(COLUMN_READ_BY, model.readBy)
        cv.put(COLUMN_VERSION, model.v)

        var urls = ""
        for (url in model.urls) {
            urls += url.url + ";"
        }

        cv.put(COLUMN_URLS, urls)

        mDatabase.update(MESSAGES_TABLE, cv, COLUMN_MESSAGE_ID + " = ?", arrayOf(model.id))

        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
    }

    fun removeRoom(roomId: String) {
        mDatabase.beginTransaction()

        mDatabase.delete(ROOM_TABLE, COLUMN_ROOM_ID + " = ?", arrayOf(roomId))

        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
    }

    private inner class DBWorker(context: Context, name: String, version: Int) : SQLiteOpenHelper(context, name, null, version) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE " + ROOM_TABLE + " ("
                    + COLUMN_ID + " integer primary key autoincrement,"
                    + COLUMN_ROOM_ID + " text unique,"
                    + COLUMN_NAME + " text,"
                    + COLUMN_TOPIC + " text,"
                    + COLUMN_USERS_IDS + " text,"
                    + COLUMN_URI + " text,"
                    + COLUMN_ONE_TO_ONE + " integer,"
                    + COLUMN_USERS_COUNT + " integer,"
                    + COLUMN_UNREAD_ITEMS + " integer,"
                    + COLUMN_HIDE + " integer,"
                    + COLUMN_LIST_POSITION + " integer,"
                    + COLUMN_URL + " text,"
                    + COLUMN_VERSION + " integer);")

            db.execSQL("CREATE TABLE " + MESSAGES_TABLE + " ("
                    + COLUMN_ID + " integer primary key autoincrement,"
                    + COLUMN_MESSAGE_ID + " text unique,"
                    + COLUMN_ROOM_ID + " text,"
                    + COLUMN_TEXT + " text,"
                    + COLUMN_HTML + " text,"
                    + COLUMN_SENT + " text,"
                    + COLUMN_EDITED_AT + " text,"
                    + COLUMN_FROM_USER_ID + " text,"
                    + COLUMN_URLS + " text,"
                    + COLUMN_UNREAD + " integer,"
                    + COLUMN_READ_BY + " integer,"
                    + COLUMN_VERSION + " integer);")

            db.execSQL("CREATE TABLE " + CACHED_MESSAGES_TABLE + " ("
                    + COLUMN_ID + " integer primary key autoincrement,"
                    + COLUMN_MESSAGE_ID + " text unique,"
                    + COLUMN_ROOM_ID + " text,"
                    + COLUMN_TEXT + " text,"
                    + COLUMN_HTML + " text,"
                    + COLUMN_SENT + " text,"
                    + COLUMN_EDITED_AT + " text,"
                    + COLUMN_FROM_USER_ID + " text,"
                    + COLUMN_URLS + " text,"
                    + COLUMN_UNREAD + " integer,"
                    + COLUMN_READ_BY + " integer,"
                    + COLUMN_VERSION + " integer);")

            db.execSQL("CREATE TABLE " + USERS_TABLE + " ("
                    + COLUMN_ID + " integer primary key autoincrement,"
                    + COLUMN_USER_ID + " text unique,"
                    + COLUMN_USERNAME + " text,"
                    + COLUMN_DISPLAY_NAME + " text,"
                    + COLUMN_AVATAR_SMALL_URL + " text,"
                    + COLUMN_AVATAR_MEDIUM_URL + " text);")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            when (oldVersion) {
                1 -> {
                    db.beginTransaction()

                    try {
                        db.execSQL("ALTER TABLE " + ROOM_TABLE
                                + " ADD COLUMN " + COLUMN_HIDE + " integer," +
                                COLUMN_LIST_POSITION + " integer")

                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
                2 -> db.execSQL("CREATE TABLE " + CACHED_MESSAGES_TABLE + " ("
                        + COLUMN_ID + " integer primary key autoincrement,"
                        + COLUMN_MESSAGE_ID + " text unique,"
                        + COLUMN_ROOM_ID + " text,"
                        + COLUMN_TEXT + " text,"
                        + COLUMN_HTML + " text,"
                        + COLUMN_SENT + " text,"
                        + COLUMN_EDITED_AT + " text,"
                        + COLUMN_FROM_USER_ID + " text,"
                        + COLUMN_URLS + " text,"
                        + COLUMN_UNREAD + " integer,"
                        + COLUMN_READ_BY + " integer,"
                        + COLUMN_VERSION + " integer);")
            }
        }
    }

    companion object {
        val DB_VERSION = 3
        val DB_NAME = "gitter_db"

        val ROOM_TABLE = "room"
        val MESSAGES_TABLE = "messages"
        // This table for messages that comes from service and not shown to user
        val CACHED_MESSAGES_TABLE = "cached_messages"
        val USERS_TABLE = "users"

        val COLUMN_ID = "_id"
        val COLUMN_ROOM_ID = "room_id"
        val COLUMN_NAME = "name"
        val COLUMN_TOPIC = "topic"
        val COLUMN_URI = "uri"
        val COLUMN_ONE_TO_ONE = "one_to_one"
        val COLUMN_USERS_COUNT = "user_count"
        val COLUMN_UNREAD_ITEMS = "unread_items"
        val COLUMN_URL = "url"
        val COLUMN_VERSION = "version"
        val COLUMN_USERS_IDS = "users_ids"
        val COLUMN_HIDE = "hideRoom"
        val COLUMN_LIST_POSITION = "list_pos"

        val COLUMN_MESSAGE_ID = "message_id"
        val COLUMN_TEXT = "text"
        val COLUMN_HTML = "html"
        val COLUMN_SENT = "sent"
        val COLUMN_EDITED_AT = "edited_at"
        val COLUMN_FROM_USER_ID = "from_user_id"
        val COLUMN_UNREAD = "unread"
        val COLUMN_READ_BY = "read_by"
        val COLUMN_URLS = "urls"

        val COLUMN_USER_ID = "user_id"
        val COLUMN_USERNAME = "username"
        val COLUMN_DISPLAY_NAME = "display_name"
        val COLUMN_AVATAR_SMALL_URL = "avatar_small_url"
        val COLUMN_AVATAR_MEDIUM_URL = "avatar_medium_url"
    }
}
