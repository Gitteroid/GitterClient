package com.ne1c.gitteroid;

import com.ne1c.gitteroid.models.data.RoomModel;

import java.util.ArrayList;
import java.util.Random;

public class RoomsGenerator {
    public static final String ROOM_ID = "abc123";
    public static ArrayList<RoomModel> generateList(int count) {
        ArrayList<RoomModel> list = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            list.add(generateRoom());
        }

        return list;
    }

    public static RoomModel generateRoom() {
        RoomModel room = new RoomModel();

        room.id = ROOM_ID;
        room.oneToOne = new Random().nextBoolean();
        room.name = "321abc";
        room.unreadItems = new Random().nextInt(10);

        return room;
    }
}
