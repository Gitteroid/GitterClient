package com.ne1c.developerstalk.ui.activities;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.events.NewMessageEvent;
import com.ne1c.developerstalk.events.ReadMessagesEvent;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.models.UserModel;
import com.ne1c.developerstalk.presenters.MainPresenter;
import com.ne1c.developerstalk.services.NewMessagesService;
import com.ne1c.developerstalk.ui.DrawShadowFrameLayout;
import com.ne1c.developerstalk.ui.fragments.ChatRoomFragment;
import com.ne1c.developerstalk.ui.views.MainView;
import com.ne1c.developerstalk.utils.UIUtils;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity implements MainView {
    public final static String BROADCAST_UNAUTHORIZED = "com.ne1c.gitterclient.UnathorizedReceiver";

    private final String SELECT_NAV_ITEM_BUNDLE = "select_nav_item";
    private final String ROOMS_BUNDLE = "rooms_bundle";

    private ChatRoomFragment mChatRoomFragment;

    private ActionBarDrawerToggle mDrawerToggle;

    private Drawer mDrawer;
    private ArrayList<IDrawerItem> mDrawerItems = new ArrayList<>();
    private AccountHeader mAccountHeader;
    private IProfile mMainProfile;

    private ArrayList<RoomModel> mRoomsList = new ArrayList<>();
    private RoomModel mActiveRoom;

    private int selectedNavItem = -1; // Default, item not selected
    private boolean loadAvatarFromNetworkFlag;

    private MainPresenter mPresenter;

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

        setContentView(R.layout.activity_main);

        mPresenter = new MainPresenter();
        mPresenter.bindView(this);

        EventBus.getDefault().register(this);
        init();
        initSavedInstanceState(savedInstanceState);
    }

    private void init() {
        registerReceiver(unauthorizedReceiver, new IntentFilter(BROADCAST_UNAUTHORIZED));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        mChatRoomFragment = (ChatRoomFragment) getFragmentManager().findFragmentByTag("chatRoom");

        if (mChatRoomFragment == null) {
            mChatRoomFragment = new ChatRoomFragment();
            getFragmentManager().beginTransaction().add(mChatRoomFragment, "chatRoom").commit();
        }

        setNavigationView();
    }

    private void initSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, mChatRoomFragment).commit();

            mPresenter.loadCachedRooms();
            mPresenter.loadRooms();

            mPresenter.loadProfile();
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
                            .withSelectable(true), mDrawer.getDrawerItems().size() - 2);
                } else {
                    mDrawer.addItemAtPosition(
                            new PrimaryDrawerItem()
                                    .withIcon(R.mipmap.ic_room)
                                    .withName(room.name)
                                    .withBadgeStyle(badgeStyle)
                                    .withSelectable(true), mDrawer.getDrawerItems().size() - 2);
                }
            }

            if (mRoomsList.size() > 0) {
                // If activity open from notification
                mActiveRoom = getIntent().getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY);

                if (mActiveRoom == null || mActiveRoom.id == null) {
                    mActiveRoom = mRoomsList.get(selectedNavItem - 1);
                }

                for (int i = 0; i < mRoomsList.size(); i++) {
                    if (mRoomsList.get(i).id.equals(mActiveRoom.id)) {
                        selectedNavItem = i + 1;
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
            RoomModel intentRoom = intent.getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY);

            // If selected room not equal room id from notification, than load room
            if (mActiveRoom == null || !intentRoom.id.equals(mActiveRoom.id)) {
                mActiveRoom = intentRoom;

                for (int i = 0; i < mRoomsList.size(); i++) {
                    if (mRoomsList.get(i).id.equals(mActiveRoom.id)) {
                        selectedNavItem = i + 1;
                    }
                }

                mDrawer.setSelectionAtPosition(selectedNavItem + 1);
                if (mRoomsList.size() > 0) {
                    EventBus.getDefault().post(mRoomsList.get(selectedNavItem - 1));
                }
                setTitle(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(selectedNavItem)).getName().toString());
            }

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
                .withOnAccountHeaderListener((view, iProfile, b) -> {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(Utils.GITHUB_URL + Utils.getInstance().getUserPref().url)));
                    return false;
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

        mDrawer = new DrawerBuilder().withActivity(this)
                .withTranslucentStatusBar(false)
                .withAccountHeader(mAccountHeader)
                .withActionBarDrawerToggle(mDrawerToggle)
                .withActionBarDrawerToggle(true)
                .withActionBarDrawerToggleAnimated(true)
                .addDrawerItems((IDrawerItem[]) mDrawerItems.toArray(new IDrawerItem[mDrawerItems.size()]))
                .withOnDrawerItemClickListener((view, position, iDrawerItem) -> {
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
                })
                .build();
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

        RoomModel extraRoom = getIntent().getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY);
        if (mRoomsList != null && extraRoom != null) {
            mActiveRoom = extraRoom;
            getIntent().putExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY, (Parcelable) null);

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

                    mPresenter.loadRooms();
                }
                break;
            case R.id.action_leave:
                if (mActiveRoom.oneToOne) {
                    Toast.makeText(getApplicationContext(), R.string.leave_from_one_to_one, Toast.LENGTH_SHORT).show();
                    break;
                }

                mPresenter.leaveFromRoom(mActiveRoom.id);

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setItemsDrawer(ArrayList<RoomModel> data) {
        mRoomsList.clear();
        mRoomsList.addAll(data);

        // Remove old items
        // 4 but items: "Home", "Divider", "Settings", "Sign Out".
        while (mDrawer.getDrawerItems().size() != 4) {
            mDrawer.removeItemByPosition(2); // 2? Wtf?
        }

        BadgeStyle badgeStyle = new BadgeStyle(getResources().getColor(R.color.md_green_500),
                getResources().getColor(R.color.md_green_700));
        badgeStyle.withTextColor(getResources().getColor(android.R.color.white));

        for (RoomModel room : mRoomsList) {
            if (room.unreadItems > 0) {
                String badgeText = room.unreadItems == 100 ? "99+" : Integer.toString(room.unreadItems);
                mDrawer.addItemAtPosition(new PrimaryDrawerItem().withIcon(R.mipmap.ic_room).withName(room.name)
                        .withBadge(badgeText)
                        .withBadgeStyle(badgeStyle)
                        .withSelectable(true), mDrawer.getDrawerItems().size() - 2);
            } else {
                mDrawer.addItemAtPosition(
                        new PrimaryDrawerItem()
                                .withIcon(R.mipmap.ic_room)
                                .withName(room.name)
                                .withBadgeStyle(badgeStyle)
                                .withSelectable(true), mDrawer.getDrawerItems().size() - 2);
            }
        }

        if (mRoomsList.size() > 0) {
            RoomModel room = getIntent().getParcelableExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY);
            String roomId = room != null ? room.id : null;

            if (roomId == null) {
                roomId = getIntent().getStringExtra("roomId");
                getIntent().removeExtra("roomId");
            } else {
                getIntent().removeExtra(NewMessagesService.FROM_ROOM_EXTRA_KEY);
            }

            if (roomId != null) {
                for (int i = 0; i < mRoomsList.size(); i++) {
                    if (roomId.equals(mRoomsList.get(i).id)) {
                        selectedNavItem = i + 1;
                    }
                }
            } else if (selectedNavItem == -1) {
                selectedNavItem = 1;
            }
            mDrawer.setSelectionAtPosition(selectedNavItem + 1);
        }

        mDrawer.getAdapter().notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(unauthorizedReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        mPresenter.unbindView();

        EventBus.getDefault().unregister(this);
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

    public void onEvent(NewMessageEvent message) {
        for (int i = 0; i < mRoomsList.size(); i++) {
            if (mRoomsList.get(i).id.equals(message.getRoom().id)) {
                mRoomsList.get(i).unreadItems += 1;
                final String badgeText = mRoomsList.get(i).unreadItems >= 100 ? "99+" : Integer.toString(mRoomsList.get(i).unreadItems);
                mDrawer.updateItem(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(i + 1)).withBadge(badgeText));
                mDrawer.getAdapter().notifyDataSetChanged();
            }
        }
    }

    private BroadcastReceiver unauthorizedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Select "Sign Out"
            mDrawer.setSelectionAtPosition(mDrawer.getDrawerItems().size() - 1);
        }
    };

    // Update Badge in navigation item, if message was read
    public void onEvent(ReadMessagesEvent count) {
        for (int i = 0; i < mRoomsList.size(); i++) {
            if (mRoomsList.get(i).id.equals(mActiveRoom.id)) {
                // Update mActiveRoom, fix it.
                // Make update will mActiveRoom while first load room or refresh room
                mActiveRoom = mRoomsList.get(i);
                mActiveRoom.unreadItems -= count.getCountRead();

                if (mActiveRoom.unreadItems <= 0) {
                    mActiveRoom.unreadItems = 0;
                    // Remove badge
                    mDrawer.updateItem(
                            ((PrimaryDrawerItem) mDrawer.getDrawerItems().get(i + 1)).withBadge(new StringHolder(null)));
                } else {
                    String badgeText = mActiveRoom.unreadItems >= 100 ? "99+" : Integer.toString(mActiveRoom.unreadItems);
                    mDrawer.updateItem(((PrimaryDrawerItem) mDrawer.getDrawerItems().get(i + 1)).withBadge(badgeText));
                }

                mRoomsList.set(i, mActiveRoom);
                mDrawer.getAdapter().notifyDataSetChanged();
            }
        }
    }

    @Override
    public void showRooms(ArrayList<RoomModel> rooms) {
        setItemsDrawer(rooms);
    }

    @Override
    public void showProfile(UserModel user) {
        // Update profile
        // updateProfileByIdentifier() not working
        mAccountHeader.removeProfile(mMainProfile);
        mMainProfile.withName(user.username);
        mAccountHeader.addProfiles(mMainProfile);
    }

    @Override
    public void showError(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        if (text.contains("401")) {
            mDrawer.setSelectionAtPosition(mDrawer.getDrawerItems().size() - 1);
        }
    }

    @Override
    public void leavedFromRoom() {
        finish();
    }

    @Override
    public void updatePhoto(Bitmap photo) {
        mAccountHeader.removeProfile(mMainProfile);
        mMainProfile.withIcon(photo);
        mAccountHeader.addProfiles(mMainProfile);
        loadAvatarFromNetworkFlag = true;
    }

    @Override
    public Context getAppContext() {
        return getApplicationContext();
    }

    public interface RefreshRoomCallback {
        void onRefreshRoom();
    }
}