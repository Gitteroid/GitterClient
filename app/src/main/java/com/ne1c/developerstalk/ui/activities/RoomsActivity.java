package com.ne1c.developerstalk.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.ui.DrawShadowFrameLayout;
import com.ne1c.developerstalk.ui.fragments.RoomsListFragment;
import com.ne1c.developerstalk.utils.UIUtils;
import com.ne1c.developerstalk.utils.Utils;

public class RoomsActivity extends AppCompatActivity {
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRoomsListFragment = (RoomsListFragment) getFragmentManager().findFragmentByTag("roomsList");

        if (mRoomsListFragment == null) {
            mRoomsListFragment = new RoomsListFragment();
            getFragmentManager().beginTransaction().add(mRoomsListFragment, "roomsList").commit();
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mRoomsListFragment).commit();
        }

        Utils.getInstance().startNotificationService();

        if (!Utils.getInstance().isNetworkConnected()) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_room, menu);

        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.action_search_room), new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mRoomsListFragment.startSearch();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mRoomsListFragment.endSearch();
                return true;
            }
        });

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search_room).getActionView();

        //searchView.setOnSearchClickListener(v -> mRoomsListFragment.startSearch());
        searchView.setQueryHint(getString(R.string.search));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!mRoomsListFragment.isEdit() && Utils.getInstance().isNetworkConnected()) {
                    mRoomsListFragment.searchRoomsQuery(newText);
                }

                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit_room) {
            mRoomsListFragment.setEdit(!mRoomsListFragment.isEdit());
        }

        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
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
