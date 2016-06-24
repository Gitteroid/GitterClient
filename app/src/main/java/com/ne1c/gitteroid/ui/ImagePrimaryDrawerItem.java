package com.ne1c.gitteroid.ui;


import android.view.View;
import android.widget.ImageView;

import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.ne1c.gitteroid.R;

public class ImagePrimaryDrawerItem extends PrimaryDrawerItem {
    public ImagePrimaryDrawerItem() {
        super();
        mOnPostBindViewListener = (drawerItem, view) -> {
            ImageView icon1 = (ImageView) view.findViewById(R.id.material_drawer_icon);
            DrawerImageLoader.getInstance().cancelImage(icon1);

            DrawerImageLoader.getInstance().getImageLoader().set(icon1, getIcon().getUri(), null);
            icon1.setVisibility(View.VISIBLE);
        };
    }
}
