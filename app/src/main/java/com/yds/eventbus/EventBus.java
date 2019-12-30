package com.yds.eventbus;

public class EventBus {

    static volatile EventBus defaultInstance;
    private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();

    //双检测单例
    public EventBus(){
        this(DEFAULT_BUILDER);
    }

    EventBus(EventBusBuilder builder){

    }

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
