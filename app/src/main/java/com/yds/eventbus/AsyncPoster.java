package com.yds.eventbus;

public class AsyncPoster implements Runnable,Poster{
    private final PendingPostQueue queue;
    private final EventBus eventBus;

    AsyncPoster(EventBus eventBus){
        this.eventBus = eventBus;
        queue = new PendingPostQueue();
    }

    public void enqueue(Subscription subscription,Object event){
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription,event);
        queue.enqueue(pendingPost);
        eventBus.getExecutorService().execute(this);
    }

    @Override
    public void run() {
        PendingPost pendingPost = queue.poll();
        if (pendingPost == null){
            throw new IllegalArgumentException("No pending post available");
        }
        eventBus.invokeSubscriber(pendingPost);
    }
}
