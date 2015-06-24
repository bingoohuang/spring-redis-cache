package com.github.bingoohuang.springrediscache;

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
        if (ttlSeconds > Consts.AheadRefreshSeconds) {
            runtime.loadRedisValueToCache(ttlSeconds);
        } else {
            tryRefreshOrReadRedis(runtime);
        }

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
        runtime.invokeMethod();
        long expirationSeconds = getExpirationSeconds(runtime);

        runtime.setex(expirationSeconds);
        runtime.putLocalCache(expirationSeconds);
    }

    private long getExpirationSeconds(InvocationRuntime runtime) {
        long seconds = -1;
        Object value = runtime.getValue();
        if (value instanceof RedisCacheExpirationAware)
            seconds = ((RedisCacheExpirationAware) value).expirationSeconds();

        if (seconds <= 0) seconds = runtime.expirationSeconds();
        if (seconds > 0) return Math.min(seconds, Consts.DaySeconds);

        throw new RuntimeException("bad usage @RedisCacheEnabled, expiration should be positive "
                + " or return type implements RedisCacheExpirationAware");
    }


}
