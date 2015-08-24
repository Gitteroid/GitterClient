package com.ne1c.gitterclient.Models;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageModel {
    public String id;
    public String text;
    public String html;
    public String sent;
    public String editedAt;
    public UserModel fromUser;
    public boolean unread;
    public int readBy;
    //public List<String> urls;
    public List<Mentions> mentions;
    //public String issues;
    public int v;

    public static class Mentions {
        public String screenName;
        public String userId;
    }
}
