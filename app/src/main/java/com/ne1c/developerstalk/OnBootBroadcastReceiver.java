package com.ne1c.developerstalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ne1c.developerstalk.services.NewMessagesService;

public class OnBootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, NewMessagesService.class));
    }
}
