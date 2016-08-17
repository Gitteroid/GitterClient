package com.ne1c.gitteroid.ui.activities


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.TabLayout
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.bumptech.glide.Glide
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.DependencyManager
import com.ne1c.gitteroid.events.ReadMessagesEvent
import com.ne1c.gitteroid.events.RefreshMessagesRoomEvent
import com.ne1c.gitteroid.models.RoomMapper
import com.ne1c.gitteroid.models.data.RoomModel
import com.ne1c.gitteroid.models.data.UserModel
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.presenters.MainPresenter
import com.ne1c.gitteroid.services.NotificationService
import com.ne1c.gitteroid.ui.adapters.RoomsPagerAdapter
import com.ne1c.gitteroid.ui.drawer.ImagePrimaryDrawerItem
import com.ne1c.gitteroid.ui.drawer.ProgressDrawerItem
import com.ne1c.gitteroid.ui.fragments.PickRoomsDialogFragment
import com.ne1c.gitteroid.ui.views.MainView
import com.ne1c.rainbowmvp.base.BaseActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*

class MainActivity : BaseActivity<MainPresenter>(), MainView {
    private val ROOM_IN_DRAWER_OFFSET_BOTTOM = 4 // All, Search, Settings, Sign Out
    private val ROOM_IN_DRAWER_OFFSET_TOP = 2 // Header, Home
    private val ROOMS_BUNDLE = "rooms_bundle"
    private val ROOMS_IN_DRAWER_BUNDLE = "rooms_in_drawer_bundle"
    private val ROOMS_IN_TABS_BUNDLE = "rooms_in_tabs_bundle"
    private val ROOMS_PAGER_STATE_BUNDLE = "rooms_pager_state_bundle"
    private val SELECTED_ROOM_BUNDLE = "SELECTED_ROOM_BUNDLE"

    private var mRoomsViewPager: ViewPager? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mRoomTabs: TabLayout? = null
    private var mDrawer: Drawer? = null
    private val mDrawerItems = ArrayList<IDrawerItem<*, *>>()
    private var mAccountHeader: AccountHeader? = null
    private val mMainProfile = ProfileDrawerItem()
    private var mLoadRoomsProgressView: LinearLayout? = null
    private var mErrorLoadRoomsView: LinearLayout? = null

    private val mRoomsList = ArrayList<RoomViewModel>()
    private val mRoomsInDrawer = ArrayList<RoomViewModel>() // Rooms that user will see
    private val mRoomsInTabs = ArrayList<RoomViewModel>()
    private var mStateWasRestored = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!DependencyManager.INSTANCE.dataManager.isAuthorize()) {
            startActivity(Intent(applicationContext, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
            overridePendingTransition(0, 0)
            return
        }

        setContentView(R.layout.activity_main)

        initViews()
        initState(savedInstanceState)
        initDrawerImageLoader()

        startService(Intent(this, NotificationService::class.java))
    }

    private fun initViews() {
        val toolbar = findViewById(R.id.toolbar) as Toolbar

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        mRoomsViewPager = findViewById(R.id.rooms_viewPager) as ViewPager
        mRoomsViewPager?.adapter = RoomsPagerAdapter(supportFragmentManager, mRoomsInTabs)
        mRoomsViewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                val name = mRoomsInTabs[position].name
                title = name

                mDrawer?.setSelectionAtPosition(getPositionRoomInDrawer(name) + 1, false)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
        mRoomsViewPager?.offscreenPageLimit = 10

        mRoomTabs = findViewById(R.id.rooms_tab) as TabLayout
        mRoomTabs?.setupWithViewPager(mRoomsViewPager)

        mLoadRoomsProgressView = findViewById(R.id.load_rooms_layout) as LinearLayout

        mErrorLoadRoomsView = findViewById(R.id.error_load_rooms_layout) as LinearLayout
        mErrorLoadRoomsView?.findViewById(R.id.retry_button)?.setOnClickListener {
            mErrorLoadRoomsView?.visibility = View.GONE
            mLoadRoomsProgressView?.visibility = View.VISIBLE

            mPresenter.loadRooms()
        }

        setNavigationView()
    }

    private fun initState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            cleanDrawer()

            val roomsList = savedInstanceState.getParcelableArrayList<RoomViewModel>(ROOMS_BUNDLE)
            mRoomsInDrawer.addAll(savedInstanceState.getParcelableArrayList<RoomViewModel>(ROOMS_IN_DRAWER_BUNDLE))
            mRoomsInTabs.addAll(savedInstanceState.getParcelableArrayList<RoomViewModel>(ROOMS_IN_TABS_BUNDLE))

            addRoomsToDrawer(roomsList, true)

            for (i in mRoomsInTabs.indices) {
                mRoomTabs?.addTab(mRoomTabs?.newTab()!!.setText(mRoomsInTabs[i].name), i)
            }

            val pagerAdapterState = savedInstanceState.getParcelable<Parcelable>(ROOMS_PAGER_STATE_BUNDLE)
            mRoomsViewPager?.adapter?.restoreState(pagerAdapterState, classLoader)
            mRoomsViewPager?.adapter?.notifyDataSetChanged()

            mStateWasRestored = true

            selectedRoom = savedInstanceState.getParcelable(SELECTED_ROOM_BUNDLE)
            if (selectedRoom != null) {
                val posInDrawer = getPositionRoomInDrawer(selectedRoom!!.name)
                mDrawer?.setSelectionAtPosition(posInDrawer, false)
            }
        }
    }

    private fun initDrawerImageLoader() {
        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView?, uri: Uri?, placeholder: Drawable?) {
                Glide.with(imageView?.context).load(uri).placeholder(placeholder).into(imageView)
            }

            override fun cancel(imageView: ImageView?) {
                Glide.clear(imageView)
            }
        })
    }

    private fun setNavigationView() {
        val drawerLayout = findViewById(R.id.parent_layout) as DrawerLayout
        mDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name)

        createAccountHeader()

        mDrawerItems.add(PrimaryDrawerItem()
                .withIcon(R.drawable.ic_home)
                .withName(getString(R.string.home))
                .withTextColor(Color.WHITE)
                .withIconColor(Color.WHITE)
                .withIconTintingEnabled(true)
                .withSelectable(false)
                .withSetSelected(false))

        mDrawerItems.add(ProgressDrawerItem()
                .withSelectable(false)
                .withSetSelected(false))

        mDrawerItems.add(PrimaryDrawerItem()
                .withName(getString(R.string.all))
                .withTextColor(Color.WHITE)
                .withIconColor(Color.WHITE)
                .withIconTintingEnabled(true)
                .withIcon(R.drawable.ic_format_list_bulleted)
                .withSelectable(false))

        mDrawerItems.add(PrimaryDrawerItem()
                .withName(getString(R.string.search_room))
                .withTextColor(Color.WHITE)
                .withIconColor(Color.WHITE)
                .withIconTintingEnabled(true)
                .withIcon(R.drawable.ic_magnify)
                .withSelectable(false))

        mDrawerItems.add(DividerDrawerItem())

        mDrawerItems.add(PrimaryDrawerItem()
                .withName(getString(R.string.action_settings))
                .withIcon(R.drawable.ic_settings_dark)
                .withTextColor(Color.WHITE)
                .withIconColor(Color.WHITE)
                .withIconTintingEnabled(true)
                .withSelectable(false)
                .withSetSelected(false))

        mDrawerItems.add(PrimaryDrawerItem()
                .withIcon(R.drawable.ic_logout)
                .withIconColor(Color.WHITE)
                .withIconTintingEnabled(true)
                .withName(getString(R.string.signout))
                .withTextColor(Color.WHITE))

        mDrawer = DrawerBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(false)
                .withAccountHeader(mAccountHeader!!)
                .withActionBarDrawerToggle(mDrawerToggle!!)
                .withActionBarDrawerToggle(true)
                .withActionBarDrawerToggleAnimated(true)
                .withTranslucentStatusBar(true)
                .withSliderBackgroundColorRes(R.color.navDrawerBackground)
                .addDrawerItems(*mDrawerItems.toTypedArray())
                .withOnDrawerItemClickListener(drawerItemClickListener)
                .build()

        mDrawer?.setSelection(-1) // Pick no_rooms
    }

    private fun createAccountHeader() {
        mAccountHeader = AccountHeaderBuilder().withActivity(this)
                .withHeaderBackground(R.drawable.header).withProfileImagesClickable(true)
                .withSelectionListEnabledForSingleProfile(false).addProfiles(mMainProfile)
                .withOnAccountHeaderListener { view, iProfile, b ->
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse(DataManger.GITHUB_URL + DependencyManager.INSTANCE.dataManager.getUser().url)))
                    return@withOnAccountHeaderListener false
                }.build()

        mAccountHeader?.activeProfile = mMainProfile
    }

    private fun setItemsDrawer(data: ArrayList<RoomViewModel>) {
        cleanDrawer()
        addRoomsToDrawer(data, false)

        mDrawer?.setSelectionAtPosition(ROOM_IN_DRAWER_OFFSET_TOP)
    }

    private fun cleanDrawer() {
        mRoomsList.clear()
        mRoomsInDrawer.clear()
        mRoomTabs?.removeAllTabs()

        // Remove old items
        // 4 but items: "Home", "All", "Search", "Divider", "Settings", "Sign Out".
        while (mDrawer?.drawerItems?.size != 6) {
            mDrawer?.removeItemByPosition(ROOM_IN_DRAWER_OFFSET_TOP) // 2? Wtf?
        }
    }

    private fun addRoomsToDrawer(roomsList: ArrayList<RoomViewModel>, restore: Boolean) {
        if (mRoomsInDrawer.size == 0) {
            mRoomsInDrawer.addAll(roomsList)
        }

        if (restore) {
            for (i in mRoomsInDrawer.indices) {
                mDrawer?.addItemAtPosition(formatRoomToDrawerItem(mRoomsInDrawer[i]),
                        ROOM_IN_DRAWER_OFFSET_TOP)
            }
        } else {
            for (i in mRoomsInDrawer.indices) {
                val room = mRoomsInDrawer[i]

                mDrawer?.addItemAtPosition(formatRoomToDrawerItem(room),
                        ROOM_IN_DRAWER_OFFSET_TOP)
            }
        }

        Collections.reverse(mRoomsInDrawer)

        mDrawer?.adapter?.notifyDataSetChanged()
    }

    private fun formatRoomToDrawerItem(room: RoomViewModel): ImagePrimaryDrawerItem {
        val badgeStyle = BadgeStyle(resources.getColor(R.color.md_green_500),
                resources.getColor(R.color.md_green_700))
        badgeStyle.withTextColor(resources.getColor(android.R.color.white))

        val badgeText = if (room.unreadItems == 100) "99+" else Integer.toString(room.unreadItems)
        val item = ImagePrimaryDrawerItem().withIcon(R.drawable.ic_room)
                .withIcon(ImageHolder(room.getAvatarUrl()))
                .withIconColor(Color.WHITE)
                .withIconTintingEnabled(true)
                .withName(room.name)
                .withTextColor(Color.WHITE)
                .withSelectedTextColor(Color.BLACK)
                .withSelectedColorRes(R.color.md_white_1000)
                .withSelectable(true) as ImagePrimaryDrawerItem

        if (room.unreadItems != 0) {
            return item.withBadge(badgeText).withBadgeStyle(badgeStyle) as ImagePrimaryDrawerItem
        }

        return item
    }

    override fun onStart() {
        super.onStart()

        mPresenter.bindView(this)
        mPresenter.loadProfile()

        if (!mStateWasRestored) {
            mPresenter.loadRooms()
        } else {
            mLoadRoomsProgressView?.visibility = View.GONE
        }

        EventBus.getDefault().register(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(newMessageReceiver, IntentFilter(BROADCAST_NEW_MESSAGE))
        registerReceiver(unauthorizedReceiver, IntentFilter(BROADCAST_UNAUTHORIZED))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // If get intent from notification
        val intentRoom = intent.getParcelableExtra<RoomModel>(NotificationService.FROM_ROOM_EXTRA_KEY)

        val notifRoom = RoomMapper.mapToView(intentRoom)

        if (selectedRoom?.name != notifRoom.name) {
            pickRoom(notifRoom)
        }

        mDrawer?.closeDrawer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelableArrayList(ROOMS_BUNDLE, mRoomsList)
        outState.putParcelableArrayList(ROOMS_IN_DRAWER_BUNDLE, mRoomsInDrawer)
        outState.putParcelableArrayList(ROOMS_IN_TABS_BUNDLE, mRoomsInTabs)
        outState.putParcelable(ROOMS_PAGER_STATE_BUNDLE, mRoomsViewPager?.adapter?.saveState())
        outState.putParcelable(SELECTED_ROOM_BUNDLE, selectedRoom)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (mDrawerToggle != null) {
            mDrawerToggle?.syncState()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle?.onConfigurationChanged(newConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            android.R.id.home ->
                if (!mDrawer?.isDrawerOpen!!) {
                    mDrawer?.openDrawer()
                } else {
                    mDrawer?.closeDrawer()
                }
            R.id.action_refresh ->
                if (selectedRoom != null) {
                    EventBus.getDefault().post(RefreshMessagesRoomEvent(selectedRoom!!))
                }
            R.id.action_leave -> {
                if (selectedRoom != null) {
                    if (selectedRoom!!.oneToOne) {
                        Toast.makeText(applicationContext, R.string.leave_from_one_to_one, Toast.LENGTH_SHORT).show()
                    } else {
                        mPresenter?.leaveFromRoom(selectedRoom!!.id)
                    }
                }
            }
            R.id.close_room -> closeRoom()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun closeRoom() {
        if (mRoomTabs?.tabCount!! > 1) {
            val nowPos = mRoomTabs?.selectedTabPosition
            mRoomsInTabs.removeAt(nowPos!!)
            mRoomsViewPager?.adapter?.notifyDataSetChanged()
        } else if (mRoomTabs?.tabCount == 0) {
            Toast.makeText(this, R.string.nothing_to_close, Toast.LENGTH_SHORT).show()
        } else if (mRoomTabs?.tabCount == 1) {
            Toast.makeText(this, R.string.cannt_close_single_room, Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeRoom(force: Boolean) {
        if (force) {
            val nowPos = mRoomTabs?.selectedTabPosition
            mRoomsInTabs.removeAt(nowPos!!)
            mRoomsViewPager?.adapter?.notifyDataSetChanged()
        } else {
            closeRoom()
        }
    }

    override fun onStop() {
        mPresenter.unbindView()
        EventBus.getDefault().unregister(this)

        try {
            unregisterReceiver(unauthorizedReceiver)
            unregisterReceiver(newMessageReceiver)
        } catch (e: IllegalArgumentException) {
            // Broadcast receiver not registered
        }

        super.onStop()
    }

    private val drawerItemClickListener = Drawer.OnDrawerItemClickListener { view, position, drawerItem ->
        val item = drawerItem as PrimaryDrawerItem
        val name = item.name.text

        if (name == getString(R.string.home)) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gitter.im/home")))
        } else if (name == getString(R.string.action_settings)) {
            startActivity(Intent(applicationContext, SettingsActivity::class.java))
        } else if (name == getString(R.string.signout)) {
            signOut()
        } else if (name == getString(R.string.all)) {
            showRoomsListDialog()
        } else if (name == getString(R.string.search_room)) {
            startActivity(Intent(applicationContext, SearchRoomActivity::class.java))
        } else if (mRoomsInDrawer.size > 0) {
            clickOnRoomInDrawer(name)
        }

        mDrawer?.closeDrawer()

        return@OnDrawerItemClickListener true
    }

    private fun clickOnRoomInDrawer(name: String) {
        if (!roomAlreadyOpened(name)) {
            for (model in mRoomsInDrawer) {
                if (name == model.name) {
                    title = model.name

                    val lastIndex = mRoomTabs!!.tabCount
                    mRoomsInTabs.add(lastIndex, model)

                    mRoomsViewPager?.adapter?.notifyDataSetChanged()
                    mRoomsViewPager?.currentItem = mRoomTabs!!.tabCount
                }
            }
        } else {
            mRoomsViewPager?.setCurrentItem(getSelectionRoomPositionInTab(name), true)
        }
    }

    private fun roomAlreadyOpened(name: String): Boolean {
        return getSelectionRoomPositionInTab(name) != -1
    }

    private fun getSelectionRoomPositionInTab(name: String): Int {
        for (i in mRoomsInTabs.indices) {
            if (mRoomsInTabs[i].name == name) {
                return i
            }
        }

        return -1
    }

    private fun signOut() {
        DependencyManager.INSTANCE.dataManager.cleatProfile()
        startActivity(Intent(applicationContext, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))

        stopService(Intent(applicationContext, NotificationService::class.java))

        finish()
    }

    private val unauthorizedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            signOut()
        }
    }

    private val newMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val roomId = intent.getStringExtra(ROOM_ID_INTENT_KEY)

            var searchRoomInDrawer = false

            for (i in mRoomsInDrawer.indices) {
                if (mRoomsInDrawer[i].id == roomId) {
                    val room = mRoomsInDrawer[i]

                    updateBadgeForRoom(room, 1, false)

                    searchRoomInDrawer = true
                }
            }

            if (!searchRoomInDrawer) {
                for (i in mRoomsList.indices) {
                    if (mRoomsList[i].id == roomId) {
                        val room = mRoomsList[i]
                        mRoomsInDrawer.add(room)

                        val newPos = mDrawer?.drawerItems?.size!! - ROOM_IN_DRAWER_OFFSET_BOTTOM
                        mDrawer?.addItemAtPosition(formatRoomToDrawerItem(room), newPos)

                        updateBadgeForRoom(room, 1, false)

                    }
                }
            }
        }
    }

    // Update Badge in navigation item, if message was read
    @Subscribe
    fun updateBudgetReadMessages(event: ReadMessagesEvent) {
        val idSelectedRoom = selectedRoom?.id

        for (i in mRoomsInDrawer.indices) {
            if (mRoomsInDrawer[i].id == idSelectedRoom) {
                val model = mRoomsInDrawer[i]
                updateBadgeForRoom(model, event.countRead, true)
            }
        }
    }

    private fun updateBadgeForRoom(model: RoomViewModel, diffMessages: Int, asRead: Boolean) {
        if (asRead) {
            model.unreadItems -= diffMessages
        } else {
            model.unreadItems += diffMessages
        }

        val item = (mDrawer!!.drawerItems[getPositionRoomInDrawer(model.name)]) as PrimaryDrawerItem
        if (model.unreadItems <= 0) {
            model.unreadItems = 0
            // Remove badge
            mDrawer?.updateItem(item.withBadge(StringHolder(null)))
        } else {
            val badgeStyle = BadgeStyle(resources.getColor(R.color.md_green_500),
                    resources.getColor(R.color.md_green_700))
            badgeStyle.withTextColor(resources.getColor(android.R.color.white))

            val badgeText = if (model.unreadItems >= 100) "99+" else Integer.toString(model.unreadItems)

            mDrawer?.updateItem(item.withBadge(badgeText).withBadgeStyle(badgeStyle))
        }

        mDrawer?.adapter?.notifyDataSetChanged()
    }

    override fun showRooms(rooms: ArrayList<RoomViewModel>) {
        if (mLoadRoomsProgressView?.visibility == View.VISIBLE) {
            mLoadRoomsProgressView?.visibility = View.GONE
        }

        if (mErrorLoadRoomsView?.visibility == View.VISIBLE) {
            mErrorLoadRoomsView?.visibility == View.GONE
        }

        setItemsDrawer(rooms)
    }

    override fun showProfile(user: UserModel) {
        mMainProfile.withName(user.username)
        mMainProfile.withEmail(user.displayName)
        mMainProfile.withIcon(user.avatarUrlMedium)

        mAccountHeader?.updateProfile(mMainProfile)
    }

    override fun showError(resId: Int) {
        Toast.makeText(applicationContext, resId, Toast.LENGTH_SHORT).show()
    }

    override fun leavedFromRoom() {
        val name = selectedRoom?.name
        closeRoom(true)

        mDrawer?.removeItemByPosition(getPositionRoomInDrawer(name!!))
        mDrawer?.adapter?.notifyAdapterDataSetChanged()

        for (i in mRoomsInDrawer.indices) {
            if (mRoomsInDrawer[i].name == name) {
                mRoomsInDrawer.removeAt(i)
                break
            }
        }

        for (i in mRoomsList.indices) {
            if (mRoomsList[i].name == name) {
                mRoomsList.removeAt(i)
                break
            }
        }


        mDrawer?.setSelectionAtPosition(ROOM_IN_DRAWER_OFFSET_TOP)
    }

    override fun saveAllRooms(rooms: ArrayList<RoomViewModel>) {
        mRoomsList.clear()
        mRoomsList.addAll(rooms)
    }

    private fun showRoomsListDialog() {
        val fragment = PickRoomsDialogFragment()
        fragment.show(supportFragmentManager, mRoomsList, object : PickRoomsDialogFragment.OnPickedRoom {
            override fun result(model: RoomViewModel) {
                pickRoom(model)
            }
        })
    }

    private fun getPositionRoomInDrawer(name: String): Int {
        for (i in mRoomsInDrawer.indices) {
            if (mRoomsInDrawer[i].name == name) {
                return i + ROOM_IN_DRAWER_OFFSET_TOP - 1
            }
        }

        return -1
    }

    private var selectedRoom: RoomViewModel? = null
        get() {
            val index = mDrawer?.currentSelectedPosition!! - ROOM_IN_DRAWER_OFFSET_TOP

            if (index >= 0 && index < mRoomsInDrawer.size) {
                return mRoomsInDrawer[index]
            }

            return null
        }

    private fun pickRoom(model: RoomViewModel) {
        if (getPositionRoomInDrawer(model.name) == -1) {
            // -1 because added "All"
            val newPosition = mDrawer?.drawerItems?.size!! - ROOM_IN_DRAWER_OFFSET_BOTTOM

            mRoomsInDrawer.add(model)
            mDrawer?.addItemAtPosition(formatRoomToDrawerItem(model), newPosition)
            mDrawer?.adapter?.notifyAdapterDataSetChanged()

            mDrawer?.setSelectionAtPosition(newPosition)
        } else {
            val position = getPositionRoomInDrawer(model.name) + 1
            mDrawer?.setSelectionAtPosition(position)
        }
    }

    override fun errorLoadRooms() {
        if (mRoomsList.size == 0) {
            if (mDrawer!!.drawerItems[1] is ProgressDrawerItem) {
                (mDrawer!!.drawerItems[1] as ProgressDrawerItem).mode = ProgressDrawerItem.Mode.NO_LOADING_TEXT
                mDrawer!!.adapter.notifyAdapterItemChanged(1)
            }
        }

        mLoadRoomsProgressView?.visibility = View.GONE
        mErrorLoadRoomsView?.visibility = View.VISIBLE
    }

    override fun getPresenterTag(): String = MainPresenter.TAG

    companion object {
        val BROADCAST_UNAUTHORIZED = "com.ne1c.gitterclient.UnathorizedReceiver"
        val BROADCAST_NEW_MESSAGE = "com.ne1c.gitterclient.NewMessageReceiver"
        val MESSAGE_INTENT_KEY = "message"
        val ROOM_ID_INTENT_KEY = "room"
    }
}