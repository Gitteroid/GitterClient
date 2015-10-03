package com.ne1c.developerstalk.EventBusModels;

// Class for send event via EventBus
// This class contains count messages was read by user
public class ReadMessagesEventBus {
    private int mCountRead;

    public int getCountRead() {
        return mCountRead;
    }

    public void setCountRead(int countRead) {
        mCountRead = countRead;
    }
}
