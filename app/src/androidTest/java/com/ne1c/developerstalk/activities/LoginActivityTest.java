package com.ne1c.developerstalk.activities;

import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.ne1c.developerstalk.BaseTest;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.ui.activities.LoginActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest extends BaseTest {

    @Rule
    public ActivityTestRule<LoginActivity> mActivityRule = new ActivityTestRule<>(LoginActivity.class);

    @Test
    public void buttonSignInTest() {
        onView(ViewMatchers.withId(R.id.auth_but)).perform(click());
        onView(withId(R.id.auth_but)).check(matches(not(isDisplayed())));

        // Wait for load page
        pause(20000);

        onView(withId(R.id.auth_webView)).check(matches(isDisplayed()));

        onWebView(withId(R.id.auth_webView))
                .withElement(findElement(Locator.CLASS_NAME, "login"))
                .check(webMatches(getText(), containsString("EXISTING USER LOGIN")));

        onWebView(withId(R.id.auth_webView))
                .withElement(findElement(Locator.PARTIAL_LINK_TEXT, "EXISTING USER LOGIN"))
                .perform(webClick());

        // Wait for load page
        pause(20000);

        onWebView(withId(R.id.auth_webView))
                .withElement(findElement(Locator.ID, "bubble auth-form-container"))
                .check(webMatches(getText(), containsString("Sign in to GitHub")));


    }
}
