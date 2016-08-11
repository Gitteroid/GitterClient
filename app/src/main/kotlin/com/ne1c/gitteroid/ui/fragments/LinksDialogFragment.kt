package com.ne1c.gitteroid.ui.fragments

import android.app.DialogFragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.ListView

import com.ne1c.gitteroid.models.data.MessageModel

import java.util.ArrayList

class LinksDialogFragment : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle): View? {
        val v = inflater.inflate(android.R.layout.list_content, container, false)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val list = v.findViewById(android.R.id.list) as ListView

        val links = arguments.getParcelableArrayList<MessageModel.Urls>("links")
        val urls = arrayOfNulls<String>(links!!.size)

        for (i in urls.indices) {
            urls[i] = links[i].url
        }

        val adapter = ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, urls)
        list.adapter = adapter

        list.setOnItemClickListener { parent, view, position, id ->
            activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(urls[position])))
        }

        return v
    }


}
