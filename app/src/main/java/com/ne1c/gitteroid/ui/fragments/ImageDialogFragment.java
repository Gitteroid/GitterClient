package com.ne1c.gitteroid.ui.fragments;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class ImageDialogFragment extends DialogFragment {
    private ImageView mImageView;
    private String mImageUrl;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mImageView = new ImageView(getActivity());

        Glide.with(this).load(mImageUrl).into(mImageView);

        return mImageView;
    }
}
