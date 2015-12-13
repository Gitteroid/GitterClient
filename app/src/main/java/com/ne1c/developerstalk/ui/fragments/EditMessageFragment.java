package com.ne1c.developerstalk.ui.fragments;


import android.app.DialogFragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.events.UpdateMessageEvent;
import com.ne1c.developerstalk.models.MessageModel;

import de.greenrobot.event.EventBus;

public class EditMessageFragment extends DialogFragment {

    private EditText mEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_edit_message, container, false);
        getDialog().setTitle(getString(R.string.edit_message));

        mEditText = (EditText) v.findViewById(R.id.edit_text);

        final MessageModel model = getArguments().getParcelable("message");

        mEditText.setText(model.text);

         v.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 getDialog().dismiss();
             }
         });

        v.findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(mEditText.getText().toString())) {
                    UpdateMessageEvent message = new UpdateMessageEvent();
                    model.text = mEditText.getText().toString();
                    message.setMessageModel(model);
                    EventBus.getDefault().post(message);

                    getDialog().dismiss();
                } else {
                    Toast.makeText(getActivity(), R.string.message_empty, Toast.LENGTH_SHORT).show();
                }
            }
        });

        return v;
    }
}
