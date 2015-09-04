package com.ne1c.gitterclient.Activities;


import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

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
import com.ne1c.gitterclient.Fragments.ChatRoomFragment;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.Models.UserModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Services.NewMessagesService;
import com.ne1c.gitterclient.Utils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

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

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private Drawer mDrawer;
    private ArrayList<IDrawerItem> mDrawerItems = new ArrayList<>();
    private AccountHeader mAccountHeader;
    private IProfile mMainProfile;

    private ArrayList<RoomModel> mRoomsList;
    private RoomModel mActiveRoom;

    private RestAdapter mRestAdapter;

    private int selectedNavItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(newMessageReceiver, new IntentFilter(BROADCAST_NEW_MESSAGE));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        setNavigationView();

        mRestAdapter = new RestAdapter.Builder()
                .setEndpoint(Utils.getInstance().GITTER_API_URL)
                .build();

        loadHeaderNavView();

        mChatRoomFragment = new ChatRoomFragment();


        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mChatRoomFragment).commit();
            if (Utils.getInstance().isNetworkConnected()) {
                getLoaderManager().initLoader(LOAD_ROOM_ID, null, this).forceLoad();
            }
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

                EventBus.getDefault().post(mRoomsList.get(selectedNavItem - 1));
                setTitle(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(selectedNavItem)).getName().toString());
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

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
        mDrawerLayout = (DrawerLayout) findViewById(R.id.parent_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.app_name, R.string.app_name);

        UserModel model = Utils.getInstance().getUserPref();
        if (model != null) {
            mMainProfile = new ProfileDrawerItem().withName(model.username).withIcon(ImageLoader.getInstance().loadImageSync(model.avatarUrlMedium));
        } else {
            mMainProfile = new ProfileDrawerItem().withName("Anonymous");
        }

        mAccountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.bgHeader)
                .withProfileImagesClickable(true)
                .addProfiles(mMainProfile)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile iProfile, boolean b) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(Utils.getInstance().GITHUB_URL + Utils.getInstance().getUserPref().url)));
                        return false;
                    }
                })
                .build();

        mDrawerItems.add(new PrimaryDrawerItem().withIcon(R.mipmap.ic_home).withName(getString(R.string.home))
                .withSelectable(false)
                .withSetSelected(false));
        mDrawerItems.add(new DividerDrawerItem());
        mDrawerItems.add(new PrimaryDrawerItem().withName(getString(R.string.action_settings)).withIcon(R.mipmap.ic_settings));
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
                        } else if (item.getName().toString().equals(getString(R.string.exit))) {
                            finish();
                        } else if (mRoomsList != null && mRoomsList.size() > 0) {
                            for (int i = 0; i < mRoomsList.size(); i++) {
                                if (((PrimaryDrawerItem) iDrawerItem).getName().toString().equals(mRoomsList.get(i).name)) {
                                    selectedNavItem = i + 1; // Because item in navigation menu
                                    mActiveRoom = mRoomsList.get(selectedNavItem - 1);

                                    setTitle(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(selectedNavItem)).getName().toString());

                                    EventBus.getDefault().post(mRoomsList.get(i));
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
                    EventBus.getDefault().post(mActiveRoom);
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
                loader = new RoomAsyncLoader(getApplicationContext());
                return loader;
            default:
                return loader;
        }
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<RoomModel>> loader, ArrayList<RoomModel> data) {
        mRoomsList = data;

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

            EventBus.getDefault().post(data.get(selectedNavItem - 1));
            setTitle(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(selectedNavItem)).getName().toString());
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<RoomModel>> loader) {

    }

    private void loadHeaderNavView() {
        final UserModel model = Utils.getInstance().getUserPref();
        if (model != null) {
            mMainProfile.withName(model.username);
            mAccountHeader.updateProfileByIdentifier(mMainProfile);

            ImageLoader.getInstance().loadImage(model.avatarUrlMedium, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, final Bitmap loadedImage) {
                    super.onLoadingComplete(imageUri, view, loadedImage);

                    mMainProfile.withIcon(loadedImage);
                    mAccountHeader.updateProfileByIdentifier(mMainProfile);
                    updateUserFromServer();
                }
            });
        } else {
            updateUserFromServer();
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
            return methods.getCurrentUserRooms(Utils.getInstance().getBearer());
        }
    }

    @Override
    protected void onDestroy() {
        getLoaderManager().destroyLoader(LOAD_ROOM_ID);
        unregisterReceiver(newMessageReceiver);
        super.onDestroy();
    }

    private void updateUserFromServer() {
        IApiMethods methods = mRestAdapter.create(IApiMethods.class);
        methods.getCurrentUser(Utils.getInstance().getBearer(), new Callback<ArrayList<UserModel>>() {
            @Override
            public void success(final ArrayList<UserModel> userModel, Response response) {
                Utils.getInstance().writeUserToPref(userModel.get(0));

                ImageLoader.getInstance().loadImage(userModel.get(0).avatarUrlMedium, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, final Bitmap loadedImage) {
                        super.onLoadingComplete(imageUri, view, loadedImage);

                        mMainProfile.withName(userModel.get(0).username);
                        mMainProfile.withIcon(loadedImage);
                        mAccountHeader.updateProfileByIdentifier(mMainProfile);
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
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

    public interface NewMessageFragmentCallback {
        void newMessage(MessageModel model);
    }
}
