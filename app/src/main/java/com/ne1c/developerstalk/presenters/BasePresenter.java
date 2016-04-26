package com.ne1c.developerstalk.presenters;

public abstract class BasePresenter<T> {
    public abstract void bindView(T view);

    public abstract void unbindView();

    public abstract void onCreate();

    public abstract void onDestroy();
}
