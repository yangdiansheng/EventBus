package com.yds.eventbus;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Subscribe {
    //标记观察者调用方法
    //粘性事件
    //事件执行的线程
    boolean sticky() default false;
    ThreadMode getTreadMode() default ThreadMode.POSTING;
    //定义优先级
    int priority() default 0;

}
