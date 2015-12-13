package com.ne1c.developerstalk.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, NewMessagesService.class));
    }
}
