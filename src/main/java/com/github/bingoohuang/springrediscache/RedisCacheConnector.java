package com.github.bingoohuang.springrediscache;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

import java.util.concurrent.Callable;

public class RedisCacheConnector {
    static final ThreadLocal<Optional<Object>> THREADLOCAL = new ThreadLocal<Optional<Object>>();
    static final Object CLEARTAG = new Object();

    /**
     * Clear the cache related to callable.
     * First the local cache is cleared.
     * Then check the redis with local, is their values are equal, then the redis cache will be cleared.
     * Other the redis cache will be left.
     * @param callable
     * @param <T>
     */
    public static <T> void clearCache(Callable<T> callable) {
        connectCache(callable, CLEARTAG);
    }

    public static <T> T connectCache(Callable<T> callable, Object cachedValue) {
        THREADLOCAL.set(Optional.fromNullable(cachedValue));
        try {
            return callable.call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            THREADLOCAL.remove();
        }
    }

    /**
     * 刷新老缓存，并取得新值。
     * @param callable
     * @param <T>
     * @return
     */
    public static <T> T refreshCache(Callable<T> callable) {
        clearCache(callable);

        try {
            return callable.call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
