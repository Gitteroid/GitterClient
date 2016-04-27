package com.ne1c.gitteroid.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.View;

import com.ne1c.gitteroid.R;

public class UIUtils {
    private static final int[] RES_IDS_ACTION_BAR_SIZE = { R.attr.actionBarSize };

    public static int calculateActionBarSize(Context context) {
        if (context == null) {
            return 0;
        }

        Resources.Theme curTheme = context.getTheme();
        if (curTheme == null) {
            return 0;
        }

        TypedArray att = curTheme.obtainStyledAttributes(RES_IDS_ACTION_BAR_SIZE);
        if (att == null) {
            return 0;
        }

        float size = att.getDimension(0, 0);
        att.recycle();
        return (int) size;
    }

    public static void setContentTopClearance(View rootView, int clearance) {
        if (rootView != null) {
            rootView.setPadding(rootView.getPaddingLeft(), clearance,
                    rootView.getPaddingRight(), rootView.getPaddingBottom());
        }
    }
}
