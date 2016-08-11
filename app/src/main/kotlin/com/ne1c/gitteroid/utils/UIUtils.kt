package com.ne1c.gitteroid.utils

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View

import com.ne1c.gitteroid.R

object UIUtils {
    private val RES_IDS_ACTION_BAR_SIZE = intArrayOf(R.attr.actionBarSize)

    fun calculateActionBarSize(context: Context?): Int {
        if (context == null) {
            return 0
        }

        val curTheme = context.theme ?: return 0

        val att = curTheme.obtainStyledAttributes(RES_IDS_ACTION_BAR_SIZE) ?: return 0

        val size = att.getDimension(0, 0f)
        att.recycle()
        return size.toInt()
    }

    fun setContentTopClearance(rootView: View?, clearance: Int) {
        rootView?.setPadding(rootView.paddingLeft, clearance,
                rootView.paddingRight, rootView.paddingBottom)
    }

    fun convertPxToDp(px: Int, displayMetrics: DisplayMetrics): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px.toFloat(), displayMetrics)
    }
}
