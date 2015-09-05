package com.ne1c.gitterclient.Activities;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ne1c.gitterclient.Fragments.PreferencesFragment;

import java.util.Random;

public class SettingsActivity extends AppCompatActivity {

    private final int FRAGMENT_CONTAINER_ID = Integer.valueOf(666);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout parent = new FrameLayout(getApplicationContext());
        parent.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(parent);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        FrameLayout fragmentContaiter = new FrameLayout(getApplicationContext());
        fragmentContaiter.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        parent.addView(fragmentContaiter);
        fragmentContaiter.setId(FRAGMENT_CONTAINER_ID);

        getFragmentManager().beginTransaction().replace(fragmentContaiter.getId(), new PreferencesFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
