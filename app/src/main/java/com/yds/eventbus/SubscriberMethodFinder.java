package com.yds.eventbus;

import android.util.Log;

import com.yds.eventbus.meta.SubscriberInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查找观察者类的注册方法
 */
public class SubscriberMethodFinder {
    /*
     * In newer class files, compilers may add methods. Those are called bridge or synthetic methods.
     * EventBus must ignore both. There modifiers are not public but defined in the Java class file format:
     * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.6-200-A.1
     */
    private static final int BRIDGE = 0x40;
    private static final int SYNTHETIC = 0x1000;

    private static final Map<Class<?>,List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final int POOL_SIZE = 4;
    private static final FindState[] FIND_STATE_POOL = new FindState[POOL_SIZE];
    private static final int MODIFIERS_IGNORE = Modifier.ABSTRACT | Modifier.STATIC | BRIDGE | SYNTHETIC;

    List<SubscriberMethod> findSubscribeMethod(Class<?> subscribeClass){
//        List<SubscriberMethod> subscribeMethods = METHOD_CACHE.get(subscribeClass);
        List<SubscriberMethod> subscribeMethods = null;
        if (subscribeMethods != null){
            return subscribeMethods;
        }
        subscribeMethods = findUsingReflection(subscribeClass);
        if (subscribeMethods == null){
            throw new RuntimeException("Subscriber " + subscribeClass
                    + " and its super classes have no public methods with the @Subscribe annotation");
        } else {
            METHOD_CACHE.put(subscribeClass,subscribeMethods);
            return subscribeMethods;
        }
    }

    //在注解中找到订阅的方法
    private List<SubscriberMethod> findUsingReflection(Class<?> subscribeClass) {
        //使用对象池减少对象创建
        FindState findState = prepareFindState();
        findState.initForSubscriber(subscribeClass);
        while (findState.clazz != null){
            findUsingReflectionInSingleClass(findState);
            findState.moveToSuperclass();
        }
        return getMethodsAndRelease(findState);
    }

    private void findUsingReflectionInSingleClass(FindState findState) {
        Method[] methods;
        try{
            methods = findState.clazz.getDeclaredMethods();
        } catch (Exception e){
            methods = findState.clazz.getMethods();
            findState.skipSuperClasses = true;
        }
        for (Method method:methods){
            int modifiers = method.getModifiers();
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0){
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1){
                    Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                    if (subscribeAnnotation != null){
                        Class<?> eventType = parameterTypes[0];
                        if (findState.checkAdd(method,eventType)){
                            ThreadMode threadMode = subscribeAnnotation.getTreadMode();
                            SubscriberMethod subscriberMethod = new SubscriberMethod(method,eventType,threadMode,subscribeAnnotation.priority(),subscribeAnnotation.sticky());
                            findState.subscriberMethods.add(subscriberMethod);
                            Log.i("yyy",subscriberMethod.toString());
                        }
                    }
                }
             }
        }
    }

    private List<SubscriberMethod> getMethodsAndRelease(FindState findState) {
        List<SubscriberMethod> subscriberMethods = new ArrayList<>(findState.subscriberMethods);
        findState.recycle();
        //归还到对象池中
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                if (FIND_STATE_POOL[i] == null) {
                    FIND_STATE_POOL[i] = findState;
                    break;
                }
            }
        }
        return subscriberMethods;
    }

    private FindState prepareFindState(){
        //从对象池中取出对象
        synchronized (FIND_STATE_POOL){
            for (int i = 0; i < POOL_SIZE; i++) {
                FindState state = FIND_STATE_POOL[i];
                if (state != null) {
                    FIND_STATE_POOL[i] = null;
                    return state;
                }
            }
        }
        return new FindState();
    }

    //在注解中找到订阅的方法
    private List<SubscriberMethod> findUsingInfo(Class<?> subscribeClass) {
        return null;
    }

    static class FindState {
        //订阅方法
        final List<SubscriberMethod> subscriberMethods = new ArrayList<>();
        final Map<Class, Object> anyMethodByEventType = new HashMap<>();
        final Map<String, Class> subscriberClassByMethodKey = new HashMap<>();
        final StringBuilder methodKeyBuilder = new StringBuilder(128);

        //订阅事件的class
        Class<?> subscriberClass;
        //订阅事件的class 用于忽略一些继承类
        Class<?> clazz;
        //是否跳过父类
        boolean skipSuperClasses;
        SubscriberInfo subscriberInfo;

        void initForSubscriber(Class<?> subscriberClass) {
            this.subscriberClass = clazz = subscriberClass;
            skipSuperClasses = false;
            subscriberInfo = null;
        }

        void recycle() {
            subscriberMethods.clear();
            anyMethodByEventType.clear();
            subscriberClassByMethodKey.clear();
            methodKeyBuilder.setLength(0);
            subscriberClass = null;
            clazz = null;
            skipSuperClasses = false;
            subscriberInfo = null;
        }

        boolean checkAdd(Method method, Class<?> eventType) {
            // 2 level check: 1st level with event type only (fast), 2nd level with complete signature when required.
            // Usually a subscriber doesn't have methods listening to the same event type.
            Object existing = anyMethodByEventType.put(eventType, method);
            if (existing == null) {
                return true;
            } else {
                if (existing instanceof Method) {
                    if (!checkAddWithMethodSignature((Method) existing, eventType)) {
                        // Paranoia check
                        throw new IllegalStateException();
                    }
                    // Put any non-Method object to "consume" the existing Method
                    anyMethodByEventType.put(eventType, this);
                }
                return checkAddWithMethodSignature(method, eventType);
            }
        }

        private boolean checkAddWithMethodSignature(Method method, Class<?> eventType) {
            methodKeyBuilder.setLength(0);
            methodKeyBuilder.append(method.getName());
            methodKeyBuilder.append('>').append(eventType.getName());

            String methodKey = methodKeyBuilder.toString();
            Class<?> methodClass = method.getDeclaringClass();
            Class<?> methodClassOld = subscriberClassByMethodKey.put(methodKey, methodClass);
            if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)) {
                // Only add if not already found in a sub class
                return true;
            } else {
                // Revert the put, old class is further down the class hierarchy
                subscriberClassByMethodKey.put(methodKey, methodClassOld);
                return false;
            }
        }

        void moveToSuperclass() {
            if (skipSuperClasses) {
                clazz = null;
            } else {
                clazz = clazz.getSuperclass();
                String clazzName = clazz.getName();
                /** Skip system classes, this just degrades performance. */
                if (clazzName.startsWith("java.") || clazzName.startsWith("javax.") || clazzName.startsWith("android.")) {
                    clazz = null;
                }
            }
        }
    }
}
