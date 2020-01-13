package com.yds.eventbus.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.yds.eventbus.EventBus;
import com.yds.eventbus.R;
import com.yds.eventbus.Subscribe;
import com.yds.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().register(MainActivity.this);
            }
        });
        findViewById(R.id.post_in_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post("come from main");
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(sticky = true)
    public void handleMessage(Object o){
        Log.d("yyy","handleMessage");
    }

    @Subscribe(getTreadMode = ThreadMode.ASYNC)
    public void handleMessage1(Object o){
        Log.d("yyy","handleMessage1");
    }

    @Subscribe(priority = 2)
    public void handleMessage2(String o){
        Log.d("yyy","handleMessage2" + o);
    }
}
