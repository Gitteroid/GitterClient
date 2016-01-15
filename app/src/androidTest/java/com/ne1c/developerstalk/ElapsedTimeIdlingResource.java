package com.ne1c.developerstalk;

import android.support.test.espresso.IdlingResource;

public class ElapsedTimeIdlingResource implements IdlingResource {
    private final long mStartTime;
    private final long mWaitingTime;

    private ResourceCallback mResourceCallback;

    public ElapsedTimeIdlingResource(long waitingTime) {
        mWaitingTime = waitingTime;
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return ElapsedTimeIdlingResource.class.getName() + ":" + mWaitingTime;
    }

    @Override
    public boolean isIdleNow() {
        long elapsed = System.currentTimeMillis() - mStartTime;
        boolean idle = (elapsed >= mWaitingTime);

        if (idle) {
            mResourceCallback.onTransitionToIdle();
        }

        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        mResourceCallback = callback;
    }
}
