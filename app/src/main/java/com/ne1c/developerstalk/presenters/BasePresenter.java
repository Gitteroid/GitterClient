package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.ui.views.BaseView;

public interface BasePresenter {
    void bindView(BaseView view);
    void unbindView();
}
