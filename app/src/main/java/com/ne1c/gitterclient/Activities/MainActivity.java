package com.ne1c.gitterclient.Activities;


import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.Models.UserModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Services.NewMessagesService;
import com.ne1c.gitterclient.Utils;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ArrayList<RoomModel>> {

    public final static String BROADCAST_NEW_MESSAGE = "com.ne1c.gitterclient.NewMessageReceiver";
    public final static String BROADCAST_CHANGE_USER = "com.ne1c.gitterclient.ChangeUserReceiver";

    public final String SELECT_NAV_ITEM_BUNDLE = "select_nav_item";
    public final String ROOMS_BUNDLE = "rooms_bundle";
    private final int LOAD_ROOM_ID = 1;

    private ChatRoomFragment mChatRoomFragment;

    private NavigationView mNavView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private RoundedImageView mNavAvatar;
    private TextView mNavNickname;

    private ArrayList<RoomModel> mRoomsList;
    private RoomModel mActiveRoom;

    private RestAdapter mRestAdapter;

    private int selectedNavItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(newMessageReceiver, new IntentFilter(BROADCAST_NEW_MESSAGE));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setNavigationView();

        mRestAdapter = new RestAdapter.Builder()
                .setEndpoint(Utils.getInstance().GITTER_API_URL)
                .build();

        loadHeaderNavView();

        mChatRoomFragment = new ChatRoomFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, mChatRoomFragment).commit();

        if (savedInstanceState == null) {
            if (Utils.getInstance().isNetworkConnected()) {
                getLoaderManager().initLoader(LOAD_ROOM_ID, null, this).forceLoad();
            }
        } else {
            selectedNavItem = savedInstanceState.getInt(SELECT_NAV_ITEM_BUNDLE);
            mRoomsList = savedInstanceState.getParcelableArrayList(ROOMS_BUNDLE);

            for (RoomModel room : mRoomsList) {
                MenuItem item = mNavView.getMenu().add(R.id.rooms_group, 0, Menu.NONE, room.name);
                item.setIcon(R.mipmap.ic_room);
            }

            if (mRoomsList.size() > 0) {
                EventBus.getDefault().post(mRoomsList.get(selectedNavItem - 1));
                mNavView.getMenu().getItem(selectedNavItem).setChecked(true);

                mActiveRoom = mRoomsList.get(selectedNavItem - 1);

                setTitle(mNavView.getMenu().getItem(selectedNavItem).getTitle());
            }

            mNavView.getMenu().setGroupCheckable(R.id.rooms_group, true, true);
            mNavView.getMenu().getItem(0).setCheckable(false);
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
                if (menuItem.getItemId() == R.id.home_nav_item) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://gitter.im/home")));
                }

                if (menuItem.getGroupId() == R.id.rooms_group && menuItem.getItemId() != R.id.home_nav_item) {
                    for (int i = 0; i < mRoomsList.size(); i++) {
                        if (menuItem.getTitle().equals(mRoomsList.get(i).name)) {
                            selectedNavItem = i + 1; // Because item in navigation menu
                            mActiveRoom = mRoomsList.get(selectedNavItem - 1);

                            mNavView.getMenu().getItem(selectedNavItem).setChecked(true);
                            setTitle(mNavView.getMenu().getItem(selectedNavItem).getTitle());

                            EventBus.getDefault().post(mRoomsList.get(i));
                        }
                    }

                    menuItem.setChecked(true);
                }

                mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        mDrawerToggle.syncState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECT_NAV_ITEM_BUNDLE, selectedNavItem);
        outState.putParcelableArrayList(ROOMS_BUNDLE, mRoomsList);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public Loader<ArrayList<RoomModel>> onCreateLoader(int id, Bundle args) {
        Loader<ArrayList<RoomModel>> loader = null;
        switch (id) {
            case LOAD_ROOM_ID:
                loader = new RoomAsyncLoader(getApplicationContext());
                return loader;
            default:
                return loader;
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
            selectedNavItem = 1;

            // If activity open from notification
            if (getIntent().getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY) == null) {
                mActiveRoom = mRoomsList.get(selectedNavItem - 1);
            } else {
                mActiveRoom = getIntent().getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY);

                for (int i = 0; i < mRoomsList.size(); i++) {
                    if (mRoomsList.get(i).id.equals(mActiveRoom.id)) {
                        selectedNavItem = i + 1;
                    }
                }
            }

            EventBus.getDefault().post(data.get(selectedNavItem - 1));
            mNavView.getMenu().getItem(selectedNavItem).setChecked(true);
            setTitle(mNavView.getMenu().getItem(selectedNavItem).getTitle());
        }

        mNavView.getMenu().setGroupCheckable(R.id.rooms_group, true, true);
        mNavView.getMenu().getItem(0).setCheckable(false);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<RoomModel>> loader) {

    }

    private void loadHeaderNavView() {
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
        methods.getCurrentUser(Utils.getInstance().getBearer(), new Callback<ArrayList<UserModel>>() {
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
            return methods.getCurrentUserRooms(Utils.getInstance().getBearer());
        }
    }

    @Override
    protected void onDestroy() {
        getLoaderManager().destroyLoader(LOAD_ROOM_ID);
        unregisterReceiver(newMessageReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MessageModel message = intent.getParcelableExtra(NewMessagesService.NEW_MESSAGE_EXTRA_KEY);
            RoomModel room = intent.getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY);

            if (room.id.equals(mActiveRoom.id)) {
                mChatRoomFragment.newMessage(message);
            }
        }
    };

    public interface NewMessageFragmentCallback {
        void newMessage(MessageModel model);
    }
}
