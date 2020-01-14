package com.yds.eventbus;

/**
 * 发送事件在后台线程处理
 */
public class BackgroundPoster implements Poster,Runnable{

    private final PendingPostQueue queue;
    private final EventBus eventBus;
    private volatile boolean executorRunning;

    BackgroundPoster(EventBus eventBus){
        this.eventBus = eventBus;
        queue = new PendingPostQueue();
    }


    @Override
    public void enqueue(Subscription subscription, Object event) {
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this){
            queue.enqueue(pendingPost);
            if (!executorRunning){
                executorRunning = true;
                //获取从外部设置的线程池
                eventBus.getExecutorService().execute(this);
            }
        }
    }

    @Override
    public void run() {
        try {
            try {
                while (true){
                    PendingPost pendingPost = queue.poll(10000);
                    if(pendingPost == null){
                        //在检测一遍
                        synchronized (this){
                            pendingPost = queue.poll();
                            if(pendingPost == null){
                                executorRunning =  false;
                                return;
                            }
                        }
                    }
                    eventBus.invokeSubscriber(pendingPost);
                }
            } catch (InterruptedException e){
                //输出异常日志
            }

        } finally {
            executorRunning = false;
        }

    }

}
