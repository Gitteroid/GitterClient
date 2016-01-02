package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.BuildConfig;
import com.ne1c.developerstalk.MockRxSchedulersFactory;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.services.DataManger;
import com.ne1c.developerstalk.ui.views.MainView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import rx.Observable;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class MainPresenterTest {
    @Mock
    MainView view;

    @Mock
    DataManger dataManger;

    private MainPresenter presenter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        presenter = new MainPresenter(new MockRxSchedulersFactory(), dataManger);
        presenter.bindView(view);
    }

    @Test
    public void successLoadRooms() {
        ArrayList<RoomModel> rooms = new ArrayList<>();

        when(dataManger.getRooms()).thenReturn(Observable.just(rooms));

        presenter.loadRooms();

        verify(view, times(1)).showRooms(rooms);
        verify(view, never()).showError(anyString());
    }

    @Test
    public void failLoadRooms() {
        ArrayList<RoomModel> rooms = new ArrayList<>();
        String error = "error_text";

        when(dataManger.getRooms()).thenReturn(Observable.error(new Throwable(error)));

        presenter.loadRooms();

        verify(view, never()).showRooms(rooms);
        verify(view, times(1)).showError(error);
    }

    @After
    public void end() {
        presenter.unbindView();
    }
}
