package com.ne1c.gitteroid.ui.activities;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.ne1c.gitteroid.R;
import com.ne1c.gitteroid.di.components.DaggerSearchRoomComponent;
import com.ne1c.gitteroid.di.components.SearchRoomComponent;
import com.ne1c.gitteroid.presenters.SearchRoomPresenter;

import javax.inject.Inject;

public class SearchRoomActivity extends BaseActivity {
    @Inject
    SearchRoomPresenter mPresenter;

    private SearchRoomComponent mComponent;

    private EditText mSearchEditText;
    private LinearLayout mNoResultLayout;
    private RecyclerView mResultRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_room);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSearchEditText = (EditText) findViewById(R.id.search_editText);
        mNoResultLayout = (LinearLayout) findViewById(R.id.no_result_layout);

        findViewById(R.id.clear_imageButton).setOnClickListener(v -> mSearchEditText.getText().clear());
    }

    @Override
    protected void initDiComponent() {
        mComponent = DaggerSearchRoomComponent.builder()
                .applicationComponent(getAppComponent())
                .build();

        mComponent.inject(this);
    }

    @Override
    protected void onDestroy() {
        mComponent = null;

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
