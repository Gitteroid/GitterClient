package com.ne1c.gitteroid.ui.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.ne1c.gitteroid.R;

public class DialogMarkdownFragment extends DialogFragment {
    public static final int REQUEST_CODE = 111;

    private final String[] namesMarkdown = {"Singline code", "Multiline code", "Bold", "Italic", "Strikethrough", "Quote", "Link", "Image"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dialog_markdown, container, false);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        GridView markdownGrid = (GridView) v.findViewById(R.id.markdown_gridview);
        markdownGrid.setAdapter(new GridAdapter());

        markdownGrid.setOnItemClickListener((parent, view, position, id) -> sendData(position));

        return v;
    }

    private void sendData(int layoutId) {
        getTargetFragment().onActivityResult(getTargetRequestCode(), REQUEST_CODE, new Intent().putExtra("layout_id", layoutId));
        getDialog().dismiss();
    }

    private class GridAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return namesMarkdown.length;
        }

        @Override
        public Object getItem(int position) {
            return namesMarkdown[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.item_grid_markdown, parent, false);
            }

            TextView name = (TextView) convertView.findViewById(R.id.markdown_name_textview);
            name.setText(namesMarkdown[position]);

            return convertView;
        }
    }
}
