package com.ne1c.developerstalk.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.ne1c.developerstalk.ui.DrawShadowFrameLayout;
import com.ne1c.developerstalk.ui.fragments.RoomsListFragment;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.services.NewMessagesService;
import com.ne1c.developerstalk.utils.UIUtils;
import com.ne1c.developerstalk.utils.Utils;

public class RoomsActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private RoomsListFragment mRoomsListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Utils.getInstance().getAccessToken().isEmpty()) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        setContentView(R.layout.activity_rooms);

        // User data already exist
        if (!Utils.getInstance().getUserPref().id.isEmpty()) {
            startService(new Intent(getApplicationContext(), NewMessagesService.class));
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mRoomsListFragment = (RoomsListFragment) getFragmentManager().findFragmentByTag("roomsList");

        if (mRoomsListFragment == null) {
            mRoomsListFragment = new RoomsListFragment();
            getFragmentManager().beginTransaction().add(mRoomsListFragment, "roomsList").commit();
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mRoomsListFragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_room, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit_room) {
            mRoomsListFragment.setEdit(!mRoomsListFragment.isEdit());
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        int actionBarSize = UIUtils.calculateActionBarSize(this);
        DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) findViewById(R.id.shadow_layout);
        if (drawShadowFrameLayout != null) {
            drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
        }

        UIUtils.setContentTopClearance(findViewById(R.id.fragment_container), actionBarSize);
    }
}
