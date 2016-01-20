package com.ne1c.developerstalk.ui.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.services.NewMessagesService;
import com.ne1c.developerstalk.ui.fragments.PreferencesFragment;
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

        FrameLayout fragmentContainer = new FrameLayout(getApplicationContext());
        fragmentContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        parent.addView(fragmentContainer);
        fragmentContainer.setId(FRAGMENT_CONTAINER_ID);

        getFragmentManager().beginTransaction().replace(fragmentContainer.getId(), new PreferencesFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        // Restart service for update prefs in service
        stopService(new Intent(this, NewMessagesService.class));
        startService(new Intent(this, NewMessagesService.class));
        super.finish();
    }

    private void showDialog() {
        new AlertDialog.Builder(SettingsActivity.this)
                .setTitle("Warning!")
                .setMessage(R.string.changes)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SettingsActivity.super.finish();
                    }
                }).show();
    }
}
