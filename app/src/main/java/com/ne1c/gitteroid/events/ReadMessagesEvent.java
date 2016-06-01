package com.ne1c.gitteroid.events;

// Class for send event via EventBus
// This class contains count messages was read by user
public class ReadMessagesEvent {
    public int countRead;
    public String roomId;
}
