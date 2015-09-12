package com.ne1c.gitterclient.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.Models.UserModel;

import java.util.ArrayList;

public class ClientDatabase {

    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "gitter_db";

    public static final String ROOM_TABLE = "room";
    public static final String MESSAGES_TABLE = "messages";
    public static final String USERS_TABLE = "users";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_ROOM_ID = "room_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_TOPIC = "topic";
    public static final String COLUMN_URI = "uri";
    public static final String COLUMN_ONE_TO_ONE = "one_to_one";
    public static final String COLUMN_USERS_COUNT = "user_count";
    public static final String COLUMN_UNREAD_ITEMS = "unread_items";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_USERS_IDS = "users_ids";

    public static final String COLUMN_MESSAGE_ID = "message_id";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_HTML = "html";
    public static final String COLUMN_SENT = "sent";
    public static final String COLUMN_EDITED_AT = "edited_at";
    public static final String COLUMN_FROM_USER_ID = "from_user_id";
    public static final String COLUMN_UNREAD = "unread";
    public static final String COLUMN_READ_BY = "read_by";

    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_DISPLAY_NAME = "display_name";
    public static final String COLUMN_AVATAR_SMALL_URL = "avatar_small_url";
    public static final String COLUMN_AVATAR_MEDIUM_URL = "avatar_medium_url";

    private DBWorker mDBWorker;
    private SQLiteDatabase mDatabase;

    public ClientDatabase(Context context) {
        mDBWorker = new DBWorker(context, DB_NAME, DB_VERSION);
        mDatabase = mDBWorker.getWritableDatabase();
    }

    public ArrayList<RoomModel> getRooms() {
        ArrayList<RoomModel> list = new ArrayList<>();
        Cursor cursor = mDatabase.query(ROOM_TABLE, null, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            int columnId = cursor.getColumnIndex(COLUMN_ROOM_ID);
            int columnName = cursor.getColumnIndex(COLUMN_NAME);
            int columnTopic = cursor.getColumnIndex(COLUMN_TOPIC);
            int columnUsersIds = cursor.getColumnIndex(COLUMN_USERS_IDS);
            int columnOneToOne = cursor.getColumnIndex(COLUMN_ONE_TO_ONE);
            int columnUsersCount = cursor.getColumnIndex(COLUMN_USERS_COUNT);
            int columnUnreadItems = cursor.getColumnIndex(COLUMN_UNREAD_ITEMS);
            int columnUrl = cursor.getColumnIndex(COLUMN_URL);
            int columnVersion = cursor.getColumnIndex(COLUMN_VERSION);

            do {
                RoomModel model = new RoomModel();
                model.id = cursor.getString(columnId);
                model.name = cursor.getString(columnName);
                model.topic = cursor.getString(columnTopic);
                model.oneToOne = cursor.getInt(columnOneToOne) == 1;
                model.userCount = cursor.getInt(columnUsersCount);
                model.unreadItems = cursor.getInt(columnUnreadItems);
                model.url = cursor.getString(columnUrl);
                model.v = cursor.getInt(columnVersion);

                String idsStr = cursor.getString(columnUsersIds);
                String[] idsArr = getUsersIds(idsStr);
                model.users = getUsers(idsArr);

                list.add(model);
            } while (cursor.moveToNext());
        }

        return list;
    }

    public UserModel getUser(String userId) {
        Cursor cursor = mDatabase.query(USERS_TABLE, null, COLUMN_USER_ID + " = ?",
                new String[]{userId}, null, null, null, null);

        if (cursor.moveToFirst()) {
            int columnUserId = cursor.getColumnIndex(COLUMN_USER_ID);
            int columnUsername = cursor.getColumnIndex(COLUMN_USERNAME);
            int columnDisplayname = cursor.getColumnIndex(COLUMN_DISPLAY_NAME);
            int columnAvatarSmall = cursor.getColumnIndex(COLUMN_AVATAR_SMALL_URL);
            int columnAvatarMedium = cursor.getColumnIndex(COLUMN_AVATAR_MEDIUM_URL);

            do {
                UserModel model = new UserModel();
                model.id = cursor.getString(columnUserId);
                model.username = cursor.getString(columnUsername);
                model.displayName = cursor.getString(columnDisplayname);
                model.avatarUrlMedium = cursor.getString(columnAvatarMedium);
                model.avatarUrlSmall = cursor.getString(columnAvatarSmall);

                return model;
            } while (cursor.moveToNext());
        }

        return null;
    }

    public ArrayList<UserModel> getUsers(String[] ids) {
        ArrayList<UserModel> list = new ArrayList<>();
        Cursor cursor = mDatabase.query(USERS_TABLE, null, COLUMN_USER_ID + " = ?",
               ids, null, null, null, null);

        if (cursor.moveToFirst()) {
            int columnUserId = cursor.getColumnIndex(COLUMN_USER_ID);
            int columnUsername = cursor.getColumnIndex(COLUMN_USERNAME);
            int columnDisplayname = cursor.getColumnIndex(COLUMN_DISPLAY_NAME);
            int columnAvatarSmall = cursor.getColumnIndex(COLUMN_AVATAR_SMALL_URL);
            int columnAvatarMedium = cursor.getColumnIndex(COLUMN_AVATAR_MEDIUM_URL);

            do {
                UserModel model = new UserModel();
                model.id = cursor.getString(columnUserId);
                model.username = cursor.getString(columnUsername);
                model.displayName = cursor.getString(columnDisplayname);
                model.avatarUrlMedium = cursor.getString(columnAvatarMedium);
                model.avatarUrlSmall = cursor.getString(columnAvatarSmall);

                list.add(model);
            } while (cursor.moveToNext());
        }

        return list;
    }

    public void insertMessage(MessageModel model, String roomId) {
        ArrayList<MessageModel> list = new ArrayList<>();
        list.add(model);
        insertMessages(list, roomId);
    }

    public ArrayList<MessageModel> getMessages(String roomId) {
        ArrayList<MessageModel> list = new ArrayList<>();
        Cursor cursor = mDatabase.query(MESSAGES_TABLE, null, COLUMN_ROOM_ID + " = ?", new String[]{roomId}, null, null, null,"10");

        if (cursor.moveToFirst()) {
            int columnMessageId = cursor.getColumnIndex(COLUMN_MESSAGE_ID);
            int columnText = cursor.getColumnIndex(COLUMN_TEXT);
            int columnHtml = cursor.getColumnIndex(COLUMN_HTML);
            int columnSent = cursor.getColumnIndex(COLUMN_SENT);
            int columnEditedAt = cursor.getColumnIndex(COLUMN_EDITED_AT);
            int columnFromUserId = cursor.getColumnIndex(COLUMN_FROM_USER_ID);
            int columnUnread = cursor.getColumnIndex(COLUMN_UNREAD);
            int columnReadBy = cursor.getColumnIndex(COLUMN_READ_BY);
            int columnVersion = cursor.getColumnIndex(COLUMN_VERSION);

            do {
                MessageModel model = new MessageModel();
                model.id = cursor.getString(columnMessageId);
                model.text = cursor.getString(columnText);
                model.html = cursor.getString(columnHtml);
                model.sent = cursor.getString(columnSent);
                model.editedAt = cursor.getString(columnEditedAt);
                model.fromUser = getUser(cursor.getString(columnFromUserId));
                model.unread = cursor.getInt(columnUnread) == 1;
                model.readBy = cursor.getInt(columnReadBy);
                model.v = cursor.getInt(columnVersion);

                list.add(model);
            } while (cursor.moveToNext());
        }

        return list;
    }

    // Get users ids, from string
    private String[] getUsersIds(String ids) {
        ArrayList<String> list = new ArrayList<>();

        int startIndex = 0;
        for (int i = 0; i < ids.length(); i++) {
            if (ids.equals(';')) {
                list.add(ids.substring(startIndex, i));
                startIndex = i + 1;
            }
        }

        return list.toArray(new String[list.size()]);
    }

    public void insertUsers(ArrayList<UserModel> list) {
        mDatabase.beginTransaction();

        for (UserModel model : list) {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_USER_ID, model.id);
            cv.put(COLUMN_USERNAME, model.username);
            cv.put(COLUMN_DISPLAY_NAME, model.displayName);
            cv.put(COLUMN_AVATAR_SMALL_URL, model.avatarUrlSmall);
            cv.put(COLUMN_AVATAR_MEDIUM_URL, model.avatarUrlMedium);

            mDatabase.insertWithOnConflict(USERS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }

        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    public void insertRooms(ArrayList<RoomModel> list) {
        mDatabase.beginTransaction();

        for (RoomModel model : list) {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_ROOM_ID, model.id);
            cv.put(COLUMN_NAME, model.name);
            cv.put(COLUMN_TOPIC, model.topic);
            cv.put(COLUMN_ONE_TO_ONE, model.oneToOne ? 1 : 0);
            cv.put(COLUMN_USERS_COUNT, model.userCount);
            cv.put(COLUMN_UNREAD_ITEMS, model.unreadItems);
            cv.put(COLUMN_URL, model.url);
            cv.put(COLUMN_VERSION, model.v);

            String userIds = "";
            for (UserModel user : model.users) {
                userIds += user.id + ";";
            }
            cv.put(COLUMN_USERS_IDS, userIds);

            mDatabase.insertWithOnConflict(ROOM_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }

        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    public void insertMessages(ArrayList<MessageModel> list, String roomId) {
        // Write users once, that will not write, after every iteration of loop
        ArrayList<UserModel> users = new ArrayList<>();

        mDatabase.beginTransaction();

        for (MessageModel model : list) {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_MESSAGE_ID, model.id);
            cv.put(COLUMN_ROOM_ID, roomId);
            cv.put(COLUMN_TEXT, model.text);
            cv.put(COLUMN_HTML, model.html);
            cv.put(COLUMN_SENT, model.sent);
            cv.put(COLUMN_EDITED_AT, model.editedAt);
            cv.put(COLUMN_FROM_USER_ID, model.fromUser.id);
            cv.put(COLUMN_UNREAD, model.unread ? 1 : 0);
            cv.put(COLUMN_READ_BY, model.readBy);
            cv.put(COLUMN_VERSION, model.v);

            users.add(model.fromUser);

            mDatabase.insertWithOnConflict(MESSAGES_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }

        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        insertUsers(users);
    }

    public void close() {
        mDatabase.close();
    }

    private class DBWorker extends SQLiteOpenHelper {
        public DBWorker(Context context, String name, int version) {
            super(context, name, null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
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
                    + COLUMN_URL + " text,"
                    + COLUMN_VERSION + " integer);");

            db.execSQL("CREATE TABLE " + MESSAGES_TABLE + " ("
                    + COLUMN_ID + " integer primary key autoincrement,"
                    + COLUMN_MESSAGE_ID + " text unique,"
                    + COLUMN_ROOM_ID + " text,"
                    + COLUMN_TEXT + " text,"
                    + COLUMN_HTML + " text,"
                    + COLUMN_SENT + " text,"
                    + COLUMN_EDITED_AT + " text,"
                    + COLUMN_FROM_USER_ID + " text,"
                    + COLUMN_UNREAD + " integer,"
                    + COLUMN_READ_BY + " integer,"
                    + COLUMN_VERSION + " integer);");

            db.execSQL("CREATE TABLE " + USERS_TABLE + " ("
                    + COLUMN_ID + " integer primary key autoincrement,"
                    + COLUMN_USER_ID + " text unique,"
                    + COLUMN_USERNAME + " text,"
                    + COLUMN_DISPLAY_NAME + " text,"
                    + COLUMN_AVATAR_SMALL_URL + " text,"
                    + COLUMN_AVATAR_MEDIUM_URL + " text);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
