package com.ne1c.gitteroid.ui.fragments


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.TextView

import com.ne1c.gitteroid.R

class DialogMarkdownFragment : DialogFragment() {

    private val namesMarkdown = arrayOf("Singline code", "Multiline code", "Bold", "Italic", "Strikethrough", "Quote", "Link", "Image")

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater!!.inflate(R.layout.fragment_dialog_markdown, container, false)

        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)

        val markdownGrid = v.findViewById(R.id.markdown_gridview) as GridView
        markdownGrid.adapter = GridAdapter()

        markdownGrid.setOnItemClickListener { parent, view, position, id -> sendData(position) }

        return v
    }

    private fun sendData(layoutId: Int) {
        targetFragment.onActivityResult(targetRequestCode, REQUEST_CODE, Intent().putExtra("layout_id", layoutId))
        dialog.dismiss()
    }

    private inner class GridAdapter : BaseAdapter() {

        override fun getCount(): Int {
            return namesMarkdown.size
        }

        override fun getItem(position: Int): Any {
            return namesMarkdown[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = LayoutInflater.from(activity).inflate(R.layout.item_grid_markdown, parent, false)
            }

            val name = convertView!!.findViewById(R.id.markdown_name_textview) as TextView
            name.text = namesMarkdown[position]

            return convertView
        }
    }

    companion object {
        val REQUEST_CODE = 111
    }
}
