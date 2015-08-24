package com.ne1c.gitterclient.Activities;


import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.makeramen.roundedimageview.RoundedImageView;
import com.ne1c.gitterclient.Fragments.ChatRoomFragment;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.Models.UserModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Utils;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ArrayList<RoomModel>> {

    private NavigationView mNavView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private RoundedImageView mNavAvatar;
    private TextView mNavNickname;

    private ArrayList<RoomModel> mRoomsList;

    private RestAdapter mRestAdapter;

    private final int LOAD_ROOM_ID = 001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setNavigationView();

        mRestAdapter = new RestAdapter.Builder()
                .setEndpoint(Utils.getInstance().GITTER_API_URL)
                .build();

        boolean auth = getIntent().getBooleanExtra("auth", true);

        loadHeaderNavView(auth);

        getFragmentManager().beginTransaction().replace(R.id.fragment_container, new ChatRoomFragment()).commit();

        if (Utils.getInstance().isNetworkConnected()) {
            getLoaderManager().initLoader(LOAD_ROOM_ID, null, this).forceLoad();
        }
    }

    private void setNavigationView() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.parent_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.app_name, R.string.app_name);
        mNavView = (NavigationView) findViewById(R.id.navigation_view);
        mNavAvatar = (RoundedImageView) mNavView.findViewById(R.id.avatar_header_image);
        mNavNickname = (TextView) mNavView.findViewById(R.id.nickname_header_text);

        mNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                if (menuItem.getGroupId() == R.id.rooms_group && menuItem.getItemId() != R.id.home_nav_item) {
                    for (int i = 0; i < mRoomsList.size(); i++) {
                        if (menuItem.getTitle().equals(mRoomsList.get(i).name)) {
                            EventBus.getDefault().post(mRoomsList.get(i));
                        }
                    }
                }

                menuItem.setChecked(true);
                mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        mDrawerToggle.syncState();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                if (!mNavView.isShown()) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                } else {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<ArrayList<RoomModel>> onCreateLoader(int id, Bundle args) {
        Loader<ArrayList<RoomModel>> loader = null;
        switch (id) {
            case LOAD_ROOM_ID:
                loader = new RoomAsyncLoader(getApplicationContext());
                return loader;
            default: return loader;
        }
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<RoomModel>> loader, ArrayList<RoomModel> data) {
        mRoomsList = data;

        for (RoomModel room : data) {
            MenuItem item = mNavView.getMenu().add(R.id.rooms_group, 0, Menu.NONE, room.name);
            item.setIcon(R.mipmap.ic_room);
        }

        if (data.size() > 0) {
            EventBus.getDefault().post(data.get(0));
            mNavView.getMenu().getItem(1).setChecked(true);
        }

        mNavView.getMenu().setGroupCheckable(R.id.rooms_group, true, true);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<RoomModel>> loader) {

    }

    private void loadHeaderNavView(boolean auth) {
        if (auth) {
            final UserModel model = Utils.getInstance().getUserPref();

            if (model != null) {
                ImageLoader.getInstance().displayImage(model.avatarUrlMedium, mNavAvatar);
                mNavNickname.setText(model.displayName);
                mNavAvatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(Utils.getInstance().GITHUB_URL + model.url)));
                    }
                });
            }

            IApiMethods methods = mRestAdapter.create(IApiMethods.class);
            methods.getCurrentUser(new Callback<ArrayList<UserModel>>() {
                @Override
                public void success(final ArrayList<UserModel> userModel, Response response) {
                    Utils.getInstance().writeUserToPref(userModel.get(0));
                    ImageLoader.getInstance().displayImage(userModel.get(0).avatarUrlMedium, mNavAvatar);
                    mNavNickname.setText(userModel.get(0).displayName);
                    mNavAvatar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(Utils.getInstance().GITHUB_URL + userModel.get(0).url)));
                        }
                    });
                }

                @Override
                public void failure(RetrofitError error) {
                    Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    static class RoomAsyncLoader extends AsyncTaskLoader<ArrayList<RoomModel>> {

        private RestAdapter mAdapter;

        public RoomAsyncLoader(Context context) {
            super(context);

            mAdapter = new RestAdapter.Builder()
                    .setEndpoint(Utils.getInstance().GITTER_API_URL)
                    .build();
        }

        @Override
        public ArrayList<RoomModel> loadInBackground() {
            IApiMethods methods = mAdapter.create(IApiMethods.class);
            ArrayList<RoomModel> list = methods.getCuurentUserRooms();

            return list;
        }
    }
}
