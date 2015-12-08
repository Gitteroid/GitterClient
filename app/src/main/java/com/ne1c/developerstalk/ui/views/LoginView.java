package com.ne1c.developerstalk.ui.views;

public interface LoginView extends BaseView {
    void showDialog();
    void dismissDialog();
    void successAuth();
    void errorAuth(String error);
}
