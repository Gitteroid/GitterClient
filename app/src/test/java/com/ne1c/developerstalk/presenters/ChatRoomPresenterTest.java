package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.Application;
import com.ne1c.developerstalk.BuildConfig;
import com.ne1c.developerstalk.MockRxSchedulersFactory;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.services.DataManger;
import com.ne1c.developerstalk.ui.views.ChatView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collection;

import rx.Observable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class,
        application = Application.class,
        sdk = 19)
public class ChatRoomPresenterTest {
    private ChatRoomPresenter presenter;

    @Mock
    ChatView view;

    @Mock
    DataManger dataManger;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        presenter = new ChatRoomPresenter(new MockRxSchedulersFactory(), dataManger);
        presenter.bindView(view);
    }

    @Test
    public void successLoadCachedMessages() {
        ArrayList messages = mock(ArrayList.class);
        String str = anyString();

        when(dataManger.getCachedMessages(str)).thenReturn(Observable.just(messages));

        presenter.loadCachedMessages(str);

        verify(view, times(1)).showMessages(messages);
    }

    @Test
    public void failLoadCachedMessages() {
        String str = anyString();

        when(dataManger.getCachedMessages(str)).thenReturn(Observable.error(new Throwable(str)));

        presenter.loadCachedMessages(str);

        verify(view, times(1)).showError(str);
    }

    @Test
    public void successSendMessage() {
        String str = anyString();

        MessageModel emptyMessage = any(MessageModel.class);

        when(dataManger.sendMessage(str, str)).thenReturn(Observable.just(emptyMessage));

        presenter.sendMessage(str, str);

        verify(view, times(1)).deliveredMessage(emptyMessage);
    }

    @Test
    public void failSendMessage() {
        String str = "";

        when(dataManger.sendMessage(str, str)).thenReturn(Observable.error(new Throwable(str)));

        presenter.sendMessage(str, str);

        verify(view, times(1)).errorDeliveredMessage();
    }

    @After
    public void end() {
        presenter.unbindView();
    }
}
