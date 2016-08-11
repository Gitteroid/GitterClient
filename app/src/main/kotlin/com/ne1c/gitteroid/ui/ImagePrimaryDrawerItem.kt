package com.ne1c.gitteroid.ui


import android.view.View
import android.widget.ImageView

import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.OnPostBindViewListener
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.ne1c.gitteroid.R

class ImagePrimaryDrawerItem : PrimaryDrawerItem() {
    init {
        mOnPostBindViewListener = OnPostBindViewListener { iDrawerItem, view ->
            DrawerImageLoader.getInstance().cancelImage(view.findViewById(R.id.material_drawer_icon) as ImageView)
            DrawerImageLoader.getInstance().imageLoader.set(view.findViewById(R.id.material_drawer_icon) as ImageView,
                    getIcon().uri, null)
            view.findViewById(R.id.material_drawer_icon).visibility = View.VISIBLE
        }
    }
}
