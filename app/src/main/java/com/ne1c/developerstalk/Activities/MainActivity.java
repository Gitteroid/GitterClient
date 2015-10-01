package com.ne1c.developerstalk.Activities;


import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.BitmapTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.ne1c.developerstalk.Database.ClientDatabase;
import com.ne1c.developerstalk.Fragments.ChatRoomFragment;
import com.ne1c.developerstalk.Models.MessageModel;
import com.ne1c.developerstalk.Models.RoomModel;
import com.ne1c.developerstalk.Models.UserModel;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.RetrofitServices.IApiMethods;
import com.ne1c.developerstalk.Services.NewMessagesService;
import com.ne1c.developerstalk.Utils;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ArrayList<RoomModel>> {
    public final static String BROADCAST_NEW_MESSAGE = "com.ne1c.gitterclient.NewMessageReceiver";
    public final static String BROADCAST_MESSAGE_DELIVERED = "com.ne1c.gitterclient.MessageDeliveredReceiver";
    public final static String BROADCAST_UNATHORIZED = "com.ne1c.gitterclient.UnathorizedReceiver";

    private final String SELECT_NAV_ITEM_BUNDLE = "select_nav_item";
    private final String ROOMS_BUNDLE = "rooms_bundle";
    private final int LOAD_ROOM_ID = 1;
    private final int LOAD_ROOM_DATABASE_ID = 2;

    private ChatRoomFragment mChatRoomFragment;

    private ActionBarDrawerToggle mDrawerToggle;

    private Drawer mDrawer;
    private ArrayList<IDrawerItem> mDrawerItems = new ArrayList<>();
    private AccountHeader mAccountHeader;
    private IProfile mMainProfile;

    private ArrayList<RoomModel> mRoomsList;
    private RoomModel mActiveRoom;

    private RestAdapter mRestAdapter;
    private ClientDatabase mClientDatabase;

    private int selectedNavItem;
    private boolean loadAvatarFromNetworkFlag;

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

        // User data already exist
        if (!Utils.getInstance().getUserPref().id.isEmpty()) {
            startService(new Intent(getApplicationContext(), NewMessagesService.class));
        }

        setContentView(R.layout.activity_main);

        init();
        initSavedInstaneState(savedInstanceState);
    }

    private void init() {
        registerReceiver(newMessageReceiver, new IntentFilter(BROADCAST_NEW_MESSAGE));
        registerReceiver(messageDeliveredReceiver, new IntentFilter(BROADCAST_MESSAGE_DELIVERED));
        registerReceiver(unauthorizedReceiver, new IntentFilter(BROADCAST_UNATHORIZED));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        mRestAdapter = new RestAdapter.Builder()
                .setEndpoint(Utils.GITTER_API_URL)
                .build();

        mClientDatabase = new ClientDatabase(getApplicationContext());

        mChatRoomFragment = (ChatRoomFragment) getFragmentManager().findFragmentByTag("chatRoom");

        if (mChatRoomFragment == null) {
            mChatRoomFragment = new ChatRoomFragment();
            getFragmentManager().beginTransaction().add(mChatRoomFragment, "chatRoom").commit();
        }

        setNavigationView();
    }

    private void initSavedInstaneState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mChatRoomFragment).commit();
            getLoaderManager().initLoader(LOAD_ROOM_DATABASE_ID, null, this).forceLoad();

            if (Utils.getInstance().isNetworkConnected()) {
                getLoaderManager().initLoader(LOAD_ROOM_ID, null, this).forceLoad();
            }

            updateUserFromServer();
        } else {
            selectedNavItem = savedInstanceState.getInt(SELECT_NAV_ITEM_BUNDLE);
            mRoomsList = savedInstanceState.getParcelableArrayList(ROOMS_BUNDLE);

            BadgeStyle badgeStyle = new BadgeStyle(getResources().getColor(R.color.md_green_500),
                    getResources().getColor(R.color.md_green_700));
            badgeStyle.withTextColor(getResources().getColor(android.R.color.white));

            for (RoomModel room : mRoomsList) {
                if (room.unreadItems > 0) {
                    String badgeText = room.unreadItems == 100 ? "99+" : Integer.toString(room.unreadItems);
                    mDrawer.addItemAtPosition(new PrimaryDrawerItem().withIcon(R.mipmap.ic_room).withName(room.name)
                            .withBadge(badgeText)
                            .withBadgeStyle(badgeStyle)
                            .withSelectable(true), mDrawer.getDrawerItems().size() - 3);
                } else {
                    mDrawer.addItemAtPosition(
                            new PrimaryDrawerItem()
                                    .withIcon(R.mipmap.ic_room)
                                    .withName(room.name)
                                    .withBadgeStyle(badgeStyle)
                                    .withSelectable(true), mDrawer.getDrawerItems().size() - 3);
                }
            }

            if (mRoomsList.size() > 0) {
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

                mDrawer.setSelectionAtPosition(selectedNavItem + 1);

                setTitle(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(selectedNavItem)).getName().toString());
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // If get intent from notification
        if (mRoomsList != null) {
            mActiveRoom = intent.getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY);

            for (int i = 0; i < mRoomsList.size(); i++) {
                if (mRoomsList.get(i).id.equals(mActiveRoom.id)) {
                    selectedNavItem = i + 1;
                }
            }

            mDrawer.setSelectionAtPosition(selectedNavItem + 1);
            EventBus.getDefault().post(mRoomsList.get(selectedNavItem - 1));
            setTitle(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(selectedNavItem)).getName().toString());

            mDrawer.closeDrawer();
        }
    }

    private void setNavigationView() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.parent_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name);

        UserModel model = Utils.getInstance().getUserPref();
        if (!model.id.isEmpty()) {
            mMainProfile = new ProfileDrawerItem().withName(model.username);
            loadImageFromCache();
        } else {
            mMainProfile = new ProfileDrawerItem().withName("Anonymous");
        }

        mAccountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.mipmap.header)
                .withProfileImagesClickable(true)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(mMainProfile)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile iProfile, boolean b) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(Utils.GITHUB_URL + Utils.getInstance().getUserPref().url)));
                        return false;
                    }
                })
                .build();

        mAccountHeader.setActiveProfile(mMainProfile);

        mDrawerItems.add(new PrimaryDrawerItem().withIcon(R.mipmap.ic_home).withName(getString(R.string.home))
                .withSelectable(false)
                .withSetSelected(false));
        mDrawerItems.add(new DividerDrawerItem());
        mDrawerItems.add(new PrimaryDrawerItem().withName(getString(R.string.action_settings)).withIcon(R.mipmap.ic_settings)
                .withSelectable(false)
                .withSetSelected(false));
        mDrawerItems.add(new PrimaryDrawerItem().withIcon(R.mipmap.ic_logout).withName(getString(R.string.signout)));
        mDrawerItems.add(new PrimaryDrawerItem().withIcon(R.mipmap.ic_exit_to_app).withName(getString(R.string.exit)));

        mDrawer = new DrawerBuilder().withActivity(this)
                .withTranslucentStatusBar(false)
                .withAccountHeader(mAccountHeader)
                .withActionBarDrawerToggle(mDrawerToggle)
                .withActionBarDrawerToggle(true)
                .withActionBarDrawerToggleAnimated(true)
                .addDrawerItems((IDrawerItem[]) mDrawerItems.toArray(new IDrawerItem[mDrawerItems.size()]))
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem iDrawerItem) {
                        if (!(iDrawerItem instanceof PrimaryDrawerItem)) {
                            return false;
                        }

                        PrimaryDrawerItem item = (PrimaryDrawerItem) iDrawerItem;

                        if (item.getName().toString().equals(getString(R.string.home))) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://gitter.im/home")));
                        } else if (item.getName().toString().equals(getString(R.string.action_settings))) {
                            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                        } else if (item.getName().toString().equals(getString(R.string.signout))) {
                            getSharedPreferences(Utils.getInstance().USERINFO_PREF, MODE_PRIVATE).edit().clear().apply();

                            startActivity(new Intent(getApplicationContext(), LoginActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                            stopService(new Intent(getApplicationContext(), NewMessagesService.class));
                            finish();
                        } else if (item.getName().toString().equals(getString(R.string.exit))) {
                            finish();
                        } else if (mRoomsList != null && mRoomsList.size() > 0) {
                            if (mActiveRoom == null || !mActiveRoom.name.equals(item.getName().getText())) {
                                for (int i = 0; i < mRoomsList.size(); i++) {
                                    if (((PrimaryDrawerItem) iDrawerItem).getName().toString().equals(mRoomsList.get(i).name)) {
                                        selectedNavItem = i + 1; // Because item "Home" in navigation menu
                                        mActiveRoom = mRoomsList.get(selectedNavItem - 1);

                                        setTitle(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(selectedNavItem)).getName().toString());
                                        EventBus.getDefault().post(mRoomsList.get(i));
                                    }
                                }
                            }
                        }

                        mDrawer.closeDrawer();
                        return true;
                    }
                })
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mRoomsList != null) {
            if (getIntent().getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY) != null) {
                mActiveRoom = getIntent().getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY);

                for (int i = 0; i < mRoomsList.size(); i++) {
                    if (mRoomsList.get(i).id.equals(mActiveRoom.id)) {
                        selectedNavItem = i + 1;
                    }
                }

                mDrawer.setSelectionAtPosition(selectedNavItem + 1);
                EventBus.getDefault().post(mRoomsList.get(selectedNavItem - 1));
                setTitle(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(selectedNavItem)).getName().toString());

                mDrawer.closeDrawer();
            }
        } else {
            getLoaderManager().initLoader(LOAD_ROOM_DATABASE_ID, null, this).forceLoad();
        }
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
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                if (!mDrawer.isDrawerOpen()) {
                    mDrawer.openDrawer();
                } else {
                    mDrawer.closeDrawer();
                }
                break;
            case R.id.action_refresh:
                if (mActiveRoom != null) {
                    mChatRoomFragment.onRefreshRoom();
                }
                break;
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
                loader = new RoomAsyncLoader(this, RoomAsyncLoader.FROM_SERVER);
                return loader;
            case LOAD_ROOM_DATABASE_ID:
                loader = new RoomAsyncLoader(this, RoomAsyncLoader.FROM_DATABASE);
                return loader;
            default:
                return loader;
        }
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<RoomModel>> loader, ArrayList<RoomModel> data) {
        if (loader.getId() == LOAD_ROOM_DATABASE_ID && Utils.getInstance().isNetworkConnected()) {
            getLoaderManager().initLoader(LOAD_ROOM_ID, null, this).forceLoad();
        } else if (loader.getId() == LOAD_ROOM_ID) {
            mClientDatabase.insertRooms(data);
        }

        setItemsDrawer(data);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<RoomModel>> loader) {

    }

    private void setItemsDrawer(ArrayList<RoomModel> data) {
        mRoomsList = data;

        // Remove old items
        // 5 but items: "Home", "Divider", "Settings", etc.
        while (mDrawer.getDrawerItems().size() != 5) {
            mDrawer.removeItemByPosition(2); // 2? Wtf?
        }

        BadgeStyle badgeStyle = new BadgeStyle(getResources().getColor(R.color.md_green_500),
                getResources().getColor(R.color.md_green_700));
        badgeStyle.withTextColor(getResources().getColor(android.R.color.white));

        for (RoomModel room : data) {
            if (room.unreadItems > 0) {
                String badgeText = room.unreadItems == 100 ? "99+" : Integer.toString(room.unreadItems);
                mDrawer.addItemAtPosition(new PrimaryDrawerItem().withIcon(R.mipmap.ic_room).withName(room.name)
                        .withBadge(badgeText)
                        .withBadgeStyle(badgeStyle)
                        .withSelectable(true), mDrawer.getDrawerItems().size() - 3);
            } else {
                mDrawer.addItemAtPosition(
                        new PrimaryDrawerItem()
                                .withIcon(R.mipmap.ic_room)
                                .withName(room.name)
                                .withBadgeStyle(badgeStyle)
                                .withSelectable(true), mDrawer.getDrawerItems().size() - 3);
            }
        }

        if (data.size() > 0) {
            selectedNavItem = 1;
            mDrawer.setSelectionAtPosition(selectedNavItem + 1);
        }
    }

    @Override
    protected void onDestroy() {
        getLoaderManager().destroyLoader(LOAD_ROOM_ID);
        getLoaderManager().destroyLoader(LOAD_ROOM_DATABASE_ID);

        try {
            unregisterReceiver(newMessageReceiver);
            unregisterReceiver(messageDeliveredReceiver);
            unregisterReceiver(unauthorizedReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (mClientDatabase != null) {
            mClientDatabase.close();
        }

        super.onDestroy();
    }

    private void loadImageFromCache() {
        Glide.with(this).load(Utils.getInstance().getUserPref().avatarUrlMedium).diskCacheStrategy(DiskCacheStrategy.RESULT).into(new SimpleTarget<GlideDrawable>() {
            @Override
            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                if (!loadAvatarFromNetworkFlag) {
                    mAccountHeader.removeProfile(mMainProfile);
                    mMainProfile.withIcon(resource.getCurrent());
                    mAccountHeader.addProfiles(mMainProfile);
                }
            }
        });
    }

    private void updateUserFromServer() {
        IApiMethods methods = mRestAdapter.create(IApiMethods.class);
        methods.getCurrentUser(Utils.getInstance().getBearer(), new Callback<ArrayList<UserModel>>() {
            @Override
            public void success(final ArrayList<UserModel> userModel, Response response) {
                // If this is the first launch of the application
                if (Utils.getInstance().getUserPref().id.isEmpty()) {
                    Utils.getInstance().writeUserToPref(userModel.get(0));
                    startService(new Intent(getApplicationContext(), NewMessagesService.class));
                } else {
                    Utils.getInstance().writeUserToPref(userModel.get(0));
                }

                // Update profile
                // updateProfileByIdentifier() not working
                mAccountHeader.removeProfile(mMainProfile);
                mMainProfile.withName(userModel.get(0).username);
                mAccountHeader.addProfiles(mMainProfile);

                // Update profile
                Glide.with(MainActivity.this).load(userModel.get(0).avatarUrlMedium).asBitmap()
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                mAccountHeader.removeProfile(mMainProfile);
                                mMainProfile.withIcon(resource);
                                mAccountHeader.addProfiles(mMainProfile);
                                loadAvatarFromNetworkFlag = true;
                            }
                        });
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                if (error.getMessage().contains("401")) {
                    mDrawer.setSelectionAtPosition(mDrawer.getDrawerItems().size() - 1);
                }
            }
        });
    }

    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MessageModel message = intent.getParcelableExtra(NewMessagesService.NEW_MESSAGE_EXTRA_KEY);
            RoomModel room = intent.getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY);

            if (room.id.equals(mActiveRoom.id)) {
                mChatRoomFragment.newMessage(message);
            } else {
                for (int i = 0; i < mRoomsList.size(); i++) {
                    if (mRoomsList.get(i).id.equals(room.id)) {
                        mRoomsList.get(i).unreadItems += 1;
                        final String badgeText = mRoomsList.get(i).unreadItems >= 100 ? "99+" : Integer.toString(mRoomsList.get(i).unreadItems);
                        mDrawer.updateItem(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(i + 1)).withBadge(badgeText));
                        mDrawer.getAdapter().notifyDataSetChanged();
                    }
                }
            }
        }
    };

    private BroadcastReceiver messageDeliveredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MessageModel model = intent.getParcelableExtra(NewMessagesService.NEW_MESSAGE_EXTRA_KEY);

            if (!model.id.isEmpty()) {
                mChatRoomFragment.messageDelivered(model);
            } else {
                mChatRoomFragment.messageErrorDelivered(model);
            }
        }
    };

    private BroadcastReceiver unauthorizedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Select "Sign Out"
            mDrawer.setSelectionAtPosition(mDrawer.getDrawerItems().size() - 1);
        }
    };

    static class RoomAsyncLoader extends AsyncTaskLoader<ArrayList<RoomModel>> {
        public static final int FROM_SERVER = 0;
        public static final int FROM_DATABASE = 1;

        private RestAdapter mAdapter;
        private final int mFlag;

        private MainActivity mActivity;

        public RoomAsyncLoader(MainActivity activity, int flag) {
            super(activity);

            mActivity = activity;

            mFlag = flag;

            if (mFlag == FROM_SERVER) {
                mAdapter = new RestAdapter.Builder()
                        .setEndpoint(Utils.GITTER_API_URL)
                        .build();
            }
        }

        @Override
        public ArrayList<RoomModel> loadInBackground() {
            if (mFlag == FROM_SERVER) {
                IApiMethods methods = mAdapter.create(IApiMethods.class);
                return methods.getCurrentUserRooms(Utils.getInstance().getBearer());
            } else if (mFlag == FROM_DATABASE) {
                return mActivity.mClientDatabase.getRooms();
            } else {
                return null;
            }
        }
    }

    public interface NewMessageFragmentCallback {
        void newMessage(MessageModel model);

        void messageDelivered(MessageModel model);

        void messageErrorDelivered(MessageModel model);
    }

    public interface RefreshRoomCallback {
        void onRefreshRoom();
    }
}
