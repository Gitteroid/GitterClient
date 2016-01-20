package com.ne1c.developerstalk;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingResource;
import android.text.format.DateUtils;

import java.util.concurrent.TimeUnit;

public class BaseTest {
    protected IdlingResource waitFor(long seconds) {
        long waitingTime = DateUtils.SECOND_IN_MILLIS * seconds;

        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(waitingTime * 2, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(waitingTime * 2, TimeUnit.MILLISECONDS);

        // Now we wait
        IdlingResource idlingResource = new ElapsedTimeIdlingResource(waitingTime);
        Espresso.registerIdlingResources(idlingResource);

        return idlingResource;
    }
}
