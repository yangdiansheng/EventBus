package com.yds.eventbus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    private static volatile EventBus defaultInstance;
    private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();
    private final SubscriberMethodFinder subscriberMethodFinder;
    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    private final Map<Object, List<Class<?>>> typesBySubscriber;

    //双检测单例
    public EventBus(){
        this(DEFAULT_BUILDER);
    }

    EventBus(EventBusBuilder builder){
        subscriberMethodFinder = new SubscriberMethodFinder();
        subscriptionsByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();

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
        //解析观察者的注册的方法
        List<SubscriberMethod> subscribeMethods = subscriberMethodFinder.findSubscribeMethod(subscriberClass);
        synchronized (this){
            for(SubscriberMethod subscriberMethod:subscribeMethods){
                //将观察者和注册的方法关联起来
                subscribe(subscriber,subscriberMethod);
            }
        }

    }

    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
        //订阅方法参数类型
        Class<?> eventType = subscriberMethod.eventType;
        //订阅对象和订阅方法绑定
        Subscription newSubscription = new Subscription(subscriber,subscriberMethod);
        //订阅方法参数和订阅方法对应关系
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionsByEventType.put(eventType, subscriptions);
        } else {
            //如果包含了订阅该参数下的方法，直接抛异常。禁止多次注册
            if (subscriptions.contains(newSubscription)) {
                throw new RuntimeException("Subscriber " + subscriber.getClass() + " already registered to event "
                        + eventType);
            }
        }

        int size = subscriptions.size();
        for (int i = 0; i <= size; i++) {
            //根据优先级添加订阅方法
            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
                subscriptions.add(i, newSubscription);
                break;
            }
        }

        //订阅对象，和订阅方法中参数类型对应关系
        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscribedEvents);
        }
        subscribedEvents.add(eventType);

        if (subscriberMethod.sticky) {
            //处理粘性时间
        }

    }

    public void unregister(Object subscriber){

    }
}
