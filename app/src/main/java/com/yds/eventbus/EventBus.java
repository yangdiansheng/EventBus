package com.yds.eventbus;

public class EventBus {

    static volatile EventBus defaultInstance;
    //双检测单例
    private EventBus(){}

    public EventBus getDefault(){
        if (defaultInstance == null){
            synchronized (EventBus.class){
                if (defaultInstance == null){
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }

    public void register(Object subscriber){

    }
}
