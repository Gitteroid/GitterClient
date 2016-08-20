package com.ne1c.gitteroid.ui.activities

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.presenters.SearchRoomPresenter
import com.ne1c.gitteroid.ui.adapters.SearchRoomsAdapter
import com.ne1c.gitteroid.ui.views.SearchRoomsView
import com.ne1c.rainbowmvp.base.BaseActivity
import java.util.*

class SearchRoomActivity : BaseActivity<SearchRoomPresenter>(), SearchRoomsView {
    private var mSearchEditText: EditText? = null
    private var mNoResultLayout: LinearLayout? = null
    private var mResultRecyclerView: RecyclerView? = null
    private var mProgressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_room)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mSearchEditText = findViewById(R.id.search_editText) as EditText
        mNoResultLayout = findViewById(R.id.no_result_layout) as LinearLayout
        mResultRecyclerView = findViewById(R.id.result_recyclerView) as RecyclerView
        mProgressBar = findViewById(R.id.progressBar) as ProgressBar

        findViewById(R.id.clear_imageButton).setOnClickListener { mSearchEditText!!.text.clear() }

        mSearchEditText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun beforeTextChanged(query: CharSequence?, p1: Int, p2: Int, p3: Int) {
                mPresenter.searchRooms(query.toString())
            }
        })
    }

    override fun onStart() {
        super.onStart()

        mPresenter.bindView(this)
    }

    override fun onStop() {
        mPresenter.unbindView()

        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun showError(resId: Int) {
        Snackbar.make(findViewById(android.R.id.content), resId, Snackbar.LENGTH_SHORT )
    }

    override fun showDialog() {
        mProgressBar?.visibility = View.VISIBLE
        mNoResultLayout?.visibility = View.GONE
        mResultRecyclerView?.visibility = View.GONE
    }

    override fun dismissDialog() {
        mProgressBar?.visibility = View.GONE
        mNoResultLayout?.visibility = View.GONE
        mResultRecyclerView?.visibility = View.GONE
    }

    override fun errorSearch() {
        mNoResultLayout?.visibility = View.VISIBLE
    }

    override fun resultSearch(rooms: ArrayList<RoomViewModel>) {
        mResultRecyclerView?.visibility = View.VISIBLE
        mResultRecyclerView?.adapter = SearchRoomsAdapter(rooms)
    }

    override fun getPresenterTag(): String = SearchRoomPresenter.TAG
}
