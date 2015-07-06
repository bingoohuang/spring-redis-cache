package com.github.bingoohuang.springrediscache;

import java.lang.reflect.Method;

class StoreValueProcessor implements CacheProcessor {
    private final InvocationRuntime runtime;

    StoreValueProcessor(InvocationRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public Object process() {
        Object value = runtime.getLocalCache();
        if (value != null) {
            tryRefreshAhead(runtime);
            return value;
        }

        try {
            runtime.waitCacheLock();
            value = runtime.getLocalCache();
            if (value != null) return value;

            return forceRefresh(runtime);
        } finally {
            runtime.unlockLocalCache();
        }
    }

    private void tryRefreshAhead(final InvocationRuntime runtime) {
        if (!runtime.isAheadRefreshEnabled()) return;
        if (runtime.isBeforeAheadSeconds()) return;

        if (!runtime.tryLockLocalCache()) return;

        runtime.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    forceRefresh(runtime);
                } finally {
                    runtime.unlockLocalCache();
                }
            }
        });
    }

    private Object forceRefresh(final InvocationRuntime runtime) {
        long ttlSeconds = runtime.redisTtlSeconds();

        if (ttlSeconds > (runtime.isAheadRefreshEnabled() ? Consts.AheadRefreshSeconds : 0)) {
            runtime.loadRedisValueToCache(ttlSeconds);
        }

        if (runtime.getValue() == null) tryRefreshOrReadRedis(runtime);

        return runtime.getValue();
    }

    private void tryRefreshOrReadRedis(InvocationRuntime runtime) {
        boolean lock = false;
        try {
            lock = runtime.tryRedisLock();
            if (lock) {
                invokeMethodAndSaveCache(runtime);
                return;
            }
        } finally {
            runtime.unlockRedis(lock);
        }

        runtime.waitLockReleaseAndReadRedis();
    }

    private void invokeMethodAndSaveCache(InvocationRuntime runtime) {
        Object cachedValue = runtime.invokeMethod();
        if (cachedValue != null) {
            long expirationSeconds = getExpirationSeconds(runtime);

            runtime.setex(expirationSeconds);
            runtime.putLocalCache(expirationSeconds);
        }
    }

    private long getExpirationSeconds(InvocationRuntime runtime) {
        Object value = runtime.getValue();
        if (value == null) return -1;

        long seconds = -1;
        if (value instanceof RedisCacheExpirationAware)
            seconds = ((RedisCacheExpirationAware) value).expirationSeconds();
        if (seconds <= 0) seconds = tryRedisCacheExpirationTag(value);

        if (seconds <= 0) seconds = runtime.expirationSeconds();
        if (seconds > 0) return Math.min(seconds, Consts.DaySeconds);
        return seconds;
    }

    private long tryRedisCacheExpirationTag(Object value) {
        Method method = RedisCacheUtils.findRedisCacheExpirationAwareTagMethod(value.getClass());
        if (method == null) return -1;

        try {
            Number result = (Number) method.invoke(value);
            return result.longValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
