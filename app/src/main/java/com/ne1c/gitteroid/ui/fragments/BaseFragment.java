package com.ne1c.gitteroid.ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.ne1c.gitteroid.Application;
import com.ne1c.gitteroid.di.HasComponent;
import com.ne1c.gitteroid.di.components.ApplicationComponent;

public abstract class BaseFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDiComponent();
    }

    protected abstract void initDiComponent();

    public <T> T getActivityComponent(Class<T> clazz) {
        Activity activity = getActivity();
        HasComponent<T> has = (HasComponent<T>) activity;
        return has.getComponent();
    }

    public ApplicationComponent getAppComponent() {
        return ((Application) getActivity().getApplication()).getComponent();
    }
}
