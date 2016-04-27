package com.ne1c.gitteroid.ui.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ne1c.gitteroid.Application;
import com.ne1c.gitteroid.di.components.ApplicationComponent;

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
