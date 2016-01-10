package com.ne1c.developerstalk;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;

import com.ne1c.developerstalk.ui.activities.LoginActivity;

import org.junit.Before;

public class LoginActivityTest extends ActivityInstrumentationTestCase2<LoginActivity> {
    private LoginActivity mActivity;

    public LoginActivityTest(Class<LoginActivity> activityClass) {
        super(activityClass);
    }

    @Before
    protected void setUp() throws Exception {
        super.setUp();

        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mActivity = getActivity();
    }
}
