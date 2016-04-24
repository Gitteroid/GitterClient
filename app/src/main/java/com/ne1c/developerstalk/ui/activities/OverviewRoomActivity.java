package com.ne1c.developerstalk.ui.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.models.data.RoomModel;
import com.ne1c.developerstalk.ui.DrawShadowFrameLayout;
import com.ne1c.developerstalk.ui.fragments.ChatRoomFragment;
import com.ne1c.developerstalk.utils.UIUtils;

public class OverviewRoomActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview_room);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RoomModel roomModel = getIntent().getParcelableExtra("room");

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, ChatRoomFragment.newInstance(roomModel))
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        int actionBarSize = UIUtils.calculateActionBarSize(this);
        DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) findViewById(R.id.shadow_layout);
        if (drawShadowFrameLayout != null) {
            drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
        }
        UIUtils.setContentTopClearance(findViewById(R.id.content_layout), actionBarSize);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }
}
