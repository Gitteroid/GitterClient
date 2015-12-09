package com.ne1c.developerstalk.ui.fragments;


import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ne1c.developerstalk.models.MessageModel;

import java.util.ArrayList;

public class LinksDialogFragment extends DialogFragment {


    public LinksDialogFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(android.R.layout.list_content, container, false);

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        ListView list = (ListView) v.findViewById(android.R.id.list);

        ArrayList<MessageModel.Urls> links = getArguments().getParcelableArrayList("links");
        final String[] urls = new String[links.size()];

        for (int i = 0; i < urls.length; i++) {
            urls[i] = links.get(i).url;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, urls);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getActivity().startActivity(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(urls[position])));
            }
        });

        return v;
    }


}
