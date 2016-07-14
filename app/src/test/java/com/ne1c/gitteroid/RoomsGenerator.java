package com.ne1c.gitteroid;

import com.ne1c.gitteroid.models.data.RoomModel;

import java.util.ArrayList;
import java.util.Random;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;

public class RoomsGenerator {
    public static ArrayList<RoomModel> generateList(int count) {
        ArrayList<RoomModel> list = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            list.add(generateRoom());
        }

        return list;
    }

    public static RoomModel generateRoom() {
        RoomModel room = new RoomModel();

        room.id = anyString();
        room.oneToOne = new Random().nextBoolean();
        room.name = anyString();
        room.unreadItems = anyInt();

        return room;
    }
}
