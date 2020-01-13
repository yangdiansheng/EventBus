package com.yds.eventbus;

/**
 * Posts events.
 * 发送事件的队列
 *
 * @author William Ferguson
 */
public interface Poster {
    /**
     * Enqueue an event to be posted for a particular subscription.
     *
     * @param subscription Subscription which will receive the event.
     * @param event        Event that will be posted to subscribers.
     */
    void enqueue(Subscription subscription, Object event);
}
