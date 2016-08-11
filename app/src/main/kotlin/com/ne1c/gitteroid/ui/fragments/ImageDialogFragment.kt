package com.ne1c.gitteroid.ui.fragments

import android.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import com.bumptech.glide.Glide

class ImageDialogFragment : DialogFragment() {
    private var mImageView: ImageView? = null
    private val mImageUrl: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle): View? {
        mImageView = ImageView(activity)

        Glide.with(this).load(mImageUrl).into(mImageView!!)

        return mImageView
    }
}
