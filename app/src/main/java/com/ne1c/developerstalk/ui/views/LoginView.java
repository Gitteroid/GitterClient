package com.ne1c.developerstalk.ui.views;

public interface LoginView extends BaseView {
    void showProgress();

    void hideProgress();
    void successAuth();
    void errorAuth(String error);
}
