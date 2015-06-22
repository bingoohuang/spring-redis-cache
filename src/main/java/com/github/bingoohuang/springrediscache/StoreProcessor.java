package com.github.bingoohuang.springrediscache;

class StoreProcessor implements CacheProcessor {
    private final InvocationRuntime runtime;

    StoreProcessor(InvocationRuntime runtime) {
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
        if (runtime.isBeforeAheadMillis()) return;
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
        long ttl = runtime.redisTtl();
        if (ttl * 1000 > runtime.aheadMillis()) {
            runtime.loadRedisValueToCache(ttl);
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
        long millis = getExpirationMillis(runtime);

        runtime.setex(millis);
        runtime.putLocalCache(millis);
    }

    private long getExpirationMillis(InvocationRuntime runtime) {
        long millis = -1;
        Object value = runtime.getValue();
        if (value instanceof RedisCacheExpirationAware)
            millis = ((RedisCacheExpirationAware) value).expirationMillis();

        if (millis <= 0) millis = runtime.expirationMillis();
        if (millis > 0) return Math.min(millis, Consts.DayMillis);

        throw new RuntimeException("bad usage @CacheEnabled, expirationMillis should be positive "
                + " or return type implements CacheExpirationAware");
    }


}
