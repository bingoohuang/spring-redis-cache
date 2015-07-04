package com.github.bingoohuang.springrediscache;

import com.google.common.base.Throwables;

import java.util.concurrent.Callable;

public class RedisCacheConnecter {
    static ThreadLocal<Object> threadLocal = new ThreadLocal<Object>();

    public static <T> T connectCache(Callable<T> callable, T cachedValue) {
        threadLocal.set(cachedValue);
        try {
            return callable.call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            threadLocal.remove();
        }
    }
}
