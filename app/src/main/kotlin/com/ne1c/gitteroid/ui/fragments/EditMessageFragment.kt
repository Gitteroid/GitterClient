package com.ne1c.gitteroid.ui.fragments

import android.app.DialogFragment
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.events.UpdateMessageEvent
import com.ne1c.gitteroid.models.view.MessageViewModel
import org.greenrobot.eventbus.EventBus

class EditMessageFragment : DialogFragment() {

    private var mEditText: EditText? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle): View? {
        val v = inflater.inflate(R.layout.fragment_edit_message, container, false)
        dialog.setTitle(getString(R.string.edit_message))

        mEditText = v.findViewById(R.id.edit_text) as EditText

        val model = arguments.getParcelable<MessageViewModel>("message")

        mEditText!!.setText(model!!.text)

        v.findViewById(R.id.cancel_button).setOnClickListener { v1 -> dialog.dismiss() }

        v.findViewById(R.id.ok_button).setOnClickListener { v1 ->
            if (!TextUtils.isEmpty(mEditText!!.text.toString())) {
                val message = UpdateMessageEvent()
                model.text = mEditText!!.text.toString()

                message.messageModel = model

                EventBus.getDefault().post(message)

                dialog.dismiss()
            } else {
                Toast.makeText(activity, R.string.message_empty, Toast.LENGTH_SHORT).show()
            }
        }

        return v
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
