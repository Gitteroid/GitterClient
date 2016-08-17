package com.ne1c.gitteroid.ui.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.view.View
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.ui.fragments.ChatRoomFragment
import java.util.*

class RoomsPagerAdapter(fm: FragmentManager,
                        private val mRoomsList: ArrayList<RoomViewModel>) : FragmentPagerAdapter(fm) {

    override fun isViewFromObject(view: View?, `object`: Any): Boolean {
        return super.isViewFromObject(view, `object`)
    }

    override fun getItem(position: Int): Fragment {
        return ChatRoomFragment.newInstance(mRoomsList[position])
    }

    override fun getPageTitle(position: Int): CharSequence {
        return mRoomsList[position].name
    }

    override fun getItemPosition(`object`: Any?): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getCount(): Int {
        return mRoomsList.size
    }

    override fun getItemId(position: Int): Long {
        return mRoomsList[position].id.hashCode().toLong()
    }
}
