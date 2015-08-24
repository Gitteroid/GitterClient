package com.ne1c.gitterclient.Models;

import java.util.ArrayList;

public class RoomModel {
    public String id;
    public String name;
    public String topic;
    public boolean oneToOne;
    public ArrayList<UserModel> users = new ArrayList<>();
    public int unreadItems;
    public int userCount;
    public int mentions;
    public String lastAccessTime;
    public boolean lurk;
    public String url;
    public String githubType;
    public int v;
}
