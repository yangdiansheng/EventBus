package com.yds.eventbus;

public enum  ThreadMode {
    POSTING,//在哪个线程被调用就执行在哪个线程
    MAIN,//执行在主线程,同步调用，如果发送的为主线程会阻塞主线程，
    MAIN_ORDER,//执行在主线程，异步调用，事件执行时排序的
    BACK_GORUND,//如果发送线程不是主线程那么执行在发送线程，如果是主线程那么开启新的线程
    ASYNC,//在子线程中执行
}
