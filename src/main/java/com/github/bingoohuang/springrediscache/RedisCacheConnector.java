package com.github.bingoohuang.springrediscache;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

import java.util.concurrent.Callable;

public class RedisCacheConnector {
    static ThreadLocal<Optional<Object>> threadLocal = new ThreadLocal<Optional<Object>>();

    public static <T> T connectCache(Callable<T> callable, Object cachedValue) {
        threadLocal.set(Optional.fromNullable(cachedValue));
        try {
            return callable.call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            threadLocal.remove();
        }
    }
}
