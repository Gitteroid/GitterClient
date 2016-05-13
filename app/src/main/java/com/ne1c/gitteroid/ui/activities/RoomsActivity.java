package com.ne1c.gitteroid.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.ne1c.gitteroid.R;
import com.ne1c.gitteroid.ui.fragments.RoomsListFragment;
import com.ne1c.gitteroid.utils.Utils;

public class RoomsActivity extends AppCompatActivity {
    private RoomsListFragment mRoomsListFragment;
    private Toolbar mToolbar;

    private String mSearchQuery = null;

    private boolean mRestoreSearch = false;

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

        Utils.getInstance().startNotificationService();

        if (!Utils.getInstance().isNetworkConnected()) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_room, menu);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search_room));

        searchView.setQueryHint(getString(R.string.search));

        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.action_search_room), new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (!mRoomsListFragment.isEdit()) {
                    mRoomsListFragment.startSearch();
                    return true;
                } else {
                    Toast.makeText(getApplicationContext(), R.string.end_edit_list, Toast.LENGTH_SHORT).show();
                }

                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mRoomsListFragment.endSearch();
                mSearchQuery = null;
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (mRestoreSearch) {
                    mRestoreSearch = false;
                    return true;
                }

                if (!mRoomsListFragment.isEdit() && Utils.getInstance().isNetworkConnected()) {
                    mRoomsListFragment.searchRoomsQuery(newText);
                    mSearchQuery = newText;
                }

                return true;
            }
        });

        if (mSearchQuery != null) {
            menu.findItem(R.id.action_search_room).expandActionView();
            searchView.setQuery(mSearchQuery, false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit_room) {
            mRoomsListFragment.setEdit(!mRoomsListFragment.isEdit());

            if (mRoomsListFragment.isEdit()) {
                item.setTitle(R.string.save_list_room);
            } else {
                item.setTitle(R.string.edit_list_room);
            }
        }

        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("search", mSearchQuery);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mSearchQuery = savedInstanceState.getString("search", null);
        mRestoreSearch = true;
    }
}
