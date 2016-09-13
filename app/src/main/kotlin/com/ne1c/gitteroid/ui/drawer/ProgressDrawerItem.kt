package com.ne1c.gitteroid.ui.drawer

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.ne1c.gitteroid.R

class ProgressDrawerItem : IDrawerItem<ProgressDrawerItem, ProgressDrawerItem.ViewHolder> {
    private var tag: Any? = null
    private var identifier: Long = -1
    private var enabled = false
    private var selected = false

    var mode = Mode.PROGRESS

    override fun equals(id: Long): Boolean {
        return this.equals(id)
    }

    override fun getTag(): Any? {
        return tag
    }

    override fun withEnabled(enabled: Boolean): ProgressDrawerItem {
        this.enabled = enabled
        return this
    }

    override fun equals(id: Int): Boolean {
        return this.equals(id)
    }

    override fun withTag(tag: Any?): ProgressDrawerItem {
        this.tag = tag
        return this
    }

    override fun generateView(ctx: Context?, parent: ViewGroup?): View {
        return getViewHolder(parent).itemView
    }

    override fun generateView(ctx: Context?): View {
        return LayoutInflater.from(ctx).inflate(R.layout.item_progress_drawer, null, false)
    }

    override fun withSelectable(selectable: Boolean): ProgressDrawerItem {
        return this
    }

    override fun getLayoutRes(): Int {
        return R.layout.item_progress_drawer
    }

    override fun getType(): Int {
        return hashCode()
    }

    override fun withSetSelected(selected: Boolean): ProgressDrawerItem {
        this.selected = selected
        return this
    }

    override fun getViewHolder(parent: ViewGroup?): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent?.context)
                .inflate(R.layout.item_progress_drawer, null, false))
    }

    override fun bindView(holder: ViewHolder) {
        if (mode == Mode.PROGRESS) {
            holder.progressBar.visibility = View.VISIBLE
            holder.noRoomsTextView.visibility = View.GONE
        } else if (mode == Mode.NO_LOADING_TEXT){
            holder.progressBar.visibility = View.GONE
            holder.noRoomsTextView.visibility = View.VISIBLE
        }
    }

    override fun isSelected(): Boolean {
        return selected
    }

    override fun isSelectable(): Boolean {
        return false
    }

    override fun isEnabled(): Boolean {
        return enabled
    }

    override fun getIdentifier(): Long {
        return identifier
    }

    override fun withIdentifier(identifier: Long): ProgressDrawerItem {
        this.identifier = identifier
        return this
    }

    class ViewHolder : RecyclerView.ViewHolder {
        val progressBar: ProgressBar
        val noRoomsTextView: TextView

        constructor(itemView: View) : super(itemView) {
            progressBar = itemView.findViewById(R.id.progressBar) as ProgressBar
            noRoomsTextView = itemView.findViewById(R.id.no_rooms_textView) as TextView
        }
    }

    enum class Mode {
        PROGRESS, NO_LOADING_TEXT
    }
}