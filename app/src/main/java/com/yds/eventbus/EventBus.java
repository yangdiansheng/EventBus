package com.yds.eventbus;

import java.util.List;

public class EventBus {

    private static volatile EventBus defaultInstance;
    private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();
    private final SubscriberMethodFinder subscriberMethodFinder;

    //双检测单例
    public EventBus(){
        this(DEFAULT_BUILDER);
    }

    EventBus(EventBusBuilder builder){
        subscriberMethodFinder = new SubscriberMethodFinder();

    }

    public static EventBus getDefault(){
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
        Class<?> subscriberClass = subscriber.getClass();
        List<SubscriberMethod> subscribeMethods = subscriberMethodFinder.findSubscribeMethod(subscriberClass);

    }

    public void unregister(Object subscriber){

    }
}
