package com.ne1c.gitteroid.ui.activities

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.di.components.DaggerSearchRoomComponent
import com.ne1c.gitteroid.di.components.SearchRoomComponent
import com.ne1c.gitteroid.presenters.SearchRoomPresenter

import javax.inject.Inject

class SearchRoomActivity : BaseActivity() {
    @Inject
    internal var mPresenter: SearchRoomPresenter? = null

    private var mComponent: SearchRoomComponent? = null

    private var mSearchEditText: EditText? = null
    private var mNoResultLayout: LinearLayout? = null
    private val mResultRecyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_room)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mSearchEditText = findViewById(R.id.search_editText) as EditText
        mNoResultLayout = findViewById(R.id.no_result_layout) as LinearLayout

        findViewById(R.id.clear_imageButton).setOnClickListener { v -> mSearchEditText!!.text.clear() }
    }

    override fun initDiComponent() {
        mComponent = DaggerSearchRoomComponent.builder().applicationComponent(appComponent).build()

        mComponent!!.inject(this)
    }

    override fun onDestroy() {
        mComponent = null

        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }

        return super.onOptionsItemSelected(item)
    }
}
