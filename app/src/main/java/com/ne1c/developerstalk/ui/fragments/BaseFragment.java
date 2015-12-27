package com.ne1c.developerstalk.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import com.ne1c.developerstalk.Application;
import com.ne1c.developerstalk.di.HasComponent;
import com.ne1c.developerstalk.di.components.ApplicationComponent;

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
