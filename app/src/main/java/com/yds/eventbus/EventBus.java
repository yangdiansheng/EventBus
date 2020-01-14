package com.yds.eventbus;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class EventBus {

    private static volatile EventBus defaultInstance;
    private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();
    private final SubscriberMethodFinder subscriberMethodFinder;
    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    private final Map<Object, List<Class<?>>> typesBySubscriber;
    private final ThreadLocal<PostingThreadState> currentPostingThreadState = new ThreadLocal<PostingThreadState>() {
        @Override
        protected PostingThreadState initialValue() {
            return new PostingThreadState();
        }
    };
    private MainThreadSupport mainThreadSupport;
    private final Poster mainThreadPoster;
    private final BackgroundPoster backgroundPoster;
    private final AsyncPoster asyncPoster;
    private final ExecutorService executorService;

    //双检测单例
    public EventBus(){
        this(DEFAULT_BUILDER);
    }

    EventBus(EventBusBuilder builder){
        subscriberMethodFinder = new SubscriberMethodFinder();
        subscriptionsByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();
        mainThreadSupport = builder.getMainThreadSupport();
        mainThreadPoster = mainThreadSupport != null ? mainThreadSupport.createPoster(this) : null;
        backgroundPoster = new BackgroundPoster(this);
        executorService = builder.executorService;
        asyncPoster = new AsyncPoster(this);
    }

    ExecutorService getExecutorService(){
        return executorService;
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

    public void post(Object event){
        //从ThreadLocal中取发送端数据
        PostingThreadState postingState = currentPostingThreadState.get();
        //取出消息队列
        List<Object> eventQueue = postingState.eventQueue;
        eventQueue.add(event);

        //如果没有在处理消息，那么开始处理消息
        if (!postingState.isPosting){
            //发送消息的线程是否是在主线程
            postingState.isMainThread = isMainThread();
            postingState.isPosting = true;
            //事件是否被取消了
            if (postingState.canceled) {
                throw new EventBusException("Internal error. Abort state was not reset");
            }
            try {
                //开始处理消息
                while (!eventQueue.isEmpty()) {
                    postSingleEvent(eventQueue.remove(0), postingState);
                }
            } finally {
                postingState.isPosting = false;
                postingState.isMainThread = false;
            }
        }

    }

    private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
        Class<?> eventClass = event.getClass();
        boolean subscriptionFound = false;

        //处理继承关系
//        if (eventInheritance) {
//            List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
//            int countTypes = eventTypes.size();
//            for (int h = 0; h < countTypes; h++) {
//                Class<?> clazz = eventTypes.get(h);
//                subscriptionFound |= postSingleEventForEventType(event, postingState, clazz);
//            }
//        } else {
            subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
//        }
//        if (!subscriptionFound) {
//            if (logNoSubscriberMessages) {
//                logger.log(Level.FINE, "No subscribers registered for event " + eventClass);
//            }
//            if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class &&
//                    eventClass != SubscriberExceptionEvent.class) {
//                post(new NoSubscriberEvent(this, event));
//            }
//        }
    }

    private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this) {
            //取出订阅方法
            subscriptions = subscriptionsByEventType.get(eventClass);
        }
        if (subscriptions != null && !subscriptions.isEmpty()) {
            for (Subscription subscription : subscriptions) {
                postingState.event = event;
                postingState.subscription = subscription;
                boolean aborted = false;
                try {
                    //包装成EventBus处理的事件
                    postToSubscription(subscription, event, postingState.isMainThread);
                    aborted = postingState.canceled;
                } finally {
                    postingState.event = null;
                    postingState.subscription = null;
                    postingState.canceled = false;
                }
                if (aborted) {
                    break;
                }
            }
            return true;
        }
        return false;
    }


    private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
        switch (subscription.subscriberMethod.threadMode) {
            case POSTING:
                invokeSubscriber(subscription, event);
                break;
            case MAIN:
                //如果在主线程中直接调用
                if (isMainThread) {
                    invokeSubscriber(subscription, event);
                } else {
                    //如果不在主线程中添加到主线程事件处理队列中
                    mainThreadPoster.enqueue(subscription, event);
                }
                break;
            case MAIN_ORDERED:
                //直接发送到主线程处理队列中
                if (mainThreadPoster != null) {
                    mainThreadPoster.enqueue(subscription, event);
                } else {
                    // temporary: technically not correct as poster not decoupled from subscriber
                    invokeSubscriber(subscription, event);
                }
                break;
            case BACKGROUND:
                //如果在主线程那么么发到后台线程处理，不在主线程直接调用
                if (isMainThread) {
                    backgroundPoster.enqueue(subscription, event);
                } else {
                    invokeSubscriber(subscription, event);
                }
                break;
            case ASYNC:
                asyncPoster.enqueue(subscription, event);
                break;
//            default:
//                throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
        }
    }

    /**
     * Checks if the current thread is running in the main thread.
     * If there is no main thread support (e.g. non-Android), "true" is always returned. In that case MAIN thread
     * subscribers are always called in posting thread, and BACKGROUND subscribers are always called from a background
     * poster.
     */
    private boolean isMainThread() {
        return mainThreadSupport != null ? mainThreadSupport.isMainThread() : true;
    }

    /**
     * Invokes the subscriber if the subscriptions is still active. Skipping subscriptions prevents race conditions
     * between {@link #unregister(Object)} and event delivery. Otherwise the event might be delivered after the
     * subscriber unregistered. This is particularly important for main thread delivery and registrations bound to the
     * live cycle of an Activity or Fragment.
     *
     * 真正的处理注册方法，
     */
    void invokeSubscriber(PendingPost pendingPost) {
        Object event = pendingPost.event;
        Subscription subscription = pendingPost.subscription;
        PendingPost.releasePendingPost(pendingPost);
        //如果还在注册中那么就调用注册方法
        if (subscription.active) {
            invokeSubscriber(subscription, event);
        }
    }

    void invokeSubscriber(Subscription subscription, Object event) {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (InvocationTargetException e) {
            handleSubscriberException(subscription, event, e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }

    private void handleSubscriberException(Subscription subscription, Object event, Throwable cause) {
       //统一处理异常
    }

    //post时对应的数据封装类
    /** For ThreadLocal, much faster to set (and get multiple values). */
    final static class PostingThreadState {
        final List<Object> eventQueue = new ArrayList<>();
        boolean isPosting;
        boolean isMainThread;
        Subscription subscription;
        Object event;
        boolean canceled;
    }

    public void unregister(Object subscriber){

    }
}
