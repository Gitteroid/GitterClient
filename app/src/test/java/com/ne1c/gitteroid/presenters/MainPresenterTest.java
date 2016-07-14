package com.ne1c.gitteroid.presenters;

import com.ne1c.gitteroid.TestRxSchedulerFactory;
import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.ui.views.MainView;
import com.ne1c.gitteroid.utils.Utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import rx.Observable;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Utils.class)
public class MainPresenterTest {
    public static final String ROOM_ID = "room_id";

    @Mock
    private DataManger mDataManger;

    @Mock
    private MainView mView;

    private MainPresenter mPresenter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(Utils.class);
        PowerMockito.when(Utils.getInstance()).thenReturn(mock(Utils.class));

        mPresenter = new MainPresenter(new TestRxSchedulerFactory(), mDataManger);

        mPresenter.bindView(mView);
    }

    @Test
    public void testLeaveFromRoom_success() {
        PowerMockito.when(Utils.getInstance().isNetworkConnected()).thenReturn(true);

        when(mDataManger.leaveFromRoom(ROOM_ID)).thenReturn(Observable.just(true));

        mPresenter.leaveFromRoom(ROOM_ID);

        verify(mView, times(1)).leavedFromRoom();
        verify(mView, never()).showError(anyInt());
    }

    @Test
    public void testLeaveFromRoom_fail() {
        PowerMockito.when(Utils.getInstance().isNetworkConnected()).thenReturn(true);

        when(mDataManger.leaveFromRoom(ROOM_ID)).thenReturn(Observable.error(new Throwable()));

        mPresenter.leaveFromRoom(ROOM_ID);

        verify(mView, never()).leavedFromRoom();
        verify(mView, times(1)).showError(anyInt());
    }

    @Test
    public void testLeaveFromRoom_noNetwork() {
        PowerMockito.when(Utils.getInstance().isNetworkConnected()).thenReturn(false);

        mPresenter.leaveFromRoom(ROOM_ID);

        verify(mView, never()).leavedFromRoom();
        verify(mView, times(1)).showError(anyInt());
    }

    @After
    public void end() {
        mPresenter.unbindView();
        mPresenter.onDestroy();
    }
}
