package com.ne1c.developerstalk.presenters;

import android.content.Context;

import com.ne1c.developerstalk.MockRxSchedulersFactory;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.services.DataManger;
import com.ne1c.developerstalk.ui.views.ChatView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;

import retrofit.client.Response;
import retrofit.mime.TypedInput;
import rx.Observable;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChatRoomPresenterTest {
    private static final String ROOM_ID = "jf9w4j3fmn389f394n";
    private static final String MESSAGE_TEXT = "message";
    private static final String ERROR = "text_with_error";
    @Mock
    ChatView view;
    @Mock
    DataManger dataManger;
    private ChatRoomPresenter presenter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        presenter = new ChatRoomPresenter(new MockRxSchedulersFactory(), dataManger);
        presenter.bindView(view);

        when(view.getAppContext()).thenReturn(mock(Context.class));
    }

    @Test
    public void successLoadCachedMessages() {
        ArrayList<MessageModel> messages = mock(new ArrayList<MessageModel>().getClass());

        when(dataManger.getCachedMessages(anyString())).thenReturn(Observable.just(messages));

        presenter.loadCachedMessages(ROOM_ID);

        verify(view, times(1)).showMessages(messages);
        verify(view, never()).showError(ERROR);
    }

    @Test
    public void failLoadCachedMessages() {
        when(dataManger.getCachedMessages(anyString())).thenReturn(Observable.error(new Throwable(ERROR)));

        presenter.loadCachedMessages(ROOM_ID);

        verify(view, times(1)).showError(ERROR);
    }

    @Test
    public void successSendMessage() {
        MessageModel emptyMessage = mock(MessageModel.class);

        when(dataManger.sendMessage(anyString(), anyString())).thenReturn(Observable.just(emptyMessage));

        presenter.sendMessage(ROOM_ID, MESSAGE_TEXT);

        verify(view, times(1)).deliveredMessage(emptyMessage);
        verify(view, never()).showError(ERROR);
    }

    @Test
    public void failSendMessage() {
        when(dataManger.sendMessage(anyString(), anyString())).thenReturn(Observable.error(new Throwable(ERROR)));

        presenter.sendMessage(ROOM_ID, MESSAGE_TEXT);

        verify(view, times(1)).errorDeliveredMessage();
    }

    @Test
    public void successLoadMessagesBeforeId() {
        ArrayList<MessageModel> messages = mock(new ArrayList<MessageModel>().getClass());

        when(dataManger.getMessagesBeforeId(anyString(), anyInt(), anyString())).thenReturn(Observable.just(messages));

        presenter.loadMessagesBeforeId(ROOM_ID, 100500, ROOM_ID);

        verify(view, times(1)).successLoadBeforeId(messages);
        verify(view, never()).showError(ERROR);
    }

    @Test
    public void failLoadMessagesBeforeId() {
        ArrayList<MessageModel> messages = mock(new ArrayList<MessageModel>().getClass());

        when(dataManger.getMessagesBeforeId(anyString(), anyInt(), anyString())).thenReturn(Observable.error(new Throwable(ERROR)));

        presenter.loadMessagesBeforeId(ROOM_ID, 100500, ROOM_ID);

        verify(view, never()).successLoadBeforeId(messages);
        verify(view, times(1)).showError(ERROR);
    }

    @Test
    public void successLoadMessages() {
        ArrayList<MessageModel> messages = mock(new ArrayList<MessageModel>().getClass());

        when(dataManger.getMessages(anyString(), anyInt())).thenReturn(Observable.just(messages));

        presenter.loadMessages(ROOM_ID, 100500);

        verify(view, times(1)).showMessages(messages);
        verify(view, never()).showError(ERROR);
    }

    @Test
    public void failLoadMessages() {
        ArrayList<MessageModel> messages = mock(new ArrayList<MessageModel>().getClass());

        when(dataManger.getMessages(anyString(), anyInt())).thenReturn(Observable.error(new Throwable(ERROR)));

        presenter.loadMessages(ROOM_ID, 100500);

        verify(view, never()).showMessages(messages);
        verify(view, times(1)).showError(ERROR);
    }

    @Test
    public void successUploadMessages() {
        MessageModel message = mock(MessageModel.class);

        when(dataManger.updateMessage(anyString(), anyString(), anyString())).thenReturn(Observable.just(message));

        presenter.updateMessages(ROOM_ID, ROOM_ID, MESSAGE_TEXT);

        verify(dataManger, times(1)).insertMessageToDb(message, ROOM_ID);
        verify(view, times(1)).successUpdate(message);
        verify(view, never()).showError(ERROR);
    }

    @Test
    public void failUploadMessages() {
        MessageModel message = mock(MessageModel.class);

        when(dataManger.updateMessage(anyString(), anyString(), anyString())).thenReturn(Observable.error(new Throwable(ERROR)));

        presenter.updateMessages(ROOM_ID, ROOM_ID, MESSAGE_TEXT);

        verify(dataManger, never()).insertMessageToDb(message, ROOM_ID);
        verify(view, never()).successUpdate(message);
        verify(view, times(1)).showError(anyString());
    }

    @Test
    public void successMarkMessageAsRead() {
        Response resp = new Response("", 200, "", new ArrayList<>(), mock(TypedInput.class));
        String[] ids = new String[5];

        when(dataManger.readMessages(ROOM_ID, ids)).thenReturn(Observable.just(resp));

        presenter.markMessageAsRead(100500, 100500, ROOM_ID, ids);

        verify(view, times(1)).successRead(100500, 100500, ROOM_ID, 4);
        verify(view, never()).showError(ERROR);
    }

    @Test
    public void failMarkMessageAsRead() {
        String[] ids = new String[5];

        when(dataManger.readMessages(ROOM_ID, ids)).thenReturn(Observable.error(new Throwable(ERROR)));

        presenter.markMessageAsRead(100500, 100500, ROOM_ID, ids);

        verify(view, never()).successRead(100500, 100500, ROOM_ID, 4);
        verify(view, times(1)).showError(ERROR);
    }

    @After
    public void end() {
        presenter.unbindView();
    }
}
