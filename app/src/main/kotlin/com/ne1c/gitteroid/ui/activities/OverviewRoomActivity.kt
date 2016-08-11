package com.ne1c.gitteroid.ui.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.ui.fragments.ChatRoomFragment

class OverviewRoomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overview_room)

        val toolbar = findViewById(R.id.toolbar) as Toolbar

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val roomModel = intent.getParcelableExtra<RoomViewModel>("room")

        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, ChatRoomFragment.newInstance(roomModel, true)).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }
}
