package com.yds.eventbus.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.yds.eventbus.EventBus;
import com.yds.eventbus.R;
import com.yds.eventbus.Subscribe;
import com.yds.eventbus.ThreadMode;

import java.util.concurrent.Executors;

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
        findViewById(R.id.post_in_thread).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Executors.newSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.getDefault().post("come from thread");
                    }
                });

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

    @Subscribe(getTreadMode = ThreadMode.MAIN)
    public void handleMessage1(String o){
        Log.d("yyy","handleMessage1" + o);
        Log.d("yyy",Thread.currentThread().getName() + "");
    }

    @Subscribe(priority = 2)
    public void handleMessage2(String o){
        Log.d("yyy","handleMessage2" + o);
        Log.d("yyy",Thread.currentThread().getName() + "");
    }

    @Subscribe(getTreadMode = ThreadMode.BACKGROUND)
    public void handleMessage3(String o){
        Log.d("yyy","handleMessage3" + o);
        Log.d("yyy",Thread.currentThread().getName() + "");
    }

    @Subscribe(getTreadMode = ThreadMode.ASYNC)
    public void handleMessage4(String o){
        Log.d("yyy","handleMessage4" + o);
        Log.d("yyy",Thread.currentThread().getName() + "");
    }
}
