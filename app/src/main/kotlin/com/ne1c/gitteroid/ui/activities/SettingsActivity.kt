package com.ne1c.gitteroid.ui.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import com.ne1c.gitteroid.GitteroidApplication
import com.ne1c.gitteroid.ui.fragments.PreferencesFragment

class SettingsActivity : AppCompatActivity() {
    private val FRAGMENT_CONTAINER_ID = Integer.valueOf(666)!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parent = FrameLayout(applicationContext)
        parent.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)

        setContentView(parent)

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }

        val fragmentContainer = FrameLayout(applicationContext)
        fragmentContainer.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)

        parent.addView(fragmentContainer)
        fragmentContainer.id = FRAGMENT_CONTAINER_ID

        fragmentManager.beginTransaction().replace(fragmentContainer.id, PreferencesFragment()).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        (application as GitteroidApplication).startNotificationService()
        super@SettingsActivity.finish()
    }
}
