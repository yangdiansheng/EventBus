package com.yds.eventbus;

import java.lang.reflect.Method;

public class SubscriberMethod {

    final Method method;
    final ThreadMode threadMode;
    final Class<?> eventType;
    final int priority;
    final boolean sticky;
    /** Used for efficient comparison */
    String methodString;

    public SubscriberMethod(Method method, Class<?> eventType, ThreadMode threadMode, int priority, boolean sticky) {
        this.method = method;
        this.threadMode = threadMode;
        this.eventType = eventType;
        this.priority = priority;
        this.sticky = sticky;
    }

    @Override
    public String toString() {
        return "SubscriberMethod{" +
                "method=" + method +
                ", threadMode=" + threadMode +
                ", eventType=" + eventType +
                ", priority=" + priority +
                ", sticky=" + sticky +
                ", methodString='" + methodString + '\'' +
                '}';
    }
}


