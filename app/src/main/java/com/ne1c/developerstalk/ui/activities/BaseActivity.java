package com.ne1c.developerstalk.ui.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ne1c.developerstalk.Application;
import com.ne1c.developerstalk.di.components.ApplicationComponent;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initDiComponent();
        super.onCreate(savedInstanceState);
    }

    protected abstract void initDiComponent();

    protected ApplicationComponent getAppComponent() {
        return ((Application) getApplication()).getComponent();
    }
}
