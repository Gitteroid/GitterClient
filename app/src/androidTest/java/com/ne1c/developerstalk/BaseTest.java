package com.ne1c.developerstalk;

public class BaseTest {
    public void pause(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
